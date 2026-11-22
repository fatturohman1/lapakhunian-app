package com.lapakhunian.app.feature.payments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.OkBg
import com.lapakhunian.app.core.designsystem.OkGreen
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.network.dto.NativePaymentDto
import com.lapakhunian.app.core.network.dto.SubscribeResponseDto
import com.lapakhunian.app.core.util.formatRupiah
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pilihan metode charge native (Midtrans Core API). */
data class ChargeMethodOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val isQr: Boolean = false,
)

private val METHOD_OPTIONS = listOf(
    ChargeMethodOption("va_bca", "BCA Virtual Account", "Transfer / m-banking BCA", "BCA"),
    ChargeMethodOption("va_bni", "BNI Virtual Account", "Transfer / m-banking BNI", "BNI"),
    ChargeMethodOption("va_bri", "BRI Virtual Account", "Transfer / m-banking BRI", "BRI"),
    ChargeMethodOption("va_permata", "Permata Virtual Account", "Transfer / m-banking Permata", "PERMATA"),
    ChargeMethodOption("qris", "QRIS", "Scan dari GoPay, OVO, DANA, ShopeePay, dan m-banking", "QR", isQr = true),
)

/** Tahapan layar: pilih metode → detail tagihan (VA/QRIS). */
enum class Stage { METHOD, DETAILS }

data class NativePaymentUiState(
    val stage: Stage = Stage.METHOD,
    /** true saat membuat tagihan / memeriksa resume — metode dinonaktifkan. */
    val preparing: Boolean = false,
    val payment: NativePaymentDto? = null,
    /** id langganan target polling (subscribe/resume). */
    val subscriptionId: String? = null,
    val paid: Boolean = false,
    val error: String? = null,
    /** URL Snap untuk fallback "bayar via halaman web" → navigate ke PaymentScreen lama. */
    val fallbackUrl: String? = null,
    /** Query tambahan route payment (subscriptionId/listingId/purchaseId) agar polling benar. */
    val fallbackQuery: String = "",
)

