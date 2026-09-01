package com.xvox.music.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
                pressedScale = 0.88f,
                onClick = onClick
            )
    ) {
        val stroke =
            1.8.dp.toPx()

        when (type) {
            HomeHeaderIconType.SCAN -> {
                drawArc(
                    color = color,
                    startAngle = -45f,
                    sweepAngle = 285f,
                    useCenter = false,
                    topLeft = Offset(
                        size.width * .28f,
                        size.height * .28f
                    ),
                    size =
                        androidx.compose.ui.geometry.Size(
                            size.width * .44f,
                            size.height * .44f
                        ),
                    style = Stroke(
                        stroke,
                        cap =
                            StrokeCap.Round
                    )
                )
            }

            HomeHeaderIconType.HEART -> {
                val path =
                    Path().apply {
                        moveTo(
                            size.width * .5f,
                            size.height * .7f
                        )
                        cubicTo(
                            size.width * .18f,
                            size.height * .52f,
                            size.width * .25f,
                            size.height * .27f,
                            size.width * .5f,
                            size.height * .4f
                        )
                        cubicTo(
                            size.width * .75f,
                            size.height * .27f,
                            size.width * .82f,
                            size.height * .52f,
                            size.width * .5f,
                            size.height * .7f
                        )
                    }

                drawPath(
                    path,
                    color
                )
            }

            HomeHeaderIconType.PLAYLIST -> {
                repeat(3) { index ->
                    val y =
                        size.height *
                            (.36f +
                                index * .14f)

                    drawLine(
                        color,
                        Offset(
                            size.width * .3f,
                            y
                        ),
                        Offset(
                            size.width * .67f,
                            y
                        ),
                        stroke,
                        StrokeCap.Round
                    )
                }
            }

            HomeHeaderIconType.SONGS -> {
                drawLine(
                    color,
                    Offset(
                        size.width * .55f,
                        size.height * .29f
                    ),
                    Offset(
                        size.width * .55f,
                        size.height * .63f
                    ),
                    stroke,
                    StrokeCap.Round
                )

                drawLine(
                    color,
                    Offset(
                        size.width * .55f,
                        size.height * .29f
                    ),
                    Offset(
                        size.width * .7f,
                        size.height * .34f
                    ),
                    stroke,
                    StrokeCap.Round
                )

                drawCircle(
                    color,
                    size.width * .08f,
                    Offset(
                        size.width * .47f,
                        size.height * .67f
                    )
                )
            }
        }
    }
}
