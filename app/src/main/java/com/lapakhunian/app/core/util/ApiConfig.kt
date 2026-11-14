package com.lapakhunian.app.core.util

/**
 * Konfigurasi API global. Diisi saat Application.onCreate dari BuildConfig,
 * supaya layer domain tidak bergantung pada BuildConfig langsung.
 */
object ApiConfig {
    var baseUrl: String = "https://lapakhunian.com"
        private set

    fun init(baseUrl: String) {
        this.baseUrl = baseUrl.trimEnd('/')
    }

    /** "/uploads/x.webp" → "https://lapakhunian.com/uploads/x.webp" */
    fun absoluteUrl(path: String?): String? = when {
        path.isNullOrBlank() -> null
        path.startsWith("http") -> path
        else -> baseUrl + (if (path.startsWith("/")) path else "/$path")
    }
}
