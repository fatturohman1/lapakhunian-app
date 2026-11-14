package com.lapakhunian.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.lapakhunian.app.core.network.dto.UserHistoryItemDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class CompensationUiState(
    val loading: Boolean = true,
    val needsLogin: Boolean = false,
    val items: List<UserHistoryItemDto> = emptyList(),
    val error: String? = null,
)

/** Riwayat kompensasi leads/slot yang diberikan admin (GET /api/user/history, type=compensation). */
@HiltViewModel
class CompensationViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(CompensationUiState())
    val state: StateFlow<CompensationUiState> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    suspend fun load() {
        if (session.token() == null) {
            _state.update { it.copy(loading = false, needsLogin = true) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.userHistory().fold(
                onSuccess = { items ->
                    _state.update {
                        it.copy(
                            loading = false,
                            items = items.filter { h -> h.type == "compensation" },
                        )
                    }
                },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message) } },
            )
        }
    }

    fun retry() { viewModelScope.launch { load() } }
}

private val compensationDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID"))

private fun formatCompensationDate(iso: String?): String = runCatching {
    OffsetDateTime.parse(iso).format(compensationDateFormat)
}.getOrDefault("")

@Composable
fun CompensationScreen(
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
    viewModel: CompensationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            Text("Kompensasi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800, modifier = Modifier.padding(start = 6.dp))
        }

        when {
            state.needsLogin -> EmptyState(
                title = "Masuk dulu ya",
                message = "Kompensasi tersedia untuk akun agen.",
                icon = AppIcons.Person,
                actionLabel = "Masuk",
                onAction = onGoLogin,
            )
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Memuat…", color = MutedGray)
            }
            state.error != null && state.items.isEmpty() -> EmptyState(
                title = "Gagal memuat",
                message = state.error ?: "Periksa koneksi internet kamu lalu coba lagi.",
                actionLabel = "Coba lagi",
                onAction = { viewModel.retry() },
            )
            state.items.isEmpty() -> EmptyState(
                title = "Belum ada kompensasi",
                message = "Kompensasi leads/slot yang diberikan admin akan muncul di sini.",
                icon = AppIcons.Gift,
            )
            else -> {
                Text(
                    "Kompensasi diberikan langsung oleh tim LapakHunian dan otomatis masuk ke saldo kamu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                ) {
                    state.items.forEach { item ->
                        CompensationItem(item)
                    }
                }
            }
        }
        Box(Modifier.size(20.dp))
    }
}

@Composable
private fun CompensationItem(item: UserHistoryItemDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhIcon(
            AppIcons.Gift,
            tint = PrimaryDark,
            size = 20.dp,
            modifier = Modifier
                .size(42.dp)
                .background(TintGreen, RoundedCornerShape(13.dp)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(item.title ?: "Kompensasi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W800)
            Text(
                item.description.orEmpty().ifBlank { "Diberikan oleh admin" },
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                formatCompensationDate(item.date),
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            item.amount.orEmpty(),
            color = Color(0xFF15803D),
            fontWeight = FontWeight.W800,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
