package com.lapakhunian.app.core.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Sumber ID kunjungan anonim (seam untuk unit test). */
interface VisitorSession {
    val sessionId: String
}

/**
 * ID kunjungan anonim per instalasi untuk analytics pengunjung (paritas dengan
 * sessionId web di sessionStorage). Bukan data personal — tidak terkait akun.
 * Non-sensitif → plain SharedPreferences (beda dengan SessionManager).
 */
@Singleton
class VisitorPrefs @Inject constructor(
    @ApplicationContext context: Context,
) : VisitorSession {
    private val prefs = context.getSharedPreferences("lapak_visitor", Context.MODE_PRIVATE)

    override val sessionId: String by lazy {
        prefs.getString(KEY_SESSION_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_SESSION_ID, it).apply()
        }
    }

    private companion object {
        const val KEY_SESSION_ID = "session_id"
    }
}
