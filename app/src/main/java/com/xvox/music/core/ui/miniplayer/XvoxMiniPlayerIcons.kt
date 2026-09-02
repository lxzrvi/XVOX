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

enum class XvoxMiniIconType {
    PLAY,
    PAUSE,
    HEART,
    ADD,
    CLOSE
}

@Composable
fun XvoxMiniIcon(
    type: XvoxMiniIconType,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val stroke =
            size.minDimension *
                0.095f

        when (type) {
            XvoxMiniIconType.PLAY -> {
                val path =
                    Path().apply {
                        moveTo(
                            size.width * 0.34f,
                            size.height * 0.22f
                        )

                        lineTo(
                            size.width * 0.76f,
                            size.height * 0.50f
                        )

                        lineTo(
                            size.width * 0.34f,
                            size.height * 0.78f
                        )

                        close()
                    }

                drawPath(
                    path = path,
                    color = color
                )
            }

            XvoxMiniIconType.PAUSE -> {
                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.37f,
                        size.height * 0.27f
                    ),
                    end = Offset(
                        size.width * 0.37f,
                        size.height * 0.73f
                    ),
                    strokeWidth =
                        stroke * 1.35f,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.63f,
                        size.height * 0.27f
                    ),
                    end = Offset(
                        size.width * 0.63f,
                        size.height * 0.73f
                    ),
                    strokeWidth =
                        stroke * 1.35f,
                    cap = StrokeCap.Round
                )
            }

            XvoxMiniIconType.ADD -> {
                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.25f,
                        size.height * 0.5f
                    ),
                    end = Offset(
                        size.width * 0.75f,
                        size.height * 0.5f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.5f,
                        size.height * 0.25f
                    ),
                    end = Offset(
                        size.width * 0.5f,
                        size.height * 0.75f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            XvoxMiniIconType.CLOSE -> {
                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.3f,
                        size.height * 0.3f
                    ),
                    end = Offset(
                        size.width * 0.7f,
                        size.height * 0.7f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.7f,
                        size.height * 0.3f
                    ),
                    end = Offset(
                        size.width * 0.3f,
                        size.height * 0.7f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            XvoxMiniIconType.HEART -> {
                val path =
                    Path().apply {
                        moveTo(
                            size.width * 0.5f,
                            size.height * 0.76f
                        )

                        cubicTo(
                            size.width * 0.14f,
                            size.height * 0.55f,
                            size.width * 0.20f,
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
                            size.width * 0.80f,
                            size.height * 0.27f,
                            size.width * 0.86f,
                            size.height * 0.55f,
                            size.width * 0.5f,
                            size.height * 0.76f
                        )
                    }

                drawPath(
                    path = path,
                    color = color,
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