@HiltViewModel
class NativePaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dashRepo: DashboardRepository,
    private val propertyRepo: PropertyRepository,
) : ViewModel() {

    private val planId: String? = savedStateHandle.get<String>("planId")?.takeIf { it.isNotBlank() }
    private val voucher: String? = savedStateHandle.get<String>("voucher")?.takeIf { it.isNotBlank() }
    private val listingId: String? = savedStateHandle.get<String>("listingId")?.takeIf { it.isNotBlank() }
    private val purchaseId: String? = savedStateHandle.get<String>("purchaseId")?.takeIf { it.isNotBlank() }
    private val resumeSub: String? = savedStateHandle.get<String>("resumeSub")?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(NativePaymentUiState())
    val state: StateFlow<NativePaymentUiState> = _state.asStateFlow()

    init {
        if (resumeSub != null) resumePending()
    }

    private fun resumePending() {
        _state.update { it.copy(preparing = true, error = null, subscriptionId = resumeSub) }
        viewModelScope.launch {
            dashRepo.checkPendingSubscription().fold(
                onSuccess = { check ->
                    when {
                        check.status == "active" -> _state.update { it.copy(preparing = false, paid = true) }
                        check.payment != null -> {
                            _state.update {
                                it.copy(
                                    preparing = false,
                                    stage = Stage.DETAILS,
                                    payment = check.payment,
                                )
                            }
                            pollSubscribe()
                        }
                        else -> _state.update {
                            it.copy(
                                preparing = false,
                                error = "Tagihan ini dibuat lewat halaman web — lanjutkan dari kartu tagihan di Langganan Agen.",
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(preparing = false, error = e.message ?: "Tidak ada tagihan pending") }
                },
            )
        }
    }

    /** Pilih metode → buat tagihan (Core API) → tampilkan detail VA/QR → mulai polling. */
    fun pickMethod(methodId: String) {
        if (_state.value.preparing) return
        _state.update { it.copy(preparing = true, error = null) }
        viewModelScope.launch {
            // Normalize ke Result<SubscribeResponseDto> — subscribePlan membungkus dalam SubscribeOutcome.
            val result: Result<SubscribeResponseDto>? = when {
                planId != null -> dashRepo.subscribePlan(planId, voucher, methodId).map { it.sub }
                purchaseId != null -> dashRepo.payLeadPurchase(purchaseId, methodId)
                listingId != null -> dashRepo.renewListing(listingId, methodId)
                else -> null
            }
            if (result == null) {
                _state.update { it.copy(preparing = false, error = "Tidak ada tagihan") }
                return@launch
            }
            result.fold(
                onSuccess = { sub ->
                    val payment = sub.payment
                    if (sub.status == "active" && payment == null) {
                        // voucher menutup seluruh biaya / paket gratis → langsung aktif
                        _state.update { it.copy(preparing = false, paid = true) }
                        return@launch
                    }
                    if (payment == null) {
                        _state.update {
                            it.copy(preparing = false, error = "Detail tagihan tidak tersedia. Coba lagi.")
                        }
                        return@launch
                    }
                    _state.update {
                        it.copy(
                            preparing = false,
                            stage = Stage.DETAILS,
                            payment = payment,
                            subscriptionId = sub.subscriptionId ?: it.subscriptionId,
                        )
                    }
                    when {
                        planId != null -> pollSubscribe()
                        purchaseId != null -> pollPurchase()
                        listingId != null -> pollRenew()
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(preparing = false, error = e.message ?: "Gagal membuat tagihan") }
                },
            )
        }
    }

    /** Fallback: buat tagihan Snap tanpa method → buka halaman web (kartu/GoPay). */
    fun payViaWeb() {
        if (_state.value.preparing) return
        _state.update { it.copy(preparing = true, error = null) }
        viewModelScope.launch {
            val result: Result<SubscribeResponseDto>? = when {
                planId != null -> dashRepo.subscribePlan(planId, voucher, null).map { it.sub }
                purchaseId != null -> dashRepo.payLeadPurchase(purchaseId, null)
                listingId != null -> dashRepo.renewListing(listingId, null)
                else -> null
            }
            if (result == null) {
                _state.update { it.copy(preparing = false, error = "Tidak ada tagihan") }
                return@launch
            }
            result.fold(
                onSuccess = { sub ->
                    val url = sub.paymentRedirectUrl
                    if (url.isNullOrBlank()) {
                        _state.update { it.copy(preparing = false, error = ERROR_NO_URL) }
                    } else {
                        // Bawa arg alur asli supaya PaymentScreen (web) polling alur yang sama.
                        val extraQuery = when {
                            planId != null -> sub.subscriptionId
                                ?.let { "&subscriptionId=${android.net.Uri.encode(it)}" }
                                .orEmpty()
                            listingId != null -> "&listingId=${android.net.Uri.encode(listingId)}"
                            purchaseId != null -> "&purchaseId=${android.net.Uri.encode(purchaseId)}"
                            else -> ""
                        }
                        _state.update {
                            it.copy(preparing = false, fallbackUrl = url, fallbackQuery = extraQuery)
                        }
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(preparing = false, error = e.message ?: "Gagal membuat tagihan") }
                },
            )
        }
    }

    /** Satu pengecekan manual (dipakai setelah polling berhenti). */
    fun checkNow() {
        viewModelScope.launch {
            when {
                _state.value.subscriptionId != null -> {
                    dashRepo.agentSubscription().onSuccess { sub ->
                        if (sub?.status == "active" && sub.id == _state.value.subscriptionId) {
                            _state.update { it.copy(paid = true, error = null) }
                        }
                    }
                }
                purchaseId != null -> dashRepo.agentLeads().onSuccess { leads ->
                    val p = leads.purchases.firstOrNull { it.id == purchaseId }
                    if (p?.status == "completed") _state.update { it.copy(paid = true, error = null) }
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

    private fun pollSubscribe() {
        viewModelScope.launch {
            var cycles = 0
            while (isActive && _state.value.paid.not() && cycles < MAX_POLL_CYCLES) {
                delay(POLL_INTERVAL_MS)
                cycles++
                dashRepo.agentSubscription().onSuccess { sub ->
                    if (sub?.status == "active" && sub.id == _state.value.subscriptionId) {
                        _state.update { it.copy(paid = true) }
                    }
                }
            }
            if (_state.value.paid.not()) _state.update { it.copy(error = ERROR_TIMEOUT) }
        }
    }

    private fun pollPurchase() {
        viewModelScope.launch {
            var cycles = 0
            while (isActive && _state.value.paid.not() && cycles < MAX_POLL_CYCLES) {
                delay(POLL_INTERVAL_MS)
                cycles++
                dashRepo.agentLeads().onSuccess { leads ->
                    val p = leads.purchases.firstOrNull { it.id == purchaseId }
                    if (p?.status == "completed") _state.update { it.copy(paid = true) }
                }
            }
            if (_state.value.paid.not()) _state.update { it.copy(error = ERROR_TIMEOUT) }
        }
    }

    private fun pollRenew() {
        viewModelScope.launch {
            var cycles = 0
            while (isActive && _state.value.paid.not() && cycles < MAX_POLL_CYCLES) {
                delay(POLL_INTERVAL_MS)
                cycles++
                // allowCacheFallback = false: hindari cache Room basi → false "Pembayaran Berhasil".
                runCatching { propertyRepo.detail(listingId!!, allowCacheFallback = false) }.onSuccess { result ->
                    result.onSuccess { property ->
                        if (property.status == com.lapakhunian.app.core.domain.model.ListingStatus.APPROVED) {
                            _state.update { it.copy(paid = true) }
                        }
                    }
                }
            }
            if (_state.value.paid.not()) _state.update { it.copy(error = ERROR_TIMEOUT) }
        }
    }

    private companion object {
        const val MAX_POLL_CYCLES = 200
        const val POLL_INTERVAL_MS = 3_000L
        const val ERROR_TIMEOUT = "Waktu tunggu habis — pembayaran belum terkonfirmasi. Ketuk 'Cek Status Sekarang'."
        const val ERROR_NO_URL = "URL pembayaran tidak tersedia dari server"
    }
}

private fun bankLabel(bank: String?): String = when (bank?.lowercase()) {
    "bca" -> "BCA"
    "bni" -> "BNI"
    "bri" -> "BRI"
    "permata" -> "Permata"
    else -> bank?.uppercase() ?: "Bank"
}

@Composable
fun NativePaymentScreen(
    onBack: () -> Unit,
    onFallbackPay: (url: String, extraQuery: String) -> Unit,
    viewModel: NativePaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.paid) {
        if (state.paid) {
            delay(1600)
            onBack()
        }
    }

    LaunchedEffect(state.fallbackUrl) {
        state.fallbackUrl?.let { url ->
            onFallbackPay(url, state.fallbackQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Baris atas: tombol kembali + judul
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 6.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
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
            Spacer(Modifier.size(12.dp))
            Text(
                if (state.stage == Stage.DETAILS) "Tagihan Pembayaran" else "Pilih Cara Bayar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
            )
        }

        when {
            state.paid -> PaidContent()
            state.preparing && state.stage == Stage.METHOD -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 90.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            state.stage == Stage.METHOD -> MethodPickerContent(
                preparing = state.preparing,
                onPick = viewModel::pickMethod,
                onPayViaWeb = viewModel::payViaWeb,
            )
            else -> BillContent(
                payment = state.payment,
                error = state.error,
                onCopy = { label, value ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                    Toast.makeText(context, "Disalin ke papan klip", Toast.LENGTH_SHORT).show()
                },
                onCheckNow = viewModel::checkNow,
            )
        }
    }
}

@Composable
private fun PaidContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(OkBg),
            contentAlignment = Alignment.Center,
        ) {
            PhIcon(AppIcons.CheckCircle, tint = OkGreen, size = 42.dp)
        }
        Text(
            "Pembayaran Berhasil!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W800,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            "Status terkonfirmasi otomatis. Kembali…",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun MethodPickerContent(
    preparing: Boolean,
    onPick: (String) -> Unit,
    onPayViaWeb: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            "Metode VA & QRIS dikonfirmasi langsung di aplikasi. Kartu kredit dan GoPay lewat halaman web.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        )
        METHOD_OPTIONS.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(enabled = !preparing) { onPick(option.id) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(TintGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    if (option.isQr) {
                        PhIcon(AppIcons.QrCode, tint = OkGreen, size = 22.dp)
                    } else {
                        Text(
                            option.badge,
                            color = com.lapakhunian.app.core.designsystem.PrimaryDark,
                            fontSize = if (option.badge.length > 3) 8.sp else 11.sp,
                            fontWeight = FontWeight.W800,
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(option.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.W700)
                    Text(
                        option.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        TextButton(
            onClick = onPayViaWeb,
            enabled = !preparing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        ) {
            Text(
                "Atau bayar via halaman web (kartu / GoPay)",
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray,
                fontWeight = FontWeight.W700,
            )
        }
    }
}

@Composable
private fun BillContent(
    payment: NativePaymentDto?,
    error: String?,
    onCopy: (String, String) -> Unit,
    onCheckNow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        payment?.amount?.let { amount ->
            Text(
                formatRupiah(amount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.W800,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
        }

        if (payment?.kind == "va") {
            VaBillContent(payment, onCopy)
        } else if (payment?.kind == "qris") {
            QrisBillContent(payment)
        } else {
            Text(
                "Detail tagihan tidak tersedia.",
                color = ErrorRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }

        payment?.expiresAt?.let { deadline ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                PhIcon(AppIcons.Clock, tint = MutedGray, size = 14.dp)
                Text(
                    "Bayar sebelum $deadline WIB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        // Penanda halaman hidup: spinner + teks berarti polling status sedang berjalan
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            if (error == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = MutedGray,
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(
                "Status dicek otomatis setiap 3 detik — pembayaran terkonfirmasi, paket langsung aktif.",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                textAlign = TextAlign.Center,
            )
        }

        error?.let {
            Text(
                it,
                color = ErrorRed,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(
                onClick = onCheckNow,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) { Text("Cek Status Sekarang", fontWeight = FontWeight.W700) }
        }
    }
}

@Composable
private fun VaBillContent(payment: NativePaymentDto, onCopy: (String, String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 14.dp)) {
        Text(
            "Nomor Virtual Account ${bankLabel(payment.bank)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W700,
            color = MutedGray,
        )
        Text(
            payment.vaNumber.orEmpty(),
            fontSize = 24.sp,
            fontWeight = FontWeight.W800,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(),
        )
        Button(
            onClick = { onCopy("Virtual Account", payment.vaNumber.orEmpty()) },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        ) {
            PhIcon(AppIcons.Copy, tint = Color.White, size = 16.dp)
            Spacer(Modifier.size(8.dp))
            Text("Salin Nomor VA", fontWeight = FontWeight.W700)
        }
    }
}

@Composable
private fun QrisBillContent(payment: NativePaymentDto) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 14.dp)) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            val url = payment.qrUrl
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = "Kode QRIS",
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
            } else {
                PhIcon(AppIcons.QrCode, tint = MutedGray, size = 40.dp)
            }
        }
        Text(
            "Scan QR dengan aplikasi e-wallet atau m-banking apa pun",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
