package com.lapakhunian.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.data.repository.ApiException
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import android.content.Context
import android.net.Uri
import android.widget.Toast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TitipJualUiState(
    val name: String = "",
    val phone: String = "",
    val propertyTitle: String = "",
    val address: String = "",
    val estimatedPrice: String = "",
    val photos: List<Uri> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

/** Form konsinyasi publik — tanpa login (rate limit server 5 kirim/15 mnt/IP). */
@HiltViewModel
class TitipJualViewModel @Inject constructor(
    private val repo: PropertyRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(TitipJualUiState())
    val state: StateFlow<TitipJualUiState> = _state.asStateFlow()

    fun update(transform: (TitipJualUiState) -> TitipJualUiState) = _state.update(transform)

    /** Tambah/hapus foto dari pilihan — maks 3. */
    fun togglePhoto(uri: Uri) {
        _state.update {
            when {
                uri in it.photos -> it.copy(photos = it.photos - uri)
                it.photos.size >= 3 -> it.copy(error = "Maksimal 3 foto")
                else -> it.copy(photos = it.photos + uri, error = null)
            }
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.loading) return
        if (s.name.isBlank() || s.phone.isBlank() || s.propertyTitle.isBlank() ||
            s.address.isBlank() || s.estimatedPrice.isBlank()
        ) {
            _state.update { it.copy(error = "Semua field wajib diisi") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // Upload foto satu per satu — gagal satu → hentikan dengan pesan
            val urls = mutableListOf<String>()
            for (uri in s.photos) {
                val bytes = runCatching {
                    appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
                if (bytes == null) continue
                val mime = appContext.contentResolver.getType(uri) ?: "image/jpeg"
                val filename = "titip-${System.currentTimeMillis()}-${urls.size}.jpg"
                repo.titipJualUploadPhoto(bytes, filename, mime).fold(
                    onSuccess = { urls.add(it) },
                    onFailure = { e ->
                        _state.update {
                            it.copy(
                                loading = false,
                                error = (e as? ApiException)?.message ?: "Upload foto gagal. Coba lagi.",
                            )
                        }
                        return@launch
                    },
                )
            }
            repo.submitTitipJual(
                s.name.trim(),
                s.phone.trim(),
                s.propertyTitle.trim(),
                s.address.trim(),
                s.estimatedPrice.trim(),
                urls,
            ).fold(
                onSuccess = {
                    _state.update { it.copy(loading = false) }
                    onSuccess()
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = (e as? ApiException)?.message
                                ?: "Tidak bisa terhubung ke server. Periksa koneksi internet kamu.",
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun TitipJualScreen(
    onBack: () -> Unit,
    viewModel: TitipJualViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Photo Picker sistem — tanpa permission storage (Android 13+; fallback lama otomatis)
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(3),
    ) { uris ->
        uris.forEach { viewModel.togglePhoto(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Box(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackIconButton(onClick = onBack)
            Text(
                "Titip Jual",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            "Punya properti mau dijual? Kirim datanya — tim kami akan menghubungimu untuk survei & penawaran. Tanpa akun.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
            modifier = Modifier.padding(top = 10.dp),
        )
        Box(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = { v -> viewModel.update { it.copy(name = v, error = null) } },
            label = { Text("Nama kamu") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.phone,
            onValueChange = { v -> viewModel.update { it.copy(phone = v, error = null) } },
            label = { Text("Nomor WhatsApp") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        )
        OutlinedTextField(
            value = state.propertyTitle,
            onValueChange = { v -> viewModel.update { it.copy(propertyTitle = v, error = null) } },
            label = { Text("Judul properti (cth: Rumah 2 lantai Bogor)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        )
        OutlinedTextField(
            value = state.address,
            onValueChange = { v -> viewModel.update { it.copy(address = v, error = null) } },
            label = { Text("Alamat lengkap") },
            shape = RoundedCornerShape(14.dp),
            minLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        )
        OutlinedTextField(
            value = state.estimatedPrice,
            onValueChange = { v -> viewModel.update { it.copy(estimatedPrice = v, error = null) } },
            label = { Text("Perkiraan harga (cth: 850000000)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        )

        // Foto properti (maks 3) — membantu admin menilai sebelum menghubungi
        Text(
            "Foto properti (maks 3, opsional)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.W700,
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        ) {
            state.photos.forEach { uri ->
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .clickable { viewModel.togglePhoto(uri) },
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Foto properti — tap untuk hapus",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(86.dp),
                    )
                }
            }
            if (state.photos.size < 3) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .background(TintGreen, RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp),
                        )
                        .clickable {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PhIcon(AppIcons.Camera, tint = PrimaryDark, size = 22.dp)
                        Text(
                            "Tambah",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryDark,
                            fontWeight = FontWeight.W700,
                        )
                    }
                }
            }
        }

        state.error?.let { message ->
            Text(
                message,
                color = ErrorRed,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = {
                viewModel.submit {
                    Toast.makeText(
                        context,
                        "Terkirim! Tim kami akan menghubungimu segera 🙌",
                        Toast.LENGTH_LONG,
                    ).show()
                    onBack()
                }
            },
            enabled = !state.loading,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 32.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Kirim", fontWeight = FontWeight.W700)
            }
        }
    }
}
