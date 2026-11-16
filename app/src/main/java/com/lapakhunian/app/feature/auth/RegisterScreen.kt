package com.lapakhunian.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.components.BackIconButton

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onGoLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("owner") }
    var showPassword by remember { mutableStateOf(false) }

    if (state.registerSuccess) {
        RegistrationConfirmation(
            state = state,
            onResend = { viewModel.resendVerification(state.registeredEmail) },
            onContinue = onRegisterSuccess,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Box(Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BackIconButton(onClick = onGoLogin)
            Text(
                "Daftar Akun",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; viewModel.clearError() },
            label = { Text("Nama lengkap") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
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
                .padding(top = 14.dp),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearError() },
            label = { Text("Password (min. 8 karakter)") },
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
                .padding(top = 14.dp),
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; viewModel.clearError() },
            label = { Text("No. WhatsApp (opsional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        )

        Text(
            "Daftar sebagai",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.W700,
            modifier = Modifier.padding(top = 20.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoleCard("Pemilik / Buyer", "owner", role == "owner", Modifier.weight(1f)) { role = "owner" }
            RoleCard("Agen Properti", "agent", role == "agent", Modifier.weight(1f)) { role = "agent" }
        }

        Text(
            "Setelah daftar, kami kirim email verifikasi. Akun bisa login setelah email diverifikasi.",
            style = MaterialTheme.typography.labelMedium,
            color = MutedGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        )

        state.error?.let { message ->
            Text(message, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp))
        }

        Button(
            onClick = { viewModel.register(name, email, password, role, phone) },
            enabled = !state.loading,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 32.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text("Buat Akun", fontWeight = FontWeight.W700)
            }
        }
    }
}

@Composable
private fun RoleCard(
    label: String,
    value: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) TintGreen else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PhIcon(AppIcons.Person,
            contentDescription = null,
            tint = if (selected) PrimaryDark else MutedGray,
            modifier = Modifier.size(24.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W700,
            color = if (selected) PrimaryDark else MutedGray,
        )
    }
}
