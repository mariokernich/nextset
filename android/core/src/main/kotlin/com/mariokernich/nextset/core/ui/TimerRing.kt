package com.mariokernich.nextset.core.ui

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

/**
 * Progress ring that empties clockwise from twelve o'clock.
 *
 * @param progress remaining fraction, 1 = full.
 */
fun DrawScope.drawTimerRing(
    progress: Float,
    tint: Color,
    track: Color,
    strokeWidth: Float,
    glow: Boolean = true,
) {
    val diameter = size.minDimension - strokeWidth
    val arcSize = Size(diameter, diameter)
    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
    drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokeWidth))

    val sweep = 360f * progress.coerceIn(0.0005f, 1f)
    if (glow) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = android.graphics.Paint.Cap.ROUND
                color = tint.copy(alpha = 0.3f * tint.alpha).toArgb()
                maskFilter = BlurMaskFilter(strokeWidth * 0.5f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawArc(
                topLeft.x, topLeft.y, topLeft.x + diameter, topLeft.y + diameter,
                -90f, sweep, false, paint,
            )
        }
    }
    drawArc(
        brush = Brush.verticalGradient(listOf(lerp(tint, Color.White, 0.18f), tint)),
        startAngle = -90f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
}
