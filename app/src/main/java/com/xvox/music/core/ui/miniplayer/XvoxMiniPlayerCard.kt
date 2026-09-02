package com.xvox.music.core.ui.miniplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.SongArtwork

@Composable
fun XvoxMiniPlayerCard(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    direction: Int,
    togglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    val shape =
        RoundedCornerShape(
            15.dp
        )

    val interaction =
        remember {
            MutableInteractionSource()
        }

    val progress =
        if (
            duration > 0L
        ) {
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

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(
                    colors.surface.copy(
                        alpha = 0.88f
                    )
                )
                .border(
                    width = 0.65.dp,
                    color =
                        colors.cardBorder,
                    shape =
                        shape
                )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = 4.dp,
                        top = 4.dp,
                        end = 50.dp,
                        bottom = 4.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .size(
                            50.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                11.dp
                            )
                        )
            ) {
                SongArtwork(
                    artwork =
                        song.artworkUri,
                    requestSize = 160,
                    modifier =
                        Modifier.fillMaxSize()
                )
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            start = 9.dp,
                            end = 5.dp
                        ),
                verticalArrangement =
                    Arrangement.Center
            ) {
                Text(
                    text =
                        song.title,
                    color =
                        colors.primaryText,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text =
                        song.artist,
                    color =
                        colors.secondaryText,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.CenterEnd
                    )
                    .padding(
                        end = 7.dp
                    )
                    .size(
                        38.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        colors.cardElevated
                            .copy(
                                alpha = 0.68f
                            )
                    )
                    .border(
                        width = 0.5.dp,
                        color =
                            colors.cardBorder,
                        shape =
                            CircleShape
                    )
                    .clickable(
                        interactionSource =
                            interaction,
                        indication = null,
                        onClick =
                            togglePlay
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            XvoxMiniPlayerIcon(
                icon =
                    if (
                        isPlaying
                    ) {
                        XvoxMiniIcon.PAUSE
                    } else {
                        XvoxMiniIcon.PLAY
                    },
                color =
                    colors.primaryText,
                modifier =
                    Modifier.size(
                        18.dp
                    )
            )
        }

        /*
         * Draw LAST so active progress replaces the top
         * border visually.
         *
         * 2dp inset prevents rounded-edge collision.
         */
        if (
            progress > 0f
        ) {
            Canvas(
                modifier =
                    Modifier.fillMaxSize()
            ) {
                val inset =
                    2.dp.toPx()

                val available =
                    (
                        size.width -
                            inset * 2f
                        )
                        .coerceAtLeast(
                            0f
                        )

                val end =
                    inset +
                        available *
                            progress

                drawLine(
                    color =
                        colors.progressActive,
                    start =
                        Offset(
                            x = inset,
                            y =
                                0.8.dp
                                    .toPx()
                        ),
                    end =
                        Offset(
                            x = end,
                            y =
                                0.8.dp
                                    .toPx()
                        ),
                    strokeWidth =
                        1.5.dp.toPx(),
                    cap =
                        StrokeCap.Round
                )
            }
        }
    }
}
