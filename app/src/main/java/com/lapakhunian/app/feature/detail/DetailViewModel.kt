package com.lapakhunian.app.feature.detail

import androidx.lifecycle.SavedStateHandle
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

data class DetailUiState(
    val loading: Boolean = true,
    val property: Property? = null,
    val error: String? = null,
    val isFavorite: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PropertyRepository,
    private val session: SessionManager,
) : ViewModel() {

    val propertyId: String = savedStateHandle.get<String>("propertyId").orEmpty()

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.trackView(propertyId) // view tracker, fire-and-forget
            repo.detail(propertyId).fold(
                onSuccess = { property ->
                    _state.update { it.copy(loading = false, property = property, error = null) }
                    loadFavoriteState()
                },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message ?: "Gagal memuat") } },
            )
        }
    }

    private suspend fun loadFavoriteState() {
        if (session.tokenFlow.first() == null) return
        val favIds = runCatching { repo.favorites().first() }
            .getOrDefault(emptyList())
            .map { it.id }
            .toSet()
        _state.update { it.copy(isFavorite = propertyId in favIds) }
    }

    /** Toggle favorit persist ke API; guest → arahkan login. Gagal API → revert. */
    fun toggleFavorite(onNeedsLogin: () -> Unit) {
        val property = _state.value.property ?: return
        if (session.tokenFlow.value == null) {
            onNeedsLogin()
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
