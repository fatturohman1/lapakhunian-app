package com.lapakhunian.app.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.network.SearchFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repo: PropertyRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _favIds = MutableStateFlow<Set<String>>(emptySet())
    val favIds: StateFlow<Set<String>> = _favIds.asStateFlow()

    private val refreshTick = MutableStateFlow(0)

    val properties: Flow<PagingData<Property>> =
        combine(_filters.debounce(250), refreshTick) { filters, _ -> filters }
            .flatMapLatest { repo.search(it) }
            .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            if (session.tokenFlow.first() != null) {
                _favIds.value = runCatching { repo.favorites().first() }
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
    }

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

    /** Toggle favorit optimis; guest → arahkan login. Gagal API → revert. */
    fun toggleFavorite(property: Property, onNeedsLogin: () -> Unit) {
        if (session.tokenFlow.value == null) {
            onNeedsLogin()
            return
        }
        val id = property.id
        val wasFavorite = id in _favIds.value
        _favIds.value = if (wasFavorite) _favIds.value - id else _favIds.value + id
        viewModelScope.launch {
            repo.setFavorite(id, !wasFavorite).onFailure {
                _favIds.value = if (wasFavorite) _favIds.value + id else _favIds.value - id
            }
        }
    }

    fun retry() {
        refreshTick.value = refreshTick.value + 1
    }
}
