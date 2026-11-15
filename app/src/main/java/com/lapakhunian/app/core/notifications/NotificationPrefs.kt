package com.lapakhunian.app.core.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot hitungan pesan belum dibaca untuk deteksi "chat baru" oleh worker
 * notifikasi. Non-sensitif → plain SharedPreferences (beda dengan SessionManager).
 * Diset sinkron oleh ChatListViewModel (foreground) & ChatNotificationWorker (background)
 * supaya tidak ada notifikasi ganda/terlewat.
 */
@Singleton
class NotificationPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("lapak_notifications", Context.MODE_PRIVATE)

    var knownUnread: Int
        get() = prefs.getInt(KEY_KNOWN_UNREAD, 0)
        set(value) = prefs.edit().putInt(KEY_KNOWN_UNREAD, value).apply()

    /**
     * Token FCM per device — dibuffer bila diterima sebelum login, diregistrasi
     * ke server setelah login (server perlu token untuk mengirim push per user).
     */
    var fcmToken: String?
        get() = prefs.getString(KEY_FCM_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_FCM_TOKEN, value).apply()

    private companion object {
        const val KEY_KNOWN_UNREAD = "known_unread"
        const val KEY_FCM_TOKEN = "fcm_token"
    }
}
