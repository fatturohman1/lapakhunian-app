package com.lapakhunian.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.local.GuestFavoritesStore
import com.lapakhunian.app.core.data.local.PropertySnapshot
import com.lapakhunian.app.core.data.local.RecentlyViewedStore
import com.lapakhunian.app.core.data.local.toSnapshot
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.network.dto.BannerDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val featured: List<Property> = emptyList(),
    val latest: List<Property> = emptyList(),
    val offline: Boolean = false,
    /** Banner promo aktif dari admin (position "homepage") — kosong → strip tak tampil. */
    val banners: List<BannerDto> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: PropertyRepository,
    private val session: SessionManager,
    recentlyViewedStore: RecentlyViewedStore,
    private val guest: GuestFavoritesStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val recentlyViewed: StateFlow<List<PropertySnapshot>> = recentlyViewedStore.items

    private val _serverFavIds = MutableStateFlow<Set<String>>(emptySet())
    val favIds: StateFlow<Set<String>> =
        combine(session.tokenFlow, _serverFavIds, guest.items) { token, server, local ->
            if (token == null) local.map { it.id }.toSet() else server
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init { load() }

    /** Gagal jaringan → repository otomatis fallback cache Room. */
    fun load() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val featured = repo.featured(6)
            val latest = repo.latest(4)
            val banners = repo.activeBanners()
            val offline = featured.isEmpty() && latest.isEmpty()
            _state.update {
                it.copy(
                    loading = false,
                    featured = featured,
                    latest = latest,
                    offline = offline,
                    banners = banners,
                )
            }
            if (session.tokenFlow.first() != null) {
                _serverFavIds.value = runCatching { repo.favorites().first() }
                    .getOrDefault(emptyList())
                    .map { it.id }
                    .toSet()
            }
        }
    }

    /** Toggle favorit optimis; tamu → simpan lokal di perangkat. Gagal API → revert. */
    fun toggleFavorite(property: Property) {
        if (session.tokenFlow.value == null) {
            guest.toggle(property.toSnapshot())
            return
        }
        val id = property.id
        val wasFavorite = id in _serverFavIds.value
        _serverFavIds.value = if (wasFavorite) _serverFavIds.value - id else _serverFavIds.value + id
        viewModelScope.launch {
            repo.setFavorite(id, !wasFavorite).onFailure {
                _serverFavIds.value = if (wasFavorite) _serverFavIds.value + id else _serverFavIds.value - id
            }
        }
    }
}
