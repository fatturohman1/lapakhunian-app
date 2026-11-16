package com.lapakhunian.app.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.network.PropertyApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Terima push FCM dari server (data payload { title, body }): pesan chat baru,
 * lead, dsb — berjalan walau app ditutup. Token per device dibuffer di
 * [NotificationPrefs] lalu diregistrasi ke server (sudah login → langsung;
 * belum → dibuffer, diregistrasi saat login).
 */
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var prefs: NotificationPrefs
    @Inject lateinit var session: SessionManager
    @Inject lateinit var api: PropertyApi

    /** Pesan data-only dari server — tampilkan sebagai notifikasi. */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: message.notification?.title.orEmpty()
        val body = message.data["body"] ?: message.notification?.body.orEmpty()
        if (body.isBlank()) return
        LocalNotifier.postMessage(this, title, body, message.data["conversationId"])
    }

    override fun onNewToken(token: String) {
        prefs.fcmToken = token
        if (session.tokenFlow.value != null) {
            registerToken(token)
        }
    }

    private fun registerToken(token: String) {
        runBlocking {
            runCatching { api.registerDeviceToken(token) }
                .onFailure { android.util.Log.w("FcmService", "register token gagal", it) }
        }
    }
}
