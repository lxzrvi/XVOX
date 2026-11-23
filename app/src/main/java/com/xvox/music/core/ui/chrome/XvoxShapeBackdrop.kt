package com.xvox.music.core.ui.chrome

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A tall accent wash that starts saturated at the top edge and melts into the page colour by
 * roughly two thirds of the screen. It moves with the content: [scroll] is how far the page has
 * been scrolled in px, and the wash rises by that much and leaves the screen.
 */
fun Modifier.xvoxHeaderWash(accent: Color, isLight: Boolean, scroll: () -> Int): Modifier = drawBehind {
    val top = -scroll().toFloat()
    val h = size.height * .68f
    if (top + h <= 0f) return@drawBehind
    val a = if (isLight) .95f else .60f
    drawRect(
        brush = Brush.verticalGradient(
            0f to accent.copy(alpha = a),
            .22f to accent.copy(alpha = a * .88f),
            .45f to accent.copy(alpha = a * .52f),
            .72f to accent.copy(alpha = a * .18f),
            1f to accent.copy(alpha = 0f),
            startY = top,
            endY = top + h
        ),
        topLeft = androidx.compose.ui.geometry.Offset(0f, top),
        size = androidx.compose.ui.geometry.Size(size.width, h)
    )
}
