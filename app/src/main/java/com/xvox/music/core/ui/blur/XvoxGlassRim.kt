// NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/blur/XvoxGlassRim.kt
package com.xvox.music.core.ui.blur

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme

/**
 * BitChord liquid-glass rim, ported without the backdrop library.
 *
 * BitChord draws (1) a 0.5dp hairline (white 10% on dark, black 6% on light) and (2) a directional
 * "Highlight.Default": a 0.5dp inner stroke, white 50%, blended with Plus, whose strength is
 * |cos(edgeNormal - 45deg)| -> bright on the top-left and bottom-right edges, dark on the other two.
 * A sweep gradient around the centre gives the same edge-normal angle on pills, circles and cards.
 */
internal val XvoxGlassEdgeWidth: Dp = .5.dp

@Composable
fun Modifier.xvoxGlassRim(
    shape: Shape,
    /**
     * 0..1 multiplier of the directional highlight. BitChord's own navbar and mini player draw only
     * the flat 0.5dp white-10% edge, so the sweep is off unless a caller asks for it.
     */
    highlight: Float = 0f,
    /** Draw the flat hairline under the highlight. */
    hairline: Boolean = true,
    hairlineColor: Color = Color.White.copy(alpha = .10f)
): Modifier {
    val light = XvoxTheme.colors.isLight
    // On a light page the additive white would vanish, so the highlight is drawn as soft white-on-source.
    val peak = (if (light) .85f else .5f) * highlight
    return drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = glassRimPath(outline)
        if (hairline) {
            drawPath(path, hairlineColor, style = Stroke(width = XvoxGlassEdgeWidth.toPx() * 2f))
        }
        if (peak > 0.001f) {
            val white = Color.White
            fun a(t: Float) = white.copy(alpha = peak * t)
            // sweep starts at 0deg = east, clockwise (screen y is down). |cos(theta - 45deg)|.
            val brush = Brush.sweepGradient(
                colorStops = arrayOf(
                    0f to a(.7071f),
                    .125f to a(1f),
                    .25f to a(.7071f),
                    .375f to a(0f),
                    .5f to a(.7071f),
                    .625f to a(1f),
                    .75f to a(.7071f),
                    .875f to a(0f),
                    1f to a(.7071f)
                ),
                center = Offset(size.width / 2f, size.height / 2f)
            )
            clipPath(path) {
                drawPath(
                    path,
                    brush,
                    style = Stroke(width = XvoxGlassEdgeWidth.toPx() * 2f),
                    blendMode = if (light) BlendMode.SrcOver else BlendMode.Plus
                )
            }
        }
    }
}

private fun DrawScope.glassRimPath(outline: Outline): Path = when (outline) {
    is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
    is Outline.Generic -> outline.path
}
