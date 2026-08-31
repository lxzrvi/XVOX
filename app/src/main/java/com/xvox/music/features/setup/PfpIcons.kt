package com.xvox.music.features.setup

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PfpIcon(
    type: PfpType,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.075f
        val center = Offset(size.width / 2f, size.height / 2f)

        when (type) {
            PfpType.HEART -> {
                val path = Path().apply {
                    moveTo(center.x, size.height * 0.82f)

                    cubicTo(
                        size.width * 0.12f,
                        size.height * 0.58f,
                        size.width * 0.16f,
                        size.height * 0.22f,
                        size.width * 0.37f,
                        size.height * 0.22f
                    )

                    cubicTo(
                        size.width * 0.47f,
                        size.height * 0.22f,
                        center.x,
                        size.height * 0.32f,
                        center.x,
                        size.height * 0.32f
                    )

                    cubicTo(
                        center.x,
                        size.height * 0.32f,
                        size.width * 0.53f,
                        size.height * 0.22f,
                        size.width * 0.63f,
                        size.height * 0.22f
                    )

                    cubicTo(
                        size.width * 0.84f,
                        size.height * 0.22f,
                        size.width * 0.88f,
                        size.height * 0.58f,
                        center.x,
                        size.height * 0.82f
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

            PfpType.STAR -> {
                val path = Path()

                repeat(10) { index ->
                    val radius =
                        if (index % 2 == 0) {
                            size.minDimension * 0.36f
                        } else {
                            size.minDimension * 0.16f
                        }

                    val angle =
                        -PI / 2 + index * PI / 5

                    val point = Offset(
                        center.x + cos(angle).toFloat() * radius,
                        center.y + sin(angle).toFloat() * radius
                    )

                    if (index == 0) {
                        path.moveTo(point.x, point.y)
                    } else {
                        path.lineTo(point.x, point.y)
                    }
                }

                path.close()

                drawPath(
                    path,
                    color,
                    style = Stroke(
                        width = stroke,
                        join = StrokeJoin.Round
                    )
                )
            }

            PfpType.CIRCLE -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.29f,
                    center = center,
                    style = Stroke(width = stroke)
                )

                drawCircle(
                    color = color,
                    radius = stroke * 0.75f,
                    center = center
                )
            }

            PfpType.DIAMOND -> {
                val path = Path().apply {
                    moveTo(center.x, size.height * 0.14f)
                    lineTo(size.width * 0.84f, center.y)
                    lineTo(center.x, size.height * 0.86f)
                    lineTo(size.width * 0.16f, center.y)
                    close()
                }

                drawPath(
                    path,
                    color,
                    style = Stroke(
                        width = stroke,
                        join = StrokeJoin.Round
                    )
                )
            }

            PfpType.HEXAGON -> {
                val path = Path()

                repeat(6) { index ->
                    val angle =
                        -PI / 2 + index * PI / 3

                    val radius =
                        size.minDimension * 0.34f

                    val point = Offset(
                        center.x + cos(angle).toFloat() * radius,
                        center.y + sin(angle).toFloat() * radius
                    )

                    if (index == 0) {
                        path.moveTo(point.x, point.y)
                    } else {
                        path.lineTo(point.x, point.y)
                    }
                }

                path.close()

                drawPath(
                    path,
                    color,
                    style = Stroke(
                        width = stroke,
                        join = StrokeJoin.Round
                    )
                )
            }

            PfpType.CUSTOM -> {
                drawLine(
                    color = color,
                    start = Offset(
                        center.x,
                        size.height * 0.25f
                    ),
                    end = Offset(
                        center.x,
                        size.height * 0.75f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.25f,
                        center.y
                    ),
                    end = Offset(
                        size.width * 0.75f,
                        center.y
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            PfpType.DEFAULT -> Unit
        }
    }
}
