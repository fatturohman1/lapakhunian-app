package com.lapakhunian.app.feature.auth

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lapakhunian.app.core.designsystem.components.BackIconButton

@Composable
fun ForgotPasswordScreen(initialEmail: String, onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var email by rememberSaveable(initialEmail) { mutableStateOf(initialEmail) }
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackIconButton(onClick = onBack)
            Text("Lupa password", style = MaterialTheme.typography.titleLarge)
        }
        Text("Masukkan email akunmu. Ikuti tautan di email untuk membuat password baru.")
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.clearError() },
            label = { Text("Email") },
            singleLine = true,
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.emailMessage?.let { Text(it) }
        Button(
            onClick = { viewModel.requestPasswordReset(email) },
            enabled = !state.loading && state.emailCooldownSeconds == 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(when {
                state.loading -> "Mengirim…"
                state.emailCooldownSeconds > 0 -> "Kirim lagi dalam ${state.emailCooldownSeconds} dtk"
                else -> "Kirim tautan reset"
            })
        }
        if (state.emailMessage != null) {
            OutlinedButton(onClick = { openEmailApp(context) }, modifier = Modifier.fillMaxWidth()) { Text("Buka aplikasi email") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Kembali ke login") }
    }
}

@Composable
internal fun RegistrationConfirmation(state: AuthUiState, onResend: () -> Unit, onContinue: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Periksa email kamu", style = MaterialTheme.typography.headlineSmall)
        Text("Akun berhasil dibuat. Tautan verifikasi dikirim ke ${state.registeredEmail}.")
        Text("Buka tautannya untuk mengaktifkan akun, lalu kembali ke halaman login. Jika belum terlihat, periksa folder spam.")
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.emailMessage?.let { Text(it) }
        Button(onClick = { openEmailApp(context) }, modifier = Modifier.fillMaxWidth()) { Text("Buka aplikasi email") }
        OutlinedButton(
            onClick = onResend,
            enabled = !state.loading && state.emailCooldownSeconds == 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(when {
                state.loading -> "Mengirim…"
                state.emailCooldownSeconds > 0 -> "Kirim ulang dalam ${state.emailCooldownSeconds} dtk"
                else -> "Kirim ulang verifikasi"
            })
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Ke halaman login") }
    }
}

private fun openEmailApp(context: Context) {
    runCatching {
        context.startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL))
    }.onFailure {
        Toast.makeText(context, "Aplikasi email tidak ditemukan. Buka email melalui browser atau aplikasi email kamu.", Toast.LENGTH_LONG).show()
    }
}
