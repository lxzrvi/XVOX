package com.xvox.music.player.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    modifier: Modifier = Modifier
) {
    val dark =
        lerp(
            dominant,
            Color.Black,
            if (dominant.luminance() > 0.45f) 0.30f else 0.18f
        )

    val deep =
        lerp(
            dominant,
            Color.Black,
            0.48f
        )

    val glow =
        lerp(
            dominant,
            Color.White,
            0.13f
        )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to glow,
                0.38f to dominant,
                1f to deep
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    glow.copy(alpha = 0.88f),
                    dominant.copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(
                    size.width * 0.18f,
                    size.height * 0.17f
                ),
                radius = size.maxDimension * 0.82f
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    dominant.copy(alpha = 0.82f),
                    dark.copy(alpha = 0.34f),
                    Color.Transparent
                ),
                center = Offset(
                    size.width * 0.86f,
                    size.height * 0.58f
                ),
                radius = size.maxDimension * 0.92f
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    dominant.copy(alpha = 0.58f),
                    Color.Transparent
                ),
                center = Offset(
                    size.width * 0.42f,
                    size.height * 0.92f
                ),
                radius = size.maxDimension * 0.72f
            )
        )
    }
}
