package com.lapakhunian.app.feature.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.designsystem.components.EmptyState
import com.lapakhunian.app.core.network.dto.LeadWaDto
import com.lapakhunian.app.core.util.toWaNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeadWaUiState(
    val loading: Boolean = true,
    val needsLogin: Boolean = false,
    val notAgent: Boolean = false,
    val items: List<LeadWaDto> = emptyList(),
    val error: String? = null,
    val unlockingId: String? = null,
)

@HiltViewModel
class LeadWaViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LeadWaUiState())
    val state: StateFlow<LeadWaUiState> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    suspend fun load() {
        if (session.token() == null) {
            _state.update { it.copy(loading = false, needsLogin = true) }
            return
        }
        _state.update { it.copy(loading = true, error = null, notAgent = false) }
        viewModelScope.launch {
            repo.leadWaList().fold(
                onSuccess = { items -> _state.update { it.copy(loading = false, items = items) } },
                onFailure = { e ->
                    val notAgent = e.message?.contains("agent", ignoreCase = true) == true
                    _state.update { it.copy(loading = false, notAgent = notAgent, error = if (!notAgent) e.message else null) }
                },
            )
        }
    }

    fun retry() { viewModelScope.launch { load() } }

    /** Gagal buka link wa.me (tidak ada app WhatsApp/browser) — jangan diam. */
    fun notifyWaFailed() {
        _state.update { it.copy(error = "Tidak ada aplikasi WhatsApp/browser") }
    }

    fun unlock(id: String) {
        _state.update { it.copy(unlockingId = id, error = null) }
        viewModelScope.launch {
            repo.unlockLeadWa(id).fold(
                onSuccess = { load() },
                onFailure = { e -> _state.update { it.copy(unlockingId = null, error = e.message) } },
            )
        }
    }
}

@Composable
fun LeadWaScreen(
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
    viewModel: LeadWaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack, contentDescription = "Kembali")
            Text("Lead WhatsApp", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800, modifier = Modifier.padding(start = 6.dp))
        }

        when {
            state.needsLogin -> EmptyState(title = "Masuk dulu ya", message = "Lead WhatsApp tersedia untuk akun agen.", icon = AppIcons.Person)
            state.notAgent -> EmptyState(title = "Bukan akun agen", message = "Fitur lead WhatsApp tersedia untuk akun agen.", icon = AppIcons.Business)
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Memuat…", color = MutedGray)
            }
            state.items.isEmpty() -> EmptyState(
                title = "Belum ada lead WhatsApp",
                message = "Calon pembeli yang hubungi via WhatsApp akan muncul di sini.",
                icon = AppIcons.WhatsApp,
            )
            else -> {
                state.error?.let {
                    Text(it, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                ) {
                    items(state.items, key = { it.id ?: it.hashCode().toString() }) { lead ->
                        LeadWaItem(
                            lead = lead,
                            unlocking = state.unlockingId == lead.id,
                            onUnlock = { viewModel.unlock(lead.id.orEmpty()) },
                            onOpenWa = {
                                val number = toWaNumber(lead.phone.orEmpty())
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number")),
                                    )
                                }.onFailure { viewModel.notifyWaFailed() }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeadWaItem(
    lead: LeadWaDto,
    unlocking: Boolean,
    onUnlock: () -> Unit,
    onOpenWa: () -> Unit,
) {
    val unlocked = lead.unlocked == true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(lead.name ?: "Calon pembeli", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W800)
                Text(
                    text = if (unlocked) lead.phone.orEmpty() else "Nomor terkunci",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                if (unlocked) "TERBUKA" else "TERKUNCI",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.W800,
                color = if (unlocked) Color(0xFF15803D) else Color(0xFFB45309),
                modifier = Modifier
                    .background(
                        if (unlocked) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.lapakhunian.app.core.designsystem.PhIcon(AppIcons.Location, tint = MutedGray, size = 14.dp)
            Text(
                lead.propertyTitle ?: "Properti",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        if (!lead.message.isNullOrBlank()) {
            Text(
                lead.message,
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                lead.budget?.let {
                    Text(
                        "Budget " + com.lapakhunian.app.core.util.formatRupiah(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryDark,
                        fontWeight = FontWeight.W700,
                    )
                }
                lead.timeline?.let {
                    Text("Rencana: $it", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                }
            }
            if (unlocked) {
                Button(
                    onClick = onOpenWa,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    com.lapakhunian.app.core.designsystem.PhIcon(AppIcons.Phone, tint = Color.White, size = 15.dp)
                    Text("WhatsApp", fontWeight = FontWeight.W700, modifier = Modifier.padding(start = 6.dp))
                }
            } else {
                Button(
                    onClick = onUnlock,
                    enabled = !unlocking,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        if (unlocking) "Memproses…" else "Buka Nomor — ${lead.leadCost ?: 1} Lead",
                        fontWeight = FontWeight.W700,
                    )
                }
            }
        }
    }
}
