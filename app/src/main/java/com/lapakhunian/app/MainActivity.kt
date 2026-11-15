package com.lapakhunian.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lapakhunian.app.core.designsystem.LapakHunianTheme
import com.lapakhunian.app.core.notifications.LocalNotifier
import com.lapakhunian.app.core.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* ditolak → chat tetap bisa dibaca manual, tidak nag ulang */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        // Beta: paksa status bar & navigasi sistem gaya light (ikon gelap)
        // selaras dengan theme light-only — ikon tetap terbaca walau HP set dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // Tap notifikasi chat → langsung buka tab Chat
        val openChat = intent?.getBooleanExtra(LocalNotifier.EXTRA_OPEN_CHAT, false) == true
        // App Links: https://lapakhunian.com/properti/{slug} dari share link
        val propertySlug = intent?.data?.takeIf {
            it.host == "lapakhunian.com" && it.pathSegments?.firstOrNull() == "properti"
        }?.lastPathSegment
        setContent {
            LapakHunianTheme {
                AppRoot(initialOpenChat = openChat, initialPropertySlug = propertySlug)
            }
        }
    }

    /** Android 13+: tanya izin notifikasi sekali saat pertama buka app. */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val prefs = getSharedPreferences("lapak_notifications", MODE_PRIVATE)
        if (prefs.getBoolean("perm_asked", false)) return
        prefs.edit().putBoolean("perm_asked", true).apply()
        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
