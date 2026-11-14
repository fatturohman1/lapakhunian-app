package com.lapakhunian.app.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.lapakhunian.app.R

/**
 * League Spartan (variable font, di-bundle di res/font).
 * Berat diatur lewat FontVariation.weight karena file-nya variable [wght 300..800].
 */
@OptIn(ExperimentalTextApi::class)
private fun spartan(weight: FontWeight, variation: Int) = Font(
    resId = R.font.leaguespartan,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(variation)),
)

val LeagueSpartan = FontFamily(
    spartan(FontWeight.W300, 300),
    spartan(FontWeight.W400, 400),
    spartan(FontWeight.W500, 500),
    spartan(FontWeight.W600, 600),
    spartan(FontWeight.W700, 700),
    spartan(FontWeight.W800, 800),
)

private fun family(family: FontFamily) = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}

val AppTypography = family(LeagueSpartan)
