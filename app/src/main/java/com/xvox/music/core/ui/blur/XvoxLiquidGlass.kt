package com.xvox.music.core.ui.blur

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Liquid-glass material: a light live blur (so shapes behind stay readable) plus three painted
 * layers on top: a top-lit body sheen, a diagonal specular streak, and a bright glass rim that is
 * strongest at the top-left and bottom-right corners. The rim is a translucent white gradient,
 * never a solid border line.
 *
 * [strength] scales every highlight (1 = full glossy pill, ~.5 = quiet bar).
 * The Haze style is remembered, so playback ticks and scrolling never rebuild it.
 */
@Composable
fun Modifier.xvoxLiquidGlass(
    shape: Shape,
    tint: Color,
    state: HazeState? = LocalXvoxSurfaceHazeState.current,
    blurRadius: Dp = 8.dp,
    tintAlpha: Float = .16f,
    strength: Float = 1f
): Modifier {
    val s = strength.coerceIn(0f, 1.5f)
    val style = remember(tint, blurRadius, tintAlpha) {
        HazeStyle(
            backgroundColor = tint,
            tint = HazeTint(tint.copy(alpha = tintAlpha)),
            blurRadius = blurRadius,
            noiseFactor = 0f,
            fallbackTint = HazeTint(tint.copy(alpha = (tintAlpha + .34f).coerceAtMost(.92f)))
        )
    }
    val clipped = clip(shape)
    val glass = if (state == null) {
        clipped.background(tint.copy(alpha = (tintAlpha + .34f).coerceAtMost(.92f)))
    } else {
        clipped.hazeEffect(state = state, style = style)
    }
    val rim = remember(s) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (.80f * s).coerceAtMost(1f)),
                Color.White.copy(alpha = (.10f * s).coerceAtMost(1f)),
                Color.White.copy(alpha = (.52f * s).coerceAtMost(1f))
            )
        )
    }
    val bodySheen = remember(s) {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = (.26f * s).coerceAtMost(1f)),
            .45f to Color.White.copy(alpha = (.04f * s).coerceAtMost(1f)),
            1f to Color.White.copy(alpha = (.13f * s).coerceAtMost(1f))
        )
    }
    val streakAlpha = (.30f * s).coerceAtMost(1f)

    // border() is outside drawWithContent, so the rim is painted on top of the sheen layers.
    return glass
        .border(width = 1.1.dp, brush = rim, shape = shape)
        .drawWithContent {
            drawContent()
            // 1. Body sheen: light falls from the top, a faint bounce returns at the bottom.
            drawRect(brush = bodySheen)
            // 2. Diagonal specular streak across the upper-left of the glass.
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = streakAlpha), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(size.width * .62f, size.height)
                )
            )
        }
}
