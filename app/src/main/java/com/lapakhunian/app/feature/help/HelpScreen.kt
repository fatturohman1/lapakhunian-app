package com.lapakhunian.app.feature.help

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.components.BackIconButton

private data class Faq(val question: String, val answer: String)

private val FaqList = listOf(
    Faq(
        "Bagaimana cara memasang iklan properti?",
        "Buka menu Profil → Pasang Iklan, lalu lengkapi formulir (judul, harga, spesifikasi, dan foto). Iklanmu akan tampil setelah publikasi.",
    ),
    Faq(
        "Apa itu sistem lead?",
        "Lead adalah potensi pembeli yang tertarik pada iklanmu. Untuk melihat nomor telepon pembeli atau membuka chat, agen memakai saldo lead yang bisa dibeli lewat paket lead.",
    ),
    Faq(
        "Kenapa percakapan saya terkunci?",
        "Percakapan dengan calon pembeli terkunci sampai dibuka dengan 1 lead. Setelah dibuka, kamu bisa melihat seluruh pesan dan membalasnya.",
    ),
    Faq(
        "Bagaimana cara mendapat lead WhatsApp?",
        "Calon pembeli yang menghubungi kamu melalui WhatsApp akan muncul di halaman Lead WA (Dashboard Agen) dan juga di bagian Chat dengan penanda WhatsApp.",
    ),
    Faq(
        "Bisakah saya mengedit atau menghapus iklan?",
        "Bisa. Buka Dashboard Saya (owner) atau Dashboard Agen, pilih iklan, lalu gunakan tombol edit atau perpanjang iklan.",
    ),
    Faq(
        "Apakah membuat akun gratis?",
        "Ya, membuat akun dan memasang iklan dasar gratis. Saldo lead untuk membuka nomor/chat dibeli terpisah melalui paket yang tersedia.",
    ),
)

@Composable
fun HelpScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            Text("Bantuan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800, modifier = Modifier.padding(start = 6.dp))
        }

        // Kontak cepat
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp),
        ) {
            Text("Butuh bantuan langsung?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W800)
            Text(
                "Tim kami siap membantu seputar iklan, lead, dan transaksi.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(modifier = Modifier.padding(top = 14.dp)) {
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@lapakhunian.com")))
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                ) {
                    PhIcon(AppIcons.Chat, tint = Color.White, size = 15.dp)
                    Text("Email Kami", fontWeight = FontWeight.W700, modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://lapakhunian.com")))
                        }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                ) {
                    PhIcon(AppIcons.Share, tint = PrimaryDark, size = 15.dp)
                    Text("Website", fontWeight = FontWeight.W700, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }

        // FAQ accordion
        Text(
            "Pertanyaan Umum",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W800,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        FaqList.forEachIndexed { index, faq ->
            val isOpen = expanded == index
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { expanded = if (isOpen) -1 else index }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        faq.question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.W700,
                        modifier = Modifier.weight(1f),
                    )
                    PhIcon(
                        AppIcons.Chevron,
                        tint = MutedGray,
                        size = 16.dp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                AnimatedVisibility(visible = isOpen) {
                    Text(
                        faq.answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        Box(Modifier.size(1.dp))
    }
}
