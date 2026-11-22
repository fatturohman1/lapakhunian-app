package com.lapakhunian.app.feature.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import android.net.Uri
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.designsystem.components.EmptyState
import com.lapakhunian.app.core.designsystem.components.InitialAvatar
import com.lapakhunian.app.core.designsystem.components.StatusBadge
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PhosphorWeight
import com.lapakhunian.app.core.domain.model.ListingStatus
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.domain.model.PropertyCategory
import com.lapakhunian.app.core.util.compactRupiah
import com.lapakhunian.app.core.util.formatRupiah

private val Green = Color(0xFF15A851)
private val GreenGradient = com.lapakhunian.app.core.designsystem.GreenGradient

@Composable
private fun DashTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackIconButton(onClick = onBack)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun NeedsLogin(onGoLogin: () -> Unit) {
    EmptyState(title = "Masuk dulu ya", message = "Fitur ini butuh akun LapakHunian.", icon = AppIcons.Person)
    Button(
        onClick = onGoLogin,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) { Text("Masuk", fontWeight = FontWeight.W700) }
}

// ================= OWNER DASHBOARD =================

@Composable
fun OwnerDashboardScreen(
    onBack: () -> Unit,
    onOpenWizard: () -> Unit,
    onGoLogin: () -> Unit,
    onRenew: (String) -> Unit,
    onEditListing: (String) -> Unit,
    viewModel: OwnerDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        when {
            state.needsLogin -> { NeedsLogin(onGoLogin) }
            else -> {
                val active = state.items.count { it.status == ListingStatus.APPROVED }
                val pending = state.items.count { it.status == ListingStatus.PENDING }
                val views = state.items.sumOf { it.viewCount }
                OwnerHero(
                    active = "$active",
                    review = "$pending",
                    views = "$views",
                    onBack = onBack,
                    onOpenWizard = onOpenWizard,
                )
                when {
                    state.loading -> Column(Modifier.fillMaxSize()) {
                        repeat(3) { com.lapakhunian.app.core.designsystem.components.PropertyCardSkeleton() }
                    }
                    state.error != null && state.items.isEmpty() -> EmptyState(
                        title = "Gagal memuat",
                        message = state.error.orEmpty(),
                        actionLabel = "Coba lagi",
                        onAction = { viewModel.retry() },
                    )
                    state.items.isEmpty() -> EmptyState(
                        title = "Belum ada iklan",
                        message = "Pasang iklan pertamamu — gratis 30 hari.",
                        icon = AppIcons.PlusCircle,
                    )
                    else -> {
                        state.error?.let {
                            Text(it, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                        ) {
                            item(key = "section-head") {
                                com.lapakhunian.app.core.designsystem.components.SectionHeader("Properti Saya · ${state.items.size}")
                            }
                            items(state.items, key = { it.id }) { property ->
                                Box(Modifier.padding(horizontal = 20.dp)) {
                                    OwnerPropertyItem(
                                        property = property,
                                        onSold = { viewModel.markSold(property.id) },
                                        onEdit = { onEditListing(property.id) },
                                        onDelete = { pendingDeleteId = property.id },
                                        onRenew = { onRenew(property.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Konfirmasi hapus — hindari hapus tanpa sengaja
    pendingDeleteId?.let { id ->
        val target = state.items.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Hapus iklan?", fontWeight = FontWeight.W800) },
            text = { Text(target?.title ?: "Iklan ini akan dihapus permanen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(id)
                        pendingDeleteId = null
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                ) { Text("Hapus", fontWeight = FontWeight.W700) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Batal", color = MutedGray) }
            },
        )
    }
}

@Composable
fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedGray)
    }
}

/** Hero gradien dashboard owner: back + judul, ringkasan statistik, CTA Pasang Iklan. */
@Composable
private fun OwnerHero(
    active: String,
    review: String,
    views: String,
    onBack: () -> Unit,
    onOpenWizard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GreenGradient)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                PhIcon(AppIcons.Back, contentDescription = "Kembali", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text("Dashboard Saya", fontSize = 20.sp, fontWeight = FontWeight.W800, color = Color.White)
                Text("Kelola iklan propertimu", fontSize = 12.sp, fontWeight = FontWeight.W600, color = Color.White.copy(alpha = 0.75f))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroStat(active, "Aktif", Modifier.weight(1f))
            HeroDivider()
            HeroStat(review, "Review", Modifier.weight(1f))
            HeroDivider()
            HeroStat(views, "Dilihat", Modifier.weight(1f))
        }
        Button(
            onClick = onOpenWizard,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(48.dp),
        ) {
            PhIcon(AppIcons.Sell, contentDescription = null, size = 18.dp, tint = PrimaryDark, weight = PhosphorWeight.FILL)
            Text("Pasang Iklan", fontWeight = FontWeight.W800, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.W800, color = Color.White)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.W600, color = Color.White.copy(alpha = 0.75f))
    }
}

@Composable
private fun HeroDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(Color.White.copy(alpha = 0.2f)),
    )
}

@Composable
private fun OwnerPropertyItem(
    property: com.lapakhunian.app.core.domain.model.Property,
    onSold: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRenew: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TintGreen),
        ) {
            AsyncImage(
                model = property.images.firstOrNull(),
                contentDescription = property.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(property.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W700, maxLines = 2, overflow = TextOverflow.Ellipsis)
            property.city?.let { city ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp),
                ) {
                    PhIcon(AppIcons.Location, contentDescription = null, size = 13.dp, tint = MutedGray)
                    Text(city, fontSize = 12.sp, fontWeight = FontWeight.W600, color = MutedGray, modifier = Modifier.padding(start = 4.dp))
                }
            }
            val effectiveStatus = if (property.isSold) ListingStatus.SOLD else property.status ?: ListingStatus.APPROVED
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                StatusBadge(status = effectiveStatus)
                Spacer(Modifier.weight(1f))
                Text(compactRupiah(property.price), fontSize = 13.sp, fontWeight = FontWeight.W800, color = PrimaryDark)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 5.dp),
            ) {
                PhIcon(AppIcons.Eye, contentDescription = null, size = 13.dp, tint = MutedGray)
                Text(
                    "${property.viewCount} dilihat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            // Aksi per status — mengikuti referensi HTML (scr-owner):
            // tayang → Edit/Terjual/Hapus; review → Edit; kedaluwarsa → Perpanjang; terjual/ditolak → tanpa aksi.
            if (effectiveStatus != ListingStatus.SOLD && effectiveStatus != ListingStatus.REJECTED) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (effectiveStatus) {
                        ListingStatus.EXPIRED -> MiniAction("Perpanjang", TintGreen, PrimaryDark, onRenew)
                        ListingStatus.PENDING, ListingStatus.PENDING_PAYMENT ->
                            MiniAction("Edit", MaterialTheme.colorScheme.surfaceVariant, PrimaryDark, onEdit)
                        else -> {
                            MiniAction("Edit", MaterialTheme.colorScheme.surfaceVariant, PrimaryDark, onEdit)
                            MiniAction("Terjual", Color(0xFFE7F7EE), PrimaryDark, onSold)
                            MiniAction("Hapus", Color(0xFFFEE2E2), ErrorRed, onDelete)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAction(label: String, bg: Color, fg: Color, onClick: () -> Unit, enabled: Boolean = true) {
    Text(
        label,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.W700,
        color = fg.copy(alpha = if (enabled) 1f else 0.45f),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg.copy(alpha = if (enabled) 1f else 0.45f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

// ================= LISTING WIZARD =================

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ListingWizardScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    viewModel: ListingWizardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    // multi-pilih galeri: sisa slot dikirim sebagai batas agar tidak melebihi 10 foto
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.uploadManyFromUri(context, uris)
        }
    }

    val handleBack: () -> Unit = {
        if (state.step > 1) viewModel.setStep(state.step - 1) else onBack()
    }
    androidx.activity.compose.BackHandler(onBack = handleBack)
    val steps = listOf("Informasi", "Detail", "Foto", "Harga")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DashTopBar("Pasang Iklan", handleBack)
        WizardStepHeader(steps = steps, current = state.step)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            if (state.loading) {
                // Prefill listing (mode edit) masih berjalan
                EmptyState(title = "Memuat iklan…", message = "Mengambil data listing.", icon = AppIcons.Clock)
            } else when (state.step) {
                1 -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WizardChip("Dijual", state.purpose == "dijual", modifier = Modifier.weight(1f)) { viewModel.update { it.copy(purpose = "dijual") } }
                        WizardChip("Disewa", state.purpose == "disewa", modifier = Modifier.weight(1f)) { viewModel.update { it.copy(purpose = "disewa") } }
                    }
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("Judul Iklan", fontWeight = FontWeight.W700, fontSize = 13.sp)
                                Text(
                                    "(maks 100 karakter)",
                                    fontSize = 11.sp,
                                    color = MutedGray,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                            Text(
                                "${state.title.length}/100",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.W700,
                                color = if (state.title.length > 90) Color(0xFFB45309) else MutedGray,
                            )
                        }
                        OutlinedTextField(
                            value = state.title.take(100),
                            onValueChange = { v -> viewModel.update { it.copy(title = v.take(100)) } },
                            placeholder = { Text("cth: Rumah Minimalis 2 Lantai", color = MutedGray) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text("Kategori", fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        listOf(
                            "house" to "Rumah",
                            "apartment" to "Apartemen",
                            "townhome" to "Townhouse",
                            "commercial" to "Ruko",
                            "land" to "Tanah",
                            "office" to "Office",
                        ).forEach { (v, l) ->
                            WizardChip(l, state.category == v) { viewModel.update { it.copy(category = v) } }
                        }
                    }
                    WizardField("Deskripsi", state.description, { v -> viewModel.update { it.copy(description = v) } }, Modifier.padding(top = 16.dp), minLines = 4)
                }
                2 -> {
                    Text(
                        "Spesifikasi properti — isi sesuai kondisi sebenarnya.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WizardFieldTop("Luas Tanah (m²)", state.landArea, { v -> viewModel.update { it.copy(landArea = v.filter { ch -> ch.isDigit() }) } }, Modifier.weight(1f))
                        WizardFieldTop("Luas Bangunan (m²)", state.buildingArea, { v -> viewModel.update { it.copy(buildingArea = v.filter { ch -> ch.isDigit() }) } }, Modifier.weight(1f))
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WizardFieldTop("Kamar Tidur", state.bedrooms, { v -> viewModel.update { it.copy(bedrooms = v.filter { ch -> ch.isDigit() }) } }, Modifier.weight(1f))
                        WizardFieldTop("Kamar Mandi", state.bathrooms, { v -> viewModel.update { it.copy(bathrooms = v.filter { ch -> ch.isDigit() }) } }, Modifier.weight(1f))
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WizardFieldTop("Lantai", state.floors, { v -> viewModel.update { it.copy(floors = v.filter { ch -> ch.isDigit() }) } }, Modifier.weight(1f))
                        WizardFieldTop("Listrik (VA)", state.electricity, { v -> viewModel.update { it.copy(electricity = v.filter { ch -> ch.isDigit() }) } }, Modifier.weight(1f))
                    }
                    Text("Sertifikat", fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SHM", "SHGB", "SHMSRS", "AJB").forEach { cert ->
                            WizardChip(cert, state.certificate == cert) { viewModel.update { it.copy(certificate = cert) } }
                        }
                    }
                    Text("Furnishing", fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Unfurnished" to "Kosong", "Semi Furnished" to "Semi Furnish", "Full Furnished" to "Full Furnish").forEach { (v, l) ->
                            WizardChip(l, state.furnishing == v) { viewModel.update { it.copy(furnishing = v) } }
                        }
                    }
                    Text("Fasilitas", fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        listOf("Carport", "Taman", "Kolam Renang", "Masjid", "CCTV", "One Gate System", "Bebas Banjir", "Gym").forEach { amenity ->
                            WizardChip(amenity, amenity in state.amenities) {
                                viewModel.update { s ->
                                    s.copy(amenities = if (amenity in s.amenities) s.amenities - amenity else s.amenities + amenity)
                                }
                            }
                        }
                    }
                }
                3 -> {
                    val atLimitCount = state.uploadedPhotos.size >= 10
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Ketuk foto untuk menjadikan utama (thumbnail). Maks 10 foto, ≤10MB per foto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${state.uploadedPhotos.size}/10",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W800,
                            color = if (atLimitCount) Color(0xFFB45309) else PrimaryDark,
                        )
                    }

                    // Sumber foto: kamera langsung atau galeri multi-pilih
                    val atLimit = state.uploadedPhotos.size >= 10
                    val cameraUriState = remember { mutableStateOf<android.net.Uri?>(null) }
                    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                        if (success) {
                            cameraUriState.value?.let { uri -> viewModel.uploadFromUri(context, uri) }
                        }
                    }
                    fun launchCamera() {
                        if (atLimit || state.uploading) return
                        val imagesDir = java.io.File(context.cacheDir, "images").apply { mkdirs() }
                        val file = java.io.File(imagesDir, "capture-${System.currentTimeMillis()}.jpg")
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.packageName + ".fileprovider",
                            file,
                        )
                        cameraUriState.value = uri
                        cameraLauncher.launch(uri)
                    }
                    fun launchGallery() {
                        if (atLimit || state.uploading) return
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { launchCamera() },
                            enabled = !atLimit && !state.uploading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            PhIcon(AppIcons.Camera, tint = PrimaryDark, size = 15.dp)
                            Text("Kamera", fontWeight = FontWeight.W700, modifier = Modifier.padding(start = 6.dp))
                        }
                        androidx.compose.material3.OutlinedButton(
                            onClick = { launchGallery() },
                            enabled = !atLimit && !state.uploading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            PhIcon(AppIcons.Image, tint = PrimaryDark, size = 15.dp)
                            Text("Galeri", fontWeight = FontWeight.W700, modifier = Modifier.padding(start = 6.dp))
                        }
                    }

                    if (state.uploading) {
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        )
                    }

                    // Grid foto: kosong → placeholder; foto pertama badge UTAMA
                    if (state.uploadedPhotos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .aspectRatio(1.6f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PhIcon(AppIcons.Plus, tint = MutedGray, size = 26.dp)
                                Text(
                                    if (state.uploading) "Mengunggah…" else "Belum ada foto",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.W700,
                                    color = MutedGray,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                    state.uploadedPhotos.chunked(3).forEachIndexed { rowIndex, rowPhotos ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (rowIndex == 0) 14.dp else 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            repeat(3) { col ->
                                val index = rowIndex * 3 + col
                                val url = state.uploadedPhotos.getOrNull(index)
                                if (url != null) {
                                    // ketuk foto → jadikan utama (pindah ke urutan pertama = thumbnail)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clickable { viewModel.setCoverPhoto(url) },
                                    ) {
                                        AsyncImage(
                                            model = com.lapakhunian.app.core.util.ApiConfig.absoluteUrl(url),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(14.dp)),
                                        )
                                        if (index == 0) {
                                            Text(
                                                "UTAMA",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.W800,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(6.dp)
                                                    .background(Color(0xCC0C8A44), RoundedCornerShape(50))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                            )
                                        } else {
                                            // indikator: ketuk untuk jadikan utama
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(6.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x990D141E)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                PhIcon(AppIcons.Star, tint = Color.White, size = 13.dp)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(ErrorRed)
                                                .clickable { viewModel.removePhoto(url) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("×", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.W800)
                                        }
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    if (atLimit) {
                        Text(
                            "Sudah 10 foto — hapus salah satu bila ingin mengganti.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W700,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    state.error?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp)) }
                }
                4 -> {
                    WizardFieldTop("Alamat Lengkap", state.address, { v -> viewModel.update { it.copy(address = v) } }, Modifier.padding(top = 16.dp))
                    WizardFieldTop("Kota", state.city, { v -> viewModel.update { it.copy(city = v) } }, Modifier.padding(top = 10.dp))
                    // Harga diformat Rupiah otomatis saat mengetik
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text("Harga", fontWeight = FontWeight.W700, fontSize = 13.sp)
                        OutlinedTextField(
                            value = state.price.toLongOrNull()?.let { formatRupiah(it) }.orEmpty(),
                            onValueChange = { raw ->
                                viewModel.update { it.copy(price = raw.filter { ch -> ch.isDigit() }.take(13)) }
                            },
                            placeholder = { Text("cth: 950000000", color = MutedGray) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Preview kartu iklan (sesuai referensi: "PREVIEW KARTU")
                    val previewPrice = state.price.toLongOrNull() ?: 0L
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TintGreen)
                            .padding(14.dp),
                    ) {
                        Text(
                            "PREVIEW KARTU",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W800,
                            color = PrimaryDark,
                        )
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF6EE7B7), Color(0xFF10B981)))),
                                contentAlignment = Alignment.Center,
                            ) {
                                AsyncImage(
                                    model = com.lapakhunian.app.core.util.ApiConfig.absoluteUrl(state.uploadedPhotos.firstOrNull()),
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                                Text(
                                    state.title.ifBlank { "Judul iklan…" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.W700,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    formatRupiah(previewPrice),
                                    color = PrimaryDark,
                                    fontWeight = FontWeight.W800,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                                Text(
                                    listOfNotNull(
                                        state.city.ifBlank { null },
                                        com.lapakhunian.app.core.domain.model.PropertyCategory.fromApi(state.category)?.label,
                                        if (state.purpose == "disewa") "Disewa" else "Dijual",
                                    ).joinToString(" · "),
                                    fontSize = 11.sp,
                                    color = MutedGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }

                    Text(
                        "Iklan pemilik gratis 30 hari. Setelah submit masuk review admin (status PENDING).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    state.error?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp)) }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.step > 1) {
                    Button(
                        onClick = { viewModel.setStep(state.step - 1) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            "Kembali",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.W700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Button(
                    onClick = {
                        // Validasi per step sebelum maju/submit
                        val validationError = when (state.step) {
                            1 -> if (state.title.isBlank()) "Judul iklan wajib diisi" else null
                            4 -> when {
                                (state.price.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L) <= 0 -> "Harga harus lebih dari 0"
                                state.address.isBlank() -> "Alamat wajib diisi"
                                else -> null
                            }
                            else -> null
                        }
                        viewModel.update { it.copy(validationError = validationError) }
                        if (validationError == null) {
                            if (state.step < 4) {
                                viewModel.setStep(state.step + 1)
                            } else {
                                viewModel.submit(
                                    mapOf(
                                        "title" to state.title,
                                        "description" to state.description,
                                        "price" to (state.price.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L),
                                        "address" to state.address,
                                        "city" to state.city,
                                        "category" to state.category,
                                        "purpose" to state.purpose,
                                        "listedBy" to if ((com.lapakhunian.app.core.util.RoleHolder.role) == "agent") "agent" else "owner",
                                        "bedrooms" to (state.bedrooms.toIntOrNull() ?: 0),
                                        "bathrooms" to (state.bathrooms.toIntOrNull() ?: 0),
                                        "squareFeet" to (state.landArea.toIntOrNull() ?: 0),
                                        "buildingArea" to (state.buildingArea.toIntOrNull() ?: 0),
                                        "floors" to (state.floors.toIntOrNull() ?: 1),
                                        "certificate" to state.certificate,
                                        "electricity" to state.electricity,
                                        "furnishing" to state.furnishing,
                                        "amenities" to state.amenities.toList(),
                                    ),
                                    onDone = onSubmitted,
                                )
                            }
                        }
                    },
                    enabled = !state.submitting && !state.uploading && !state.loading,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(2f),
                ) {
                    Text(
                        if (state.step < 4) "Lanjut" else "Submit Iklan",
                        fontWeight = FontWeight.W700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            state.validationError?.let {
                Text(it, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    }
}

/** Header langkah wizard — lingkaran nomor + garis penghubung (done ✓, aktif, mendatang). */
@Composable
private fun WizardStepHeader(steps: List<String>, current: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, label ->
            val stepNo = index + 1
            val done = stepNo < current
            val active = stepNo == current
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                done || active -> Green
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        PhIcon(AppIcons.CheckCircle, tint = Color.White, size = 15.dp)
                    } else {
                        Text(
                            "$stepNo",
                            color = if (active) Color.White else MutedGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W800,
                        )
                    }
                }
                Text(
                    label,
                    fontSize = 10.5.sp,
                    fontWeight = if (active || done) FontWeight.W800 else FontWeight.W700,
                    color = if (active || done) PrimaryDark else MutedGray,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            if (index < steps.lastIndex) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (stepNo < current) Green else MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }
    }
}

/** Field dengan label di atas (bukan floating) — sesuai referensi wizard. */
@Composable
private fun WizardFieldTop(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    Column(modifier = modifier) {
        Text(label, fontWeight = FontWeight.W700, fontSize = 13.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { placeholder?.let { Text(it, color = MutedGray) } },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
    }
}

@Composable
private fun WizardChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {    Text(
        label,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.W700,
        color = if (selected) Color.White else MutedGray,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Green else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun WizardField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { placeholder?.let { Text(it, color = MutedGray) } },
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

// ================= AGENT DASHBOARD =================

@Composable
fun AgentDashboardScreen(
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
    onOpenLeads: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenLeadWa: () -> Unit,
    onOpenChat: () -> Unit = {},
    onOpenWizard: () -> Unit = {},
    onOpenVoucher: () -> Unit = {},
    onOpenCompensation: () -> Unit = {},
    onOpenAgentProfile: () -> Unit = {},
    onOpenDetail: (String) -> Unit = {},
    viewModel: AgentDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Muat/refresh tiap layar kembali tampil — saldo lead & banner tagihan segar setelah bayar
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.retry()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DashTopBar("Dashboard Agen", onBack)
        when {
            state.needsLogin -> NeedsLogin(onGoLogin)
            state.notAgent -> EmptyState(
                title = "Bukan akun agen",
                message = "Daftar sebagai agen atau langganan paket untuk membuka dashboard ini.",
                icon = AppIcons.Business,
            )
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Memuat…", color = MutedGray)
            }
            state.error != null && state.agent == null -> EmptyState(
                title = "Gagal memuat",
                message = state.error.orEmpty(),
                actionLabel = "Coba lagi",
                onAction = { viewModel.retry() },
            )
            else -> {
                val agent = state.agent
                val sub = state.subscription
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Kartu profil agen — klik untuk membuka Profil Agen (referensi: agent-card → scr-agent-profile)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(onClick = onOpenAgentProfile)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        InitialAvatar(initials = (agent?.name ?: "A").take(2), size = 48)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(agent?.name ?: "Agen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W800)
                                if (agent?.profileTier == "PREMIUM") {
                                    Text(
                                        "PRO",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.W800,
                                        modifier = Modifier
                                            .padding(start = 6.dp)
                                            .background(Green, RoundedCornerShape(50))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                            Text(
                                "★ ${agent?.rating ?: 0.0} · ${agent?.reviewCount ?: 0} ulasan · ${agent?.propertiesCount ?: 0} listing",
                                style = MaterialTheme.typography.labelMedium,
                                color = MutedGray,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        PhIcon(AppIcons.Chevron, contentDescription = "Profil Agen", tint = Color(0xFFC3CAD6), modifier = Modifier.size(18.dp))
                    }

                    // SALDO LEAD + tombol Isi Paket (sesuai referensi balance-card)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0C8A44), Color(0xFF18B35B))))
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("SALDO LEAD", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.W800, letterSpacing = 1.sp)
                            Text("${agent?.leadBalance ?: 0}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.W800)
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onOpenPlans)
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PhIcon(AppIcons.Plus, tint = Color.White, size = 13.dp)
                            Text("Isi Paket", color = Color.White, fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.padding(start = 5.dp))
                        }
                    }

                    // Stat row sesuai referensi: Slot Iklan x/y · Lead Terpakai · Chat Blm Dibaca
                    val usedSlots = sub?.currentProperties ?: agent?.propertiesCount ?: 0
                    val maxSlots = sub?.maxProperties ?: 0
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatBox(if (maxSlots > 0) "$usedSlots/$maxSlots" else "$usedSlots", "Slot Iklan", Modifier.weight(1f))
                        StatBox("${agent?.totalLeadsUsed ?: 0}", "Lead Terpakai", Modifier.weight(1f))
                        StatBox("${state.unreadChats}", "Chat Blm Dibaca", Modifier.weight(1f))
                    }

                    // Paket aktif/berakhir + ikon seal-check (sesuai referensi) — ketuk untuk ke Paket & Langganan
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onOpenPlans() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PhIcon(
                            AppIcons.Verified,
                            tint = if (sub?.isActive == true) PrimaryDark else MutedGray,
                            size = 22.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .background(TintGreen, RoundedCornerShape(12.dp)),
                        )
                        Column(Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(
                                "Paket ${sub?.label ?: sub?.tier ?: "-"}" + if (sub?.isActive == true) " aktif" else " berakhir",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.W800,
                            )
                            Text(
                                when {
                                    sub?.isActive == true && sub?.daysLeft != null -> "Sisa ${sub.daysLeft} hari — ketuk untuk kelola"
                                    sub?.isActive == true -> "Status: AKTIF"
                                    else -> "Masa berlaku habis — ketuk untuk perpanjang"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MutedGray,
                            )
                        }
                        if (sub?.isActive == true) {
                            Text(
                                "AKTIF",
                                color = Color(0xFF15803D),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.W800,
                                modifier = Modifier
                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        } else {
                            Text(
                                "BERAKHIR",
                                color = Color(0xFFB45309),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.W800,
                                modifier = Modifier
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }

                    // Grid menu 8 tile (sesuai referensi qa-grid)
                    val qaTiles = listOf(
                        Triple(AppIcons.Sell, "Leads", onOpenLeads),
                        Triple(AppIcons.Crown, "Langganan", onOpenPlans),
                        Triple(AppIcons.Chat, "Pesan", onOpenChat),
                        Triple(AppIcons.Receipt, "Riwayat", onOpenHistory),
                        Triple(AppIcons.Gift, "Kompensasi", onOpenCompensation),
                        Triple(AppIcons.Phone, "Lead WA", onOpenLeadWa),
                        Triple(AppIcons.PlusCircle, "Pasang Iklan", onOpenWizard),
                        Triple(AppIcons.Ticket, "Voucher", onOpenVoucher),
                    )
                    qaTiles.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { (icon, label, action) ->
                                QaTile(icon, label, Modifier.weight(1f), action)
                            }
                        }
                    }
                }

                // Listing Terbaru (sesuai referensi)
                if (state.listings.isNotEmpty()) {
                    com.lapakhunian.app.core.designsystem.components.SectionHeader(
                        "Listing Terbaru",
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.listings.forEach { property ->
                            AgentListingCard(
                                property = property,
                                leadCount = state.leadCountByProperty[property.id] ?: 0,
                                onClick = { onOpenDetail(property.id) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Tile menu grid dashboard agen — ikon dalam kotak tint + label (sesuai referensi qa-grid). */
@Composable
private fun QaTile(icon: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
            size = 22.dp,
            modifier = Modifier
                .size(42.dp)
                .background(TintGreen, RoundedCornerShape(13.dp)),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W700)
    }
}

/** Kartu listing terbaru agen: thumbnail, judul, dilihat · leads, harga, badge status. */
@Composable
private fun AgentListingCard(property: Property, leadCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF6EE7B7), Color(0xFF10B981)))),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = property.images.firstOrNull(),
                contentDescription = property.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                property.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W700,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${property.viewCount} dilihat · $leadCount leads",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                formatRupiah(property.price),
                color = PrimaryDark,
                fontWeight = FontWeight.W800,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        StatusBadge(
            status = if (property.isSold) ListingStatus.SOLD else property.status ?: ListingStatus.APPROVED,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

// ================= AGENT LEADS =================

@Composable
fun AgentLeadsScreen(
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
    onOpenPlans: () -> Unit,
    /** Beli paket lead → layar Paket & Langganan tab Top-up Leads. */
    onOpenPackages: () -> Unit,
    onPayPurchase: (String) -> Unit,
    viewModel: AgentLeadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Muat/refresh tiap layar kembali tampil — status purchase segar setelah kembali dari pembayaran
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.retry()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DashTopBar("Leads", onBack)
        when {
            state.needsLogin -> NeedsLogin(onGoLogin)
            state.notAgent -> EmptyState(
                title = "Bukan akun agen",
                message = "Leads tersedia untuk akun agen yang berlangganan.",
                icon = AppIcons.Business,
            )
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Memuat…", color = MutedGray) }
            state.error != null && state.data == null -> EmptyState(
                title = "Gagal memuat",
                message = state.error.orEmpty(),
                actionLabel = "Coba lagi",
                onAction = { viewModel.retry() },
            )
            else -> {
                val data = state.data
                if (state.purchasing) {
                    Text(
                        "Memproses pembayaran…",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.W700,
                        color = PrimaryDark,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                } else state.error?.let {
                    Text(it, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0C8A44), Color(0xFF18B35B))))
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("SALDO LEAD", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.W800, letterSpacing = 1.sp)
                            Text("${data?.currentBalance ?: 0}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.W800)
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onOpenPlans)
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PhIcon(AppIcons.Plus, tint = Color.White, size = 13.dp)
                            Text("Beli Paket", color = Color.White, fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.padding(start = 5.dp))
                        }
                    }

                    // Lead Masuk (sesuai referensi: lead-card dengan badge + kontak + aksi)
                    Text("Lead Masuk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W800)
                    if (state.leadWa.isEmpty()) {
                        Text(
                            "Belum ada lead masuk. Calon pembeli yang menghubungi via WhatsApp muncul di sini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                    state.leadWa.forEach { lead ->
                        LeadMasukCard(
                            lead = lead,
                            unlocking = state.unlockingId == lead.id,
                            onUnlock = { viewModel.unlockLeadWa(lead.id.orEmpty()) },
                            onOpenWa = {
                                val number = com.lapakhunian.app.core.util.toWaNumber(lead.phone.orEmpty())
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number")))
                                }
                            },
                        )
                    }

                    // Top-up leads pindah ke layar "Paket & Langganan" (tab Top-up Leads)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(TintGreen)
                            .clickable { onOpenPackages() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Beli Paket Lead", fontWeight = FontWeight.W800)
                            Text(
                                "Top-up leads — sekali beli, bayar VA/QRIS di app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                        PhIcon(AppIcons.Wallet, tint = Green, size = 20.dp)
                    }

                    if ((data?.pendingCount ?: 0) > 0) {
                        Text(
                            "${data?.pendingCount} pembelian menunggu pembayaran",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFB45309),
                        )
                    }

                    Text("Riwayat Pembelian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W800)
                    if (data?.purchases.isNullOrEmpty()) {
                        Text("Belum ada pembelian.", style = MaterialTheme.typography.bodySmall, color = MutedGray)
                    }
                    data?.purchases?.take(10)?.forEach { purchase ->
                        val unpaid = purchase.status?.equals("pending", ignoreCase = true) == true ||
                            purchase.status?.equals("processing", ignoreCase = true) == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${purchase.packageInfo?.name ?: "Paket"} — ${purchase.amount ?: 0} leads", fontWeight = FontWeight.W700, fontSize = 13.5.sp)
                                Text(
                                    purchase.createdAt?.take(10) ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedGray,
                                )
                            }
                            if (unpaid) {
                                MiniAction(
                                    "Bayar",
                                    Color(0xFFFEE2E2),
                                    ErrorRed,
                                    enabled = !state.purchasing,
                                    onClick = { viewModel.payPendingPurchase(purchase.id.orEmpty(), onPay = onPayPurchase) },
                                )
                            }
                            Text(
                                (purchase.status ?: "").uppercase(),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.W800,
                                color = if (purchase.status?.equals("paid", ignoreCase = true) == true) Color(0xFF15803D) else Color(0xFFB45309),
                                modifier = Modifier
                                    .padding(start = if (unpaid) 8.dp else 0.dp)
                                    .background(
                                        if (purchase.status?.equals("paid", ignoreCase = true) == true) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                        RoundedCornerShape(50),
                                    )
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Button(
                        onClick = onOpenPlans,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 28.dp),
                    ) { Text("Isi Paket Lead", fontWeight = FontWeight.W700) }
                }
            }
        }
    }
}

/** Kartu lead masuk — sesuai referensi: nama + badge, referensi properti, kontak, aksi buka/chat. */
@Composable
private fun LeadMasukCard(
    lead: com.lapakhunian.app.core.network.dto.LeadWaDto,
    unlocking: Boolean,
    onUnlock: () -> Unit,
    onOpenWa: () -> Unit,
) {
    val unlocked = lead.unlocked == true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                lead.name ?: "Calon pembeli",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W800,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (unlocked) "TERBUKA" else "TERKUNCI",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.W800,
                color = if (unlocked) Color(0xFF15803D) else Color(0xFFB45309),
                modifier = Modifier
                    .background(
                        if (unlocked) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhIcon(AppIcons.Location, tint = MutedGray, size = 13.dp)
            Text(
                buildString {
                    append(lead.propertyTitle ?: "Properti")
                    lead.budget?.let { append(" · Budget ${compactRupiah(it)}") }
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 5.dp),
            )
        }
        // Kontak: nomor disembunyikan selama lead terkunci (ref: blur)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
        ) {
            ContactRow(AppIcons.IdCard, lead.name ?: "Calon pembeli")
            ContactRow(AppIcons.Phone, if (unlocked) lead.phone.orEmpty() else "Nomor terkunci — buka dengan lead")
            lead.timeline?.takeIf { it.isNotBlank() }?.let {
                ContactRow(AppIcons.Clock, "Rencana: $it")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (unlocked) {
                MiniAction("WhatsApp", TintGreen, PrimaryDark, onOpenWa)
            } else {
                MiniAction(
                    if (unlocking) "Memproses…" else "Buka · ${lead.leadCost ?: 1} Lead",
                    Color(0xFFDCFCE7),
                    PrimaryDark,
                    onUnlock,
                    enabled = !unlocking,
                )
            }
        }
    }
}

@Composable
private fun ContactRow(icon: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhIcon(icon, tint = MutedGray, size = 14.dp)
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

// ================= AGENT PLANS =================

@Composable
fun AgentPlansScreen(
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
    /** Pilih paket → layar pembayaran native (pilih VA/QRIS di sana). */
    onChoosePlan: (String, String) -> Unit,
    /** Lanjut tagihan Snap lama (ada paymentRedirectUrl) → Custom Tabs. */
    onResumeSnap: (String, String) -> Unit,
    /** Lanjut tagihan pending native → detail VA/QRIS di app. */
    onResumeNative: (String) -> Unit,
    /** Tab Top-up Leads: buat purchase pending lalu buka layar pembayaran native. */
    onBuyPackage: (String) -> Unit,
    startOnLeadsTab: Boolean = false,
    viewModel: AgentPlansViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Tab aktif — Leads screen membuka langsung tab Top-up Leads
    var selectedTab by remember { mutableStateOf(if (startOnLeadsTab) 1 else 0) }

    // Muat/refresh tiap layar kembali tampil — kartu pending segar setelah kembali dari pembayaran
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.retry()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Aktivasi langsung (paket gratis / diskon menutup seluruh biaya) — tanpa pembayaran
    state.successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSuccessMessage() },
            title = { Text("Berhasil!", fontWeight = FontWeight.W800) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSuccessMessage() }) {
                    Text("Oke", color = PrimaryDark, fontWeight = FontWeight.W700)
                }
            },
        )
    }

    // Konfirmasi batal tagihan pending — server cancel token Midtrans + status cancelled
    var confirmCancelPending by remember { mutableStateOf(false) }
    if (confirmCancelPending) {
        AlertDialog(
            onDismissRequest = { confirmCancelPending = false },
            title = { Text("Batalkan tagihan?", fontWeight = FontWeight.W800) },
            text = {
                Text("Pembayaran pending akan dibatalkan dan paket belum aktif. Setelah ini kamu bisa langsung memilih paket lain.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmCancelPending = false
                        viewModel.cancelPendingPayment()
                    },
                    enabled = !state.cancellingPending,
                ) {
                    Text("Ya, batalkan", color = ErrorRed, fontWeight = FontWeight.W700)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancelPending = false }) {
                    Text("Batal", color = MutedGray)
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DashTopBar("Paket & Langganan", onBack)
        when {
            state.needsLogin -> NeedsLogin(onGoLogin)
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Memuat…", color = MutedGray) }
            state.error != null && state.plans.isEmpty() -> EmptyState(
                title = "Gagal memuat",
                message = state.error.orEmpty(),
                actionLabel = "Coba lagi",
                onAction = { viewModel.retry() },
            )
            state.plans.isEmpty() -> EmptyState(
                title = "Belum ada paket",
                message = "Paket langganan belum tersedia.",
                icon = AppIcons.Crown,
            )
            else -> {
                // Tab: Langganan (properti + leads bulanan) | Top-up Leads (leads saja)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    com.lapakhunian.app.core.designsystem.components.FilterChip(
                        label = "Langganan",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                    )
                    com.lapakhunian.app.core.designsystem.components.FilterChip(
                        label = "Top-up Leads",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (selectedTab != 0) {
                    // ===== TAB TOP-UP LEADS — leads saja, sekali beli =====
                    state.error?.let {
                        Text(it, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                    Text(
                        "Top-up leads saja — sekali beli, slot properti tidak ikut. Untuk slot properti + leads tiap bulan, pakai tab Langganan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                    state.packages.forEach { pkg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(pkg.name ?: "Paket", fontWeight = FontWeight.W800)
                                Text(
                                    "${pkg.amount ?: 0} leads · LEADS SAJA",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MutedGray,
                                )
                                Text(
                                    formatRupiah(pkg.price ?: 0),
                                    color = PrimaryDark,
                                    fontWeight = FontWeight.W800,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            MiniAction(
                                if (state.buyingPackageId == pkg.id) "Memproses…" else "Beli",
                                TintGreen,
                                PrimaryDark,
                                onClick = { viewModel.purchaseLeadPackage(pkg.id.orEmpty(), onBuyPackage) },
                                enabled = state.buyingPackageId == null,
                            )
                        }
                    }
                    if (state.packages.isEmpty()) {
                        Text(
                            "Paket lead belum tersedia.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                state.error?.let {
                    Text(it, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(20.dp))
                }

                // Kartu tagihan pending — server menolak paket baru ("Tunggu pembayaran
                // langganan sebelumnya selesai") sampai tagihan ini selesai/dibatalkan
                state.pendingSub?.let { pending ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(TintGreen)
                            .padding(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PhIcon(AppIcons.Sell, tint = Green, size = 18.dp)
                            Text(
                                "Pembayaran tertunda",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.W800,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Text(
                            buildString {
                                append(pending.plan?.name ?: "Paket")
                                pending.plan?.price?.let { append(" · " + formatRupiah(it)) }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W700,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            "Selesaikan atau batalkan tagihan ini dulu untuk memilih paket lain.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        pending.createdAt?.takeIf { it.length >= 10 }?.let { created ->
                            Text(
                                "Tagihan dibuat ${created.take(10)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedGray,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Button(
                            onClick = {
                                pending.paymentRedirectUrl?.let { url ->
                                    onResumeSnap(url, pending.id.orEmpty())
                                } ?: onResumeNative(pending.id.orEmpty())
                            },
                            enabled = !state.cancellingPending,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        ) { Text("Lanjutkan Bayar", fontWeight = FontWeight.W700) }
                        TextButton(
                            onClick = { confirmCancelPending = true },
                            enabled = !state.cancellingPending,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                if (state.cancellingPending) "Membatalkan…" else "Batalkan tagihan",
                                color = ErrorRed,
                                fontWeight = FontWeight.W700,
                            )
                        }
                    }
                }

                // Kartu langganan aktif — status ringkas di atas, bukan cuma chip PAKETMU
                state.subscription?.takeIf { it.isActive == true }?.let { sub ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(TintGreen)
                            .padding(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PhIcon(AppIcons.Crown, tint = Green, size = 18.dp)
                            Text(
                                "Langganan Aktif",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.W800,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Text(
                            (sub.label ?: sub.tier ?: "Paket") + " — sisa ${sub.daysLeft ?: 0} hari",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W700,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        sub.maxProperties?.let { maxSlots ->
                            Text(
                                "${sub.currentProperties ?: 0}/$maxSlots properti terpakai",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }

                // Intro (sesuai referensi)
                Text(
                    "Paket menentukan slot iklan, leads tiap bulan, dan chat unlimited. Bisa upgrade kapan saja.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )

                // Voucher diskon checkout — dipakai saat memilih paket (diskon dihitung server)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(18.dp),
                ) {
                    Text("Punya kode voucher?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W800)
                    Text(
                        "Opsional — masukkan kode diskon, dipakai otomatis saat memilih paket.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = state.voucherCode,
                        onValueChange = { viewModel.updateVoucherCode(it) },
                        placeholder = { Text("KODEVOUCHER", color = MutedGray) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }

                state.plans.forEach { plan ->
                    val isCurrent = state.subscription?.let { sub ->
                        sub.label.equals(plan.name, ignoreCase = true) || sub.tier.equals(plan.name, ignoreCase = true)
                    } == true
                    // Langganan aktif diblokir server ("Tunggu hingga habis") — tombol mati + status, bukan jalan buntu
                    val isActivePlan = isCurrent && state.subscription?.isActive == true
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(plan.name ?: "Paket", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W800)
                            if (isCurrent) {
                                // tag paket aktif (referensi: "PAKETMU")
                                Text(
                                    "PAKETMU",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.W800,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .background(Green, RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                            // Penanda kategori — beda dengan "Paket Lead" (leads saja)
                            Text(
                                "PROPERTI + LEADS",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.W800,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .background(PrimaryDark, RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Text(
                            formatRupiah(plan.price ?: 0) + " /${plan.durationDays ?: 30} hari",
                            style = MaterialTheme.typography.titleLarge,
                            color = PrimaryDark,
                            fontWeight = FontWeight.W800,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        // Daftar fitur sesuai referensi: slot, leads/bulan, chat unlimited
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            PlanFeature("${plan.propertySlots ?: 0} slot iklan", included = true)
                            PlanFeature("${plan.monthlyLeads ?: 0} leads / bulan", included = true)
                            PlanFeature(
                                "Chat unlimited" + if (plan.unlimitedChat == true) " (unlock 0 lead)" else "",
                                included = plan.unlimitedChat == true,
                            )
                        }
                        Button(
                            onClick = { onChoosePlan(plan.id.orEmpty(), state.voucherCode) },
                            enabled = !state.cancellingPending && !isActivePlan,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                        ) {
                            Text(
                                when {
                                    isActivePlan -> "Paket Aktif" + (state.subscription?.daysLeft?.let { " — sisa $it hari" } ?: "")
                                    isCurrent -> "Perpanjang ${plan.name}"
                                    else -> "Pilih ${plan.name}"
                                },
                                fontWeight = FontWeight.W700,
                            )
                        }
                    }
                }
                Text(
                    "VA & QRIS dikonfirmasi langsung di aplikasi — status dicek otomatis. Kartu/GoPay lewat halaman web.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
                Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/** Baris fitur paket langganan (referensi: checklist hijau / silang abu). */
@Composable
private fun PlanFeature(text: String, included: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhIcon(
            if (included) AppIcons.CheckCircle else AppIcons.Close,
            tint = if (included) Color(0xFF15803D) else MutedGray,
            size = 14.dp,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (included) MaterialTheme.colorScheme.onSurface else MutedGray,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

// ================= HISTORY =================

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        DashTopBar("Riwayat & Transaksi", onBack)
        when {
            state.needsLogin -> NeedsLogin(onGoLogin)
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Memuat…", color = MutedGray) }
            state.error != null && state.rows.isEmpty() -> EmptyState(
                title = "Gagal memuat",
                message = state.error.orEmpty(),
                actionLabel = "Coba lagi",
                onAction = { viewModel.retry() },
            )
            state.rows.isEmpty() -> EmptyState(
                title = "Belum ada transaksi",
                message = "Riwayat komisi, pembelian paket lead, dan voucher muncul di sini.",
                icon = AppIcons.Receipt,
            )
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                item(key = "label") {
                    Text(
                        "TRANSAKSI",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.W800,
                        color = MutedGray,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }
                items(state.rows, key = { it.key }) { row ->
                    HistoryRowItem(row)
                }
            }
        }
    }
}

/** Item riwayat sesuai referensi: kotak ikon berwarna, judul + sub, jumlah + badge status. */
@Composable
private fun HistoryRowItem(row: HistoryRow) {
    val (bg, fg) = when (row.icon) {
        com.lapakhunian.app.core.designsystem.AppIcons.Wallet ->
            Color(0xFFDBEAFE) to Color(0xFF2563EB)
        com.lapakhunian.app.core.designsystem.AppIcons.Ticket ->
            TintGreen to PrimaryDark
        else -> Color(0xFFFEF3C7) to Color(0xFFB45309)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhIcon(
            row.icon,
            tint = fg,
            size = 20.dp,
            modifier = Modifier
                .size(40.dp)
                .background(bg, RoundedCornerShape(14.dp)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W700, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                row.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (row.amount.isNotBlank()) {
                Text(row.amount, fontWeight = FontWeight.W800, fontSize = 13.5.sp, color = PrimaryDark)
            }
            row.status?.let { status ->
                val isPaid = status == "LUNAS"
                Text(
                    status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W800,
                    color = if (isPaid) Color(0xFF15803D) else Color(0xFFB45309),
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .background(if (isPaid) Color(0xFFDCFCE7) else Color(0xFFFEF3C7), RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
    }
}
