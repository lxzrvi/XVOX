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
    val dark = lerp(dominant, Color.Black, 0.45f)
    val deep = lerp(dominant, Color.Black, 0.72f)
    val glow = lerp(dominant, Color.White, 0.22f)
    val midGlow = lerp(dominant, Color.White, 0.08f)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Base dark vertical gradient
        drawRect(
            brush = Brush.verticalGradient(
                0f to dark.copy(alpha = 0.95f),
                0.40f to deep,
                1f to Color(0xFF08080A)
            )
        )

        // Top-left lush ambient glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    glow.copy(alpha = 0.85f),
                    dominant.copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.22f, size.height * 0.20f),
                radius = size.maxDimension * 0.75f
            )
        )

        // Center-right vibrant radiant glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    midGlow.copy(alpha = 0.75f),
                    dominant.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.80f, size.height * 0.45f),
                radius = size.maxDimension * 0.80f
            )
        )

        // Bottom subtle soft glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    dominant.copy(alpha = 0.30f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.50f, size.height * 0.85f),
                radius = size.maxDimension * 0.65f
            )
        )
    }
}
