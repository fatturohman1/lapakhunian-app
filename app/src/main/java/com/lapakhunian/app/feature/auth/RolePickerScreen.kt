package com.lapakhunian.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.repository.ApiException
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.network.dto.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RolePickerUiState(
    val selected: String? = null,
    val submitting: Boolean = false,
    val error: String? = null,
)

/** Pilih role (owner/agent) untuk akun Google yang belum onboarding. */
@HiltViewModel
class RolePickerViewModel @Inject constructor(
    private val repo: PropertyRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(RolePickerUiState())
    val state: StateFlow<RolePickerUiState> = _state.asStateFlow()

    fun select(role: String) = _state.update { it.copy(selected = role, error = null) }

    /** Server: role=agent → buat record Agent (3 lead gratis + trial 30 hari). */
    fun submit(onDone: () -> Unit) {
        val role = _state.value.selected ?: return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            repo.completeOnboarding(role).fold(
                onSuccess = {
                    // refresh role di sesi lokal agar dashboard owner/agen langsung benar
                    session.updateUser(
                        session.userFlow.value?.copy(role = role) ?: UserDto(role = role),
                    )
                    _state.update { it.copy(submitting = false) }
                    onDone()
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            submitting = false,
                            error = (e as? ApiException)?.message ?: "Gagal menyimpan pilihan. Coba lagi.",
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun RolePickerScreen(
    onDone: () -> Unit,
    viewModel: RolePickerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(56.dp))
        Text(
            "Satu langkah terakhir",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W800,
        )
        Text(
            "Selamat datang di LapakHunian! Pilih peran kamu untuk melanjutkan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
            modifier = Modifier.padding(top = 10.dp),
        )
        Box(Modifier.height(28.dp))

        RoleCard(
            selected = state.selected == "owner",
            icon = AppIcons.Home,
            title = "Pencari Hunian",
            subtitle = "Cari properti, simpan favorit, dan hubungi agen langsung dari app.",
            onClick = { viewModel.select("owner") },
        )
        Box(Modifier.height(16.dp))
        RoleCard(
            selected = state.selected == "agent",
            icon = AppIcons.Sell,
            title = "Agen / Pemilik Jual",
            subtitle = "Pasang iklan properti & terima lead pembeli. Bonus 3 lead gratis + trial 30 hari.",
            onClick = { viewModel.select("agent") },
        )

        state.error?.let { message ->
            Text(
                message,
                color = ErrorRed,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        Button(
            onClick = { viewModel.submit(onDone) },
            enabled = state.selected != null && !state.submitting,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
            } else {
                Text("Lanjutkan", fontWeight = FontWeight.W700)
            }
        }
        Box(Modifier.height(32.dp))
    }
}

@Composable
private fun RoleCard(
    selected: Boolean,
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) PrimaryDark else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .height(44.dp)
                .background(TintGreen, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PhIcon(icon, contentDescription = title, tint = PrimaryDark, modifier = Modifier.padding(10.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(title, fontWeight = FontWeight.W800, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (selected) {
            PhIcon(
                AppIcons.CheckCircle,
                contentDescription = "Dipilih",
                tint = PrimaryDark,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
