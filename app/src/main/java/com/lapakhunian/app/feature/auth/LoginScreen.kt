package com.lapakhunian.app.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.lapakhunian.app.BuildConfig
import com.lapakhunian.app.R
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoRegister: () -> Unit,
    onForgotPassword: (String) -> Unit = {},
    /** Akun Google baru belum pilih role (owner/agent) → arahkan ke role picker. */
    onNeedsRoleSelection: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var googleLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Box(Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = "Logo LapakHunian",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Text(
                "Masuk ke LapakHunian",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Text(
            "Favorit properti & chat agen langsung dari app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
            modifier = Modifier.padding(top = 10.dp),
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.clearError() },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearError() },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                // Toggle lihat/sembunyikan password
                IconButton(onClick = { showPassword = !showPassword }) {
                    PhIcon(
                        if (showPassword) AppIcons.EyeSlash else AppIcons.Eye,
                        contentDescription = if (showPassword) "Sembunyikan password" else "Tampilkan password",
                        tint = MutedGray,
                        size = 20.dp,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        androidx.compose.material3.TextButton(
            onClick = { onForgotPassword(email.trim()) },
            enabled = !state.loading && !googleLoading,
            modifier = Modifier.align(Alignment.End),
        ) { Text("Lupa password?") }

        state.error?.let { message ->
            Text(
                message,
                color = ErrorRed,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = { viewModel.login(email, password, onSuccess = onLoginSuccess) },
            enabled = !state.loading && !googleLoading,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text("Masuk", fontWeight = FontWeight.W700)
            }
        }

        // Google Sign-In hanya tampil bila WEB_CLIENT_ID terkonfigurasi (gradle.properties).
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            Text(
                "atau",
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            )

            Button(
                onClick = {
                    viewModel.clearError()
                    googleLoading = true
                    scope.launch {
                        // Credential Manager → pilih akun Google → idToken.
                        // Error dibedakan: device tanpa akun / dibatalkan user / konfigurasi (SHA-1).
                        var failureMsg: String? = null
                        val idToken = try {
                            val credentialManager = CredentialManager.create(context)
                            val option = GetGoogleIdOption.Builder()
                                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                .setFilterByAuthorizedAccounts(false)
                                .setAutoSelectEnabled(false)
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(option)
                                .build()
                            val credential = credentialManager.getCredential(context, request).credential
                            if (credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                GoogleIdTokenCredential.createFrom(credential.data).idToken
                            } else {
                                null
                            }
                        } catch (e: NoCredentialException) {
                            failureMsg = "Tidak ada akun Google di perangkat ini — tambahkan akun di Setelan lalu coba lagi."
                            null
                        } catch (e: GetCredentialCancellationException) {
                            failureMsg = "Login Google dibatalkan."
                            null
                        } catch (e: Exception) {
                            android.util.Log.w("GoogleLogin", "Google Sign-In gagal", e)
                            failureMsg = when {
                                e is GetCredentialException &&
                                    e.type.contains("NO_CREDENTIAL", ignoreCase = true) ->
                                    "Tidak ada akun Google yang bisa dipakai — pastikan akun Google ada di perangkat."
                                else ->
                                    "Login Google gagal (${e.javaClass.simpleName}). " +
                                        "Pastikan akun Google ada di perangkat & konfigurasi SHA-1 sudah benar — cek Logcat tag GoogleLogin."
                            }
                            null
                        }
                        if (idToken.isNullOrBlank()) {
                            googleLoading = false
                            viewModel.setError(failureMsg ?: "Login Google gagal/dibatalkan")
                        } else {
                            viewModel.loginWithGoogle(idToken) { ok ->
                                googleLoading = false
                                if (ok) {
                                    if (viewModel.state.value.needsRoleSelection) {
                                        onNeedsRoleSelection()
                                    } else {
                                        onLoginSuccess()
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = !state.loading && !googleLoading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            ) {
                if (googleLoading) {
                    CircularProgressIndicator(
                        color = MutedGray,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    "Masuk dengan Google",
                    fontWeight = FontWeight.W700,
                    modifier = if (googleLoading) Modifier.padding(start = 10.dp) else Modifier,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 32.dp),
        ) {
            Text("Belum punya akun? ", style = MaterialTheme.typography.bodyMedium, color = MutedGray)
            Text(
                "Daftar",
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryDark,
                fontWeight = FontWeight.W700,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .clickable(onClick = onGoRegister),
            )
        }
    }
}
