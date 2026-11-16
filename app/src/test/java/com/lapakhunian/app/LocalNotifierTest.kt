package com.lapakhunian.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.lapakhunian.app.core.notifications.LocalNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class LocalNotifierTest {
    private val app = RuntimeEnvironment.getApplication()
    private val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Test fun `channel is heads-up and the legacy silent channel is removed`() {
        manager.createNotificationChannel(
            NotificationChannel("chat", "Pesan & Lead", NotificationManager.IMPORTANCE_DEFAULT),
        )
        LocalNotifier.ensureChannel(app)
        assertNull(manager.getNotificationChannel("chat"))
        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            manager.getNotificationChannel(LocalNotifier.CHANNEL_CHAT)?.importance,
        )
    }

    @Test fun `messages from different conversations stack instead of replacing`() {
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        LocalNotifier.postMessage(app, "Pesan Baru", "halo", conversationId = "c1")
        LocalNotifier.postMessage(app, "Pesan Baru", "halo lagi", conversationId = "c2")
        assertEquals(2, Shadows.shadowOf(manager).allNotifications.size)
        LocalNotifier.postMessage(app, "Pesan Baru", "terbaru", conversationId = "c1")
        assertEquals(2, Shadows.shadowOf(manager).allNotifications.size)
    }

    @Test fun `without notification permission nothing is posted`() {
        LocalNotifier.postMessage(app, "Pesan Baru", "halo", conversationId = "c1")
        assertEquals(0, Shadows.shadowOf(manager).allNotifications.size)
    }
}