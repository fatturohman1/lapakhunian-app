package com.lapakhunian.app.core.data.repository

import com.lapakhunian.app.core.network.dto.ConversationDto
import com.lapakhunian.app.core.network.dto.MessageDto
import com.lapakhunian.app.core.network.dto.MessagesEnvelope
import com.lapakhunian.app.core.network.dto.NotificationDto
import com.lapakhunian.app.core.network.PropertyApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Helper: bungkus envelope menjadi Result. */
private inline fun <T> envelope(block: () -> com.lapakhunian.app.core.network.dto.ApiEnvelope<T>): Result<T> =
    runCatching {
        val res = block()
        if (!res.success) throw ApiException(res.error ?: "Terjadi kesalahan")
        res.data ?: throw ApiException("Respon kosong")
    }

/**
 * Repository chat + notifikasi (MVP: polling, SSE opsional di M9).
 */
@Singleton
class ChatRepository @Inject constructor(
    private val api: PropertyApi,
) {
    fun conversations(): Flow<Result<List<ConversationDto>>> = polled {
        envelope { api.conversations() }.getOrThrow()
    }

    /** Snapshot percakapan sekali baca — dipakai worker notifikasi background. */
    suspend fun conversationsOnce(): List<ConversationDto> =
        envelope { api.conversations() }.getOrThrow()

    fun messages(conversationId: String): Flow<Result<MessagesEnvelope>> = polled {
        api.messages(conversationId)
    }

    suspend fun sendMessage(conversationId: String, content: String): Result<MessageDto> = envelope {
        api.sendMessage(conversationId, content)
    }

    suspend fun markRead(conversationId: String): Result<Unit> = runCatching {
        api.markConversationRead(conversationId)
        Unit
    }

    suspend fun unlock(conversationId: String): Result<Unit> = runCatching {
        val res = api.unlockConversation(conversationId)
        if (!res.success) throw ApiException(res.error ?: "Gagal membuka chat")
        Unit
    }

    fun notifications(): Flow<Result<List<NotificationDto>>> = polled {
        envelope { api.notifications() }.getOrThrow()
    }

    suspend fun markNotificationRead(id: String): Result<Unit> = runCatching {
        api.markNotificationRead(id)
        Unit
    }

    suspend fun markAllNotificationsRead(): Result<Unit> = runCatching {
        api.markAllNotificationsRead()
        Unit
    }

    /** Laporkan lead invalid (agen) → kembalikan pesan server (estimasi review 1x24 jam). */
    suspend fun disputeLead(conversationId: String, reason: String): Result<String> = runCatching {
        val res = api.disputeLead(conversationId, reason)
        if (!res.success) throw ApiException(res.error ?: "Gagal mengirim laporan")
        res.message ?: "Laporan terkirim"
    }
}

/** Flow polling: tiap tick menghasilkan Result; error jaringan tidak mematikan loop maupun collector. */
private fun <T> polled(block: suspend () -> T): Flow<Result<T>> =
    kotlinx.coroutines.flow.flow {
        while (true) {
            try {
                emit(Result.success(block()))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
            kotlinx.coroutines.delay(5000)
        }
    }
