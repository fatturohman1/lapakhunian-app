package com.lapakhunian.app.core.notifications

import com.google.firebase.messaging.FirebaseMessaging
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.network.PropertyApi
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushTokenRegistrar @Inject constructor(
    private val prefs: NotificationPrefs,
    private val session: SessionManager,
    private val api: PropertyApi,
) {
    suspend fun sync() {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        prefs.fcmToken = token
        if (session.tokenFlow.value != null) {
            runCatching { api.registerDeviceToken(token) }
        }
    }
}