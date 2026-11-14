package com.lapakhunian.app.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.repository.ChatRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.AmberBg
import com.lapakhunian.app.core.designsystem.BlueBg
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.designsystem.components.EmptyState
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.network.dto.NotificationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val loading: Boolean = true,
    val needsLogin: Boolean = false,
    val items: List<NotificationDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private var pollJob: kotlinx.coroutines.Job? = null

    init {
        // polling hanya jalan saat proses di foreground (STARTED); berhenti saat background
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { startPolling() }
            override fun onStop(owner: LifecycleOwner) { stopPolling() }
        })
        // cold start: proses sudah STARTED sebelum observer terpasang, jadi start manual
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            startPolling()
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            // token() baca langsung DataStore; tokenFlow bisa belum emit saat cold start
            if (session.token() == null) {
                _state.update { it.copy(loading = false, needsLogin = true, items = emptyList()) }
                return@launch
            }
            repo.notifications().collect { res ->
                res.fold(
                    onSuccess = { items ->
                        _state.update { it.copy(loading = false, needsLogin = false, error = null, items = items) }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(loading = false, error = e.message ?: "Gagal memuat notifikasi") }
                    },
                )
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
        // app background: pertahankan item terakhir, cukup matikan loading
        _state.update { it.copy(loading = false) }
    }

    fun markAllRead() {
        // optimis: tandai semua dibaca dulu, revert jika server gagal
        val previous = _state.value.items
        _state.update { s -> s.copy(items = s.items.map { it.copy(isRead = true) }) }
        viewModelScope.launch {
            repo.markAllNotificationsRead().fold(
                onSuccess = { _state.update { it.copy(error = null) } },
                onFailure = { e ->
                    _state.update {
                        it.copy(items = previous, error = e.message ?: "Gagal menandai semua dibaca")
                    }
                },
            )
        }
    }

    fun markRead(notif: NotificationDto) {
        val id = notif.id ?: return
        // optimis: tandai dibaca di UI dulu, sinkron ke server di belakang
        _state.update { s ->
            s.copy(items = s.items.map { if (it.id == id) it.copy(isRead = true) else it })
        }
        viewModelScope.launch { repo.markNotificationRead(id) }
    }
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenThread: (String) -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            Text(
                "Notifikasi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                modifier = Modifier.weight(1f).padding(start = 6.dp),
            )
            Text(
                "Tandai dibaca",
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryDark,
                fontWeight = FontWeight.W700,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.markAllRead() }
                    .padding(8.dp),
            )
        }
        // error sementara (mis. gagal tandai semua dibaca) — hilang saat poll berikutnya sukses
        if (state.error != null && state.items.isNotEmpty()) {
            Text(
                state.error.orEmpty(),
                color = ErrorRed,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        when {
            state.needsLogin -> EmptyState(
                title = "Masuk dulu ya",
                message = "Login untuk melihat notifikasi akun kamu.",
                icon = AppIcons.Person,
            )
            state.loading -> EmptyState(title = "Memuat…", message = "Mengambil notifikasi terbaru.", icon = AppIcons.Bell)
            state.error != null && state.items.isEmpty() -> EmptyState(
                title = "Gagal memuat",
                message = state.error ?: "Periksa koneksi internet kamu lalu coba lagi.",
                icon = AppIcons.Bell,
            )
            state.items.isEmpty() -> EmptyState(
                title = "Tidak ada notifikasi",
                message = "Notifikasi lead, pesan, dan status iklan muncul di sini.",
                icon = AppIcons.Bell,
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(state.items, key = { idx, notif -> notif.id ?: "notif-$idx" }) { _, notif ->
                    NotificationItem(notif) {
                        viewModel.markRead(notif)
                        // link web bentuknya "/dashboard/chat/{conversationId}" — ambil segmen terakhir
                        val segment = notif.link?.trim()?.split('/')?.lastOrNull { it.isNotBlank() }
                        if (segment != null && segment.length > 8 && !segment.equals("chat", ignoreCase = true)) {
                            onOpenThread(segment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notif: NotificationDto, onClick: () -> Unit) {
    val icon = when (notif.type) {
        "new_lead" -> AppIcons.Sell
        "new_message" -> AppIcons.Chat
        else -> AppIcons.CheckCircle
    }
    val tintBg = when (notif.type) {
        "new_lead" -> TintGreen
        "new_message" -> BlueBg
        else -> AmberBg
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (notif.isRead != true) {
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tintBg),
            contentAlignment = Alignment.Center,
        ) {
            PhIcon(icon, tint = PrimaryDark, size = 20.dp)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(notif.title.orEmpty(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W700)
            Text(notif.message.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MutedGray, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
