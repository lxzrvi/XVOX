package com.xvox.music.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap

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
                            size.width * 0.34f,
                            size.height * 0.23f
                        )

                        lineTo(
                            size.width * 0.76f,
                            size.height * 0.50f
                        )

                        lineTo(
                            size.width * 0.34f,
                            size.height * 0.77f
                        )

                        close()
                    }

                drawPath(
                    path = path,
                    color = color
                )
            }

            PlaybackIconType.PAUSE -> {
                val stroke =
                    size.width * 0.17f

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.36f,
                        size.height * 0.28f
                    ),
                    end = Offset(
                        size.width * 0.36f,
                        size.height * 0.72f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.64f,
                        size.height * 0.28f
                    ),
                    end = Offset(
                        size.width * 0.64f,
                        size.height * 0.72f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
