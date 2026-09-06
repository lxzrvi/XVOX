package com.xvox.music.player.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    modifier: Modifier = Modifier
) {
    val dark = lerp(dominant, Color.Black, 0.55f)
    val deep = lerp(dominant, Color.Black, 0.82f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08080A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to dark,
                    0.45f to deep,
                    1f to Color(0xFF08080A)
                )
            )
        }
    }
}
