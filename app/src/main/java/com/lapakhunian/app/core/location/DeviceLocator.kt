package com.lapakhunian.app.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Lokasi perkiraan one-shot untuk mengurutkan "Properti terdekat" dan (opsional)
 * analytics pengunjung. Last-known fix sistem dulu; bila kosong (emulator / baru
 * boot) minta satu fix dengan timeout supaya tidak menggantung tanpa hasil.
 *
 * Terdekat: lokasi hanya dipakai di perangkat, tidak dikirim ke server.
 * Analytics (VisitorTracker): koordinat coarse dikirim ke server HANYA bila
 * izin lokasi sudah diberikan sebelumnya (self-check di bawah, tanpa prompt).
 */
@Singleton
class DeviceLocator @Inject constructor(@ApplicationContext private val context: Context) :
    com.lapakhunian.app.core.analytics.VisitorCoords {
    override suspend fun lastKnown(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val granted =
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!granted) return@withContext null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val cached = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        if (cached != null) return@withContext cached.latitude to cached.longitude
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            ?: return@withContext null
        withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine { cont ->
                val listener = LocationListener { location ->
                    if (cont.isActive) cont.resume(location.latitude to location.longitude)
                }
                cont.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
                runCatching {
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }
    }
}