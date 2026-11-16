package com.lapakhunian.app.core.util

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * 6371.0 * atan2(sqrt(a), sqrt(1 - a))
}

internal fun formatDistance(km: Double): String = when {
    km < 1 -> "${(km * 1000).roundToInt()} m"
    km < 10 -> String.format(Locale("id", "ID"), "%.1f km", km)
    else -> "${km.roundToInt()} km"
}