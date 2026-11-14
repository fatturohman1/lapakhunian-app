package com.lapakhunian.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark

/**
 * Spec grid (LT/LB/KT/KM/Lantai/VA/Carport/Tahun) — preview M0:
 * kotak putih radius 16, icon Phosphor dalam tint squircle, angka bold, label kecil.
 */
data class SpecItem(val icon: String, val value: String, val label: String)

@Composable
fun SpecGrid(items: List<SpecItem>, modifier: Modifier = Modifier, columns: Int = 4) {
    val rows = items.chunked(columns)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { item ->
                    SpecCell(item, Modifier.weight(1f))
                }
                repeat(columns - row.size) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SpecCell(item: SpecItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PhIcon(
            name = item.icon,
            tint = PrimaryDark,
            size = 20.dp,
            modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                .padding(7.dp),
        )
        Text(
            text = item.value,
            fontWeight = FontWeight.W700,
            fontSize = 13.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            color = MutedGray,
        )
    }
}
