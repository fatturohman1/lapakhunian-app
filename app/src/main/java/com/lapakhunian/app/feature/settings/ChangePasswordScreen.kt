package com.lapakhunian.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.repository.ApiException
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import android.widget.Toast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val repo: PropertyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun update(transform: (ChangePasswordUiState) -> ChangePasswordUiState) =
        _state.update(transform)

    /** Password lama opsional — server minta hanya untuk akun email (Google: tanpa password). */
    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.loading) return
        if (s.newPassword.length < 8) {
            _state.update { it.copy(error = "Password baru minimal 8 karakter") }
            return
        }
        if (s.newPassword != s.confirmPassword) {
            _state.update { it.copy(error = "Konfirmasi password tidak sama") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.changePassword(
                s.currentPassword.takeIf { it.isNotBlank() },
                s.newPassword,
            ).fold(
                onSuccess = {
                    _state.update { it.copy(loading = false) }
                    onSuccess()
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = (e as? ApiException)?.message
                                ?: "Tidak bisa terhubung ke server. Periksa koneksi internet kamu.",
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Box(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackIconButton(onClick = onBack)
            Text(
                "Ubah Password",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            "Khusus akun email. Password lama boleh dikosongkan bila kamu login dengan Google.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
            modifier = Modifier.padding(top = 10.dp),
        )
        Box(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.currentPassword,
            onValueChange = { v -> viewModel.update { it.copy(currentPassword = v, error = null) } },
            label = { Text("Password lama (opsional)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.newPassword,
            onValueChange = { v -> viewModel.update { it.copy(newPassword = v, error = null) } },
            label = { Text("Password baru") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        )
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = { v -> viewModel.update { it.copy(confirmPassword = v, error = null) } },
            label = { Text("Ulangi password baru") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        )

        state.error?.let { message ->
            Text(
                message,
                color = ErrorRed,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = {
                viewModel.submit {
                    Toast.makeText(context, "Password berhasil diubah", Toast.LENGTH_LONG).show()
                    onBack()
                }
            },
            enabled = !state.loading,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 32.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Simpan Password", fontWeight = FontWeight.W700)
            }
        }
    }
}
