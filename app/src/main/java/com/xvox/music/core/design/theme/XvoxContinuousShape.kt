// NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/design/theme/XvoxContinuousShape.kt
package com.xvox.music.core.design.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Continuous-curvature ("squircle") rounded rectangle: the corner arc starts earlier on each edge
 * and eases into it, as on iOS and in Material 3 Expressive. Self-contained implementation (no
 * extra library); [smoothing] 0 = plain circular corner, .6 = iOS-like.
 */
@Immutable
class XvoxContinuousShape(private val radius: Dp, private val smoothing: Float = .6f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val r0 = with(density) { radius.toPx() }
        val r = min(r0, min(w, h) / 2f / (1f + smoothing)).coerceAtLeast(0f)
        if (r <= 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        val p = min(r * (1f + smoothing), min(w, h) / 2f)
        val k = r * .4477f // bezier handle length that keeps the arc round at the corner
        val path = Path().apply {
            moveTo(p, 0f)
            lineTo(w - p, 0f)
            cubicTo(w - k, 0f, w, k, w, p)
            lineTo(w, h - p)
            cubicTo(w, h - k, w - k, h, w - p, h)
            lineTo(p, h)
            cubicTo(k, h, 0f, h - k, 0f, h - p)
            lineTo(0f, p)
            cubicTo(0f, k, k, 0f, p, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Shape for a language: iOS/Material use continuous corners, XVOX keeps its classic circular ones. */
fun XvoxDesignTokens.shapeFor(radius: Dp): Shape = when {
    radius >= 90.dp -> CircleShape
    language == XvoxDesignLanguage.XVOX -> RoundedCornerShape(radius)
    else -> XvoxContinuousShape(radius)
}
