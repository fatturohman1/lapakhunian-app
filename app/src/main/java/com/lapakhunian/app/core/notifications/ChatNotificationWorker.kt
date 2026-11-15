package com.lapakhunian.app.core.notifications

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lapakhunian.app.core.data.repository.ChatRepository
import com.lapakhunian.app.core.data.session.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Cek pesan baru tiap 15 menit (WorkManager periodic, persist antar reboot) →
 * notifikasi lokal. Selama server belum punya trigger FCM, ini menjaga agen
 * tetap tahu ada chat/lead baru walau app sudah ditutup.
 *
 * Pembandingnya [NotificationPrefs.knownUnread] (diset juga saat app foreground)
 * — notifikasi hanya muncul saat app background dan hitungan NAIK.
 */
class ChatNotificationWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun session(): SessionManager
        fun repo(): ChatRepository
        fun prefs(): NotificationPrefs
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        if (deps.session().tokenFlow.value == null) {
            // logout → reset snapshot agar user berikutnya tidak bawa hitungan lama
            deps.prefs().knownUnread = 0
            return Result.success()
        }
        val conversations = runCatching { deps.repo().conversationsOnce() }.getOrNull()
            ?: return Result.success() // error jaringan sekali → tunggu periode berikutnya

        val total = conversations.sumOf { it.unread ?: 0 }
        val known = deps.prefs().knownUnread
        val foreground =
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

        if (!foreground && total > known) {
            val detail = conversations
                .filter { (it.unread ?: 0) > 0 }
                .take(3)
                .joinToString("\n") { "${it.contactName ?: "Kontak"} (${it.unread})" }
            LocalNotifier.postChat(applicationContext, total, detail)
        }
        deps.prefs().knownUnread = total
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "chat-notif"

        /** Jadwalkan sekali (KEEP — diabaikan bila sudah ada). Panggil dari App.onCreate. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ChatNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
