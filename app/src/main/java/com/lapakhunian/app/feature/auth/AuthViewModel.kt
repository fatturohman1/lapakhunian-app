package com.lapakhunian.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.repository.ApiException
import com.lapakhunian.app.core.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val registerSuccess: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: PropertyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Email dan password wajib diisi") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.login(email.trim(), password).fold(
                onSuccess = {
                    _state.update { it.copy(loading = false, error = null) }
                    onSuccess()
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e, "Login gagal")) }
                },
            )
        }
    }

    /** Set pesan error dari UI (mis. Credential Manager dibatalkan/gagal). */
    fun setError(message: String) = _state.update { it.copy(error = message) }

    /** Google Sign-In: idToken dari Credential Manager → POST /api/auth/mobile-google-login.
     *  onDone(true) bila sukses; gagal → state.error berisi pesan ramah + onDone(false). */
    fun loginWithGoogle(idToken: String, onDone: (Boolean) -> Unit) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.googleLogin(idToken).fold(
                onSuccess = {
                    _state.update { it.copy(loading = false, error = null) }
                    onDone(true)
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e, "Login Google gagal")) }
                    onDone(false)
                },
            )
        }
    }

    fun register(name: String, email: String, password: String, role: String, phone: String?) {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Email dan password wajib diisi") }
            return
        }
        if (password.length < 8) {
            _state.update { it.copy(error = "Password minimal 8 karakter") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.register(name.trim(), email.trim(), password, role, phone?.takeIf { it.isNotBlank() }).fold(
                onSuccess = {
                    _state.update { it.copy(loading = false, registerSuccess = true) }
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e, "Registrasi gagal")) }
                },
            )
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /** Pesan error dari server lewat; error jaringan lain → pesan ramah (bukan teks OkHttp mentah). */
    private fun friendlyError(e: Throwable, fallback: String): String =
        (e as? ApiException)?.message ?: e.message?.takeIf { m -> m.contains("sesi", ignoreCase = true) }
            ?: "Tidak bisa terhubung ke server. Periksa koneksi internet kamu."
}

