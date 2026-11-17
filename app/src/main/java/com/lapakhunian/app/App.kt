package com.lapakhunian.app

import android.app.Application
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.notifications.ChatNotificationWorker
import com.lapakhunian.app.core.notifications.LocalNotifier
import com.lapakhunian.app.core.notifications.PushTokenRegistrar
import com.lapakhunian.app.core.util.ApiConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject lateinit var session: SessionManager
    @Inject lateinit var pushTokenRegistrar: PushTokenRegistrar

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ApiConfig.init(BuildConfig.API_BASE_URL)
        LocalNotifier.ensureChannel(this)
        ChatNotificationWorker.schedule(this)
        if (session.isLoggedIn) {
            scope.launch { pushTokenRegistrar.sync() }
        }
    }
}