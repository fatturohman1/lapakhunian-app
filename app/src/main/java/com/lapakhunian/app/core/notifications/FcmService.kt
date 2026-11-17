package com.lapakhunian.app.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var prefs: NotificationPrefs
    @Inject lateinit var registrar: PushTokenRegistrar

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: message.notification?.title.orEmpty()
        val body = message.data["body"] ?: message.notification?.body.orEmpty()
        if (body.isBlank()) return
        LocalNotifier.postMessage(this, title, body, message.data["conversationId"])
    }

    override fun onNewToken(token: String) {
        prefs.fcmToken = token
        scope.launch { registrar.sync() }
    }
}