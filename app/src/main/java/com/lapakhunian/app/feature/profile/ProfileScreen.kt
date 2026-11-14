package com.lapakhunian.app.feature.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ChevronGray
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.InitialAvatar
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.network.dto.UserDto
import com.lapakhunian.app.core.util.ApiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: PropertyRepository,
    private val chatRepo: com.lapakhunian.app.core.data.repository.ChatRepository,
    session: SessionManager,
) : ViewModel() {

    val user: StateFlow<UserDto?> = session.userFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null,
    )

    /** Jumlah favorit & chat belum dibaca — tag di menu Profil (sesuai referensi). */
    private val _favCount = MutableStateFlow(0)
    val favCount: StateFlow<Int> = _favCount.asStateFlow()

    private val _unreadChats = MutableStateFlow(0)
    val unreadChats: StateFlow<Int> = _unreadChats.asStateFlow()

    init {
        viewModelScope.launch {
            session.tokenFlow.collect { token ->
                if (token == null) {
                    _favCount.value = 0
                    _unreadChats.value = 0
                } else {
                    _favCount.value = runCatching { repo.favorites().first() }
                        .getOrDefault(emptyList())
                        .size
                    _unreadChats.value = chatRepo.conversations().first().fold(
                        onSuccess = { list -> list.sumOf { c -> c.unread ?: 0 } },
                        onFailure = { 0 },
                    )
                }
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            onDone()
        }
    }
}

@Composable
fun ProfileScreen(
    onGoLogin: () -> Unit,
    onGoRegister: () -> Unit,
    onOpenOwnerDashboard: () -> Unit,
    onOpenAgentDashboard: () -> Unit,
    onOpenAgentLeads: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenNotifications: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenKpr: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
    onOpenVoucher: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsState()
    val favCount by viewModel.favCount.collectAsState()
    val unreadChats by viewModel.unreadChats.collectAsState()
    var pendingLogout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Profil",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W800,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )

        if (user == null) {
            // Guest
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
            ) {
                Text("Masuk ke akun kamu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W800)
                Text(
                    "Simpan favorit, chat agen, dan pasang iklan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Button(
                    onClick = onGoLogin,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) { Text("Masuk", fontWeight = FontWeight.W700) }
                Text(
                    "Belum punya akun? Daftar di sini",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryDark,
                    fontWeight = FontWeight.W700,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clickable(onClick = onGoRegister),
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!user?.image.isNullOrBlank()) {
                    AsyncImage(
                        model = ApiConfig.absoluteUrl(user?.image),
                        contentDescription = "Foto profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape),
                    )
                } else {
                    InitialAvatar(initials = (user?.name ?: "U").take(2), size = 52)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                ) {
                    Text(user?.name ?: "User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W800)
                    Text(
                        user?.email.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                Text(
                    when (user?.role) {
                        "agent" -> "AGEN"
                        "owner" -> "OWNER"
                        else -> (user?.role ?: "").uppercase()
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryDark,
                    fontWeight = FontWeight.W800,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }

            MenuGroup(
                items = buildList {
                    if (user?.role == "agent") {
                        add(MenuEntry(AppIcons.Dashboard, "Dashboard Agen", onOpenAgentDashboard as Any))
                        add(MenuEntry(AppIcons.Sell, "Leads Saya", onOpenAgentLeads as Any))
                    } else {
                        add(MenuEntry(AppIcons.Dashboard, "Dashboard Saya", onOpenOwnerDashboard as Any))
                    }
                    add(MenuEntry(AppIcons.PlusCircle, "Pasang Iklan", onOpenWizard as Any))
                    add(MenuEntry(AppIcons.Favorite, "Favorit", onOpenFavorites as Any, if (favCount > 0) "$favCount" else null))
                    add(MenuEntry(AppIcons.Chat, "Chat", onOpenChat as Any, if (unreadChats > 0) "$unreadChats baru" else null))
                },
            )
        }

        if (user != null) {
            MenuGroup(
                items = listOf(
                    MenuEntry(AppIcons.Receipt, "Riwayat & Transaksi", onOpenHistory as Any),
                    MenuEntry(AppIcons.Ticket, "Voucher", onOpenVoucher as Any),
                    MenuEntry(AppIcons.Calculator, "Simulasi KPR", onOpenKpr as Any),
                ),
            )
            MenuGroup(
                items = listOf(
                    MenuEntry(AppIcons.Edit, "Edit Profil", onEditProfile as Any),
                    MenuEntry(AppIcons.Bell, "Notifikasi", onOpenNotifications as Any),
                    MenuEntry(AppIcons.Gear, "Pengaturan", onOpenSettings as Any),
                    MenuEntry(AppIcons.Question, "Bantuan", onOpenHelp as Any),
                ),
            )
            MenuGroup(
                items = listOf(
                    MenuEntry(AppIcons.Logout, "Keluar", "logout"),
                ),
                logout = { pendingLogout = true },
            )
        }
        Box(Modifier.height(20.dp))
    }

    // Konfirmasi sebelum logout — cegah keluar tidak sengaja.
    if (pendingLogout) {
        AlertDialog(
            onDismissRequest = { pendingLogout = false },
            title = { Text("Keluar dari akun?", fontWeight = FontWeight.W800) },
            text = { Text("Kamu akan keluar dari akun LapakHunian.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingLogout = false
                    viewModel.logout(onGoLogin)
                }) {
                    Text("Keluar", color = ErrorRed, fontWeight = FontWeight.W700)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLogout = false }) {
                    Text("Batal")
                }
            },
        )
    }
}

@Composable
private fun MenuGroup(
    items: List<MenuEntry>,
    logout: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        items.forEach { entry ->
            val (icon, label, action, tag) = entry
            val isLogout = action == "logout"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        when {
                            isLogout && logout != null -> logout()
                            action is Function0<*> -> (action as () -> Unit)()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhIcon(
                    icon,
                    tint = if (isLogout) ErrorRed else PrimaryDark,
                    size = 21.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isLogout) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(13.dp),
                        ),
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W600,
                    color = if (isLogout) ErrorRed else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                )
                tag?.let {
                    // tag kanan ala referensi: jumlah favorit / "2 baru" / "1 bisa klaim"
                    Text(
                        it,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W800,
                        color = PrimaryDark,
                        modifier = Modifier
                            .background(TintGreen, RoundedCornerShape(50))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
                PhIcon(AppIcons.Chevron, contentDescription = null, tint = ChevronGray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Menu item Profil: ikon, label, aksi, dan tag kanan opsional (jumlah/status). */
private data class MenuEntry(val icon: String, val label: String, val action: Any?, val tag: String? = null)
