package com.lapakhunian.app.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lapakhunian.app.R

/**
 * Metrik font Phosphor (dihitung dari TTF res/font): ascent 0.938em, descent 0.062em,
 * glyph ikon digambar 0..1em di atas baseline. Line box = fontSize memotong 0.062em
 * bagian atas glyph (ascent < em) — root cause icon "terpotong".
 * Line box 1.124em (fontSize * 1.124) memuat glyph utuh DAN center-nya pas
 * tengah (diverifikasi ukur pixel: offset glyph vs center container ≈ 0).
 */
private const val PHOSPHOR_LINE_HEIGHT_EM = 1.124f

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

    // Utama: render ImageVector (path vektor dari PhosphorVectors*) — bebas dari
    // clipping metrik font / fontScale / quirk OEM, presisi identik di semua device.
    val vec = if (weight == PhosphorWeight.FILL) {
        PhosphorVectorsFill.ICONS[name] ?: PhosphorVectorsRegular.ICONS[name]
    } else {
        PhosphorVectorsRegular.ICONS[name]
    }
    if (vec != null) {
        val effectiveTint = if (tint == Color.Unspecified) {
            androidx.compose.material3.LocalContentColor.current
        } else tint
        // PENTING: modifier pemanggil (mis. Modifier.size(40).background(...)) pasang di Box
        // LUAR, Image ukuran `size` di dalamnya. Kalau digabung satu chain
        // (size(40).background().size(21)), constraint tetap 40..40 dari size luar →
        // size(21) ikut ter-clamp → icon menggambar penuh kotak (kelihatan besar+tebal).
        // Box mengoper constraint LOOSE ke anak, jadi ukuran icon dihormati.
        Box(
            modifier = descModifier,
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = vec,
                contentDescription = null,
                colorFilter = ColorFilter.tint(effectiveTint),
                modifier = Modifier.size(size),
            )
        }
        return
    }

    // Fallback: render font (icon yang belum ter-generate)
    val filled = weight == PhosphorWeight.FILL && name in PhosphorCodepoints.FILL
    val codepoint = (if (filled) PhosphorCodepoints.FILL[name] else PhosphorCodepoints.REGULAR[name]) ?: return
    Box(
        modifier = descModifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = codepoint,
            modifier = Modifier.clearAndSetSemantics { },
            fontFamily = if (filled) PhosphorFont.Fill else PhosphorFont.Regular,
            fontSize = spSize,
            fontStyle = FontStyle.Normal,
            color = tint,
            lineHeight = spSize * PHOSPHOR_LINE_HEIGHT_EM,
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
