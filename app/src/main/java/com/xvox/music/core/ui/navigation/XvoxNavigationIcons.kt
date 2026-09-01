package com.xvox.music.core.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun XvoxNavigationIcon(
    destination: XvoxDestination,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val stroke =
            size.minDimension * 0.085f

        val lineStyle =
            Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

        when (destination) {
            XvoxDestination.HOME -> {
                val roof =
                    Path().apply {
                        moveTo(
                            size.width * 0.14f,
                            size.height * 0.45f
                        )

                        lineTo(
                            size.width * 0.50f,
                            size.height * 0.16f
                        )

                        lineTo(
                            size.width * 0.86f,
                            size.height * 0.45f
                        )
                    }

                drawPath(
                    path = roof,
                    color = color,
                    style = lineStyle
                )

                val house =
                    Path().apply {
                        moveTo(
                            size.width * 0.23f,
                            size.height * 0.40f
                        )

                        lineTo(
                            size.width * 0.23f,
                            size.height * 0.84f
                        )

                        lineTo(
                            size.width * 0.77f,
                            size.height * 0.84f
                        )

                        lineTo(
                            size.width * 0.77f,
                            size.height * 0.40f
                        )
                    }

                drawPath(
                    path = house,
                    color = color,
                    style = lineStyle
                )

                val door =
                    Path().apply {
                        moveTo(
                            size.width * 0.40f,
                            size.height * 0.84f
                        )

                        lineTo(
                            size.width * 0.40f,
                            size.height * 0.61f
                        )

                        lineTo(
                            size.width * 0.60f,
                            size.height * 0.61f
                        )

                        lineTo(
                            size.width * 0.60f,
                            size.height * 0.84f
                        )
                    }

                drawPath(
                    path = door,
                    color = color,
                    style = lineStyle
                )
            }

            XvoxDestination.SEARCH -> {
                drawCircle(
                    color = color,
                    radius =
                        size.minDimension *
                            0.285f,
                    center = Offset(
                        size.width * 0.44f,
                        size.height * 0.44f
                    ),
                    style = lineStyle
                )

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.64f,
                        size.height * 0.64f
                    ),
                    end = Offset(
                        size.width * 0.86f,
                        size.height * 0.86f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            XvoxDestination.SETTINGS -> {
                drawCircle(
                    color = color,
                    radius =
                        size.minDimension *
                            0.13f,
                    center = Offset(
                        size.width / 2f,
                        size.height / 2f
                    ),
                    style = lineStyle
                )

                repeat(8) { index ->
                    val angle =
                        Math.toRadians(
                            (
                                index * 45.0 -
                                    90.0
                                )
                        )

                    val cos =
                        kotlin.math.cos(
                            angle
                        ).toFloat()

                    val sin =
                        kotlin.math.sin(
                            angle
                        ).toFloat()

                    val inner =
                        size.minDimension *
                            0.31f

                    val outer =
                        size.minDimension *
                            0.42f

                    val center =
                        Offset(
                            size.width / 2f,
                            size.height / 2f
                        )

                    drawLine(
                        color = color,
                        start = Offset(
                            center.x +
                                cos * inner,
                            center.y +
                                sin * inner
                        ),
                        end = Offset(
                            center.x +
                                cos * outer,
                            center.y +
                                sin * outer
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                drawCircle(
                    color = color,
                    radius =
                        size.minDimension *
                            0.31f,
                    center = Offset(
                        size.width / 2f,
                        size.height / 2f
                    ),
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}
