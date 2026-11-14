package com.lapakhunian.app.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen

/**
 * Tombol kembali standar — hit area 44dp (standar sentuhan Material), ikon 22dp
 * agar konsisten dengan semua header halaman dalam.
 */
@Composable
fun BackIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String = "Kembali") {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PhIcon(AppIcons.Back, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface, size = 22.dp)
    }
}

/** Judul section + aksi "lihat semua" (preview M0). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W800,
        )
        Spacer(Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryDark,
                fontWeight = FontWeight.W700,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(4.dp),
            )
        }
    }
}

/** Efek shimmer untuk loading list. */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    background(
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(x = 400f * (progress * 2f - 1f), y = 0f),
            end = Offset(x = 1200f * (progress * 2f - 1f), y = 60f),
        ),
        RoundedCornerShape(12.dp),
    )
}

/** Baris skeleton kartu properti. */
@Composable
fun PropertyCardSkeleton(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Box(Modifier.size(94.dp).clip(RoundedCornerShape(14.dp)).shimmer())
        Column(Modifier.weight(1f).padding(start = 13.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer(),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .padding(vertical = 3.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer(),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.4f)
                    .padding(vertical = 3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer(),
            )
        }
    }
}

/** Empty state sederhana; opsi ikon ilustratif + tombol aksi (Coba lagi / Masuk). */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(if (icon != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                PhIcon(icon, tint = PrimaryDark, size = 32.dp)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W800,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryDark,
                fontWeight = FontWeight.W800,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(TintGreen)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            )
        }
    }
}
