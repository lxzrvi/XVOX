package com.xvox.music.player.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
fun XvoxNowPlayingBackdrop(
    currentColor: Color,
    adjacentColor: Color,
    swipeFraction: Float,
    modifier: Modifier = Modifier
) {
    val progress =
        swipeFraction
            .coerceIn(
                0f,
                1f
            )

    val dominant =
        lerp(
            currentColor,
            adjacentColor,
            progress
        )

    val light =
        lerp(
            dominant,
            Color.White,
            0.18f
        )

    val deep =
        lerp(
            dominant,
            Color.Black,
            0.28f
        )

    Canvas(
        modifier =
            modifier.fillMaxSize()
    ) {
        drawRect(
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            light,
                            dominant,
                            deep
                        ),
                    start =
                        Offset.Zero,
                    end =
                        Offset(
                            size.width,
                            size.height
                        )
                )
        )

        drawRect(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            light.copy(
                                alpha = 0.74f
                            ),
                            Color.Transparent
                        ),
                    center =
                        Offset(
                            size.width *
                                0.20f,
                            size.height *
                                0.18f
                        ),
                    radius =
                        size.maxDimension *
                            0.72f
                )
        )

        drawRect(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            dominant.copy(
                                alpha = 0.54f
                            ),
                            Color.Transparent
                        ),
                    center =
                        Offset(
                            size.width *
                                0.82f,
                            size.height *
                                0.68f
                        ),
                    radius =
                        size.maxDimension *
                            0.78f
                )
        )
    }
}
