package com.lapakhunian.app.feature.favorites

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val error: String? = null,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repo: PropertyRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    suspend fun load() {
        if (session.token() == null) {
            _state.update { it.copy(loading = false, needsLogin = true, items = emptyList()) }
            return
        }
        _state.update { it.copy(loading = true, needsLogin = false, error = null) }
        viewModelScope.launch {
            repo.favorites().collect { items ->
                _state.update { it.copy(loading = false, items = items) }
            }
        }
    }

    /** Tarik-untuk-muat-ulang: jalankan ulang load() di coroutine baru. */
    fun refresh() {
        viewModelScope.launch { load() }
    }

    /** Hapus dari favorit: optimis hilangkan item, revert kalau API gagal. */
    fun unfavorite(propertyId: String) {
        val previous = _state.value.items
        _state.update { it.copy(items = it.items.filterNot { p -> p.id == propertyId }) }
        viewModelScope.launch {
            repo.setFavorite(propertyId, false).onFailure {
                _state.update { it.copy(items = previous) }
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
            when {
                state.needsLogin -> EmptyState(
                    title = "Masuk dulu ya",
                    message = "Login untuk melihat & menyimpan properti favorit.",
                    icon = com.lapakhunian.app.core.designsystem.AppIcons.Person,
                )
                state.loading -> Column(Modifier.fillMaxSize()) {
                    repeat(3) { com.lapakhunian.app.core.designsystem.components.PropertyCardSkeleton() }
                }
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
