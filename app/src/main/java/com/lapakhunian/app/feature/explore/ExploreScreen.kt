package com.lapakhunian.app.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.designsystem.components.EmptyState
import com.lapakhunian.app.core.designsystem.components.FilterChip
import com.lapakhunian.app.core.designsystem.components.LapakSearchBar
import com.lapakhunian.app.core.designsystem.components.PropertyCardSkeleton
import com.lapakhunian.app.core.designsystem.components.PropertyListCard
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.domain.model.PropertyCategory
import com.lapakhunian.app.core.network.SearchFilters
import com.lapakhunian.app.core.util.compactRupiah

// Nilai kota = nilai `city` aktual di database (sama dengan web, city-alias.ts) —
// filter API pakai contains-insensitive jadi harus substring dari nilai DB.
private val CITIES = listOf(
    "Bekasi", "Bogor", "Depok", "Jakarta", "Jakarta Pusat", "Jakarta Selatan",
    "Sumedang", "Tangerang Selatan", "Bandung",
)

private fun PropertyCategory.icon(): String = when (this) {
    PropertyCategory.HOUSE -> AppIcons.CategoryHouse
    PropertyCategory.APARTMENT -> AppIcons.CategoryApartment
    PropertyCategory.OFFICE -> AppIcons.CategoryOffice
    PropertyCategory.LAND -> AppIcons.CategoryLand
    PropertyCategory.COMMERCIAL -> AppIcons.CategoryCommercial
    PropertyCategory.TOWNHOME -> AppIcons.CategoryTownhome
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    initialCategory: String?,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
    onGoLogin: () -> Unit = {},
    initialCity: String? = null,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val filters by viewModel.filters.collectAsState()
    val query by viewModel.query.collectAsState()
    val favIds by viewModel.favIds.collectAsState()
    val paging = viewModel.properties.collectAsLazyPagingItems()
    var showFilterSheet by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(initialCategory) {
        viewModel.setInitialCategory(initialCategory)
    }
    androidx.compose.runtime.LaunchedEffect(initialCity) {
        viewModel.setInitialCity(initialCity)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            Text(
                "Jelajah",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(start = 12.dp),
            )
            Box(Modifier.weight(1f))
            // Ikon filter standar (sliders) + hit area 44dp
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { showFilterSheet = true },
                contentAlignment = Alignment.Center,
            ) {
                PhIcon(AppIcons.Tune,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onSurface,
                    size = 22.dp,
                )
            }
        }

        LapakSearchBar(
            query = query,
            onQueryChange = viewModel::setQuery,
            onSearch = { },
            onFilterClick = { showFilterSheet = true },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        // quick chips: purpose + category
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                label = "Dijual",
                selected = filters.purpose == "dijual",
                onClick = { viewModel.update { it.copy(purpose = if (it.purpose == "dijual") "disewa" else "dijual") } },
            )
            FilterChip(
                label = "Disewa",
                selected = filters.purpose == "disewa",
                onClick = { viewModel.update { it.copy(purpose = if (it.purpose == "disewa") "dijual" else "disewa") } },
            )
            PropertyCategory.entries.forEach { category ->
                FilterChip(
                    label = category.label,
                    selected = filters.category == category.apiValue,
                    onClick = {
                        viewModel.update {
                            it.copy(category = if (it.category == category.apiValue) null else category.apiValue)
                        }
                    },
                )
            }
        }

        // Jumlah hasil hanya tampil setelah refresh selesai (hindari "0 properti ditemukan" saat loading)
        if (paging.loadState.refresh is LoadState.NotLoading) {
            Text(
                text = "${paging.itemCount} properti ditemukan",
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray,
                fontWeight = FontWeight.W600,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        // Tarik ke bawah untuk muat ulang hasil pencarian
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = paging.loadState.refresh is LoadState.Loading && paging.itemCount > 0,
            onRefresh = { paging.refresh() },
        ) {
            val refresh = paging.loadState.refresh
            when {
                refresh is LoadState.Loading && paging.itemCount == 0 -> {
                    Column(Modifier.fillMaxSize()) {
                        repeat(5) { PropertyCardSkeleton() }
                    }
                }
                refresh is LoadState.Error && paging.itemCount == 0 -> {
                    EmptyState(
                        title = "Gagal memuat",
                        message = (refresh.error as? Exception)?.message ?: "Periksa koneksi internet kamu lalu coba lagi.",
                        icon = AppIcons.Wallet,
                        actionLabel = "Coba lagi",
                        onAction = { paging.retry() },
                    )
                }
                refresh is LoadState.NotLoading && paging.itemCount == 0 -> {
                    EmptyState(
                        title = "Tidak ada hasil",
                        message = "Coba ubah filter atau kata kunci pencarian.",
                        icon = AppIcons.Search,
                    )
                }
                else -> PropertyList(
                    paging = paging,
                    favIds = favIds,
                    onOpenDetail = onOpenDetail,
                    onFavoriteClick = { property -> viewModel.toggleFavorite(property) { onGoLogin() } },
                )
            }
        }
    }

    if (showFilterSheet) {
        FilterSheet(
            filters = filters,
            onApply = { showFilterSheet = false },
            onDismiss = { showFilterSheet = false },
            onCityChange = { city -> viewModel.update { it.copy(city = city) } },
            onMinPriceChange = { min -> viewModel.update { it.copy(minPrice = min) } },
            onMaxPriceChange = { max -> viewModel.update { it.copy(maxPrice = max) } },
            onAmenityToggle = { amenity ->
                viewModel.update { f ->
                    f.copy(amenities = if (amenity in f.amenities) f.amenities - amenity else f.amenities + amenity)
                }
            },
            onReset = {
                viewModel.update { f ->
                    f.copy(city = null, minPrice = null, maxPrice = null, amenities = emptySet())
                }
            },
        )
    }
}

