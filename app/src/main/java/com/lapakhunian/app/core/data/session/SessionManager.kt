package com.lapakhunian.app.core.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lapakhunian.app.core.network.dto.UserDto
import com.lapakhunian.app.core.util.RoleHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token Bearer JWT (mobile-login / Google) + cache profil user — TERENKRIPSI via
 * EncryptedSharedPreferences (MasterKey AES256-GCM di Keystore). P2: sebelumnya plaintext DataStore.
 * tokenFlow/userFlow in-memory StateFlow supaya interceptor & UI baca sinkron;
 * keduanya diseed sinkron di init (cold start langsung tahu status login).
 * Bila Keystore gagal (perangkat rusak/dst) → fallback prefs biasa, app tetap jalan.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "lapak_secure_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse {
            context.getSharedPreferences("lapak_session_plain", Context.MODE_PRIVATE)
        }
    }

    private val _tokenFlow = MutableStateFlow<String?>(null)
    val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private val _userFlow = MutableStateFlow<UserDto?>(null)
    val userFlow: StateFlow<UserDto?> = _userFlow.asStateFlow()

    val isLoggedIn: Boolean get() = _tokenFlow.value != null

    init {
        // Seed sinkron dari prefs + rehydrate role (wizard butuh listedBy sinkron di cold start)
        val user = prefs.getString(KEY_USER, null)?.let { decodeUser(it) }
        _tokenFlow.value = prefs.getString(KEY_TOKEN, null)
        _userFlow.value = user
        RoleHolder.set(user?.role)
    }

    private fun decodeUser(raw: String): UserDto? =
        runCatching { json.decodeFromString(UserDto.serializer(), raw) }.getOrNull()

    /** Baca token (guard VM) — sudah in-memory setelah init; tetap suspend demi kompatibilitas. */
    suspend fun token(): String? = _tokenFlow.value

    suspend fun saveSession(token: String, user: UserDto) {
        RoleHolder.set(user.role)
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, json.encodeToString(UserDto.serializer(), user))
            .apply()
        _tokenFlow.value = token
        _userFlow.value = user
    }

    suspend fun updateUser(user: UserDto) {
        RoleHolder.set(user.role)
        prefs.edit().putString(KEY_USER, json.encodeToString(UserDto.serializer(), user)).apply()
        _userFlow.value = user
    }

    suspend fun clearSession() {
        clearSessionImmediate()
    }

    /** Versi non-suspend — dipakai interceptor network (401) yang bukan coroutine. */
    fun clearSessionImmediate() {
        RoleHolder.set(null)
        prefs.edit().clear().apply()
        _tokenFlow.value = null
        _userFlow.value = null
    }

    suspend fun currentUser(): UserDto? = _userFlow.value

    private companion object {
        const val KEY_TOKEN = "session_token"
        const val KEY_USER = "user_json"
    }
}
