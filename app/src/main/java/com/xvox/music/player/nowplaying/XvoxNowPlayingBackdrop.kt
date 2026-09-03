package com.xvox.music.player.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun XvoxNowPlayingBackdrop(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(
                Color(0xFF0E0705)
            )
    ) {
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD06B3E)
                            .copy(
                                alpha = 0.60f
                            ),
                        Color.Transparent
                    ),
                    center =
                        Offset(
                            size.width *
                                0.25f,
                            size.height *
                                0.20f
                        ),
                    radius =
                        size.maxDimension *
                            0.68f
                )
        )

        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF792C16)
                            .copy(
                                alpha = 0.62f
                            ),
                        Color.Transparent
                    ),
                    center =
                        Offset(
                            size.width *
                                0.80f,
                            size.height *
                                0.35f
                        ),
                    radius =
                        size.maxDimension *
                            0.72f
                )
        )

        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF351208)
                            .copy(
                                alpha = 0.92f
                            ),
                        Color.Transparent
                    ),
                    center =
                        Offset(
                            size.width *
                                0.72f,
                            size.height *
                                0.87f
                        ),
                    radius =
                        size.maxDimension *
                            0.72f
                )
        )
    }
}
