package com.xvox.music.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
        when (type) {
            HomeHeaderIconType.SCAN -> {
                val s =
                    size.minDimension

                val stroke =
                    s * 0.075f

                drawLine(
                    color = color,
                    start = Offset(
                        s * 0.458f,
                        s * 0.25f
                    ),
                    end = Offset(
                        s * 0.542f,
                        s * 0.333f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = color,
                    start = Offset(
                        s * 0.542f,
                        s * 0.333f
                    ),
                    end = Offset(
                        s * 0.458f,
                        s * 0.417f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawArc(
                    color = color,
                    startAngle = 212f,
                    sweepAngle = 245f,
                    useCenter = false,
                    topLeft = Offset(
                        s * 0.25f,
                        s * 0.333f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        s * 0.50f,
                        s * 0.50f
                    ),
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round
                    )
                )

                drawLine(
                    color = color,
                    start = Offset(
                        s * 0.542f,
                        s * 0.75f
                    ),
                    end = Offset(
                        s * 0.458f,
                        s * 0.833f
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
                            size.height * 0.80f
                        )

                        cubicTo(
                            size.width * 0.29f,
                            size.height * 0.67f,
                            size.width * 0.16f,
                            size.height * 0.51f,
                            size.width * 0.16f,
                            size.height * 0.36f
                        )

                        cubicTo(
                            size.width * 0.16f,
                            size.height * 0.23f,
                            size.width * 0.25f,
                            size.height * 0.14f,
                            size.width * 0.37f,
                            size.height * 0.14f
                        )

                        cubicTo(
                            size.width * 0.44f,
                            size.height * 0.14f,
                            size.width * 0.49f,
                            size.height * 0.18f,
                            size.width * 0.50f,
                            size.height * 0.25f
                        )

                        cubicTo(
                            size.width * 0.53f,
                            size.height * 0.18f,
                            size.width * 0.58f,
                            size.height * 0.14f,
                            size.width * 0.64f,
                            size.height * 0.14f
                        )

                        cubicTo(
                            size.width * 0.77f,
                            size.height * 0.14f,
                            size.width * 0.84f,
                            size.height * 0.23f,
                            size.width * 0.84f,
                            size.height * 0.36f
                        )

                        cubicTo(
                            size.width * 0.84f,
                            size.height * 0.52f,
                            size.width * 0.71f,
                            size.height * 0.68f,
                            size.width * 0.50f,
                            size.height * 0.80f
                        )

                        close()
                    }

                drawPath(
                    path = path,
                    color = color
                )
            }

            HomeHeaderIconType.PLAYLIST -> {
                val stroke =
                    size.minDimension * 0.09f

                val ys =
                    floatArrayOf(
                        0.32f,
                        0.50f,
                        0.68f
                    )

                ys.forEach { y ->
                    drawLine(
                        color = color,
                        start = Offset(
                            size.width * 0.30f,
                            size.height * y
                        ),
                        end = Offset(
                            size.width * 0.76f,
                            size.height * y
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                val triangle =
                    Path().apply {
                        moveTo(
                            size.width * 0.19f,
                            size.height * 0.25f
                        )
                        lineTo(
                            size.width * 0.30f,
                            size.height * 0.32f
                        )
                        lineTo(
                            size.width * 0.19f,
                            size.height * 0.39f
                        )
                        close()
                    }

                drawPath(
                    path = triangle,
                    color = color
                )
            }

            HomeHeaderIconType.SONGS -> {
                val note =
                    Path().apply {
                        moveTo(
                            size.width * 0.50f,
                            size.height * 0.20f
                        )

                        lineTo(
                            size.width * 0.50f,
                            size.height * 0.63f
                        )

                        cubicTo(
                            size.width * 0.50f,
                            size.height * 0.76f,
                            size.width * 0.42f,
                            size.height * 0.82f,
                            size.width * 0.30f,
                            size.height * 0.82f
                        )

                        cubicTo(
                            size.width * 0.17f,
                            size.height * 0.82f,
                            size.width * 0.14f,
                            size.height * 0.73f,
                            size.width * 0.14f,
                            size.height * 0.66f
                        )

                        cubicTo(
                            size.width * 0.14f,
                            size.height * 0.56f,
                            size.width * 0.22f,
                            size.height * 0.51f,
                            size.width * 0.34f,
                            size.height * 0.51f
                        )

                        cubicTo(
                            size.width * 0.42f,
                            size.height * 0.51f,
                            size.width * 0.47f,
                            size.height * 0.48f,
                            size.width * 0.47f,
                            size.height * 0.42f
                        )

                        lineTo(
                            size.width * 0.47f,
                            size.height * 0.25f
                        )

                        cubicTo(
                            size.width * 0.47f,
                            size.height * 0.20f,
                            size.width * 0.50f,
                            size.height * 0.18f,
                            size.width * 0.55f,
                            size.height * 0.18f
                        )

                        cubicTo(
                            size.width * 0.65f,
                            size.height * 0.18f,
                            size.width * 0.66f,
                            size.height * 0.27f,
                            size.width * 0.79f,
                            size.height * 0.27f
                        )

                        lineTo(
                            size.width * 0.79f,
                            size.height * 0.43f
                        )

                        cubicTo(
                            size.width * 0.66f,
                            size.height * 0.43f,
                            size.width * 0.64f,
                            size.height * 0.35f,
                            size.width * 0.55f,
                            size.height * 0.35f
                        )

                        lineTo(
                            size.width * 0.55f,
                            size.height * 0.63f
                        )

                        close()
                    }

                drawPath(
                    path = note,
                    color = color
                )
            }
        }
    }
}
