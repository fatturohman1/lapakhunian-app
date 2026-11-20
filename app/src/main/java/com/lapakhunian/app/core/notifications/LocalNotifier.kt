package com.lapakhunian.app.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lapakhunian.app.MainActivity
import com.lapakhunian.app.R

/** Notifikasi lokal (channel + posting) — pengganti FCM sementara di beta. */
object LocalNotifier {
    /** heads-up ala WhatsApp; id baru karena importance channel lama tak bisa di-upgrade in-place. */
    const val CHANNEL_CHAT = "chat_heads_up"
    private const val LEGACY_CHANNEL = "chat"

    /** Extra di Intent MainActivity: buka tab Chat saat notifikasi ditap. */
    const val EXTRA_OPEN_CHAT = "open_chat"

    private const val NOTIF_ID_CHAT = 1001
    private const val NOTIF_ID_FCM = 1002

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel(LEGACY_CHANNEL)
        val channel = NotificationChannel(
            CHANNEL_CHAT,
            "Pesan & Lead",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Notifikasi pesan baru dan lead di LapakHunian" }
        manager.createNotificationChannel(channel)
    }

    /** Android 13+ butuh izin runtime POST_NOTIFICATIONS. */
    fun canPost(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    /**
     * Notifikasi umum dari FCM (data payload { title, body }) — tap → tab Chat.
     */
    fun postMessage(context: Context, title: String, body: String, conversationId: String? = null) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CHAT, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        ensureChannel(context)
        val notif = NotificationCompat.Builder(context, CHANNEL_CHAT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title.ifBlank { "LapakHunian" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        // Satu notifikasi per percakapan (menumpuk ala WhatsApp), bukan saling menggantikan.
        val id = conversationId?.hashCode() ?: NOTIF_ID_FCM
        runCatching { NotificationManagerCompat.from(context).notify(id, notif) }
    }

    /**
     * Notifikasi ringkasan pesan baru. Tap → MainActivity dengan [EXTRA_OPEN_CHAT]
     * sehingga app langsung ke tab Chat.
     */
    fun postChat(context: Context, totalUnread: Int, detail: String) {
        if (totalUnread <= 0 || !canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CHAT, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        ensureChannel(context)
        val notif = NotificationCompat.Builder(context, CHANNEL_CHAT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("LapakHunian")
            .setContentText("Kamu punya $totalUnread pesan belum dibaca")
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_ID_CHAT, notif) }
    }
}
