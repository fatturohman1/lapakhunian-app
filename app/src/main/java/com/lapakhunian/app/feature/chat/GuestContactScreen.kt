package com.lapakhunian.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.util.ApiConfig
import com.lapakhunian.app.core.util.formatRupiah
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuestContactUiState(
    val loadingProperty: Boolean = true,
    val property: Property? = null,
    val sending: Boolean = false,
    val done: Boolean = false,
    val sentPhone: String = "",
    val error: String? = null,
)

/**
 * Kontak guest TANPA login — nama + nomor WhatsApp (endpoint publik web).
 * Lead tersimpan untuk agen/pemilik; balasan dilakukan lewat WhatsApp.
 */
@HiltViewModel
class GuestContactViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PropertyRepository,
) : ViewModel() {

    val propertyId: String = savedStateHandle.get<String>("propertyId").orEmpty()

    private val _state = MutableStateFlow(GuestContactUiState())
    val state: StateFlow<GuestContactUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // konteks properti untuk kartu atas; offline → cache dao otomatis di repository
            val property = repo.detail(propertyId).getOrNull()
            _state.update { it.copy(loadingProperty = false, property = property) }
        }
    }

    fun send(name: String, phone: String, message: String) {
        val trimmedName = name.trim()
        val digits = phone.filter { it.isDigit() }
        when {
            trimmedName.length < 2 -> {
                _state.update { it.copy(error = "Nama minimal 2 karakter") }
                return
            }
            digits.length < 9 -> {
                _state.update { it.copy(error = "Nomor WhatsApp tidak valid") }
                return
            }
        }
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            repo.guestContact(propertyId, trimmedName, phone.trim(), message.trim()).fold(
                onSuccess = { _state.update { it.copy(sending = false, done = true, sentPhone = phone.trim()) } },
                onFailure = { e -> _state.update { it.copy(sending = false, error = e.message ?: "Gagal mengirim") } },
            )
        }
    }
}

@Composable
fun GuestContactScreen(
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
    viewModel: GuestContactViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Halo, saya tertarik dengan properti ini. Apakah masih tersedia?") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            Text("Hubungi Agen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800, modifier = Modifier.padding(start = 6.dp))
        }

        when {
            // ===== Sukses =====
            state.done -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(TintGreen),
                        contentAlignment = Alignment.Center,
                    ) {
                        PhIcon(AppIcons.CheckCircle, tint = Color(0xFF15803D), size = 40.dp)
                    }
                    Text(
                        "Pesan Terkirim!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W800,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        "Agen/pemilik akan menghubungi Anda via WhatsApp ke ${state.sentPhone}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    // Opsional: login untuk riwayat & chat lanjutan — lead sudah terkirim
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Ingin chat lanjutan & riwayat tersimpan?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.W800,
                        )
                        OutlinedButton(
                            onClick = onGoLogin,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                        ) {
                            PhIcon(AppIcons.Person, tint = PrimaryDark, size = 16.dp)
                            Text("Masuk dengan Akun", fontWeight = FontWeight.W700, modifier = Modifier.padding(start = 6.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedGray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                    ) { Text("Kembali ke Properti", fontWeight = FontWeight.W700) }
                }
            }

            // ===== Form =====
            else -> {
                // Kartu properti yang ditanyakan — konteks inquiry
                state.property?.let { property ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(onClick = onBack)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF6EE7B7), Color(0xFF10B981)))),
                        ) {
                            AsyncImage(
                                model = ApiConfig.absoluteUrl(property.images.firstOrNull()),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            Text(
                                property.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.W800,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                listOfNotNull(property.city, property.purpose?.let { if (it == "dijual" || it == "jual") "Dijual" else "Disewa" })
                                    .joinToString(" · ") + if (property.price > 0) " · ${formatRupiah(property.price)}" else "",
                                fontSize = 11.5.sp,
                                color = MutedGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        PhIcon(AppIcons.Chevron, contentDescription = null, tint = Color(0xFFC3CAD6), modifier = Modifier.size(18.dp))
                    }
                }

                // Banner: kenapa tanpa login (mengadopsi copy web)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TintGreen)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PhIcon(AppIcons.Info, tint = PrimaryDark, size = 16.dp)
                    Text(
                        "Tanpa login — cukup nama & WhatsApp, agen/pemilik akan menghubungi Anda via WhatsApp.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.W600,
                        color = PrimaryDark,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                // Nama
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("Nama Lengkap", fontWeight = FontWeight.W700, fontSize = 13.sp)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(80) },
                        placeholder = { Text("cth: Budi Santoso", color = MutedGray) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }

                // Nomor WhatsApp
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
                    Text("Nomor WhatsApp", fontWeight = FontWeight.W700, fontSize = 13.sp)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(15) },
                        placeholder = { Text("cth: 081234567890", color = MutedGray) },
                        supportingText = { Text("Format bebas — 08xx atau 62xx", color = MutedGray, fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }

                // Pesan opsional
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("Pesan (opsional)", fontWeight = FontWeight.W700, fontSize = 13.sp)
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it.take(1000) },
                        placeholder = { Text("Tanya harga, kondisi, jadwal survey…", color = MutedGray) },
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }

                state.error?.let {
                    Text(
                        it,
                        color = ErrorRed,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }

                Button(
                    onClick = { viewModel.send(name, phone, message) },
                    enabled = !state.sending && name.isNotBlank() && phone.isNotBlank(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                ) {
                    if (state.sending) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Text("Mengirim…", fontWeight = FontWeight.W700, modifier = Modifier.padding(start = 8.dp))
                    } else {
                        Text("Kirim ke Agen", fontWeight = FontWeight.W700)
                    }
                }
            }
        }
    }
}
