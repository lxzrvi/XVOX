package com.xvox.music.core.ui.miniplayer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

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
        val stroke =
            size.minDimension *
                0.095f

        when (icon) {
            XvoxMiniIcon.PLAY -> {
                val path =
                    Path().apply {
                        moveTo(
                            size.width * 0.34f,
                            size.height * 0.22f
                        )

                        lineTo(
                            size.width * 0.77f,
                            size.height * 0.5f
                        )

                        lineTo(
                            size.width * 0.34f,
                            size.height * 0.78f
                        )

                        close()
                    }

                drawPath(
                    path,
                    color
                )
            }

            XvoxMiniIcon.PAUSE -> {
                drawLine(
                    color,
                    Offset(
                        size.width * 0.36f,
                        size.height * 0.27f
                    ),
                    Offset(
                        size.width * 0.36f,
                        size.height * 0.73f
                    ),
                    stroke * 1.35f,
                    StrokeCap.Round
                )

                drawLine(
                    color,
                    Offset(
                        size.width * 0.64f,
                        size.height * 0.27f
                    ),
                    Offset(
                        size.width * 0.64f,
                        size.height * 0.73f
                    ),
                    stroke * 1.35f,
                    StrokeCap.Round
                )
            }

            XvoxMiniIcon.ADD -> {
                drawLine(
                    color,
                    Offset(
                        size.width * 0.25f,
                        size.height * 0.5f
                    ),
                    Offset(
                        size.width * 0.75f,
                        size.height * 0.5f
                    ),
                    stroke,
                    StrokeCap.Round
                )

                drawLine(
                    color,
                    Offset(
                        size.width * 0.5f,
                        size.height * 0.25f
                    ),
                    Offset(
                        size.width * 0.5f,
                        size.height * 0.75f
                    ),
                    stroke,
                    StrokeCap.Round
                )
            }

            XvoxMiniIcon.CLOSE -> {
                drawLine(
                    color,
                    Offset(
                        size.width * 0.3f,
                        size.height * 0.3f
                    ),
                    Offset(
                        size.width * 0.7f,
                        size.height * 0.7f
                    ),
                    stroke,
                    StrokeCap.Round
                )

                drawLine(
                    color,
                    Offset(
                        size.width * 0.7f,
                        size.height * 0.3f
                    ),
                    Offset(
                        size.width * 0.3f,
                        size.height * 0.7f
                    ),
                    stroke,
                    StrokeCap.Round
                )
            }

            XvoxMiniIcon.HEART -> {
                val path =
                    Path().apply {
                        moveTo(
                            size.width * 0.5f,
                            size.height * 0.76f
                        )

                        cubicTo(
                            size.width * 0.14f,
                            size.height * 0.54f,
                            size.width * 0.19f,
                            size.height * 0.27f,
                            size.width * 0.39f,
                            size.height * 0.27f
                        )

                        cubicTo(
                            size.width * 0.47f,
                            size.height * 0.27f,
                            size.width * 0.5f,
                            size.height * 0.36f,
                            size.width * 0.5f,
                            size.height * 0.36f
                        )

                        cubicTo(
                            size.width * 0.5f,
                            size.height * 0.36f,
                            size.width * 0.53f,
                            size.height * 0.27f,
                            size.width * 0.61f,
                            size.height * 0.27f
                        )

                        cubicTo(
                            size.width * 0.81f,
                            size.height * 0.27f,
                            size.width * 0.86f,
                            size.height * 0.54f,
                            size.width * 0.5f,
                            size.height * 0.76f
                        )
                    }

                drawPath(
                    path,
                    color,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
