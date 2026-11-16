package com.lapakhunian.app.feature.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.local.GuestFavoritesStore
import com.lapakhunian.app.core.data.local.PropertySnapshot
import com.lapakhunian.app.core.data.local.toProperty
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.components.EmptyState
import com.lapakhunian.app.core.designsystem.components.PropertyListCard
import com.lapakhunian.app.core.domain.model.Property
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val loading: Boolean = true,
    val items: List<Property> = emptyList(),
    val needsLogin: Boolean = false,
    val guestMode: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repo: PropertyRepository,
    private val session: SessionManager,
    private val guest: GuestFavoritesStore,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch { session.tokenFlow.collect { refresh() } }
        viewModelScope.launch {
            guest.items.collect { if (session.tokenFlow.value == null) refresh() }
        }
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val token = session.tokenFlow.value
            if (token == null) {
                val local = guest.items.value.map { it.toProperty() }
                _state.value = FavoritesUiState(
                    loading = false,
                    items = local,
                    needsLogin = local.isEmpty(),
                    guestMode = true,
                )
                return@launch
            }
            _state.update { it.copy(loading = true, needsLogin = false, guestMode = false, error = null) }
            val mergeError = mergeGuestFavorites()
            try {
                val items = repo.favorites().first()
                if (session.tokenFlow.value == token) {
                    _state.update { it.copy(loading = false, items = items, error = mergeError) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = mergeError ?: "Favorit belum dapat dimuat. Periksa koneksi lalu coba lagi.")
                }
            }
        }
    }

    /** Favorit tamu digabung ke akun saat login; yang gagal tetap disimpan lokal untuk dicoba lagi. */
    private suspend fun mergeGuestFavorites(): String? {
        val pending = guest.items.value
        if (pending.isEmpty()) return null
        val failed = mutableListOf<PropertySnapshot>()
        for (snapshot in pending) {
            val result = repo.setFavorite(snapshot.id, true)
            val error = result.exceptionOrNull()
            if (error is kotlinx.coroutines.CancellationException) throw error
            if (error != null) failed += snapshot
        }
        return if (failed.isEmpty()) {
            guest.clear()
            null
        } else {
            guest.replaceAll(failed)
            "Sebagian favorit sementara gagal digabung ke akun. Coba lagi."
        }
    }

    /** Hapus dari favorit: optimis hilangkan item, revert kalau API gagal. Tamu → hapus lokal. */
    fun unfavorite(propertyId: String) {
        val token = session.tokenFlow.value
        if (token == null) {
            guest.remove(propertyId)
            return
        }
        val previous = _state.value.items.firstOrNull { it.id == propertyId } ?: return
        _state.update { it.copy(items = it.items.filterNot { p -> p.id == propertyId }, error = null) }
        viewModelScope.launch {
            repo.setFavorite(propertyId, false).onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                if (session.tokenFlow.value == token) {
                    _state.update {
                        it.copy(
                            items = if (it.items.any { item -> item.id == propertyId }) it.items else it.items + previous,
                            error = "Favorit belum berhasil dihapus. Coba lagi.",
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onOpenDetail: (String) -> Unit,
    onGoLogin: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Tarik ke bawah untuk muat ulang daftar favorit
    PullToRefreshBox(
        isRefreshing = state.loading && state.items.isNotEmpty(),
        onRefresh = viewModel::refresh,
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "Favorit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )
            if (state.guestMode && state.items.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 10.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .background(com.lapakhunian.app.core.designsystem.TintGreen)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Favorit sementara", fontWeight = FontWeight.W800, style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Tersimpan di perangkat ini. Masuk untuk menyimpan permanen di akunmu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.lapakhunian.app.core.designsystem.MutedGray,
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = onGoLogin) {
                        Text("Masuk", fontWeight = FontWeight.W700, color = com.lapakhunian.app.core.designsystem.PrimaryDark)
                    }
                }
            }
            if (state.error != null && state.items.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = viewModel::refresh) { Text("Coba lagi") }
                }
            }
            when {
                state.needsLogin -> EmptyState(
                    title = "Masuk dulu ya",
                    message = "Login untuk melihat & menyimpan properti favorit.",
                    icon = com.lapakhunian.app.core.designsystem.AppIcons.Person,
                    actionLabel = "Masuk",
                    onAction = onGoLogin,
                )
                state.loading && state.items.isEmpty() -> Column(Modifier.fillMaxSize()) {
                    repeat(3) { com.lapakhunian.app.core.designsystem.components.PropertyCardSkeleton() }
                }
                state.error != null && state.items.isEmpty() -> EmptyState(
                    title = "Gagal memuat favorit",
                    message = state.error.orEmpty(),
                    actionLabel = "Coba lagi",
                    onAction = viewModel::refresh,
                )
                state.items.isEmpty() -> EmptyState(
                    title = "Belum ada favorit",
                    message = "Ketuk ikon hati di properti untuk menyimpannya di sini.",
                    icon = com.lapakhunian.app.core.designsystem.AppIcons.Favorite,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    items(state.items, key = { it.id }) { property ->
                        PropertyListCard(
                            title = property.title,
                            location = listOfNotNull(property.city, property.certificate).joinToString(" · "),
                            price = property.price,
                            imageUrl = property.images.firstOrNull(),
                            isFavorite = true,
                            categoryIcon = com.lapakhunian.app.core.designsystem.AppIcons.CategoryHouse,
                            seed = property.id.hashCode(),
                            onFavoriteClick = { viewModel.unfavorite(property.id) },
                            onClick = { onOpenDetail(property.id) },
                        )
                    }
                }
            }
        }
    }
}
