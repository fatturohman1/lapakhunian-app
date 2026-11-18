package com.lapakhunian.app.feature.chat

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.repository.ChatRepository
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.notifications.NotificationPrefs
import com.lapakhunian.app.core.network.dto.ConversationDto
import com.lapakhunian.app.core.network.dto.LeadWaDto
import com.lapakhunian.app.core.network.dto.MessageDto
import com.lapakhunian.app.core.network.dto.MessagesEnvelope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ChatListUiState(
    val loading: Boolean = true,
    val needsLogin: Boolean = false,
    val items: List<ConversationDto> = emptyList(),
    val waLeads: List<LeadWaDto> = emptyList(),
    val totalUnread: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val dashboardRepo: DashboardRepository,
    private val session: SessionManager,
    private val notifPrefs: NotificationPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListUiState())
    val state: StateFlow<ChatListUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    /** Layar Chat terlihat? false = VM hanya untuk badge tab Chat → polling lebih lambat. */
    private var screenVisible = false

    init {
        // polling hanya jalan saat proses di foreground (STARTED); berhenti saat background
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { startPolling() }
            override fun onStop(owner: LifecycleOwner) { stopPolling() }
        })
        // VM activity-scoped (badge tab Chat + layar Chat): ikuti login/logout —
        // polling restart tiap token berganti, bukan dicek sekali di init.
        viewModelScope.launch {
            session.tokenFlow.collect { token ->
                if (token == null) {
                    stopPolling()
                    notifPrefs.knownUnread = 0
                    _state.update {
                        it.copy(
                            loading = false,
                            needsLogin = true,
                            items = emptyList(),
                            waLeads = emptyList(),
                            totalUnread = 0,
                            error = null,
                        )
                    }
                } else if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    startPolling()
                }
            }
        }
    }

    /** Dipanggil layar Chat: visible → 5 dtk, tersembunyi (badge saja) → 30 dtk hemat trafik. */
    fun setScreenVisible(visible: Boolean) {
        if (screenVisible == visible) return
        screenVisible = visible
        if (session.tokenFlow.value != null) startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            // token() baca langsung DataStore; tokenFlow bisa belum emit saat cold start
            if (session.token() == null) {
                _state.update { it.copy(loading = false, needsLogin = true, items = emptyList()) }
                return@launch
            }
            repo.conversations(if (screenVisible) 5_000L else 30_000L).collect { res ->
                // lead WhatsApp ikut disegarkan tiap siklus polling; bukan agen → kosong
                val waLeads = dashboardRepo.leadWaList().getOrNull().orEmpty()
                res.fold(
                    onSuccess = { items ->
                        val totalUnread = items.sumOf { c -> c.unread ?: 0 }
                        // sync snapshot agar worker notifikasi tidak salah banding saat background
                        notifPrefs.knownUnread = totalUnread
                        _state.update {
                            it.copy(
                                loading = false,
                                needsLogin = false,
                                error = null,
                                items = items,
                                waLeads = waLeads,
                                totalUnread = totalUnread,
                            )
                        }
                    },
                    onFailure = { e ->
                        _state.update {
                            it.copy(
                                loading = false,
                                error = e.message ?: "Gagal memuat percakapan",
                                // pertahankan lead lama agar tidak flicker saat offline
                                waLeads = waLeads.ifEmpty { it.waLeads },
                            )
                        }
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
}

data class ThreadUiState(
    val loading: Boolean = true,
    val messages: List<MessageDto> = emptyList(),
    val conversation: ConversationDto? = null,
    val locked: Boolean = false,
    val error: String? = null,
    val sending: Boolean = false,
    val draft: String = "",
    val failedMessages: List<MessageDto> = emptyList(),
    val disputing: Boolean = false,
)

@HiltViewModel
class ThreadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ChatRepository,
) : ViewModel() {

    val conversationId: String = savedStateHandle.get<String>("conversationId").orEmpty()

    private val _state = MutableStateFlow(ThreadUiState())
    val state: StateFlow<ThreadUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            // ambil info conversation sekali untuk header (nama kontak)
            runCatching {
                repo.conversations().first()
            }.onSuccess { res ->
                res.getOrNull()?.firstOrNull { it.id == conversationId }?.let { conversation ->
                    _state.update { it.copy(conversation = conversation) }
                }
            }
        }
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
            var lastCount = -1
            while (isActive) {
                // polled() sudah menghasilkan Result (error jaringan tidak melempar exception)
                repo.messages(conversationId).first().fold(
                    onSuccess = { envelope: MessagesEnvelope ->
                        _state.update {
                            it.copy(
                                loading = false,
                                messages = envelope.data + it.messages.filter { message -> message.id?.startsWith("temp-") == true },
                                locked = envelope.locked ?: false,
                                error = null,
                            )
                        }
                        if (lastCount != envelope.data.size) {
                            lastCount = envelope.data.size
                            runCatching { repo.markRead(conversationId) }
                        }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(loading = false, error = e.message ?: "Gagal memuat pesan") }
                    },
                )
                delay(5000) // polling MVP; SSE di M9
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun updateDraft(text: String) {
        _state.update { it.copy(draft = text) }
    }

    fun send(text: String) = sendMessage(text)

    fun retryFailed(messageId: String) {
        val message = _state.value.failedMessages.firstOrNull { it.id == messageId } ?: return
        sendMessage(message.content.orEmpty(), messageId)
    }

    private fun sendMessage(text: String, retryId: String? = null) {
        val content = text.trim()
        if (content.isEmpty() || _state.value.sending || _state.value.locked) return
        val tempId = "temp-${java.util.UUID.randomUUID()}"
        val optimistic = MessageDto(
            id = tempId,
            content = content,
            createdAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            fromMe = true,
        )
        _state.update {
            it.copy(
                messages = it.messages + optimistic,
                draft = if (retryId == null) "" else it.draft,
                failedMessages = it.failedMessages.filterNot { message -> message.id == retryId },
                sending = true,
                error = null,
            )
        }
        viewModelScope.launch {
            repo.sendMessage(conversationId, content).fold(
                onSuccess = { sent ->
                    _state.update {
                        it.copy(
                            sending = false,
                            error = null,
                            messages = it.messages.filterNot { m -> m.id == tempId || m.id == sent.id } + sent,
                        )
                    }
                },
                onFailure = { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    _state.update {
                        it.copy(
                            sending = false,
                            error = e.message ?: "Gagal mengirim",
                            messages = it.messages.filterNot { m -> m.id == tempId },
                            failedMessages = it.failedMessages + optimistic,
                        )
                    }
                },
            )
        }
    }

    fun unlock() {
        viewModelScope.launch {
            repo.unlock(conversationId).fold(
                onSuccess = { _state.update { it.copy(locked = false) } },
                onFailure = { e -> _state.update { it.copy(error = e.message ?: "Gagal membuka chat") } },
            )
        }
    }

    /** Laporkan lead invalid → server buat dispute (admin review 1x24 jam, refund bila disetujui). */
    fun disputeLead(reason: String, onDone: (Result<String>) -> Unit) {
        if (_state.value.disputing) return
        _state.update { it.copy(disputing = true) }
        viewModelScope.launch {
            repo.disputeLead(conversationId, reason.trim()).fold(
                onSuccess = { message ->
                    _state.update { it.copy(disputing = false) }
                    onDone(Result.success(message))
                },
                onFailure = { e ->
                    _state.update { it.copy(disputing = false) }
                    onDone(Result.failure(e))
                },
            )
        }
    }
}
