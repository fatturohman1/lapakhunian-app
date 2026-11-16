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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
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
import com.lapakhunian.app.core.util.formatRupiah

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
    onOpenCompare: (String) -> Unit = {},
    initialCity: String? = null,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val filters by viewModel.filters.collectAsState()
    val query by viewModel.query.collectAsState()
    val favIds by viewModel.favIds.collectAsState()
    val compareSelection by viewModel.compareSelection.collectAsState()
    var compareMode by remember { mutableStateOf(false) }
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
            // Mode bandingkan: pilih 2-3 kartu lalu buka layar pembanding
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (compareMode) TintGreen else Color.Transparent)
                    .clickable {
                        compareMode = !compareMode
                        if (!compareMode) viewModel.clearCompare()
                    },
                contentAlignment = Alignment.Center,
            ) {
                PhIcon(
                    AppIcons.Compare,
                    contentDescription = "Mode bandingkan",
                    tint = if (compareMode) PrimaryDark else MaterialTheme.colorScheme.onSurface,
                    size = 22.dp,
                )
            }
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
                onClick = { viewModel.update { it.copy(purpose = "dijual") } },
            )
            FilterChip(
                label = "Disewa",
                selected = filters.purpose == "disewa",
                onClick = { viewModel.update { it.copy(purpose = "disewa") } },
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

        val filterSummary = listOfNotNull(
            filters.city,
            filters.minPrice?.let { "Min ${formatRupiah(it)}" },
            filters.maxPrice?.let { "Maks ${formatRupiah(it)}" },
            filters.amenities.takeIf { it.isNotEmpty() }?.joinToString(", "),
        ).joinToString(" · ")
        if (filterSummary.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(filterSummary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = viewModel::resetFilters) { Text("Hapus filter") }
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
            modifier = Modifier.weight(1f),
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
                    onFavoriteClick = { property -> viewModel.toggleFavorite(property) },
                    compareMode = compareMode,
                    selectedIds = compareSelection.map { it.id }.toSet(),
                    onToggleCompare = viewModel::toggleCompare,
                )
            }
        }

        if (compareMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.TextButton(
                    onClick = {
                        compareMode = false
                        viewModel.clearCompare()
                    },
                ) { Text("Batal") }
                Box(Modifier.weight(1f))
                androidx.compose.material3.Button(
                    enabled = compareSelection.size >= 2,
                    onClick = {
                        onOpenCompare(compareSelection.joinToString(",") { it.id })
                        viewModel.clearCompare()
                        compareMode = false
                    },
                    shape = CircleShape,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("Bandingkan (${compareSelection.size})", fontWeight = FontWeight.W700)
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterSheet(
            filters = filters,
            onApply = { draft ->
                viewModel.applyFilters(draft)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Composable
private fun PropertyList(
    paging: LazyPagingItems<Property>,
    favIds: Set<String>,
    onOpenDetail: (String) -> Unit,
    onFavoriteClick: (Property) -> Unit,
    compareMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleCompare: (Property) -> Unit = {},
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
                compareMode = compareMode,
                compareSelected = property.id in selectedIds,
                onCompareToggle = { onToggleCompare(property) },
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
internal fun FilterSheet(
    filters: SearchFilters,
    onApply: (SearchFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(filters) { mutableStateOf(filters) }
    var minimum by remember(filters) { mutableStateOf(filters.minPrice?.toString().orEmpty()) }
    var maximum by remember(filters) { mutableStateOf(filters.maxPrice?.toString().orEmpty()) }
    val minPrice = minimum.toLongOrNull()
    val maxPrice = maximum.toLongOrNull()
    val invalidNumber = (minimum.isNotEmpty() && minPrice == null) || (maximum.isNotEmpty() && maxPrice == null)
    val invalidRange = minPrice != null && maxPrice != null && minPrice > maxPrice
    val valid = !invalidNumber && !invalidRange

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filter Pencarian", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Batal") }
            }
            SheetGroup("Kota") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip("Semua Kota", draft.city == null, { draft = draft.copy(city = null) })
                    CITIES.forEach { city ->
                        FilterChip(city, draft.city == city, { draft = draft.copy(city = city) })
                    }
                }
            }
            SheetGroup("Rentang harga") {
                androidx.compose.material3.OutlinedTextField(
                    value = minimum,
                    onValueChange = { minimum = it.filter { char -> char in '0'..'9' } },
                    label = { Text("Harga minimum (Rp)") },
                    supportingText = { Text(minPrice?.let { formatRupiah(it) } ?: "Tanpa batas minimum") },
                    isError = invalidNumber || invalidRange,
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.material3.OutlinedTextField(
                    value = maximum,
                    onValueChange = { maximum = it.filter { char -> char in '0'..'9' } },
                    label = { Text("Harga maksimum (Rp)") },
                    supportingText = { Text(maxPrice?.let { formatRupiah(it) } ?: "Tanpa batas maksimum") },
                    isError = invalidNumber || invalidRange,
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!valid) {
                    Text(
                        if (invalidNumber) "Nominal harga terlalu besar." else "Harga minimum tidak boleh melebihi maksimum.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            SheetGroup("Fasilitas") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AMENITIES.forEach { amenity ->
                        FilterChip(amenity, amenity in draft.amenities, {
                            draft = draft.copy(amenities = if (amenity in draft.amenities) draft.amenities - amenity else draft.amenities + amenity)
                        })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { draft = SearchFilters(); minimum = ""; maximum = "" },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                ) { Text("Reset", fontWeight = FontWeight.W700) }
                Button(
                    onClick = { if (valid) onApply(draft.copy(minPrice = minPrice, maxPrice = maxPrice)) },
                    enabled = valid,
                    shape = RoundedCornerShape(50),
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
