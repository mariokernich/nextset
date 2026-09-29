package com.mariokernich.nextset.core.ui

import androidx.compose.ui.graphics.Color

/**
 * NextSet's colours, as in the iOS app: graphite for everything, coral only
 * where it counts – the last seconds of a rest, plus the ring in the logo.
 */
object Palette {
    val graphite = Color(0xFF2E2E2C)
    val graphiteNight = Color(0xFFEFEEEB)

    /** Fill for prominent buttons. */
    val graphiteStrong = Color(0xFF1E1D1B)
    val onGraphiteStrongNight = Color(0xFF171614)

    /** Accent for small text, with enough contrast on light backgrounds. */
    val graphiteText = Color(0xFF393836)
    val graphiteTextNight = Color(0xFFE9E8E5)

    /** The brand colour. */
    val coral = Color(0xFFE0643C)
    val coralNight = Color(0xFFFA8C58)

    /** Gradients of the logo. */
    val bellTop = Color(0xFF434240)
    val bellBottom = Color(0xFF171614)
    val ringTop = Color(0xFFF49665)
    val ringBottom = Color(0xFFDF5F35)
}
