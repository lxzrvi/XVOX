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
 * Default (no photo) app background, generated from the accent + theme.
 * style 0.0 = soft accent MESH gradient, 0.5 = RAYS, 1.0 = organic PATCHES; in-between values
 * cross-fade and drift the shapes, so one slider walks through all textures.
 * It is drawn once (static), so it costs nothing while scrolling; [blurDp] blurs it.
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
    /** 0 Mesh, 1 Rays, 2 Patches, 3 Aurora, 4 Linear, 5 Spotlight. */
    gradStyle: Int = 0,
    /** Rotation of the whole pattern in degrees. */
    angle: Float = 35f
) {
    val s = when (gradStyle) { 0 -> 0f; 1 -> .5f; 2 -> 1f; else -> -1f }
    val strength = if (isLight) .26f else .34f
    val a1 = remember(accent, custom) { custom.getOrNull(0) ?: shiftHue(accent, 0f) }
    val a2 = remember(accent, custom) { custom.getOrNull(1) ?: shiftHue(a1, 38f) }
    val a3 = remember(accent, custom) { custom.getOrNull(2) ?: shiftHue(a1, -42f) }
    fun tri(center: Float) = (1f - (s - center).let { if (it < 0) -it else it } / .5f).coerceIn(0f, 1f)
    val wMesh = tri(0f)
    val wRays = tri(.5f)
    val wPatch = tri(1f)

    Box(modifier.background(base).clipToBounds()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { if (blurDp > 0.dp) { scaleX = 1.12f; scaleY = 1.12f } }
                .then(if (blurDp > 0.dp) Modifier.blur(blurDp, BlurredEdgeTreatment.Unbounded) else Modifier)
        ) {
            val w = size.width
            val h = size.height
            val diag = max(w, h)
            val drift = s * 2f * PI.toFloat()

            val centre = Offset(w / 2f, h / 2f)
            withTransform({
                rotate(angle - 35f, centre)
                scale(1.45f, 1.45f, centre)
            }) {
            if (gradStyle == 3) {
                // Aurora: soft diagonal ribbons.
                val k = strength * 1.1f
                val cols = listOf(a1, a2, a3, a1)
                for (i in 0 until 4) {
                    val y = h * (.12f + .26f * i)
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, cols[i].copy(alpha = k), Color.Transparent),
                            start = Offset(0f, y - diag * .12f), end = Offset(w, y + diag * .12f)
                        )
                    )
                }
            }
            if (gradStyle == 4) {
                val k = strength * 1.5f
                drawRect(
                    Brush.linearGradient(
                        listOf(a1.copy(alpha = k), a2.copy(alpha = k * .85f), a3.copy(alpha = k * .7f)),
                        start = Offset(w * .1f, h * .05f), end = Offset(w * .9f, h * .95f)
                    )
                )
            }
            if (gradStyle == 5) {
                val k = strength * 1.6f
                drawRect(
                    Brush.radialGradient(
                        listOf(a1.copy(alpha = k), a2.copy(alpha = k * .5f), Color.Transparent),
                        center = Offset(w * .5f, h * .22f), radius = diag * .85f
                    )
                )
            }
            if (wMesh > 0.01f) {
                val k = strength * wMesh
                drawRect(Brush.radialGradient(listOf(a1.copy(alpha = k), Color.Transparent),
                    center = Offset(w * (.15f + .10f * cos(drift)), h * .10f), radius = diag * .75f))
                drawRect(Brush.radialGradient(listOf(a2.copy(alpha = k * .9f), Color.Transparent),
                    center = Offset(w * .95f, h * (.45f + .08f * sin(drift))), radius = diag * .70f))
                drawRect(Brush.radialGradient(listOf(a3.copy(alpha = k * .8f), Color.Transparent),
                    center = Offset(w * (.30f + .12f * sin(drift)), h * 1.0f), radius = diag * .80f))
            }
            if (wRays > 0.01f) {
                val k = strength * .9f * wRays
                val origin = Offset(w * (.80f + .10f * cos(drift)), -h * .06f)
                val rays = 9
                for (i in 0 until rays) {
                    val ang = (PI.toFloat() * (.50f + .02f * i) + drift * .08f)
                    val spread = .075f
                    val len = diag * 2f
                    val p = Path().apply {
                        moveTo(origin.x, origin.y)
                        lineTo(origin.x + len * cos(ang - spread), origin.y + len * sin(ang - spread))
                        lineTo(origin.x + len * cos(ang + spread), origin.y + len * sin(ang + spread))
                        close()
                    }
                    val c = if (i % 3 == 0) a1 else if (i % 3 == 1) a2 else a3
                    drawPath(p, Brush.linearGradient(
                        listOf(c.copy(alpha = k), Color.Transparent),
                        start = origin, end = Offset(origin.x, origin.y + diag * 1.1f)))
                }
            }
            if (wPatch > 0.01f) {
                val k = strength * 1.1f * wPatch
                val blobs = listOf(
                    Triple(Offset(w * .20f, h * .18f), diag * .34f, a1),
                    Triple(Offset(w * .82f, h * .30f), diag * .28f, a2),
                    Triple(Offset(w * .35f, h * .62f), diag * .36f, a3),
                    Triple(Offset(w * .88f, h * .78f), diag * .30f, a1),
                    Triple(Offset(w * .10f, h * .92f), diag * .26f, a2)
                )
                blobs.forEachIndexed { i, (c0, r, col) ->
                    val c = Offset(c0.x + diag * .04f * cos(drift + i), c0.y + diag * .04f * sin(drift * 1.3f + i))
                    drawCircle(Brush.radialGradient(listOf(col.copy(alpha = k), col.copy(alpha = k * .35f), Color.Transparent),
                        center = c, radius = r), radius = r, center = c)
                }
            }
            }
        }
    }
}

private fun shiftHue(color: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(android.graphics.Color.argb(255, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()), hsv)
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    hsv[1] = (hsv[1] * .95f).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
