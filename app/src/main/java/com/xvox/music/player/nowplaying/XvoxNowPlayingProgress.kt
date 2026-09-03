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
                Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        colors.progressTrack,
                        CircleShape
                    )
            )

            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            progress
                        )
                        .height(3.dp)
                        .background(
                            colors.progressActive,
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
                    formatPlayerTime(
                        position
                    ),
                color =
                    colors.secondaryText,
                fontSize = 11.sp
            )

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )

            Text(
                text =
                    formatPlayerTime(
                        duration
                    ),
                color =
                    colors.secondaryText,
                fontSize = 11.sp
            )
        }
    }
}

fun formatPlayerTime(
    millis: Long
): String {
    val total =
        millis.coerceAtLeast(0L) /
            1000L

    return buildString {
        append(total / 60L)
        append(':')
        append(
            (total % 60L)
                .toString()
                .padStart(
                    2,
                    '0'
                )
        )
    }
}
