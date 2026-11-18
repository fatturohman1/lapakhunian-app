package com.lapakhunian.app.core.util

import com.lapakhunian.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class UpdateInfo(val version: String, val notes: String)

object UpdateChecker {

    // Unduhan lewat proxy web lapakhunian.com/app/download (menyajikan APK release terbaru
    // dengan Content-Disposition benar) — bukan link GitHub langsung, agar URL yang dilihat
    // user tetap di domain brand. Cek versi sendiri tetap dari GitHub API (tak terlihat user).
    const val DOWNLOAD_URL = "https://lapakhunian.com/app/download"

    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/fatturohman1/lapakhunian-app/releases/latest"

    // Klien terpisah dari NetworkModule: tanpa plugin auth/Origin agar
    // token sesi tidak pernah terkirim ke api.github.com.
    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(io.ktor.client.plugins.HttpTimeout) { requestTimeoutMillis = 15_000 }
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** Mengembalikan info versi terbaru jika lebih baru dari yang terpasang, null jika sudah terbaru. Throw jika gagal cek. */
    suspend fun check(): UpdateInfo? {
        val response = client.get(LATEST_RELEASE_API) {
            header("Accept", "application/vnd.github+json")
        }
        if (!response.status.isSuccess()) error("HTTP ${response.status.value}")
        return parseRelease(response.bodyAsText(), BuildConfig.VERSION_NAME)
    }

    internal fun parseRelease(body: String, installed: String): UpdateInfo? {
        val obj = json.parseToJsonElement(body).jsonObject
        val tag = obj["tag_name"]?.jsonPrimitive?.content.orEmpty().removePrefix("v")
        require(tag.isNotBlank()) { "Versi rilis tidak tersedia" }
        if (compareVersions(tag, installed) <= 0) return null
        val notes = cleanReleaseNotes(obj["body"]?.jsonPrimitive?.contentOrNull.orEmpty())
        return UpdateInfo(tag, notes)
    }

    /** Notes rilis GitHub (markdown auto-notes) → daftar poin plain text, maks 8 baris. */
    internal fun cleanReleaseNotes(body: String): String =
        body.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("**Full Changelog**") }
            .map { it.removePrefix("* ").removePrefix("- ") }
            .map { it.replace(Regex("""\s+by @\S+ in https?://\S+"""), "") }
            .map { it.replace(Regex("""\*\*|__|`"""), "") }
            .filter { it.isNotEmpty() }
            .take(8)
            .joinToString("\n") { "• $it" }

    /** Bandingkan versi "1.0.0-beta.5": numerik per segmen, rilis stabil > pre-release, beta.N lebih besar dari beta.M jika N > M. */
    internal fun compareVersions(a: String, b: String): Int {
        val aCore = a.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val bCore = b.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(aCore.size, bCore.size)) {
            val diff = aCore.getOrElse(i) { 0 } - bCore.getOrElse(i) { 0 }
            if (diff != 0) return diff
        }
        val aPre = a.contains('-')
        val bPre = b.contains('-')
        if (aPre != bPre) return if (aPre) -1 else 1
        if (aPre) {
            val aNum = a.substringAfterLast('.').toIntOrNull() ?: 0
            val bNum = b.substringAfterLast('.').toIntOrNull() ?: 0
            return aNum - bNum
        }
        return 0
    }
}