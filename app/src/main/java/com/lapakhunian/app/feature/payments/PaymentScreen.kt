package com.lapakhunian.app.feature.payments

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.OkGreen
import com.lapakhunian.app.core.designsystem.OkBg
import com.lapakhunian.app.core.designsystem.PhIcon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val loading: Boolean = true,
    /** URL Midtrans Snap yang dibuka di Custom Tabs */
    val paymentUrl: String? = null,
    val paid: Boolean = false,
    val error: String? = null,
    /** true jika ada flow tagihan (renew / subscribe / purchase) yang bisa dicek manual */
    val canCheckStatus: Boolean = false,
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dashRepo: DashboardRepository,
    private val propertyRepo: PropertyRepository,
) : ViewModel() {

    private val listingId: String? = savedStateHandle.get<String>("listingId")
    private val initialUrl: String? = savedStateHandle.get<String>("paymentUrl")
    private val purchaseId: String? = savedStateHandle.get<String>("purchaseId")
    private val subscriptionId: String? = savedStateHandle.get<String>("subscriptionId")?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(
        PaymentUiState(canCheckStatus = listingId != null || purchaseId != null || initialUrl != null),
    )
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (initialUrl != null) {
                _state.update {
                    it.copy(
                        loading = false,
                        paymentUrl = initialUrl,
                        canCheckStatus = subscriptionId != null,
                        error = if (subscriptionId == null) "Identitas tagihan tidak tersedia. Periksa langganan di dashboard." else null,
                    )
                }
                if (subscriptionId != null) pollSubscribe()
            } else if (purchaseId != null) {
                dashRepo.payLeadPurchase(purchaseId).fold(
                    onSuccess = { sub ->
                        if (sub.paymentRedirectUrl != null) {
                            _state.update { it.copy(loading = false, paymentUrl = sub.paymentRedirectUrl) }
                            pollPurchase(purchaseId)
                        } else {
                            _state.update { it.copy(loading = false, error = ERROR_NO_URL) }
                        }
                    },
                    onFailure = { e -> _state.update { it.copy(loading = false, error = e.message ?: "Gagal membuat tagihan") } },
                )
            } else if (listingId != null) {
                dashRepo.renewListing(listingId).fold(
                    onSuccess = { sub ->
                        if (sub.paymentRedirectUrl != null) {
                            _state.update { it.copy(loading = false, paymentUrl = sub.paymentRedirectUrl) }
                            pollRenew(listingId)
                        } else {
                            _state.update { it.copy(loading = false, error = ERROR_NO_URL) }
                        }
                    },
                    onFailure = { e -> _state.update { it.copy(loading = false, error = e.message ?: "Gagal membuat tagihan") } },
                )
            } else {
                _state.update { it.copy(loading = false, error = "Tidak ada tagihan") }
            }
        }
    }

    /** Satu pengecekan status manual (dipakai setelah polling berhenti). */
    fun checkNow() {
        viewModelScope.launch {
            when {
                initialUrl != null -> checkSubscriptionStatus()
                purchaseId != null -> dashRepo.agentLeads().onSuccess { leads ->
                    val p = leads.purchases.firstOrNull { it.id == purchaseId }
                    if (p?.status == "completed") {
                        _state.update { it.copy(paid = true, error = null) }
                    }
                }
                listingId != null -> runCatching {
                    propertyRepo.detail(listingId, allowCacheFallback = false)
                }.onSuccess { result ->
                    result.onSuccess { property ->
                        if (property.status == com.lapakhunian.app.core.domain.model.ListingStatus.APPROVED) {
                            _state.update { it.copy(paid = true, error = null) }
                        }
                    }
                }
            }
        }
    }

    private fun markTimeout() {
        _state.update { it.copy(loading = false, error = ERROR_TIMEOUT) }
    }

    private fun pollRenew(listingId: String) {
        viewModelScope.launch {
            var cycles = 0
            while (isActive && _state.value.paid.not() && cycles < MAX_POLL_CYCLES) {
                delay(POLL_INTERVAL_MS)
                cycles++
                // allowCacheFallback = false: hindari cache Room basi → false "Pembayaran Berhasil".
                // Kegagalan transient (jaringan) diabaikan, tetap polling.
                runCatching { propertyRepo.detail(listingId, allowCacheFallback = false) }.onSuccess { result ->
                    result.onSuccess { property ->
                        if (property.status == com.lapakhunian.app.core.domain.model.ListingStatus.APPROVED) {
                            _state.update { it.copy(paid = true) }
                        }
                    }
                }
            }
            if (_state.value.paid.not()) markTimeout()
        }
    }

    private suspend fun checkSubscriptionStatus() {
        val expectedId = subscriptionId ?: return
        dashRepo.agentSubscription().onSuccess { subscription ->
            if (subscription?.id == expectedId && subscription.status == "active") {
                _state.update { it.copy(paid = true, error = null) }
            }
        }
    }

    private fun pollSubscribe() {
        viewModelScope.launch {
            var cycles = 0
            while (isActive && _state.value.paid.not() && cycles < MAX_POLL_CYCLES) {
                delay(POLL_INTERVAL_MS)
                cycles++
                checkSubscriptionStatus()
            }
            if (_state.value.paid.not()) markTimeout()
        }
    }

    private fun pollPurchase(purchaseId: String) {
        viewModelScope.launch {
            var cycles = 0
            while (isActive && _state.value.paid.not() && cycles < MAX_POLL_CYCLES) {
                delay(POLL_INTERVAL_MS)
                cycles++
                dashRepo.agentLeads().onSuccess { leads ->
                    val p = leads.purchases.firstOrNull { it.id == purchaseId }
                    if (p?.status == "completed") {
                        _state.update { it.copy(paid = true) }
                    }
                }
            }
            if (_state.value.paid.not()) markTimeout()
        }
    }

    private companion object {
        const val MAX_POLL_CYCLES = 200
        const val POLL_INTERVAL_MS = 3_000L
        const val ERROR_TIMEOUT = "Waktu tunggu habis — pembayaran belum terkonfirmasi. Cek status di dashboard, atau ketuk 'Cek Status Sekarang'."
        const val ERROR_NO_URL = "URL pembayaran tidak tersedia dari server"
    }
}

