package com.xvox.music.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

enum class PlaybackIconType {
    PLAY,
    PAUSE
}

@Composable
fun PlaybackIcon(
    type: PlaybackIconType,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        when (type) {
            PlaybackIconType.PLAY -> {
                val path =
                    Path().apply {
                        moveTo(
                            size.width * 0.22f,
                            size.height * 0.08f
                        )

                        cubicTo(
                            size.width * 0.11f,
                            size.height * 0.02f,
                            size.width * 0.04f,
                            size.height * 0.10f,
                            size.width * 0.04f,
                            size.height * 0.21f
                        )

                        lineTo(
                            size.width * 0.04f,
                            size.height * 0.79f
                        )

                        cubicTo(
                            size.width * 0.04f,
                            size.height * 0.90f,
                            size.width * 0.11f,
                            size.height * 0.98f,
                            size.width * 0.22f,
                            size.height * 0.92f
                        )

                        lineTo(
                            size.width * 0.88f,
                            size.height * 0.62f
                        )

                        cubicTo(
                            size.width * 1.02f,
                            size.height * 0.55f,
                            size.width * 1.02f,
                            size.height * 0.45f,
                            size.width * 0.88f,
                            size.height * 0.38f
                        )

                        close()
                    }

                drawPath(
                    path = path,
                    color = color
                )
            }

            PlaybackIconType.PAUSE -> {
                val radius =
                    size.minDimension * 0.14f

                drawRoundRect(
                    color = color,
                    topLeft =
                        androidx.compose.ui.geometry.Offset(
                            size.width * 0.08f,
                            size.height * 0.04f
                        ),
                    size = Size(
                        size.width * 0.34f,
                        size.height * 0.92f
                    ),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(
                            radius,
                            radius
                        )
                )

                drawRoundRect(
                    color = color,
                    topLeft =
                        androidx.compose.ui.geometry.Offset(
                            size.width * 0.58f,
                            size.height * 0.04f
                        ),
                    size = Size(
                        size.width * 0.34f,
                        size.height * 0.92f
                    ),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(
                            radius,
                            radius
                        )
                )
            }
        }
    }
}
