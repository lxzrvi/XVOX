package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun XvoxNowPlayingProgress(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress =
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

    Column(
        modifier =
            modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(duration) {
                    detectTapGestures { point ->
                        if (
                            duration > 0L &&
                            size.width > 0
                        ) {
                            val fraction =
                                (
                                    point.x /
                                        size.width.toFloat()
                                )
                                    .coerceIn(
                                        0f,
                                        1f
                                    )

                            onSeek(
                                (
                                    duration.toDouble() *
                                        fraction.toDouble()
                                )
                                    .toLong()
                            )
                        }
                    }
                },
            contentAlignment =
                Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        color =
                            Color.White.copy(
                                alpha = 0.15f
                            ),
                        shape =
                            CircleShape
                    )
            )

            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            fraction =
                                progress
                        )
                        .height(3.dp)
                        .background(
                            color =
                                Color(
                                    0xFF1ED760
                                ),
                            shape =
                                CircleShape
                        )
                )
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
                    formatTime(
                        position
                    ),
                color =
                    Color.White.copy(
                        alpha = 0.65f
                    ),
                fontSize =
                    11.sp
            )

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )

            Text(
                text =
                    formatTime(
                        duration
                    ),
                color =
                    Color.White.copy(
                        alpha = 0.65f
                    ),
                fontSize =
                    11.sp
            )
        }
    }
}

private fun formatTime(
    millis: Long
): String {
    val totalSeconds =
        millis
            .coerceAtLeast(0L) /
            1000L

    val minutes =
        totalSeconds / 60L

    val seconds =
        totalSeconds % 60L

    return buildString {
        append(minutes)
        append(':')
        append(
            seconds
                .toString()
                .padStart(
                    length = 2,
                    padChar = '0'
                )
        )
    }
}