@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Auto-buka Custom Tabs sekali saja (guard terhadap rekomposisi/rotasi)
    var openedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.paymentUrl) {
        state.paymentUrl?.let { url ->
            if (!openedOnce) {
                openedOnce = true
                openCustomTab(context, url)
            }
        }
    }

    LaunchedEffect(state.paid) {
        if (state.paid) {
            delay(1600)
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Tombol kembali di pojok kiri atas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                PhIcon(AppIcons.Back, contentDescription = "Kembali", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(if (state.paid) OkBg else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                PhIcon(
                    if (state.paid) AppIcons.CheckCircle else AppIcons.Sell,
                    tint = if (state.paid) OkGreen else MaterialTheme.colorScheme.onPrimaryContainer,
                    size = 42.dp,
                )
            }
            Text(
                when {
                    state.paid -> "Pembayaran Berhasil!"
                    state.loading -> "Menyiapkan tagihan…"
                    state.paymentUrl != null -> "Menunggu Pembayaran"
                    else -> "Pembayaran Belum Selesai"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.W800,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                when {
                    state.paid -> "Status terkonfirmasi otomatis. Kembali ke dashboard…"
                    state.loading -> "Membuat tagihan Midtrans Snap"
                    state.paymentUrl != null && state.canCheckStatus -> "Halaman pembayaran dibuka di browser. Setelah bayar, status dicek otomatis setiap 3 detik."
                    state.paymentUrl != null -> "Halaman pembayaran dibuka di browser. Periksa status langganan di dashboard."
                    else -> "Status pembayaran dapat dicek ulang secara manual."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )

            state.error?.let {
                Text(it, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
            }

            if (state.error != null && !state.paid && state.canCheckStatus) {
                Button(
                    onClick = { viewModel.checkNow() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                ) { Text("Cek Status Sekarang", fontWeight = FontWeight.W700) }
            }

            if (!state.paid && !state.loading && state.paymentUrl != null) {
                Button(
                    onClick = { state.paymentUrl?.let { openCustomTab(context, it) } },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                ) { Text("Buka Halaman Bayar Lagi", fontWeight = FontWeight.W700) }
            }

            Text(
                "Midtrans Snap · VA / e-wallet / kartu",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

private fun openCustomTab(context: Context, url: String) {
    runCatching {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        intent.launchUrl(context, Uri.parse(url))
    }
}
