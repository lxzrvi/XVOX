package com.xvox.music.core.ui.miniplayer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

enum class XvoxMiniIcon {
    PLAY,
    PAUSE,
    HEART,
    ADD,
    CLOSE
}

@Composable
fun XvoxMiniPlayerIcon(
    icon: XvoxMiniIcon,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        when (icon) {
            XvoxMiniIcon.PLAY -> {
                val path =
                    Path().apply {
                        moveTo(
                            size.width * 0.18f,
                            size.height * 0.07f
                        )
                        lineTo(
                            size.width * 0.91f,
                            size.height * 0.42f
                        )
                        cubicTo(
                            size.width * 1.02f,
                            size.height * 0.47f,
                            size.width * 1.02f,
                            size.height * 0.53f,
                            size.width * 0.91f,
                            size.height * 0.58f
                        )
                        lineTo(
                            size.width * 0.18f,
                            size.height * 0.93f
                        )
                        cubicTo(
                            size.width * 0.08f,
                            size.height * 0.98f,
                            0f,
                            size.height * 0.91f,
                            0f,
                            size.height * 0.80f
                        )
                        lineTo(
                            0f,
                            size.height * 0.20f
                        )
                        cubicTo(
                            0f,
                            size.height * 0.09f,
                            size.width * 0.08f,
                            size.height * 0.02f,
                            size.width * 0.18f,
                            size.height * 0.07f
                        )
                        close()
                    }

                drawPath(
                    path = path,
                    color = color
                )
            }

            XvoxMiniIcon.PAUSE -> {
                val radius =
                    size.minDimension * 0.13f

                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        0f,
                        0f
                    ),
                    size = Size(
                        size.width * 0.42f,
                        size.height
                    ),
                    cornerRadius =
                        CornerRadius(radius)
                )

                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        size.width * 0.58f,
                        0f
                    ),
                    size = Size(
                        size.width * 0.42f,
                        size.height
                    ),
                    cornerRadius =
                        CornerRadius(radius)
                )
            }

            XvoxMiniIcon.HEART -> {
                val path =
                    Path().apply {
                        moveTo(
                            size.width * 0.50f,
                            size.height * 0.89f
                        )
                        cubicTo(
                            size.width * 0.25f,
                            size.height * 0.74f,
                            size.width * 0.08f,
                            size.height * 0.54f,
                            size.width * 0.08f,
                            size.height * 0.35f
                        )
                        cubicTo(
                            size.width * 0.08f,
                            size.height * 0.20f,
                            size.width * 0.18f,
                            size.height * 0.10f,
                            size.width * 0.32f,
                            size.height * 0.10f
                        )
                        cubicTo(
                            size.width * 0.41f,
                            size.height * 0.10f,
                            size.width * 0.47f,
                            size.height * 0.15f,
                            size.width * 0.50f,
                            size.height * 0.23f
                        )
                        cubicTo(
                            size.width * 0.53f,
                            size.height * 0.15f,
                            size.width * 0.59f,
                            size.height * 0.10f,
                            size.width * 0.68f,
                            size.height * 0.10f
                        )
                        cubicTo(
                            size.width * 0.82f,
                            size.height * 0.10f,
                            size.width * 0.92f,
                            size.height * 0.20f,
                            size.width * 0.92f,
                            size.height * 0.35f
                        )
                        cubicTo(
                            size.width * 0.92f,
                            size.height * 0.54f,
                            size.width * 0.75f,
                            size.height * 0.74f,
                            size.width * 0.50f,
                            size.height * 0.89f
                        )
                        close()
                    }

                drawPath(
                    path = path,
                    color = color
                )
            }

            XvoxMiniIcon.ADD -> {
                val thickness =
                    size.minDimension * 0.25f

                val radius =
                    thickness / 2f

                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        0f,
                        size.height / 2f -
                            thickness / 2f
                    ),
                    size = Size(
                        size.width,
                        thickness
                    ),
                    cornerRadius =
                        CornerRadius(radius)
                )

                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        size.width / 2f -
                            thickness / 2f,
                        0f
                    ),
                    size = Size(
                        thickness,
                        size.height
                    ),
                    cornerRadius =
                        CornerRadius(radius)
                )
            }

            XvoxMiniIcon.CLOSE -> {
                val path =
                    Path().apply {
                        moveTo(
                            size.width * 0.59f,
                            size.height * 0.50f
                        )
                        lineTo(
                            size.width * 0.86f,
                            size.height * 0.23f
                        )
                        cubicTo(
                            size.width * 0.92f,
                            size.height * 0.17f,
                            size.width * 0.92f,
                            size.height * 0.10f,
                            size.width * 0.86f,
                            size.height * 0.06f
                        )
                        cubicTo(
                            size.width * 0.81f,
                            0f,
                            size.width * 0.74f,
                            0f,
                            size.width * 0.68f,
                            size.height * 0.06f
                        )
                        lineTo(
                            size.width * 0.50f,
                            size.height * 0.32f
                        )
                        lineTo(
                            size.width * 0.23f,
                            size.height * 0.06f
                        )
                        cubicTo(
                            size.width * 0.17f,
                            0f,
                            size.width * 0.10f,
                            0f,
                            size.width * 0.05f,
                            size.height * 0.06f
                        )
                        cubicTo(
                            0f,
                            size.height * 0.10f,
                            0f,
                            size.height * 0.17f,
                            size.width * 0.05f,
                            size.height * 0.23f
                        )
                        lineTo(
                            size.width * 0.32f,
                            size.height * 0.50f
                        )
                        lineTo(
                            size.width * 0.05f,
                            size.height * 0.77f
                        )
                        cubicTo(
                            0f,
                            size.height * 0.83f,
                            0f,
                            size.height * 0.90f,
                            size.width * 0.05f,
                            size.height * 0.95f
                        )
                        cubicTo(
                            size.width * 0.10f,
                            size.height,
                            size.width * 0.17f,
                            size.height,
                            size.width * 0.23f,
                            size.height * 0.95f
                        )
                        lineTo(
                            size.width * 0.50f,
                            size.height * 0.68f
                        )
                        lineTo(
                            size.width * 0.68f,
                            size.height * 0.95f
                        )
                        cubicTo(
                            size.width * 0.74f,
                            size.height,
                            size.width * 0.81f,
                            size.height,
                            size.width * 0.86f,
                            size.height * 0.95f
                        )
                        cubicTo(
                            size.width * 0.92f,
                            size.height * 0.90f,
                            size.width * 0.92f,
                            size.height * 0.83f,
                            size.width * 0.86f,
                            size.height * 0.77f
                        )
                        close()
                    }

                drawPath(
                    path = path,
                    color = color
                )
            }
        }
    }
}
