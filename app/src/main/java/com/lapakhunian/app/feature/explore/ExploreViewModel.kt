package com.lapakhunian.app.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lapakhunian.app.core.data.local.GuestFavoritesStore
import com.lapakhunian.app.core.data.local.toSnapshot
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.location.DeviceLocator
import com.lapakhunian.app.core.network.SearchFilters
import com.lapakhunian.app.core.util.distanceKm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repo: PropertyRepository,
    private val session: SessionManager,
    private val guest: GuestFavoritesStore,
    private val locator: DeviceLocator,
) : ViewModel() {

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _serverFavIds = MutableStateFlow<Set<String>>(emptySet())
    val favIds: StateFlow<Set<String>> =
        combine(session.tokenFlow, _serverFavIds, guest.items) { token, server, local ->
            if (token == null) local.map { it.id }.toSet() else server
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val refreshTick = MutableStateFlow(0)

    val properties: Flow<PagingData<Property>> =
        combine(_filters.debounce(250), refreshTick) { filters, _ -> filters }
            .flatMapLatest { repo.search(it) }
            .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            if (session.tokenFlow.first() != null) {
                _serverFavIds.value = runCatching { repo.favorites().first() }
                    .getOrDefault(emptyList())
                    .map { it.id }
                    .toSet()
            }
        }
    }

    fun setQuery(text: String) {
        _query.value = text
        _filters.value = _filters.value.copy(search = text)
    }

    fun update(transform: (SearchFilters) -> SearchFilters) {
        _filters.value = transform(_filters.value)
        if (_nearbyItems.value != null) loadNearby()
    }

    fun applyFilters(filters: SearchFilters) {
        _query.value = filters.search
        _filters.value = filters
        if (_nearbyItems.value != null) loadNearby()
    }

    fun resetFilters() = applyFilters(SearchFilters())

    fun setInitialCategory(category: String?) {
        if (category != null) {
            _filters.value = _filters.value.copy(category = category)
        }
    }

    fun setInitialCity(city: String?) {
        if (city != null) {
            _filters.value = _filters.value.copy(city = city)
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

    fun retry() {
        refreshTick.value = refreshTick.value + 1
    }

    private val _compareSelection = MutableStateFlow<List<Property>>(emptyList())
    val compareSelection: StateFlow<List<Property>> = _compareSelection.asStateFlow()

    /** Pilihan pembanding maks 3; toggle item yang sudah terpilih mengeluarkannya. */
    fun toggleCompare(property: Property) {
        _compareSelection.value = when {
            _compareSelection.value.any { it.id == property.id } ->
                _compareSelection.value.filterNot { it.id == property.id }
            _compareSelection.value.size >= 3 -> _compareSelection.value
            else -> _compareSelection.value + property
        }
    }

    fun clearCompare() {
        _compareSelection.value = emptyList()
    }

    // ===== Properti terdekat (izin lokasi coarse, one-shot, tidak dikirim ke server) =====

    private val _nearbyItems = MutableStateFlow<List<Property>?>(null)
    val nearbyItems: StateFlow<List<Property>?> = _nearbyItems.asStateFlow()

    private val _nearbyOrigin = MutableStateFlow<Pair<Double, Double>?>(null)
    val nearbyOrigin: StateFlow<Pair<Double, Double>?> = _nearbyOrigin.asStateFlow()

    private val _nearbyError = MutableStateFlow<String?>(null)
    val nearbyError: StateFlow<String?> = _nearbyError.asStateFlow()

    private val _nearbyLoading = MutableStateFlow(false)
    val nearbyLoading: StateFlow<Boolean> = _nearbyLoading.asStateFlow()

    /** null items = mode paging biasa; toggle kedua mematikan mode terdekat. */
    fun toggleNearby() {
        if (_nearbyItems.value != null || _nearbyLoading.value) {
            _nearbyItems.value = null
            _nearbyOrigin.value = null
            _nearbyError.value = null
            return
        }
        _nearbyLoading.value = true
        _nearbyError.value = null
        viewModelScope.launch {
            val origin = locator.lastKnown()
            if (origin == null) {
                _nearbyError.value = "Lokasi perangkat tidak tersedia. Aktifkan lokasi/GPS lalu coba lagi."
            } else {
                _nearbyOrigin.value = origin
                fetchNearby(origin)
            }
            _nearbyLoading.value = false
        }
    }

    fun retryNearby() {
        if (_nearbyOrigin.value != null) loadNearby()
    }

    fun dismissNearbyError() {
        _nearbyError.value = null
    }

    private fun loadNearby() {
        val origin = _nearbyOrigin.value ?: return
        _nearbyLoading.value = true
        _nearbyError.value = null
        viewModelScope.launch {
            fetchNearby(origin)
            _nearbyLoading.value = false
        }
    }

    private suspend fun fetchNearby(origin: Pair<Double, Double>) {
        repo.nearbyCandidates(_filters.value).fold(
            onSuccess = { list ->
                _nearbyItems.value = list.sortedWith(
                    compareBy<Property> { it.latitude == null || it.longitude == null }
                        .thenBy { p ->
                            val lat = p.latitude
                            val lon = p.longitude
                            if (lat == null || lon == null) Double.MAX_VALUE
                            else distanceKm(origin.first, origin.second, lat, lon)
                        },
                )
            },
            onFailure = {
                _nearbyError.value = "Gagal memuat properti terdekat. Periksa koneksi lalu coba lagi."
            },
        )
    }
}
