package com.lapakhunian.app.feature.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import com.lapakhunian.app.BuildConfig
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ChevronGray
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.components.BackIconButton

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenChangePassword: () -> Unit = {},
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            Text("Pengaturan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800, modifier = Modifier.padding(start = 6.dp))
        }

        SettingGroup {
            SettingRow(AppIcons.Refresh, "Bersihkan Cache Gambar", "Membebaskan penyimpanan dari gambar sementara") {
                // Coil: kosongkan memory + disk cache
                val loader = SingletonImageLoader.get(context)
                runCatching {
                    loader.memoryCache?.clear()
                    loader.diskCache?.clear()
                }
                Toast.makeText(context, "Cache dibersihkan", Toast.LENGTH_SHORT).show()
            }
        }

        SettingGroup {
            SettingRow(AppIcons.Lock, "Ubah Password", "Khusus akun email — Google tak perlu", onClick = onOpenChangePassword)
            SettingRow(AppIcons.Shield, "Kebijakan Privasi", onClick = onOpenPrivacy)
            SettingRow(AppIcons.Info, "Syarat & Ketentuan", onClick = onOpenTerms)
            SettingRow(AppIcons.Star, "Beri Penilaian", "Bantu kami berkembang di Play Store") {
                val pkg = context.packageName
                val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                runCatching { context.startActivity(market) }
                    .onFailure {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
                        }
                    }
            }
        }

        SettingGroup {
            // Versi — hanya info, tanpa chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhIcon(
                    AppIcons.Info,
                    tint = PrimaryDark,
                    size = 21.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(13.dp)),
                )
                Text(
                    "Versi Aplikasi",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                )
                Text(
                    BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                    fontWeight = FontWeight.W700,
                )
            }
        }

        Box(Modifier.size(1.dp))
    }
}

@Composable
private fun SettingGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        content()
    }
}

@Composable
private fun SettingRow(
    icon: String,
    label: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhIcon(
            icon,
            tint = PrimaryDark,
            size = 21.dp,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(13.dp)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.W600)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MutedGray, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (onClick != null) {
            PhIcon(AppIcons.Chevron, contentDescription = null, tint = ChevronGray, modifier = Modifier.size(18.dp))
        }
    }
}

// ===== Halaman statis: Kebijakan Privasi & Syarat & Ketentuan =====

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    StaticDocScreen(
        title = "Kebijakan Privasi",
        onBack = onBack,
        sections = listOf(
            "LapakHunian menghargai privasi penggunanya. Dokumen ini menjelaskan data apa yang kami proses saat kamu memakai aplikasi.",
            "Data yang kami kumpulkan — Data akun (nama, email, foto) saat kamu mendaftar; data iklan properti yang kamu unggah; isi percakapan dengan agen; dan data lead (nama, nomor telepon, pesan) yang terkait dengan iklanmu.",
            "Penggunaan data — Data digunakan untuk menjalankan fitur aplikasi: menampilkan iklan, menghubungkan pembeli dengan pemilik/agen, mengelola saldo lead, serta meningkatkan kualitas layanan.",
            "Berbagi data — Data iklanmu tampil publik agar dapat dilihat calon pembeli. Data pribadi seperti nomor telepon hanya dibagikan kepada pihak yang telah membuka lead terkait.",
            "Penyimpanan & keamanan — Data tersimpan di server kami dan dilindungi dengan autentikasi token. Kamu dapat keluar dari akun kapan saja melalui menu Profil.",
            "Hubungi kami — Pertanyaan seputar privasi dapat dikirim ke support@lapakhunian.com.",
        ),
    )
}

@Composable
fun TermsScreen(onBack: () -> Unit) {
    StaticDocScreen(
        title = "Syarat & Ketentuan",
        onBack = onBack,
        sections = listOf(
            "Dengan memakai LapakHunian, kamu menyetujui ketentuan berikut. Baca dengan saksama sebelum memakai fitur aplikasi.",
            "Akun — Data yang kamu berikan saat mendaftar harus benar. Kamu bertanggung jawab menjaga kerahasiaan kredensial akun dan seluruh aktivitas yang terjadi pada akunmu.",
            "Iklan properti — Kamu hanya boleh memasang iklan properti yang benar-benar kamu miliki atau kamu amanahkan. Informasi harga, spesifikasi, dan foto harus valid; iklan dengan konten palsu atau menyesatkan dapat dihapus.",
            "Sistem lead & pembayaran — Pembelian paket lead bersifat final dan saldo lead digunakan untuk membuka nomor pembeli atau percakapan. Transaksi pembelian properti terjadi langsung antara pembeli dan penjual; LapakHunian tidak menjadi perantara transaksi jual-beli.",
            "Larangan — Dilarang menyalahgunakan aplikasi untuk spam, penipuan, konten ilegal, atau aktivitas yang mengganggu pengguna lain.",
            "Perubahan ketentuan — Kami dapat memperbarui syarat ini sewaktu-waktu. Perubahan berlaku sejak dipublikasikan di aplikasi.",
        ),
    )
}

@Composable
private fun StaticDocScreen(
    title: String,
    onBack: () -> Unit,
    sections: List<String>,
) {
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
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800, modifier = Modifier.padding(start = 6.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp),
        ) {
            sections.forEachIndexed { index, text ->
                if (index == 0) {
                    Text(text, style = MaterialTheme.typography.bodySmall, color = MutedGray)
                } else {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
        }
        Box(Modifier.size(20.dp))
    }
}
