package com.lapakhunian.app.feature.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.designsystem.components.EmptyState
import com.lapakhunian.app.core.designsystem.components.PropertyCardSkeleton
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.util.estimateInstallment
import com.lapakhunian.app.core.util.formatRupiah
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompareUiState(
    val loading: Boolean = true,
    val items: List<Property> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class CompareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PropertyRepository,
) : ViewModel() {

    private val ids: List<String> = savedStateHandle.get<String>("ids").orEmpty()
        .split(",").map { it.trim() }.filter { it.isNotBlank() }

    private val _state = MutableStateFlow(CompareUiState())
    val state: StateFlow<CompareUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val results = ids.map { id -> repo.detail(id) }
            val items = results.mapNotNull { it.getOrNull() }
            val failed = results.count { it.isFailure }
            _state.update {
                it.copy(
                    loading = false,
                    items = items,
                    error = if (failed > 0) "Sebagian properti gagal dimuat. Periksa koneksi lalu coba lagi." else null,
                )
            }
        }
    }
}

@Composable
fun CompareScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: CompareViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            Text(
                "Bandingkan Properti",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        if (state.error != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::load) { Text("Coba lagi") }
            }
        }

        when {
            state.loading && state.items.isEmpty() -> Column(Modifier.padding(horizontal = 20.dp)) {
                repeat(2) { PropertyCardSkeleton() }
            }
            state.items.isEmpty() -> EmptyState(
                title = "Tidak ada pembanding",
                message = "Pilih 2-3 properti dari mode bandingkan di Jelajah.",
                icon = AppIcons.Compare,
            )
            else -> {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.items.forEach { property ->
                        CompareColumn(property, onOpenDetail)
                    }
                }
                Text(
                    "*Estimasi KPR: DP 20%, bunga 7,5%, tenor 15 tahun.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        Box(Modifier.height(24.dp))
    }
}

@Composable
private fun CompareColumn(property: Property, onOpenDetail: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onOpenDetail(property.id) }
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val image = property.images.firstOrNull()
            if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = property.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf(Color(0xFF6EE7B7), Color(0xFF10B981))),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    PhIcon(AppIcons.CategoryHouse, tint = Color.White.copy(alpha = 0.92f), size = 28.dp)
                }
            }
        }
        Text(
            property.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.W800,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            formatRupiah(property.price),
            color = PrimaryDark,
            fontWeight = FontWeight.W800,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        SpecRow("Cicilan/bulan*", formatRupiah(estimateInstallment(property.price)))
        SpecRow("LT", property.landArea?.let { "$it m²" } ?: "–")
        SpecRow("LB", property.buildingArea?.let { "$it m²" } ?: "–")
        SpecRow("KT/KM", "${property.bedrooms ?: "–"}/${property.bathrooms ?: "–"}")
        SpecRow("Sertifikat", property.certificate ?: "–")
        SpecRow("Kota", property.city ?: "–")
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedGray)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W700)
    }
}