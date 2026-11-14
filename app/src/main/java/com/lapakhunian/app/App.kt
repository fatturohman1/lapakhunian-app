package com.lapakhunian.app

import android.app.Application
import com.lapakhunian.app.core.util.ApiConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiConfig.init(BuildConfig.API_BASE_URL)
    }
}
