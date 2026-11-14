package com.lapakhunian.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class HomeUiState(
    val loading: Boolean = true,
    val featured: List<Property> = emptyList(),
    val latest: List<Property> = emptyList(),
    val offline: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: PropertyRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _favIds = MutableStateFlow<Set<String>>(emptySet())
    val favIds: StateFlow<Set<String>> = _favIds.asStateFlow()

    init { load() }

    /** Gagal jaringan → repository otomatis fallback cache Room. */
    fun load() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val featured = repo.featured(6)
            val latest = repo.latest(4)
            val offline = featured.isEmpty() && latest.isEmpty()
            _state.update {
                it.copy(
                    loading = false,
                    featured = featured,
                    latest = latest,
                    offline = offline,
                )
            }
            if (session.tokenFlow.first() != null) {
                _favIds.value = runCatching { repo.favorites().first() }
                    .getOrDefault(emptyList())
                    .map { it.id }
                    .toSet()
            }
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
}
