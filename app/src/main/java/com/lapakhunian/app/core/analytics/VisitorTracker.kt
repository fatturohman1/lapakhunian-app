@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.lapakhunian.app.core.analytics

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Sumber koordinat perangkat (seam untuk unit test). */
interface VisitorCoords {
    suspend fun lastKnown(): Pair<Double, Double>?
}

@Serializable
private data class VisitorTrackBody(
    val path: String,
    val sessionId: String,
    // @EncodeDefault: source punya nilai default — tanpa anotasi ini field tidak
    // diserialisasi (encodeDefaults=false di client utama) dan server salah
    // memperlakukan kunjungan app sebagai "web".
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val source: String = "android-app",
    val lat: Double? = null,
    val lng: Double? = null,
)

/**
 * Pelapor kunjungan ke analytics web (/dashboard/visitors) — paritas dengan
 * VisitorTracker di web. Path memakai format web (/properti/{slug}) supaya
 * agregat server sama dengan kunjungan browser.
 *
 * Lokasi perangkat (coarse) hanya dilampirkan bila izin lokasi SUDAH diberikan
 * (DeviceLocator.lastKnown() self-check, tidak pernah memunculkan prompt) —
 * server menandai locSource="device"; kalau tidak ada, server memakai GeoIP.
 */
@Singleton
class VisitorTracker @Inject constructor(
    private val client: HttpClient,
    private val baseUrl: String,
    private val session: VisitorSession,
    private val locator: VisitorCoords,
) {
    suspend fun trackPropertyOpen(slug: String?, fallbackId: String) {
        track("/properti/${slug ?: fallbackId}")
    }

    suspend fun track(path: String) {
        runCatching {
            val coords = runCatching { locator.lastKnown() }.getOrNull()
            client.post("$baseUrl/api/visitors/track") {
                contentType(ContentType.Application.Json)
                setBody(
                    VisitorTrackBody(
                        path = path,
                        sessionId = session.sessionId,
                        lat = coords?.first,
                        lng = coords?.second,
                    ),
                )
            }
        }
    }
}
