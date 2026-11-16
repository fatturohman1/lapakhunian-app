package com.lapakhunian.app.feature.kpr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.GreenGradient
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.util.formatRupiah
import com.lapakhunian.app.core.util.monthlyPayment

/**
 * Kalkulator simulasi KPR (anuitas) — gaya portal properti lain.
 * Bisa dibuka dari menu Profil (harga kosong) atau dari halaman detail (harga terisi).
 */
@Composable
fun KprScreen(
    initialPrice: Long = 0L,
    onBack: () -> Unit,
) {
    var priceDigits by remember { mutableStateOf(initialPrice.takeIf { it > 0 }?.toString().orEmpty()) }
    var dpPercent by remember { mutableFloatStateOf(20f) }
    var rateText by remember { mutableStateOf("7,5") }
    var years by remember { mutableFloatStateOf(15f) }

    val price = priceDigits.toLongOrNull() ?: 0L
    val dp = (price * (dpPercent / 100f)).toLong()
    val loan = (price - dp).coerceAtLeast(0L)
    val rate = rateText.replace(',', '.').toDoubleOrNull() ?: 0.0

    val monthly = monthlyPayment(loan, rate, years.toInt())
    val totalPaid = monthly * years.toInt() * 12
    val totalInterest = (totalPaid - loan).coerceAtLeast(0L)

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
            Text("Simulasi KPR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800, modifier = Modifier.padding(start = 6.dp))
        }

        // ===== Input =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp),
        ) {
            InputLabel("Harga Properti")
            OutlinedTextField(
                value = priceDigits.toLongOrNull()?.let { formatRupiah(it) }.orEmpty(),
                onValueChange = { raw ->
                    // simpan digit saja; tampilan diformat dengan pemisah ribuan
                    priceDigits = raw.filter { it.isDigit() }.take(13)
                },
                placeholder = { Text("cth: 850000000", color = MutedGray) },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            InputLabel("Uang Muka (DP)", Modifier.padding(top = 18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = dpPercent,
                    onValueChange = { dpPercent = (it / 5f).toInt() * 5f },
                    valueRange = 0f..70f,
                    steps = 13,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${dpPercent.toInt()}%",
                    fontWeight = FontWeight.W800,
                    color = PrimaryDark,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(
                if (price > 0) "≈ " + formatRupiah(dp) else "Geser untuk atur uang muka",
                fontSize = 12.sp,
                color = MutedGray,
            )

            InputLabel("Bunga per Tahun (%)", Modifier.padding(top = 18.dp))
            OutlinedTextField(
                value = rateText,
                onValueChange = { raw ->
                    rateText = raw.filter { it.isDigit() || it == ',' || it == '.' }.take(5)
                },
                placeholder = { Text("cth: 7,5", color = MutedGray) },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            InputLabel("Tenor", Modifier.padding(top = 18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = years,
                    onValueChange = { years = it.toInt().coerceIn(1, 25).toFloat() },
                    valueRange = 1f..25f,
                    steps = 24,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${years.toInt()} tahun",
                    fontWeight = FontWeight.W800,
                    color = PrimaryDark,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }

        // ===== Hasil =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GreenGradient)
                .padding(18.dp),
        ) {
            Text("Angsuran per Bulan", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
            Text(
                if (monthly > 0) formatRupiah(monthly) else "—",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (monthly > 0) {
                ResultRow("Pokok Pinjaman", formatRupiah(loan))
                ResultRow("Total Bunga", formatRupiah(totalInterest))
                ResultRow("Total Pembayaran", formatRupiah(totalPaid))
                ResultRow("Tenor", "${(years * 12).toInt()} bulan")
            }
        }

        // ===== Perbandingan tenor =====
        if (monthly > 0) {
            Text(
                "Perbandingan Tenor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                listOf(5, 10, 15, 20, 25).forEach { t ->
                    val m = monthlyPayment(loan, rate, t)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { years = t.toFloat() }
                            .background(if (years.toInt() == t) TintGreen else Color.Transparent)
                            .padding(horizontal = 18.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "$t tahun",
                            fontWeight = if (years.toInt() == t) FontWeight.W800 else FontWeight.W600,
                            color = if (years.toInt() == t) PrimaryDark else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatRupiah(m),
                                fontWeight = FontWeight.W800,
                                color = PrimaryDark,
                                fontSize = 14.sp,
                            )
                            Text(
                                "total " + formatRupiah(m * t * 12),
                                fontSize = 11.sp,
                                color = MutedGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        Text(
            "Simulasi ini hanya estimasi. Angsuran final tergantung suku bunga bank, biaya administrasi, dan asuransi.",
            fontSize = 11.sp,
            color = MutedGray,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

/** Angsuran anuitas dipindah ke core/util/KprMath.kt agar bisa dipakai layar pembanding. */

@Composable
private fun InputLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.W700,
        color = MutedGray,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.W800, fontSize = 13.sp)
    }
}
