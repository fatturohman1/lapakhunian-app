package com.lapakhunian.app.feature.dashboard

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.designsystem.components.InitialAvatar
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.network.dto.AgentDto
import com.lapakhunian.app.core.util.ApiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentProfileEditUiState(
    val loading: Boolean = true,
    val agent: AgentDto? = null,
    val prefilled: Boolean = false,
    val bio: String = "",
    val phone: String = "",
    val company: String = "",
    val licenseNumber: String = "",
    /** Path "/uploads/..." server: prefill profil atau hasil upload baru. */
    val imageUrl: String? = null,
    val bannerUrl: String? = null,
    val imageChanged: Boolean = false,
    val bannerChanged: Boolean = false,
    val uploading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

/** Edit profil agen — field yang juga diedit di web (bio, phone, company, license, image, banner). */
@HiltViewModel
class AgentProfileEditViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val api: com.lapakhunian.app.core.network.PropertyApi,
) : ViewModel() {

    private val _state = MutableStateFlow(AgentProfileEditUiState())
    val state: StateFlow<AgentProfileEditUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            repo.agentProfile().fold(
                onSuccess = { agent ->
                    _state.update { it.copy(loading = false, agent = agent, error = null) }
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Gagal memuat profil") }
                },
            )
        }
    }

    fun retry() {
        _state.update { it.copy(loading = true, error = null) }
        load()
    }

    fun update(transform: (AgentProfileEditUiState) -> AgentProfileEditUiState) {
        _state.update(transform)
    }

    fun uploadImage(context: android.content.Context, uri: android.net.Uri, isBanner: Boolean) {
        _state.update { it.copy(uploading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw Exception("Gagal membaca file")
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val name = "${if (isBanner) "banner" else "agent"}-${System.currentTimeMillis()}.jpg"
                val res = api.uploadImage(bytes, name, mime)
                if (!res.success) throw Exception(res.error ?: "Upload gagal")
                if (isBanner) {
                    _state.update { it.copy(uploading = false, bannerUrl = res.url, bannerChanged = true) }
                } else {
                    _state.update { it.copy(uploading = false, imageUrl = res.url, imageChanged = true) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(uploading = false, error = e.message ?: "Gagal mengunggah") }
            }
        }
    }

    fun save() {
        val s = _state.value
        if (s.saving || s.loading) return
        if (s.agent == null) {
            // Profil asal belum terload — diff field tidak valid, jangan simpan buta
            _state.update { it.copy(saving = false, error = "Profil belum termuat — coba lagi") }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val original = s.agent
                val body = mutableMapOf<String, Any?>()
                // Kirim hanya field yang berubah — PATCH server whitelist bio/phone/company/license/image/banner
                if (s.bio.trim() != original?.bio.orEmpty()) body["bio"] = s.bio.trim()
                if (s.phone.trim() != original?.phone.orEmpty()) body["phone"] = s.phone.trim()
                if (s.company.trim() != original?.company.orEmpty()) body["company"] = s.company.trim()
                if (s.licenseNumber.trim() != original?.licenseNumber.orEmpty()) {
                    body["licenseNumber"] = s.licenseNumber.trim()
                }
                if (s.imageChanged && s.imageUrl != null) body["image"] = s.imageUrl
                if (s.bannerChanged && s.bannerUrl != null) body["banner"] = s.bannerUrl
                if (body.isEmpty()) {
                    _state.update { it.copy(saving = false, saved = true) }
                    return@launch
                }
                repo.updateAgentProfile(body).getOrThrow()
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = e.message ?: "Gagal menyimpan") }
            }
        }
    }
}

@Composable
fun AgentProfileEditScreen(
    onBack: () -> Unit,
    viewModel: AgentProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var pendingBanner by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.uploadImage(context, uri, isBanner = pendingBanner)
    }

    LaunchedEffect(state.loading, state.error) {
        val agent = state.agent
        if (agent != null && state.bio.isEmpty() && state.phone.isEmpty() && !state.prefilled) {
            viewModel.update {
                it.copy(
                    bio = agent.bio.orEmpty(),
                    phone = agent.phone.orEmpty(),
                    company = agent.company.orEmpty(),
                    licenseNumber = agent.licenseNumber.orEmpty(),
                    imageUrl = agent.image?.takeIf { img -> img.isNotBlank() },
                    bannerUrl = agent.banner?.takeIf { b -> b.isNotBlank() },
                    prefilled = true,
                )
            }
        }
    }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            Toast.makeText(context, "Profil agen tersimpan", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            Text(
                "Edit Profil Agen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        if (!state.loading && state.agent == null) {
            // Gagal memuat: error + coba lagi, jangan tampilkan form kosong yang menyesatkan
            Column(Modifier.padding(24.dp)) {
                Text(
                    state.error ?: "Gagal memuat profil",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = { viewModel.retry() },
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(50),
                ) { Text("Coba lagi") }
            }
        }
        if (state.agent != null) {
        // Banner — ketuk untuk ganti (fallback gradient hijau brand)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF0C8A44))))
                .clickable(enabled = !state.uploading && !state.saving) {
                    pendingBanner = true
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (!state.bannerUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ApiConfig.absoluteUrl(state.bannerUrl),
                    contentDescription = "Banner profil agen",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(30.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                PhIcon(AppIcons.Camera, tint = PrimaryDark, size = 16.dp, contentDescription = "Ganti banner")
            }
        }
        Text(
            "Ketuk banner untuk ganti",
            style = MaterialTheme.typography.labelMedium,
            color = MutedGray,
            modifier = Modifier.padding(top = 8.dp, start = 20.dp),
        )

        // Avatar overlap banner — ketuk untuk ganti
        Box(
            modifier = Modifier
                .offset(y = (-26).dp)
                .padding(horizontal = 20.dp),
        ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !state.uploading && !state.saving) {
                        pendingBanner = false
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (!state.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ApiConfig.absoluteUrl(state.imageUrl),
                        contentDescription = "Foto agen",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    InitialAvatar(initials = "AG", size = 84)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .background(PrimaryDark, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PhIcon(AppIcons.Camera, tint = Color.White, size = 15.dp)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = (-18).dp),
        ) {
            OutlinedTextField(
                value = state.bio,
                onValueChange = { v -> viewModel.update { it.copy(bio = v) } },
                label = { Text("Tentang") },
                placeholder = { Text("Ceritakan pengalaman & area layanan kamu…") },
                minLines = 3,
                maxLines = 6,
                enabled = !state.loading && !state.saving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )
            OutlinedTextField(
                value = state.phone,
                onValueChange = { v -> viewModel.update { it.copy(phone = v) } },
                label = { Text("No. HP / WhatsApp") },
                singleLine = true,
                enabled = !state.loading && !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(14.dp),
            )
            OutlinedTextField(
                value = state.company,
                onValueChange = { v -> viewModel.update { it.copy(company = v) } },
                label = { Text("Perusahaan / Kantor") },
                singleLine = true,
                enabled = !state.loading && !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(14.dp),
            )
            OutlinedTextField(
                value = state.licenseNumber,
                onValueChange = { v -> viewModel.update { it.copy(licenseNumber = v) } },
                label = { Text("No. Lisensi AREBI") },
                singleLine = true,
                enabled = !state.loading && !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(14.dp),
            )

            state.error?.let {
                Text(
                    it,
                    color = ErrorRed,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
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
                Text(
                    if (state.saving || state.uploading) "Menyimpan…" else "Simpan Profil Agen",
                    fontWeight = FontWeight.W700,
                )
            }
        }
        }
    }
}
