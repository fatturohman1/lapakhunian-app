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
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListUiState())
    val state: StateFlow<ChatListUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

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
            repo.conversations().collect { res ->
                // lead WhatsApp ikut disegarkan tiap siklus polling; bukan agen → kosong
                val waLeads = dashboardRepo.leadWaList().getOrNull().orEmpty()
                res.fold(
                    onSuccess = { items ->
                        _state.update {
                            it.copy(
                                loading = false,
                                needsLogin = false,
                                error = null,
                                items = items,
                                waLeads = waLeads,
                                totalUnread = items.sumOf { c -> c.unread ?: 0 },
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
                                messages = envelope.data,
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

    fun send(text: String) {
        val content = text.trim()
        if (content.isEmpty() || _state.value.sending) return
        val tempId = "temp-${System.currentTimeMillis()}"
        val optimistic = MessageDto(
            id = tempId,
            content = content,
            createdAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            fromMe = true,
        )
        // optimis: tampilkan pesan & kosongkan draft sebelum request jalan
        _state.update {
            it.copy(messages = it.messages + optimistic, draft = "", sending = true)
        }
        viewModelScope.launch {
            repo.sendMessage(conversationId, content).fold(
                onSuccess = { sent ->
                    // sukses: buang pesan sementara, ganti dengan pesan asli dari server
                    _state.update {
                        it.copy(
                            sending = false,
                            error = null,
                            messages = it.messages.filterNot { m -> m.id == tempId } + sent,
                        )
                    }
                },
                onFailure = { e ->
                    // gagal: buang pesan sementara, kembalikan draft agar teks tidak hilang
                    _state.update {
                        it.copy(
                            sending = false,
                            error = e.message ?: "Gagal mengirim",
                            messages = it.messages.filterNot { m -> m.id == tempId },
                            draft = content,
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
}
