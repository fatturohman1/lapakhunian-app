package com.lapakhunian.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.EmptyState
import com.lapakhunian.app.core.designsystem.components.InitialAvatar
import com.lapakhunian.app.core.designsystem.components.StatusBadge
import com.lapakhunian.app.core.domain.model.ListingStatus
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.network.dto.AgentDto
import com.lapakhunian.app.core.util.ApiConfig
import com.lapakhunian.app.core.util.formatRupiah
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentProfileUiState(
    val loading: Boolean = true,
    val needsLogin: Boolean = false,
    val notAgent: Boolean = false,
    val agent: AgentDto? = null,
    val listings: List<Property> = emptyList(),
    val error: String? = null,
)

/** Profil publik agen — sesuai referensi scr-agent-profile: banner, stat, tentang, kredensial, listing. */
@HiltViewModel
class AgentProfileViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AgentProfileUiState())
    val state: StateFlow<AgentProfileUiState> = _state.asStateFlow()

    // Tidak load di init — layar memuat via ON_RESUME (agar refresh setelah kembali dari edit)

    suspend fun load() {
        if (session.token() == null) {
            _state.update { it.copy(loading = false, needsLogin = true) }
            return
        }
        _state.update { it.copy(loading = true, error = null, notAgent = false) }
        viewModelScope.launch {
            repo.agentProfile().fold(
                onSuccess = { agent ->
                    val listings = async { repo.myListings().getOrNull().orEmpty() }
                    _state.update { it.copy(loading = false, agent = agent, listings = listings.await()) }
                },
                onFailure = { e ->
                    val isForbidden = e.message?.contains("agent", ignoreCase = true) == true
                    _state.update {
                        it.copy(
                            loading = false,
                            notAgent = isForbidden,
                            error = if (!isForbidden) e.message else null,
                        )
                    }
                },
            )
        }
    }

    fun retry() { viewModelScope.launch { load() } }
}

