package com.xvox.music.core.ui.chrome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Soft shapes with gradients behind the app: a few big rounded forms in the accent colour and a
 * neighbouring hue, blurred so they read as light rather than as objects. Sits on the plain theme
 * fill and is part of the glass source, so the bar, mini player and cards refract it.
 */
@Composable
fun XvoxShapeBackdrop(accent: Color, isLight: Boolean, blurred: Boolean, modifier: Modifier = Modifier) {
    val a = if (isLight) .22f else .30f
    val second = Color(
        red = (accent.blue * .8f + .1f).coerceIn(0f, 1f),
        green = (accent.red * .6f + .2f).coerceIn(0f, 1f),
        blue = (accent.green * .9f + .1f).coerceIn(0f, 1f)
    )
    Canvas(modifier.fillMaxSize().then(if (blurred) Modifier.blur(48.dp) else Modifier)) {
        val w = size.width
        val h = size.height
        // Large disc, top right.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(accent.copy(alpha = a), accent.copy(alpha = 0f)),
                center = Offset(w * .92f, h * .10f), radius = w * .75f
            ),
            radius = w * .75f, center = Offset(w * .92f, h * .10f)
        )
        // Tilted rounded slab, left middle.
        rotate(degrees = -24f, pivot = Offset(w * .1f, h * .45f)) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(second.copy(alpha = a * .9f), accent.copy(alpha = 0f)),
                    start = Offset(0f, h * .35f), end = Offset(w * .8f, h * .6f)
                ),
                topLeft = Offset(-w * .2f, h * .36f),
                size = Size(w * .95f, h * .2f),
                cornerRadius = CornerRadius(h * .1f)
            )
        }
        // Disc, bottom left.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(second.copy(alpha = a * .8f), second.copy(alpha = 0f)),
                center = Offset(w * .1f, h * .92f), radius = w * .7f
            ),
            radius = w * .7f, center = Offset(w * .1f, h * .92f)
        )
    }
}
