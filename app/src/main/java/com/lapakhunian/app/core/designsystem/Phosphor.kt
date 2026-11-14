package com.lapakhunian.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lapakhunian.app.R

/** Family font Phosphor (bundled di res/font). */
object PhosphorFont {
    val Duotone = FontFamily(Font(R.font.phosphor_duotone))
    val Fill = FontFamily(Font(R.font.phosphor_fill))
    val Regular = FontFamily(Font(R.font.phosphor_regular))
}

enum class PhosphorWeight(val family: FontFamily) {
    DUOTONE(PhosphorFont.Duotone),
    FILL(PhosphorFont.Fill),
    REGULAR(PhosphorFont.Regular),
}

/**
 * Icon Phosphor — satu sistem dengan preview HTML (M0).
 * Default DUOTONE (gaya preview); FILL untuk state aktif; REGULAR untuk util kecil.
 */
@Composable
fun PhIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = Color.Unspecified,
    weight: PhosphorWeight = PhosphorWeight.DUOTONE,
    contentDescription: String? = null,
) {
    val spSize = with(LocalDensity.current) { size.toSp() }
    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else modifier

    // Sementara: semua weight render REGULAR single-layer (paling stabil).
    // Box center — glyph PUA phosphor harus center di dalam kotak caller.
    val regularCp = PhosphorCodepoints.REGULAR[name]
    val fallbackCp = regularCp
        ?: PhosphorCodepoints.FILL[name]
        ?: PhosphorCodepoints.DUO_PRIMARY[name]
        ?: return
    Box(
        modifier = descModifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = fallbackCp,
            fontFamily = PhosphorFont.Regular,
            fontSize = spSize,
            lineHeight = spSize,
            fontStyle = FontStyle.Normal,
            color = tint,
        )
    }
}

/** Icon dalam kotak tint (pola preview: squircle + icon). */
@Composable
fun PhIconBox(
    name: String,
    boxSize: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color,
    bg: Color,
    weight: PhosphorWeight = PhosphorWeight.DUOTONE,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
) {
    Box(
        modifier = modifier
            .size(boxSize)
            .background(bg, RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center,
    ) {
        PhIcon(name = name, size = iconSize, tint = tint, weight = weight)
    }
}
