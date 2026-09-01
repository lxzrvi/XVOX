package com.xvox.music.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale

enum class HomeHeaderIconType {
    SCAN,
    HEART,
    PLAYLIST,
    SONGS
}

@Composable
fun HomeHeaderIcon(
    type: HomeHeaderIconType,
    onClick: () -> Unit
) {
    val color =
        XvoxTheme.colors.primaryText

    Canvas(
        modifier = Modifier
            .size(36.dp)
            .xvoxPressScale(
                pressedScale = 0.90f,
                onClick = onClick
            )
    ) {
        val stroke =
            1.75.dp.toPx()

        when (type) {
            HomeHeaderIconType.SCAN -> {
                drawArc(
                    color = color,
                    startAngle = -72f,
                    sweepAngle = 285f,
                    useCenter = false,
                    topLeft = Offset(
                        size.width * 0.27f,
                        size.height * 0.27f
                    ),
                    size = Size(
                        size.width * 0.46f,
                        size.height * 0.46f
                    ),
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round
                    )
                )

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.72f,
                        size.height * 0.28f
                    ),
                    end = Offset(
                        size.width * 0.70f,
                        size.height * 0.42f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.72f,
                        size.height * 0.28f
                    ),
                    end = Offset(
                        size.width * 0.58f,
                        size.height * 0.31f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            HomeHeaderIconType.HEART -> {
                val path =
                    Path().apply {
                        moveTo(
                            size.width * 0.50f,
                            size.height * 0.72f
                        )

                        cubicTo(
                            size.width * 0.18f,
                            size.height * 0.54f,
                            size.width * 0.22f,
                            size.height * 0.30f,
                            size.width * 0.39f,
                            size.height * 0.30f
                        )

                        cubicTo(
                            size.width * 0.47f,
                            size.height * 0.30f,
                            size.width * 0.50f,
                            size.height * 0.38f,
                            size.width * 0.50f,
                            size.height * 0.38f
                        )

                        cubicTo(
                            size.width * 0.50f,
                            size.height * 0.38f,
                            size.width * 0.53f,
                            size.height * 0.30f,
                            size.width * 0.61f,
                            size.height * 0.30f
                        )

                        cubicTo(
                            size.width * 0.78f,
                            size.height * 0.30f,
                            size.width * 0.82f,
                            size.height * 0.54f,
                            size.width * 0.50f,
                            size.height * 0.72f
                        )

                        close()
                    }

                drawPath(
                    path = path,
                    color = color
                )
            }

            HomeHeaderIconType.PLAYLIST -> {
                listOf(
                    0.35f,
                    0.50f,
                    0.65f
                ).forEach { y ->
                    drawLine(
                        color = color,
                        start = Offset(
                            size.width * 0.30f,
                            size.height * y
                        ),
                        end = Offset(
                            size.width * 0.70f,
                            size.height * y
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            HomeHeaderIconType.SONGS -> {
                val x =
                    size.width * 0.56f

                drawLine(
                    color = color,
                    start = Offset(
                        x,
                        size.height * 0.27f
                    ),
                    end = Offset(
                        x,
                        size.height * 0.63f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = color,
                    start = Offset(
                        x,
                        size.height * 0.27f
                    ),
                    end = Offset(
                        size.width * 0.72f,
                        size.height * 0.32f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = color,
                    radius =
                        size.width * 0.075f,
                    center = Offset(
                        size.width * 0.48f,
                        size.height * 0.67f
                    )
                )
            }
        }
    }
}
