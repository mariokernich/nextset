package com.mariokernich.nextset.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.min

/**
 * The NextSet logo: a kettlebell whose body is a timer dial.
 *
 * Mirrors the geometry in `Branding/build-icons.mjs` (1024 pt artboard); the
 * dial face is cut out, so the background shows through.
 */
@Composable
fun KettlebellMark(bell: Color, modifier: Modifier = Modifier, ring: Color = Palette.coral) {
    Canvas(
        modifier
            .aspectRatio(KettlebellGeometry.WIDTH / KettlebellGeometry.HEIGHT)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val scale = min(size.width / KettlebellGeometry.WIDTH, size.height / KettlebellGeometry.HEIGHT)
        withTransform({
            translate(
                left = (size.width - KettlebellGeometry.WIDTH * scale) / 2 - KettlebellGeometry.LEFT * scale,
                top = (size.height - KettlebellGeometry.HEIGHT * scale) / 2 - KettlebellGeometry.TOP * scale,
            )
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(KettlebellGeometry.handle, bell, style = Stroke(width = 96f, join = StrokeJoin.Round))
            drawPath(KettlebellGeometry.body, bell)
            drawCircle(Color.Black, radius = KettlebellGeometry.FACE_RADIUS, center = KettlebellGeometry.center, blendMode = BlendMode.Clear)
            val r = KettlebellGeometry.RING_RADIUS
            drawArc(
                color = ring,
                startAngle = -90f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(KettlebellGeometry.center.x - r, KettlebellGeometry.center.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = KettlebellGeometry.RING_WIDTH, cap = StrokeCap.Round),
            )
        }
    }
}

/** Artboard geometry of the logo. */
private object KettlebellGeometry {
    /** Bounding box of the glyph on the artboard. */
    const val LEFT = 220f
    const val TOP = 172f
    const val WIDTH = 584f
    const val HEIGHT = 752f

    val center = Offset(512f, 632f)
    const val BODY_RADIUS = 292f
    const val FACE_RADIUS = 204f
    const val RING_RADIUS = 136f
    const val RING_WIDTH = 66f

    val handle = Path().apply {
        moveTo(276f, 592f)
        lineTo(300f, 370f)
        quadraticTo(300f, 220f, 450f, 220f)
        lineTo(574f, 220f)
        quadraticTo(724f, 220f, 724f, 370f)
        lineTo(748f, 592f)
    }

    /** The round body. */
    val body = Path().apply {
        addOval(Rect(center, BODY_RADIUS))
    }
}
