package com.lapakhunian.app.feature.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.designsystem.components.InitialAvatar
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.network.PropertyApi
import com.lapakhunian.app.core.network.dto.UserDto
import com.lapakhunian.app.core.util.ApiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val loading: Boolean = true,
    val name: String = "",
    /** Path "/uploads/..." dari server (prefill dari profil, atau hasil upload baru). */
    val imageUrl: String? = null,
    val imageChanged: Boolean = false,
    val saving: Boolean = false,
    val uploading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val api: PropertyApi,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = session.currentUser()
            _state.update {
                it.copy(
                    loading = false,
                    name = user?.name.orEmpty(),
                    imageUrl = user?.image,
                )
            }
        }
    }

    fun update(transform: (EditProfileUiState) -> EditProfileUiState) {
        _state.update(transform)
    }

    fun uploadAvatar(context: android.content.Context, uri: android.net.Uri) {
        _state.update { it.copy(uploading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw Exception("Gagal membaca file")
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val name = "avatar-${System.currentTimeMillis()}.jpg"
                val res = api.uploadImage(bytes, name, mime)
                if (!res.success) throw Exception(res.error ?: "Upload gagal")
                _state.update { it.copy(uploading = false, imageUrl = res.url, imageChanged = true) }
            } catch (e: Exception) {
                _state.update { it.copy(uploading = false, error = e.message ?: "Gagal mengunggah") }
            }
        }
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "Nama tidak boleh kosong") }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, Any?>("name" to s.name.trim())
                if (s.imageChanged && s.imageUrl != null) body["image"] = s.imageUrl
                val res = api.updateProfile(body)
                if (!res.success) throw Exception(res.error ?: "Gagal menyimpan")
                // PATCH mengembalikan {name,email,image} saja — pertahankan id/role lama
                val prev = session.currentUser()
                val merged = (res.data ?: UserDto()).copy(id = prev?.id, role = prev?.role)
                session.updateUser(merged)
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = e.message ?: "Gagal menyimpan") }
            }
        }
    }
}

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.uploadAvatar(context, uri)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            Toast.makeText(context, "Profil tersimpan", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            Text(
                "Edit Profil",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !state.uploading) {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (!state.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ApiConfig.absoluteUrl(state.imageUrl),
                        contentDescription = "Foto profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    InitialAvatar(initials = state.name.take(2).ifBlank { "U" }, size = 96)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .background(PrimaryDark, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PhIcon(AppIcons.Edit, tint = Color.White, size = 15.dp)
                }
            }
            Text(
                if (state.uploading) "Mengunggah…" else "Ketuk foto untuk ganti",
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray,
                modifier = Modifier.padding(top = 10.dp),
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = { Text("Nama Lengkap") },
                singleLine = true,
                enabled = !state.loading && !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                shape = RoundedCornerShape(14.dp),
            )

            state.error?.let {
                Text(it, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
            }

            Button(
                onClick = { viewModel.save() },
                enabled = !state.saving && !state.uploading && !state.loading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 32.dp),
            ) {
                Text(if (state.saving) "Menyimpan…" else "Simpan", fontWeight = FontWeight.W700)
            }
        }
    }
}
