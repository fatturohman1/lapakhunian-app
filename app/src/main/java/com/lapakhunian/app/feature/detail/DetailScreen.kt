package com.lapakhunian.app.feature.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.FavoriteRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.EmptyState
import com.lapakhunian.app.core.designsystem.components.InitialAvatar
import com.lapakhunian.app.core.designsystem.components.SectionHeader
import com.lapakhunian.app.core.designsystem.components.SpecGrid
import com.lapakhunian.app.core.designsystem.components.SpecItem
import com.lapakhunian.app.core.designsystem.components.StaticChip
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PhosphorWeight
import com.lapakhunian.app.core.util.formatRupiah

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onContact: (String) -> Unit,
    onGoLogin: () -> Unit = {},
    onOpenKpr: (Long) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val property = state.property
    var showReport by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    val onShare: (() -> Unit)? = property?.let { p ->
        {
            // web membuka detail lewat slug: /properti/[slug] — id saja 404
            val link = "https://lapakhunian.com/properti/${p.slug ?: p.id}"
            val text = "${p.title} — ${formatRupiah(p.price)}\n$link"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            runCatching { context.startActivity(Intent.createChooser(sendIntent, null)) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && property == null -> {
                Column(Modifier.fillMaxSize()) {
                    TopBarOverlay(onBack = onBack)
                    EmptyState(title = "Gagal memuat", message = state.error ?: "Coba lagi")
                }
            }
            property != null -> {
                val images: List<String?> = if (property.images.isEmpty()) listOf(null) else property.images
                val pagerState = rememberPagerState { images.size }
                var showViewer by remember { mutableStateOf(false) }
                val isRent = property.purpose == "disewa" || property.purpose == "sewa"
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Image pager
                    Box {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.aspectRatio(390f / 290f),
                        ) { page ->
                            val image = images[page]
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (image != null) {
                                    AsyncImage(
                                        model = image,
                                        contentDescription = property.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { showViewer = true },
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(listOf(Color(0xFF6EE7B7), Color(0xFF10B981))),
                                            ),
                                    )
                                }
                            }
                        }
                        // Sembunyikan badge foto kalau properti tidak punya gambar
                        if (property.images.isNotEmpty()) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${images.size} foto",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.W600,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .background(Color(0x8C0D141E), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    Column(Modifier.padding(20.dp)) {
                        Text(
                            property.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.W800,
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Text(
                                formatRupiah(property.price),
                                style = MaterialTheme.typography.headlineSmall,
                                color = PrimaryDark,
                                fontWeight = FontWeight.W800,
                            )
                            if (isRent) {
                                Text(
                                    " /bulan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedGray,
                                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                                )
                            }
                        }
                        if (!isRent) {
                            // Pill KPR: latar tint agar terlihat bisa diketuk
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(TintGreen)
                                    .clickable { onOpenKpr(property.price) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                PhIcon(AppIcons.Calculator, tint = PrimaryDark, size = 14.dp)
                                Text(
                                    "Simulasi KPR",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.W700,
                                    color = PrimaryDark,
                                    modifier = Modifier.padding(start = 5.dp),
                                )
                            }
                        }
                        val addressLine = listOfNotNull(property.address, property.city).joinToString(", ")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 6.dp),
                        ) {
                            PhIcon(AppIcons.Location, contentDescription = null, tint = MutedGray, modifier = Modifier.size(15.dp))
                            Text(
                                addressLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }

                        // tags
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            property.purpose?.let { StaticChip(if (it == "jual" || it == "dijual") "Dijual" else "Disewa") }
                            property.category?.let { StaticChip(it.label) }
                            property.certificate?.let { StaticChip(it.uppercase()) }
                            if (property.isSold) StaticChip("Terjual")
                        }

                        SpecGrid(
                            modifier = Modifier.padding(top = 16.dp),
                            items = buildList {
                                property.bedrooms?.let { add(SpecItem(AppIcons.Bed, "$it", "Kamar Tidur")) }
                                property.bathrooms?.let { add(SpecItem(AppIcons.Bath, "$it", "Kamar Mandi")) }
                                property.landArea?.let { add(SpecItem(AppIcons.LandArea, "$it", "Luas Tanah")) }
                                property.buildingArea?.let { add(SpecItem(AppIcons.BuildingArea, "$it", "Luas Bangunan")) }
                                property.floors?.let { add(SpecItem(AppIcons.Floors, "$it", "Lantai")) }
                                property.electricity?.let { add(SpecItem(AppIcons.Sell, it, "Listrik (VA)")) }
                                property.carport?.let { add(SpecItem(AppIcons.Carport, "$it", "Carport")) }
                                property.yearBuilt?.let { add(SpecItem(AppIcons.Calendar, "$it", "Dibangun")) }
                            },
                        )

                        if (property.amenities.isNotEmpty()) {
                            SectionHeader("Fasilitas", modifier = Modifier.padding(top = 24.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                property.amenities.forEach { StaticChip(it) }
                            }
                        }

                        if (!property.description.isNullOrBlank()) {
                            SectionHeader("Deskripsi", modifier = Modifier.padding(top = 24.dp))
                            Text(
                                property.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedGray,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        // Map hanya di web — app cukup alamat + chips; ketuk alamat nanti via fitur terpisah bila perlu

                        SectionHeader("Dipasang oleh", modifier = Modifier.padding(top = 24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            InitialAvatar(initials = (property.agentName ?: "A").take(2), size = 48)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
                            ) {
                                Text(property.agentName ?: "Pemilik", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W800)
                                property.agentRating?.let {
                                    Text("★ $it", style = MaterialTheme.typography.labelMedium, color = MutedGray)
                                }
                            }
                            Text(
                                if (property.listedBy == "agent") "Listing Agen" else "Listing Pemilik",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryDark,
                                fontWeight = FontWeight.W700,
                                modifier = Modifier
                                    .background(TintGreen, RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                        Text(
                            "Nomor agen terjaga — chat dulu lewat app, aman & tercatat.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Spacer(Modifier.height(110.dp))
                    }
                }

                // Sticky CTA — scrim gradasi di atasnya agar konten yang ter-scroll tidak menabrak mentok
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0f), MaterialTheme.colorScheme.surface),
                                ),
                            )
                            .align(Alignment.TopStart)
                            .offset(y = (-28).dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            formatRupiah(property.price) + if (isRent) " /bulan" else "",
                            color = PrimaryDark,
                            fontWeight = FontWeight.W800,
                            fontSize = 14.5.sp,
                            maxLines = 1,
                        )
                        Text(
                            listOfNotNull(property.bedrooms?.let { "$it KT" }, property.bathrooms?.let { "$it KM" }, property.landArea?.let { "LT $it" })
                                .joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray,
                            fontWeight = FontWeight.W600,
                        )
                    }
                    Button(
                        onClick = { onContact(property.id) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        PhIcon(AppIcons.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Hubungi Agen", modifier = Modifier.padding(start = 8.dp))
                    }
                    }
                }

                if (showViewer) {
                    PhotoViewer(
                        images = images,
                        initialPage = pagerState.currentPage,
                        contentDescription = property.title,
                        onDismiss = { showViewer = false },
                    )
                }
            }
        }

        // back + share + favorite overlay selalu di atas
        TopBarOverlay(
            onBack = onBack,
            isFavorite = state.isFavorite,
            onFavoriteClick = { viewModel.toggleFavorite() },
            onShare = onShare,
            onReport = {
                showReport = true
            },
        )
    }

    // Dialog lapor properti
    if (showReport) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text("Lapor properti ini?", fontWeight = FontWeight.W800) },
            text = {
                Column {
                    Text(
                        "Laporkan iklan palsu/menyesatkan — direview admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Alasan (min. 10 karakter)") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = reportReason.trim().length >= 10 && !state.reporting,
                    onClick = {
                        viewModel.reportProperty(reportReason) { res ->
                            showReport = false
                            reportReason = ""
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "Laporan terkirim — terima kasih", Toast.LENGTH_LONG).show()
                                },
                                onFailure = { e ->
                                    if (e.message?.contains("login", ignoreCase = true) == true) {
                                        onGoLogin()
                                    } else {
                                        Toast.makeText(context, e.message ?: "Gagal mengirim laporan", Toast.LENGTH_LONG).show()
                                    }
                                },
                            )
                        }
                    },
                ) { Text("Kirim") }
            },
            dismissButton = { TextButton(onClick = { showReport = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun TopBarOverlay(
    onBack: () -> Unit,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.White, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            PhIcon(AppIcons.Back, contentDescription = "Kembali", tint = Color(0xFF101828), modifier = Modifier.size(20.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onReport != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White, CircleShape)
                        .clickable(onClick = onReport),
                    contentAlignment = Alignment.Center,
                ) {
                    PhIcon(AppIcons.Flag, contentDescription = "Lapor properti", tint = Color(0xFF101828), modifier = Modifier.size(20.dp))
                }
            }
            if (onShare != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White, CircleShape)
                        .clickable(onClick = onShare),
                    contentAlignment = Alignment.Center,
                ) {
                    PhIcon(AppIcons.Share, contentDescription = "Bagikan", tint = Color(0xFF101828), modifier = Modifier.size(20.dp))
                }
            }
            if (onFavoriteClick != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White, CircleShape)
                        .clickable(onClick = onFavoriteClick),
                    contentAlignment = Alignment.Center,
                ) {
                    PhIcon(
                        AppIcons.Favorite,
                        contentDescription = if (isFavorite) "Hapus dari favorit" else "Tambah ke favorit",
                        tint = if (isFavorite) FavoriteRed else Color(0xFF101828),
                        weight = if (isFavorite) PhosphorWeight.FILL else PhosphorWeight.REGULAR,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
