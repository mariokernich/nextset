package com.mariokernich.nextset.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.mariokernich.nextset.core.ui.Palette

/**
 * The colours of the iOS app on top of Material 3: graphite for everything,
 * coral for the last seconds, switches and confirm buttons.
 */
@Immutable
data class NextSetColors(
    /** Rings, icons and highlights. */
    val accent: Color,
    /** Fill for the big start/pause button. */
    val accentStrong: Color,
    val onAccentStrong: Color,
    /** Accent for small text. */
    val accentText: Color,
    /** The brand colour and the last seconds of a rest. */
    val coral: Color,
    /** The empty part of the ring. */
    val track: Color,
    /** Soft background behind the start screen. */
    val backdrop: Brush,
    /** Grouped rows in the settings. */
    val card: Color,
)

private val LocalNextSetColors = staticCompositionLocalOf<NextSetColors> { error("NextSetTheme is missing") }

object NextSetTheme {
    val colors: NextSetColors
        @Composable get() = LocalNextSetColors.current
}

/** Tabular digits, so the countdown doesn't jitter. */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

private val LightScheme = lightColorScheme(
    primary = Palette.graphiteStrong,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E4E0),
    onPrimaryContainer = Palette.graphiteStrong,
    secondary = Palette.coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E4E0),
    onSecondaryContainer = Palette.graphiteStrong,
    tertiary = Palette.coral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBE3DA),
    onTertiaryContainer = Color(0xFF5A2210),
    inversePrimary = Palette.graphiteNight,
    surfaceTint = Palette.graphite,
    background = Color(0xFFF4F3F1),
    onBackground = Color(0xFF1E1D1B),
    surface = Color(0xFFF4F3F1),
    onSurface = Color(0xFF1E1D1B),
    surfaceVariant = Color(0xFFE8E6E3),
    onSurfaceVariant = Color(0xFF66635F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAF9F7),
    surfaceContainer = Color(0xFFF0EEEB),
    surfaceContainerHigh = Color(0xFFE9E7E4),
    surfaceContainerHighest = Color(0xFFE2E0DC),
    outline = Color(0xFF8C8985),
    outlineVariant = Color(0xFFD8D5D1),
    error = Color(0xFFC62828),
)

private val DarkScheme = darkColorScheme(
    primary = Palette.graphiteNight,
    onPrimary = Palette.onGraphiteStrongNight,
    primaryContainer = Color(0xFF3A3937),
    onPrimaryContainer = Palette.graphiteNight,
    secondary = Palette.coralNight,
    onSecondary = Color(0xFF1E1D1B),
    secondaryContainer = Color(0xFF2F2E2C),
    onSecondaryContainer = Palette.graphiteNight,
    tertiary = Palette.coralNight,
    onTertiary = Color(0xFF1E1D1B),
    tertiaryContainer = Color(0xFF4A2618),
    onTertiaryContainer = Color(0xFFFFD9CB),
    inversePrimary = Palette.graphiteStrong,
    surfaceTint = Palette.graphiteNight,
    background = Color(0xFF0E0D0C),
    onBackground = Palette.graphiteNight,
    surface = Color(0xFF0E0D0C),
    onSurface = Palette.graphiteNight,
    surfaceVariant = Color(0xFF2A2927),
    onSurfaceVariant = Color(0xFFA9A6A1),
    surfaceContainerLowest = Color(0xFF080807),
    surfaceContainerLow = Color(0xFF151413),
    surfaceContainer = Color(0xFF1C1B1A),
    surfaceContainerHigh = Color(0xFF252422),
    surfaceContainerHighest = Color(0xFF2F2E2C),
    outline = Color(0xFF7D7A76),
    outlineVariant = Color(0xFF3A3937),
    error = Color(0xFFFF8A80),
)

@Composable
fun NextSetTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) {
        NextSetColors(
            accent = Palette.graphiteNight,
            accentStrong = Palette.graphiteNight,
            onAccentStrong = Palette.onGraphiteStrongNight,
            accentText = Palette.graphiteTextNight,
            coral = Palette.coralNight,
            track = Color.White.copy(alpha = 0.08f),
            backdrop = Brush.verticalGradient(listOf(Color(0xFF191817), Color(0xFF121111), Color(0xFF050505))),
            card = Color(0xFF1C1B1A),
        )
    } else {
        NextSetColors(
            accent = Palette.graphite,
            accentStrong = Palette.graphiteStrong,
            onAccentStrong = Color.White,
            accentText = Palette.graphiteText,
            coral = Palette.coral,
            track = Color.Black.copy(alpha = 0.08f),
            backdrop = Brush.verticalGradient(listOf(Color(0xFFF0EEEB), Color(0xFFE8E6E3), Color(0xFFFAFAF9))),
            card = Color.White,
        )
    }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme) {
        CompositionLocalProvider(LocalNextSetColors provides colors, content = content)
    }
}
