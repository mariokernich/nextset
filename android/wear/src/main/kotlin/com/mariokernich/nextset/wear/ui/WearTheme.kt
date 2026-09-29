package com.mariokernich.nextset.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.mariokernich.nextset.core.ui.Palette

/**
 * The watch is always dark: graphite turns into white, and coral is the
 * accent for the title, switches and the last seconds, as on the Apple Watch.
 */
object WearColors {
    val accent = Palette.graphiteNight
    val onAccent = Palette.onGraphiteStrongNight
    val coral = Palette.coralNight

    /** Quick timers and further timers: white glass of different strength. */
    val quickTimer = Color(0xFF3D3D3C)
    val preset = Color(0xFF232322)

    /** Secondary round buttons, with white labels. */
    val neutral = Color(0xFF525252)
    val track = Color.White.copy(alpha = 0.12f)
}

val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

private val Scheme = ColorScheme(
    primary = Palette.coralNight,
    primaryDim = Palette.coral,
    // Checked switch buttons: neutral glass, the switch itself is coral.
    primaryContainer = WearColors.quickTimer,
    onPrimary = Color(0xFF1E1D1B),
    onPrimaryContainer = Color.White,
    secondary = Palette.graphiteNight,
    secondaryDim = Color(0xFFBDBBB7),
    secondaryContainer = WearColors.quickTimer,
    onSecondary = Palette.onGraphiteStrongNight,
    onSecondaryContainer = Color.White,
    tertiary = Palette.coralNight,
    tertiaryDim = Palette.coral,
    tertiaryContainer = Color(0xFF4A2618),
    onTertiary = Color(0xFF1E1D1B),
    onTertiaryContainer = Color(0xFFFFD9CB),
    surfaceContainerLow = Color(0xFF1A1918),
    surfaceContainer = WearColors.preset,
    surfaceContainerHigh = Color(0xFF2E2D2C),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB4B1AC),
    outline = Color(0xFF7D7A76),
    outlineVariant = Color(0xFF3A3937),
    background = Color.Black,
    onBackground = Color.White,
)

@Composable
fun WearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
