package com.lapakhunian.app.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.local.GuestFavoritesStore
import com.lapakhunian.app.core.data.local.RecentlyViewedStore
import com.lapakhunian.app.core.data.local.toSnapshot
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.domain.model.Property
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val loading: Boolean = true,
    val property: Property? = null,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val reporting: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PropertyRepository,
    private val session: SessionManager,
    private val recentlyViewed: RecentlyViewedStore,
    private val guest: GuestFavoritesStore,
    private val visitorTracker: com.lapakhunian.app.core.analytics.VisitorTracker,
) : ViewModel() {

    val propertyId: String = savedStateHandle.get<String>("propertyId").orEmpty()

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    /** Lapor properti palsu/menyesatkan — guest → error "Harap login" dari repository. */
    fun reportProperty(reason: String, onDone: (Result<Unit>) -> Unit) {
        if (_state.value.reporting) return
        _state.update { it.copy(reporting = true) }
        viewModelScope.launch {
            repo.reportProperty(propertyId, reason.trim()).fold(
                onSuccess = {
                    _state.update { it.copy(reporting = false) }
                    onDone(Result.success(Unit))
                },
                onFailure = { e ->
                    _state.update { it.copy(reporting = false) }
                    onDone(Result.failure(e))
                },
            )
        }
    }

    init {
        viewModelScope.launch {
            repo.trackView(propertyId) // view tracker, fire-and-forget
            repo.detail(propertyId).fold(
                onSuccess = { property ->
                    recentlyViewed.record(property.toSnapshot())
                    _state.update { it.copy(loading = false, property = property, error = null) }
                    loadFavoriteState()
                    // Analytics pengunjung: catat kunjungan app dengan path format
                    // web (/properti/{slug}) supaya agregat sama di /dashboard/visitors.
                    // Lokasi coarse ikut HANYA bila izin lokasi sudah diberikan.
                    viewModelScope.launch {
                        visitorTracker.trackPropertyOpen(property.slug, property.id)
                    }
                },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message ?: "Gagal memuat") } },
            )
        }
    }

    private suspend fun loadFavoriteState() {
        if (session.tokenFlow.first() == null) {
            _state.update { it.copy(isFavorite = guest.items.value.any { s -> s.id == propertyId }) }
            return
        }
        val favIds = runCatching { repo.favorites().first() }
            .getOrDefault(emptyList())
            .map { it.id }
            .toSet()
        _state.update { it.copy(isFavorite = propertyId in favIds) }
    }

    /** Toggle favorit: login → persist ke API (gagal revert); tamu → simpan lokal di perangkat. */
    fun toggleFavorite() {
        val property = _state.value.property ?: return
        if (session.tokenFlow.value == null) {
            guest.toggle(property.toSnapshot())
            _state.update { it.copy(isFavorite = guest.items.value.any { s -> s.id == property.id }) }
            return
        }
        val wasFavorite = _state.value.isFavorite
        _state.update { it.copy(isFavorite = !wasFavorite) }
        viewModelScope.launch {
            repo.setFavorite(property.id, !wasFavorite).onFailure {
                _state.update { it.copy(isFavorite = wasFavorite) }
            }
        }
    }
}
