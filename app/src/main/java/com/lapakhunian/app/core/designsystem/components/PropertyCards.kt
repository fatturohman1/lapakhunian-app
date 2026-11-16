package com.lapakhunian.app.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.FavoriteRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PhosphorWeight
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.util.formatRupiah

/**
 * Kartu properti — 2 varian sesuai preview M0:
 * - [PropertyListCard]: thumb 94dp, dipakai di list hasil pencarian
 * - [PropertyFeaturedCard]: gambar lebar 160dp, dipakai carousel Unggulan
 *
 * imageUrl null → placeholder gradient + icon kategori Phosphor.
 */
private val GradientPlaceholders = listOf(
    Brush.linearGradient(listOf(Color(0xFF6EE7B7), Color(0xFF10B981))),
    Brush.linearGradient(listOf(Color(0xFF93C5FD), Color(0xFF3B82F6))),
    Brush.linearGradient(listOf(Color(0xFFFCD34D), Color(0xFFF59E0B))),
    Brush.linearGradient(listOf(Color(0xFFF9A8D4), Color(0xFFEC4899))),
    Brush.linearGradient(listOf(Color(0xFFC4B5FD), Color(0xFF8B5CF6))),
    Brush.linearGradient(listOf(Color(0xFF5EEAD4), Color(0xFF14B8A6))),
)

private fun placeholderBrush(seed: Int): Brush =
    GradientPlaceholders[(seed % GradientPlaceholders.size + GradientPlaceholders.size) % GradientPlaceholders.size]

@Composable
private fun ThumbImage(
    imageUrl: String?,
    icon: String,
    seed: Int,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(placeholderBrush(seed)),
                contentAlignment = Alignment.Center,
            ) {
                PhIcon(
                    name = icon,
                    contentDescription = contentDescription,
                    tint = Color.White.copy(alpha = 0.92f),
                    size = iconSize,
                )
            }
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Kartu ringkas untuk baris "Terakhir dilihat" di Beranda. */
@Composable
fun PropertyRecentCard(
    title: String,
    price: Long,
    imageUrl: String?,
    seed: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        ThumbImage(
            imageUrl = imageUrl,
            icon = AppIcons.CategoryHouse,
            seed = seed,
            iconSize = 24.dp,
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W700,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            formatRupiah(price),
            color = PrimaryDark,
            fontWeight = FontWeight.W800,
            fontSize = 12.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun PropertyListCard(
    title: String,
    location: String,
    price: Long,
    imageUrl: String?,
    beds: Int? = null,
    baths: Int? = null,
    landArea: Int? = null,
    categoryIcon: String = AppIcons.CategoryHouse,
    isFeatured: Boolean = false,
    isFavorite: Boolean = false,
    seed: Int = 0,
    onFavoriteClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    compareMode: Boolean = false,
    compareSelected: Boolean = false,
    onCompareToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = {
                if (compareMode && onCompareToggle != null) onCompareToggle() else onClick()
            }),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    ThumbImage(
                        imageUrl = imageUrl,
                        icon = categoryIcon,
                        seed = seed,
                        iconSize = 28.dp,
                        contentDescription = title,
                        modifier = Modifier
                            .width(90.dp)
                            .height(90.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                    if (compareMode) {
                        PhIcon(
                            name = AppIcons.CheckCircle,
                            weight = PhosphorWeight.FILL,
                            tint = if (compareSelected) PrimaryDark else Color.White,
                            size = 24.dp,
                            contentDescription = if (compareSelected) "Terpilih untuk bandingkan" else "Pilih untuk bandingkan",
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(if (compareSelected) Color.White else Color(0x66000000)),
                        )
                    }
                    if (isFeatured) {
                        Text(
                            text = "UNGGULAN",
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            color = PrimaryDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 13.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W700,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 3.dp),
                    ) {
                        PhIcon(AppIcons.Location, tint = MutedGray, size = 13.dp)
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                    val specs = buildList {
                        beds?.let { add("${it} KT") }
                        baths?.let { add("${it} KM") }
                        landArea?.let { add("LT $it") }
                    }
                    if (specs.isNotEmpty()) {
                        Text(
                            text = specs.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                            fontWeight = FontWeight.W600,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onFavoriteClick?.invoke() },
                    contentAlignment = Alignment.Center,
                ) {
                    PhIcon(
                        name = AppIcons.Favorite,
                        contentDescription = if (isFavorite) "Hapus dari favorit" else "Tambah ke favorit",
                        tint = if (isFavorite) FavoriteRed else MutedGray,
                        weight = if (isFavorite) PhosphorWeight.FILL else PhosphorWeight.REGULAR,
                        size = 22.dp,
                    )
                }
            }
            Text(
                text = formatRupiah(price),
                color = PrimaryDark,
                fontWeight = FontWeight.W800,
                fontSize = 13.5.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
fun PropertyFeaturedCard(
    title: String,
    location: String,
    price: Long,
    imageUrl: String?,
    beds: Int?,
    baths: Int?,
    landArea: Int?,
    categoryIcon: String = AppIcons.CategoryHouse,
    seed: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(272.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(272f / 160f),
            ) {
                ThumbImage(
                    imageUrl = imageUrl,
                    icon = categoryIcon,
                    seed = seed,
                    iconSize = 46.dp,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = "UNGGULAN",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(50))
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    color = PrimaryDark,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.5.sp,
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                    modifier = Modifier.padding(top = 3.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatRupiah(price),
                    color = PrimaryDark,
                    fontWeight = FontWeight.W800,
                    fontSize = 16.5.sp,
                    modifier = Modifier.padding(top = 7.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    beds?.let { SpecChip(AppIcons.Bed, "$it KT") }
                    baths?.let { SpecChip(AppIcons.Bath, "$it KM") }
                    landArea?.let { SpecChip(AppIcons.LandArea, "LT $it") }
                }
            }
        }
    }
}

@Composable
private fun SpecChip(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PhIcon(icon, tint = MutedGray, size = 15.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
            fontWeight = FontWeight.W600,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Avatar lingkaran inisial untuk agen/user (fallback sebelum foto termuat). */
@Composable
fun InitialAvatar(initials: String, size: Int = 48, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.take(2).uppercase(),
            color = PrimaryDark,
            fontWeight = FontWeight.W800,
            fontSize = (size * 0.34f).sp,
        )
    }
}