@Composable
private fun PropertyList(
    paging: LazyPagingItems<Property>,
    favIds: Set<String>,
    onOpenDetail: (String) -> Unit,
    onFavoriteClick: (Property) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 20.dp,
            vertical = 4.dp,
        ),
    ) {
        items(
            count = paging.itemCount,
            key = { index -> paging[index]?.id ?: "index-$index" },
        ) { index ->
            val property = paging[index] ?: return@items
            PropertyListCard(
                title = property.title,
                location = listOfNotNull(property.city, property.certificate).joinToString(" · "),
                price = property.price,
                imageUrl = property.images.firstOrNull(),
                beds = property.bedrooms,
                baths = property.bathrooms,
                landArea = property.landArea,
                categoryIcon = property.category?.icon() ?: AppIcons.CategoryHouse,
                isFeatured = property.isFeatured,
                isFavorite = property.id in favIds,
                seed = property.id.hashCode(),
                onFavoriteClick = { onFavoriteClick(property) },
                onClick = { onOpenDetail(property.id) },
            )
        }
        when (paging.loadState.append) {
            is LoadState.Loading -> item { PropertyCardSkeleton() }
            is LoadState.Error -> item {
                Text(
                    "Gagal memuat halaman berikutnya — ketuk untuk coba lagi",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { paging.retry() }
                        .padding(vertical = 16.dp),
                )
            }
            else -> Unit
        }
        item { Box(Modifier.size(16.dp)) }
    }
}

private val AMENITIES = listOf("Carport", "Taman", "Kolam Renang", "Masjid", "CCTV", "One Gate System", "Bebas Banjir", "Akses Tol")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    filters: SearchFilters,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onCityChange: (String?) -> Unit,
    onMinPriceChange: (Long?) -> Unit,
    onMaxPriceChange: (Long?) -> Unit,
    onAmenityToggle: (String) -> Unit,
    onReset: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Scrollable agar tombol Reset/Terapkan terjangkau di layar pendek
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Text("Filter Pencarian", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800)

            SheetGroup("Kota") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(label = "Semua Kota", selected = filters.city == null, onClick = { onCityChange(null) })
                    CITIES.forEach { city ->
                        FilterChip(label = city, selected = filters.city == city, onClick = { onCityChange(city) })
                    }
                }
            }
            SheetGroup("Harga minimum (Rp juta)") {
                Slider(
                    // API & SearchFilters memakai rupiah penuh; slider dalam juta → ×1jt
                    value = ((filters.minPrice ?: 0L) / 1_000_000L).toFloat().coerceIn(0f, 5000f),
                    onValueChange = { onMinPriceChange(it.toLong().takeIf { v -> v > 0 }?.times(1_000_000L)) },
                    valueRange = 0f..5000f,
                )
                Text(filters.minPrice?.let { "Rp ${compactRupiah(it)}" } ?: "Tanpa batas minimum", style = MaterialTheme.typography.labelMedium, color = MutedGray)
            }
            SheetGroup("Harga maksimum (Rp juta)") {
                Slider(
                    value = ((filters.maxPrice ?: 5_000_000_000L) / 1_000_000L).toFloat().coerceIn(0f, 5000f),
                    onValueChange = { onMaxPriceChange(it.toLong().takeIf { v -> v < 5000 }?.times(1_000_000L)) },
                    valueRange = 0f..5000f,
                )
                Text(filters.maxPrice?.let { "Rp ${compactRupiah(it)}" } ?: "Tanpa batas maksimum", style = MaterialTheme.typography.labelMedium, color = MutedGray)
            }
            SheetGroup("Fasilitas") {
                // FlowRow agar semua fasilitas terlihat tanpa scroll horizontal
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AMENITIES.forEach { amenity ->
                        FilterChip(
                            label = amenity,
                            selected = amenity in filters.amenities,
                            onClick = { onAmenityToggle(amenity) },
                        )
                    }
                }
            }

            // Aksi sheet: reset semua filter + terapkan (filter live, Terapkan hanya menutup)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                ) { Text("Reset", fontWeight = FontWeight.W700) }
                Button(
                    onClick = onApply,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                ) { Text("Terapkan", fontWeight = FontWeight.W700) }
            }
        }
    }
}

@Composable
private fun SheetGroup(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 18.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W700)
        Column(Modifier.padding(top = 8.dp)) { content() }
    }
}