@Composable
fun AgentProfileScreen(
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
    onOpenDetail: (String) -> Unit = {},
    onEditProfile: () -> Unit = {},
    viewModel: AgentProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Muat/refresh tiap layar kembali tampil — setelah edit tersimpan profil langsung segar
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.retry()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        when {
            state.needsLogin -> {
                AgentProfileTopBar(onBack, titleColor = MaterialTheme.colorScheme.onSurface)
                EmptyState(
                    title = "Masuk dulu ya",
                    message = "Profil agen tersedia untuk akun agen.",
                    icon = AppIcons.Person,
                    actionLabel = "Masuk",
                    onAction = onGoLogin,
                )
            }
            state.notAgent -> {
                AgentProfileTopBar(onBack, titleColor = MaterialTheme.colorScheme.onSurface)
                EmptyState(title = "Bukan akun agen", message = "Profil agen tersedia untuk akun agen.", icon = AppIcons.Business)
            }
            // Loading hanya full-screen saat belum ada data — refresh saat resume tidak meng-flash
            state.loading && state.agent == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Memuat…", color = MutedGray)
            }
            state.error != null && state.agent == null -> {
                AgentProfileTopBar(onBack, titleColor = MaterialTheme.colorScheme.onSurface)
                EmptyState(
                    title = "Gagal memuat",
                    message = state.error.orEmpty(),
                    actionLabel = "Coba lagi",
                    onAction = { viewModel.retry() },
                )
            }
            else -> {
                val agent = state.agent

                // Banner + appbar overlay (referensi: pager header dengan appbar transparan);
                // banner custom agen bila ada, fallback gradient hijau brand
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF0C8A44)))),
                ) {
                    agent?.banner?.takeIf { it.isNotBlank() }?.let { bnr ->
                        AsyncImage(
                            model = ApiConfig.absoluteUrl(bnr),
                            contentDescription = "Banner agen",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    AgentProfileTopBar(
                        onBack,
                        titleColor = Color.White,
                        onEdit = onEditProfile,
                    )
                }

                // Avatar + nama + URL profil publik + badge tier
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = (-24).dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(Modifier.offset(y = 12.dp)) {
                        if (!agent?.image.isNullOrBlank()) {
                            AsyncImage(
                                model = ApiConfig.absoluteUrl(agent?.image),
                                contentDescription = "Avatar agen",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White),
                            )
                        } else {
                            InitialAvatar(initials = (agent?.name ?: "A").take(2), size = 64)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                agent?.name ?: "Agen",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.W800,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (agent?.profileTier == "PREMIUM") {
                                Text(
                                    "PRO",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.W800,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .background(Color(0xFF7C3AED), RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        agent?.slug?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                "lapakhunian.com/agen/$it",
                                fontSize = 11.5.sp,
                                color = MutedGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Stat: rating/ulasan · listing aktif · saldo lead (resp.rate tidak tersedia di API)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatBox("${agent?.rating ?: 0.0}★", "${agent?.reviewCount ?: 0} ulasan", Modifier.weight(1f))
                        StatBox("${agent?.propertiesCount ?: 0}", "Listing Aktif", Modifier.weight(1f))
                        StatBox("${agent?.leadBalance ?: 0}", "Saldo Lead", Modifier.weight(1f))
                    }

                    agent?.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                        SectionCard("Tentang") {
                            Text(bio, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp))
                        }
                    }

                    // Kredensial: AREBI + kantor (yang tersedia saja)
                    if (!agent?.licenseNumber.isNullOrBlank() || !agent?.company.isNullOrBlank()) {
                        SectionCard("Kredensial") {
                            Column {
                                agent?.licenseNumber?.takeIf { it.isNotBlank() }?.let { license ->
                                    CredentialRow(AppIcons.Verified, "AREBI No. $license", "VALID")
                                }
                                agent?.company?.takeIf { it.isNotBlank() }?.let { company ->
                                    CredentialRow(AppIcons.Business, company.uppercase(), null)
                                }
                            }
                        }
                    }

                    if (state.listings.isNotEmpty()) {
                        SectionCard("Listing Agen (${agent?.propertiesCount ?: state.listings.size})") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.listings.forEach { property ->
                                    AgentProfileListing(property) { onOpenDetail(property.id) }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AgentProfileTopBar(onBack: () -> Unit, titleColor: Color, onEdit: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Hit area 44dp, tint mengikuti warna teks di atas banner
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            PhIcon(
                AppIcons.Back,
                tint = titleColor,
                contentDescription = "Kembali",
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            "Profil Agen",
            color = titleColor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W800,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        if (onEdit != null) {
            // Edit profil agen — bio/phone/company/license/banner (sama dengan dashboard web)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                PhIcon(
                    AppIcons.Edit,
                    tint = titleColor,
                    contentDescription = "Edit profil agen",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W800)
        Box(Modifier.padding(top = 10.dp)) { content() }
    }
}

@Composable
private fun CredentialRow(icon: String, label: String, badge: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhIcon(
            icon,
            tint = PrimaryDark,
            size = 19.dp,
            modifier = Modifier
                .size(38.dp)
                .background(TintGreen, RoundedCornerShape(12.dp)),
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W600,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        badge?.let {
            Text(
                it,
                fontSize = 10.sp,
                fontWeight = FontWeight.W800,
                color = Color(0xFF15803D),
                modifier = Modifier
                    .background(Color(0xFFDCFCE7), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun AgentProfileListing(property: Property, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF6EE7B7), Color(0xFF10B981)))),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = property.images.firstOrNull(),
                contentDescription = property.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 11.dp),
        ) {
            Text(
                property.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatRupiah(property.price),
                color = PrimaryDark,
                fontWeight = FontWeight.W800,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        StatusBadge(
            status = if (property.isSold) ListingStatus.SOLD else property.status ?: ListingStatus.APPROVED,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
