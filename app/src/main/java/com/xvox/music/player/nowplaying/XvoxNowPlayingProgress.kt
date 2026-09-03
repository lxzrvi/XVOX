package com.xvox.music.player.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxNowPlayingProgress(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    var dragging by remember {
        mutableStateOf(false)
    }

    var dragFraction by remember {
        mutableFloatStateOf(0f)
    }

    val realFraction =
        if (duration > 0L) {
            (
                position.toFloat() /
                    duration.toFloat()
                )
                .coerceIn(
                    0f,
                    1f
                )
        } else {
            0f
        }

    val visibleFraction =
        if (dragging) {
            dragFraction
        } else {
            realFraction
        }

    val visiblePosition =
        if (
            dragging &&
            duration > 0L
        ) {
            (
                duration *
                    visibleFraction
                )
                .toLong()
        } else {
            position
        }

    Column(
        modifier =
            modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(
                    duration
                ) {
                    fun update(
                        x: Float
                    ) {
                        if (
                            duration <= 0L ||
                            size.width <= 0
                        ) {
                            return
                        }

                        dragFraction =
                            (
                                x /
                                    size.width
                                )
                                .coerceIn(
                                    0f,
                                    1f
                                )
                    }

                    detectHorizontalDragGestures(
                        onDragStart = {
                            offset ->

                            dragging = true
                            update(
                                offset.x
                            )
                        },
                        onHorizontalDrag = {
                            change,
                            _ ->

                            change.consume()
                            update(
                                change.position.x
                            )
                        },
                        onDragEnd = {
                            if (
                                duration > 0L
                            ) {
                                onSeek(
                                    (
                                        duration *
                                            dragFraction
                                        )
                                        .toLong()
                                )
                            }

                            dragging = false
                        },
                        onDragCancel = {
                            dragging = false
                        }
                    )
                }
                .pointerInput(
                    duration
                ) {
                    detectTapGestures {
                        point ->

                        if (
                            duration > 0L &&
                            size.width > 0
                        ) {
                            val fraction =
                                (
                                    point.x /
                                        size.width
                                    )
                                    .coerceIn(
                                        0f,
                                        1f
                                    )

                            onSeek(
                                (
                                    duration *
                                        fraction
                                    )
                                    .toLong()
                            )
                        }
                    }
                },
            contentAlignment =
                Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                val y =
                    size.height / 2f

                drawLine(
                    color =
                        colors.progressTrack
                            .copy(
                                alpha = 0.54f
                            ),
                    start =
                        Offset(
                            0f,
                            y
                        ),
                    end =
                        Offset(
                            size.width,
                            y
                        ),
                    strokeWidth =
                        1.5.dp.toPx(),
                    cap =
                        StrokeCap.Round
                )

                if (
                    visibleFraction >
                    0f
                ) {
                    drawLine(
                        color =
                            colors.progressActive
                                .copy(
                                    alpha = 0.82f
                                ),
                        start =
                            Offset(
                                0f,
                                y
                            ),
                        end =
                            Offset(
                                size.width *
                                    visibleFraction,
                                y
                            ),
                        strokeWidth =
                            1.5.dp.toPx(),
                        cap =
                            StrokeCap.Round
                    )
                }
            }
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    formatPlayerTime(
                        visiblePosition
                    ),
                color =
                    colors.secondaryText,
                fontSize =
                    10.sp
            )

            Spacer(
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Text(
                text =
                    formatPlayerTime(
                        duration
                    ),
                color =
                    colors.secondaryText,
                fontSize =
                    10.sp
            )
        }
    }
}

fun formatPlayerTime(
    millis: Long
): String {
    val total =
        millis
            .coerceAtLeast(
                0L
            ) /
            1000L

    return buildString {
        append(
            total / 60L
        )

        append(':')

        append(
            (
                total %
                    60L
                )
                .toString()
                .padStart(
                    2,
                    '0'
                )
        )
    }
}
