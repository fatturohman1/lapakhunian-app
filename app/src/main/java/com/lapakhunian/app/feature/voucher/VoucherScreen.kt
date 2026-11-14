package com.lapakhunian.app.feature.voucher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.designsystem.components.EmptyState
import com.lapakhunian.app.core.network.dto.VoucherDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoucherUiState(
    val loading: Boolean = true,
    val needsLogin: Boolean = false,
    val items: List<VoucherDto> = emptyList(),
    val claiming: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class VoucherViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(VoucherUiState())
    val state: StateFlow<VoucherUiState> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    suspend fun load() {
        if (session.token() == null) {
            _state.update { it.copy(loading = false, needsLogin = true) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.activeVouchers().fold(
                onSuccess = { items -> _state.update { it.copy(loading = false, items = items) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message) } },
            )
        }
    }

    fun claim(code: String) {
        val normalized = code.trim().uppercase()
        if (normalized.isEmpty() || _state.value.claiming) return
        _state.update { it.copy(claiming = true, error = null, message = null) }
        viewModelScope.launch {
            repo.claimVoucher(normalized).fold(
                onSuccess = { res ->
                    _state.update {
                        it.copy(
                            claiming = false,
                            message = res.message ?: "Voucher berhasil diklaim!",
                            error = null,
                        )
                    }
                    load()
                },
                onFailure = { e -> _state.update { it.copy(claiming = false, error = e.message) } },
            )
        }
    }
}

@Composable
fun VoucherScreen(
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
    viewModel: VoucherViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var code by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            Text("Voucher", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800, modifier = Modifier.padding(start = 6.dp))
        }

        when {
            state.needsLogin -> EmptyState(
                title = "Masuk dulu ya",
                message = "Klaim voucher tersedia untuk akun terdaftar.",
                icon = AppIcons.Ticket,
                actionLabel = "Masuk",
                onAction = onGoLogin,
            )
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Memuat…", color = MutedGray)
            }
            else -> {
                // Hasil klaim / error
                state.message?.let {
                    Banner(text = it, bg = Color(0xFFDCFCE7), fg = Color(0xFF15803D), icon = AppIcons.CheckCircle)
                }
                state.error?.let {
                    Banner(text = it, bg = Color(0xFFFEE2E2), fg = ErrorRed, icon = AppIcons.Info)
                }

                // Klaim kode
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(18.dp),
                ) {
                    Text("Punya kode voucher?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W800)
                    Text(
                        "Masukkan kode untuk menambah saldo lead atau slot properti.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.uppercase().take(20) },
                            placeholder = { Text("KODEVOUCHER", color = MutedGray) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                viewModel.claim(code)
                                code = ""
                            },
                            enabled = !state.claiming && code.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(start = 10.dp),
                        ) {
                            Text(if (state.claiming) "…" else "Klaim", fontWeight = FontWeight.W700)
                        }
                    }
                }

                // Voucher aktif (bisa dipakai saat checkout langganan)
                Text(
                    "Voucher Aktif",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W800,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
                if (state.items.isEmpty()) {
                    Text(
                        "Belum ada voucher aktif. Pantau terus halaman ini untuk promo berikutnya.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                } else {
                    state.items.forEach { voucher ->
                        VoucherItem(voucher, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                    }
                }
            }
        }
        Box(Modifier.size(20.dp))
    }
}

@Composable
private fun Banner(text: String, bg: Color, fg: Color, icon: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhIcon(icon, tint = fg, size = 16.dp)
        Text(
            text,
            color = fg,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.W600,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun VoucherItem(voucher: VoucherDto, modifier: Modifier = Modifier) {
    // label & ikon per jenis voucher dari server (BONUS_LEADS / BONUS_PROPERTIES / diskon checkout)
    val (label, icon) = when (voucher.type) {
        "BONUS_LEADS" -> "Bonus Saldo Lead" to AppIcons.Wallet
        "BONUS_PROPERTIES" -> "Slot Properti Bonus" to AppIcons.CategoryHouse
        else -> "Diskon Checkout" to AppIcons.Sell
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhIcon(
            icon,
            tint = PrimaryDark,
            size = 22.dp,
            modifier = Modifier
                .size(44.dp)
                .background(TintGreen, RoundedCornerShape(14.dp)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(voucher.title ?: "Voucher", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W800)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MutedGray, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
