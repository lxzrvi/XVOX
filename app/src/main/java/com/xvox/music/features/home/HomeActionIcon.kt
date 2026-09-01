package com.xvox.music.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme

enum class HomeActionType {
    REFRESH,
    MENU
}

@Composable
fun HomeActionIcon(
    type: HomeActionType,
    onClick: () -> Unit
) {
    val color = XvoxTheme.colors.primaryText

    Canvas(
        modifier = Modifier
            .size(42.dp)
            .clickable(onClick = onClick)
    ) {
        val stroke = 2.dp.toPx()

        when (type) {
            HomeActionType.MENU -> {
                val left = size.width * 0.25f
                val right = size.width * 0.75f

                listOf(0.34f, 0.5f, 0.66f)
                    .forEach { y ->
                        drawLine(
                            color = color,
                            start = Offset(
                                left,
                                size.height * y
                            ),
                            end = Offset(
                                right,
                                size.height * y
                            ),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
            }

            HomeActionType.REFRESH -> {
                drawArc(
                    color = color,
                    startAngle = -55f,
                    sweepAngle = 285f,
                    useCenter = false,
                    topLeft = Offset(
                        size.width * 0.27f,
                        size.height * 0.27f
                    ),
                    size = androidx.compose.ui.geometry.Size(
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
                        size.width * 0.70f,
                        size.height * 0.27f
                    ),
                    end = Offset(
                        size.width * 0.72f,
                        size.height * 0.42f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
