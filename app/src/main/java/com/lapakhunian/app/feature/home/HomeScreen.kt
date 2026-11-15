package com.lapakhunian.app.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.designsystem.AmberBg
import com.lapakhunian.app.core.designsystem.AmberWarning
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.EmptyState
import com.lapakhunian.app.core.designsystem.components.LapakSearchBar
import com.lapakhunian.app.core.designsystem.components.PropertyCardSkeleton
import com.lapakhunian.app.core.designsystem.components.PropertyFeaturedCard
import com.lapakhunian.app.core.designsystem.components.PropertyListCard
import com.lapakhunian.app.core.designsystem.components.SectionHeader
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.domain.model.PropertyCategory
import com.lapakhunian.app.core.network.dto.BannerDto
import com.lapakhunian.app.core.util.ApiConfig
import com.lapakhunian.app.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/** Foto khas kota — Wikimedia Commons: Kalimalang Bekasi, Monas Jakarta, Balai Kota Depok, Gedung Sate Bandung (CC BY-SA). */
private data class PopularCity(val label: String, val imageRes: Int, val cityParam: String)

private val POPULAR_CITIES = listOf(
    PopularCity("Bekasi", R.drawable.city_bekasi, "Bekasi"),
    PopularCity("Jakarta", R.drawable.city_jakarta, "Jakarta"),
    PopularCity("Depok", R.drawable.city_depok, "Depok"),
    PopularCity("Bandung", R.drawable.city_bandung, "Bandung"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenExplore: (String?) -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onGoLogin: () -> Unit = {},
    onOpenCity: (String) -> Unit = {},
    onOpenTitipJual: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val favIds by viewModel.favIds.collectAsState()

    // Tarik ke bawah untuk muat ulang; indikator hanya muncul saat refresh dengan data lama masih tampil
    PullToRefreshBox(
        isRefreshing = state.loading && (state.featured.isNotEmpty() || state.latest.isNotEmpty()),
        onRefresh = { viewModel.load() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // App bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.splash_logo),
                    contentDescription = "Logo LapakHunian",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp)),
                )
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text("LapakHunian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W800)
                    Text("Bekasi & sekitarnya", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                }
                Box(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .clickable(onClick = onOpenNotifications),
                    contentAlignment = Alignment.Center,
                ) {
                    PhIcon(AppIcons.Bell, contentDescription = "Notifikasi", tint = MutedGray, modifier = Modifier.size(20.dp))
                }
            }

            if (state.offline) {
                OfflineBanner()
            }

            // Search
            LapakSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = { onOpenExplore(null) },
                onFilterClick = { onOpenExplore(null) },
                modifier = Modifier.padding(horizontal = 20.dp),
                enabled = false,
                onClick = { onOpenExplore(null) },
            )

            // Banner promo dari admin (position "homepage") — hilang bila kosong
            if (state.banners.isNotEmpty()) {
                val context = androidx.compose.ui.platform.LocalContext.current
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    items(state.banners, key = { it.id ?: it.title ?: "banner" }) { banner ->
                        BannerCard(banner) { url ->
                            openCustomTab(context, url)
                        }
                    }
                }
            }

            // CTA titip jual — konsinyasi publik tanpa login
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TintGreen)
                    .clickable(onClick = onOpenTitipJual)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhIcon(AppIcons.Sell, tint = PrimaryDark, size = 22.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        "Punya properti mau dijual?",
                        fontWeight = FontWeight.W800,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "Titip jual — tim kami survei & pasarkan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                }
                PhIcon(AppIcons.Chevron, tint = MutedGray, size = 16.dp)
            }

            if (state.loading && state.featured.isEmpty() && state.latest.isEmpty()) {
                repeat(4) { PropertyCardSkeleton() }
            } else if (state.featured.isEmpty() && state.latest.isEmpty()) {
                EmptyState(
                    title = "Belum ada properti",
                    message = "Coba lagi nanti atau periksa koneksi internet kamu.",
                    icon = AppIcons.CategoryHouse,
                )
            } else {
                // Featured carousel
                if (state.featured.isNotEmpty()) {
                    Column(Modifier.padding(top = 24.dp)) {
                        SectionHeader("Properti Unggulan", actionLabel = "Lihat semua", onAction = { onOpenExplore(null) })
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.featured, key = { it.id }) { property ->
                                PropertyFeaturedCard(
                                    title = property.title,
                                    location = property.city.orEmpty(),
                                    price = property.price,
                                    imageUrl = property.images.firstOrNull(),
                                    beds = property.bedrooms,
                                    baths = property.bathrooms,
                                    landArea = property.landArea,
                                    categoryIcon = property.category?.icon() ?: AppIcons.CategoryHouse,
                                    seed = property.id.hashCode(),
                                    onClick = { onOpenDetail(property.id) },
                                )
                            }
                        }
                    }
                }

                // Kategori
                Column(Modifier.padding(top = 24.dp)) {
                    SectionHeader("Kategori")
                    val rows = PropertyCategory.entries.chunked(3)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { category ->
                                CategoryTile(category.label, category.icon(), Modifier.weight(1f)) {
                                    onOpenExplore(category.apiValue)
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                // Kota populer
                Column(Modifier.padding(top = 24.dp)) {
                    SectionHeader("Kota Populer")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        POPULAR_CITIES.take(2).forEach { city ->
                            CityTile(city, Modifier.weight(1f)) { onOpenCity(city.cityParam) }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        POPULAR_CITIES.drop(2).forEach { city ->
                            CityTile(city, Modifier.weight(1f)) { onOpenCity(city.cityParam) }
                        }
                    }
                }

                // Terbaru
                if (state.latest.isNotEmpty()) {
                    Column(Modifier.padding(top = 24.dp)) {
                        SectionHeader("Terbaru", actionLabel = "Lihat semua", onAction = { onOpenExplore(null) })
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            state.latest.forEach { property ->
                                PropertyListCard(
                                    title = property.title,
                                    location = listOfNotNull(property.city, property.certificate).joinToString(" · "),
                                    price = property.price,
                                    imageUrl = property.images.firstOrNull(),
                                    categoryIcon = property.category?.icon() ?: AppIcons.CategoryHouse,
                                    isFavorite = property.id in favIds,
                                    seed = property.id.hashCode(),
                                    onFavoriteClick = { viewModel.toggleFavorite(property) { onGoLogin() } },
                                    onClick = { onOpenDetail(property.id) },
                                )
                            }
                        }
                    }
                }
                Box(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BannerCard(banner: BannerDto, onClick: (String) -> Unit) {
    val url = banner.linkUrl.orEmpty()
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = url.isNotBlank()) { onClick(url) },
        contentAlignment = Alignment.BottomStart,
    ) {
        AsyncImage(
            model = ApiConfig.absoluteUrl(banner.imageUrl),
            contentDescription = banner.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // scrim agar judul terbaca
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x00101828), Color(0xB3101828)))),
        )
        if (!banner.title.isNullOrBlank()) {
            Text(
                banner.title,
                color = Color.White,
                fontWeight = FontWeight.W800,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}

/** Buka link banner di Custom Tab; tanpa browser Custom Tab → fallback browser biasa. */
private fun openCustomTab(context: Context, url: String) {
    if (url.isBlank()) return
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    runCatching {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
    }.onFailure {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AmberBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhIcon(AppIcons.WifiSlash, tint = AmberWarning, size = 16.dp)
        Text(
            "Mode offline — menampilkan data tersimpan",
            style = MaterialTheme.typography.labelMedium,
            color = AmberWarning,
            fontWeight = FontWeight.W600,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun CategoryTile(
    label: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PhIcon(
            icon,
            tint = PrimaryDark,
            size = 24.dp,
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W700)
    }
}

@Composable
private fun CityTile(city: PopularCity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                // fallback gradient kalau gambar gagal dimuat
                Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF0F9C4B))),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        Image(
            painter = painterResource(city.imageRes),
            contentDescription = "Kota ${city.label}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // scrim agar teks terbaca
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0x00101828), Color(0xB3101828))),
                ),
        )
        Text(
            city.label,
            color = Color.White,
            fontWeight = FontWeight.W800,
            fontSize = 15.sp,
            modifier = Modifier.padding(14.dp),
        )
    }
}

private fun PropertyCategory.icon(): String = when (this) {
    PropertyCategory.HOUSE -> AppIcons.CategoryHouse
    PropertyCategory.APARTMENT -> AppIcons.CategoryApartment
    PropertyCategory.OFFICE -> AppIcons.CategoryOffice
    PropertyCategory.LAND -> AppIcons.CategoryLand
    PropertyCategory.COMMERCIAL -> AppIcons.CategoryCommercial
    PropertyCategory.TOWNHOME -> AppIcons.CategoryTownhome
}
