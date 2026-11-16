package com.lapakhunian.app.core.designsystem

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Beta: theme selalu light. Dark mode belum didukung —
 * blok darkColorScheme lama dihapus agar tidak terpakai tak sengaja.
 */
private val LightColors = lightColorScheme(
    primary = PrimaryDark,
    onPrimary = Color.White,
    primaryContainer = TintGreen,
    onPrimaryContainer = PrimaryDark,
    secondary = PrimaryDark,
    onSecondary = Color.White,
    secondaryContainer = TintDeep,
    onSecondaryContainer = PrimaryDark,
    background = SurfaceLight,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = LineGray,
    onSurfaceVariant = MutedGray,
    outlineVariant = LineGray,
    error = ErrorRed,
)

@Composable
fun LapakHunianTheme(
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
