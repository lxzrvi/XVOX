// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/chrome/XvoxGeneratedBackground.kt
package com.xvox.music.core.ui.chrome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Generated app background, built from the accent + theme (or two custom colours).
 * Styles: 0 Mesh, 1 Beams, 2 Blobs, 3 Aurora, 4 Linear, 5 Spotlight.
 * [angle] rotates the whole pattern; every fill is drawn well past the screen edges so no angle
 * can ever expose an empty corner. [spread] is the "oil paint" control: 0 = crisp, separate
 * colour fields, 1 = large strokes smeared together into one flowing painted surface.
 * It is drawn once (static), so it costs nothing while scrolling.
 */
@Composable
fun XvoxGeneratedBackground(
    accent: Color,
    base: Color,
    isLight: Boolean,
    style: Float,
    blurDp: Dp,
    modifier: Modifier = Modifier,
    custom: List<Color> = emptyList(),
    gradStyle: Int = 0,
    angle: Float = 35f,
    spread: Float = .5f
) {
    val strength = if (isLight) .30f else .40f
    val a1 = remember(accent, custom) { custom.getOrNull(0) ?: accent }
    val a2 = remember(accent, custom) { custom.getOrNull(1) ?: shiftHue(a1, 38f) }
    val a3 = remember(accent, custom, a1, a2) { custom.getOrNull(2) ?: if (custom.size > 1) mixColors(a1, a2) else shiftHue(a1, -42f) }
    val sp = spread.coerceIn(0f, 1f)
    val smear = (sp * 34f).dp + blurDp

    Box(modifier.background(base).clipToBounds()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { if (smear > 0.dp) { scaleX = 1.14f; scaleY = 1.14f } }
                .then(if (smear > 1.dp) Modifier.blur(smear, BlurredEdgeTreatment.Unbounded) else Modifier)
        ) {
            val w = size.width
            val h = size.height
            val diag = max(w, h)
            val centre = Offset(w / 2f, h / 2f)
            // Size of every colour field grows with spread.
            val grow = .75f + sp * .95f
            val big = Size(diag * 4f, diag * 4f)
            val bigTL = Offset(centre.x - diag * 2f, centre.y - diag * 2f)
            fun fill(brush: Brush) = drawRect(brush, topLeft = bigTL, size = big)

            withTransform({ rotate(angle - 35f, centre) }) {
                when (gradStyle) {
                    1 -> { // Beams: wide soft light shafts from the top-right.
                        val origin = Offset(w * 1.0f, -h * .10f)
                        val beams = 6
                        for (i in 0 until beams) {
                            val ang = PI.toFloat() * (.52f + .085f * i)
                            val half = (.05f + .05f * sp) * (1f + .25f * (i % 2))
                            val len = diag * 3f
                            val path = Path().apply {
                                moveTo(origin.x, origin.y)
                                lineTo(origin.x + len * cos(ang - half), origin.y + len * sin(ang - half))
                                lineTo(origin.x + len * cos(ang + half), origin.y + len * sin(ang + half))
                                close()
                            }
                            val c = when (i % 3) { 0 -> a1; 1 -> a2; else -> a3 }
                            drawPath(path, Brush.linearGradient(
                                listOf(c.copy(alpha = strength * 1.15f), c.copy(alpha = strength * .25f), Color.Transparent),
                                start = origin, end = Offset(origin.x - diag * .2f, origin.y + diag * 1.3f)))
                        }
                        fill(Brush.radialGradient(listOf(a1.copy(alpha = strength * .8f), Color.Transparent),
                            center = origin, radius = diag * .7f * grow))
                    }
                    2 -> { // Blobs: organic overlapping colour clouds.
                        val blobs = listOf(
                            Triple(Offset(w * .15f, h * .16f), .34f, a1),
                            Triple(Offset(w * .86f, h * .28f), .30f, a2),
                            Triple(Offset(w * .30f, h * .60f), .38f, a3),
                            Triple(Offset(w * .90f, h * .80f), .32f, a1),
                            Triple(Offset(w * .08f, h * .95f), .28f, a2)
                        )
                        blobs.forEachIndexed { i, (c0, rf, col) ->
                            val r = diag * rf * grow
                            // Oil strokes: several offset copies along a curve smear the paint.
                            val strokes = 1 + (sp * 4f).toInt()
                            for (k in 0 until strokes) {
                                val t = k / strokes.toFloat() * 2f * PI.toFloat() + i
                                val c = Offset(c0.x + cos(t) * r * .35f * sp, c0.y + sin(t) * r * .35f * sp)
                                drawCircle(Brush.radialGradient(
                                    listOf(col.copy(alpha = strength * 1.15f / (1f + k * .35f)), col.copy(alpha = strength * .3f), Color.Transparent),
                                    center = c, radius = r), radius = r, center = c)
                            }
                        }
                    }
                    3 -> { // Aurora: curved flowing ribbons.
                        val cols = listOf(a1, a2, a3, a1)
                        for (i in 0 until 4) {
                            val y0 = h * (.10f + .27f * i)
                            val amp = h * (.10f + .10f * sp)
                            val path = Path().apply {
                                moveTo(-w * .6f, y0)
                                cubicTo(w * .15f, y0 - amp, w * .55f, y0 + amp, w * 1.6f, y0 - amp * .4f)
                            }
                            drawPath(path, color = cols[i].copy(alpha = strength * 1.25f),
                                style = Stroke(width = diag * (.10f + .12f * sp) * grow, cap = StrokeCap.Round))
                        }
                    }
                    4 -> { // Linear: three-stop diagonal sweep.
                        val k = strength * 1.7f
                        fill(Brush.linearGradient(
                            listOf(a1.copy(alpha = k), a2.copy(alpha = k * .9f), a3.copy(alpha = k * .75f)),
                            start = Offset(centre.x - diag * .6f, centre.y - diag * .6f),
                            end = Offset(centre.x + diag * .6f, centre.y + diag * .6f)))
                    }
                    5 -> { // Spotlight: a pool of colour from the top.
                        val k = strength * 1.8f
                        fill(Brush.radialGradient(
                            listOf(a1.copy(alpha = k), a2.copy(alpha = k * .55f), Color.Transparent),
                            center = Offset(w * .5f, h * .15f), radius = diag * .95f * grow))
                    }
                    else -> { // Mesh: four big soft fields around the corners.
                        val fields = listOf(
                            Triple(Offset(w * .05f, h * .05f), a1, 1f),
                            Triple(Offset(w * .98f, h * .38f), a2, .9f),
                            Triple(Offset(w * .20f, h * 1.0f), a3, .85f),
                            Triple(Offset(w * .85f, h * .95f), a1, .6f)
                        )
                        fields.forEach { (c, col, f) ->
                            fill(Brush.radialGradient(
                                listOf(col.copy(alpha = strength * 1.2f * f), Color.Transparent),
                                center = c, radius = diag * .78f * grow))
                        }
                    }
                }
            }
        }
    }
}

private fun mixColors(a: Color, b: Color): Color = androidx.compose.ui.graphics.lerp(a, b, .5f)

private fun shiftHue(color: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(android.graphics.Color.argb(255, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()), hsv)
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    hsv[1] = (hsv[1] * .95f).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}


/** Second gradient colour derived from the accent (used when the user has not picked one). */
fun gradientPartner(accent: Color): Color = shiftHue(accent, 38f)
