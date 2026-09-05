package com.xvox.music.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

@OptIn(
    ExperimentalFoundationApi::class
)
@Composable
fun RecentArtwork(
    song: Song,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    animateEntrance: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    val cardInteraction =
        remember {
            MutableInteractionSource()
        }

    val controlInteraction =
        remember {
            MutableInteractionSource()
        }

    val pressed by
        cardInteraction
            .collectIsPressedAsState()

    val scale by
        androidx.compose.animation.core
            .animateFloatAsState(
                targetValue =
                    if (pressed) {
                        0.985f
                    } else {
                        1f
                    },
                animationSpec =
                    spring(
                        dampingRatio = 0.86f,
                        stiffness = 1400f
                    ),
                label =
                    "recentPress"
            )

    val shape =
        RoundedCornerShape(
            3.dp
        )

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(
                    colors.cardElevated
                )
                .border(
                    width = 0.7.dp,
                    color =
                        colors.cardBorder,
                    shape = shape
                )
                .combinedClickable(
                    interactionSource =
                        cardInteraction,
                    indication = null,
                    onClick = onClick,
                    onLongClick =
                        onLongClick
                )
    ) {
        SongArtwork(
            artwork =
                song.artworkUri,
            requestSize =
                RecentArtworkSize,
            modifier =
                Modifier.fillMaxSize()
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush
                            .verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(
                                        alpha = 0.78f
                                    )
                                )
                            )
                    )
        )

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight =
                FontWeight.SemiBold,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .align(
                        Alignment
                            .BottomStart
                    )
                    .fillMaxWidth(
                        0.72f
                    )
                    .padding(
                        start = 12.dp,
                        end = 8.dp,
                        bottom = 10.dp
                    )
        )

        Row(
            modifier =
                Modifier
                    .align(
                        Alignment.TopEnd
                    )
                    .padding(9.dp)
                    .height(30.dp)
                    .clip(
                        CircleShape
                    )
                    .background(
                        if (current && playing) {
                            colors.primaryAccent
                        } else {
                            Color.Black.copy(
                                alpha = 0.58f
                            )
                        }
                    )
                    .animateContentSize(
                        animationSpec =
                            spring(
                                dampingRatio =
                                    0.88f,
                                stiffness =
                                    700f
                            )
                    )
                    .combinedClickable(
                        interactionSource =
                            controlInteraction,
                        indication = null,
                        onClick = onClick,
                        onLongClick =
                            onLongClick
                    )
                    .padding(
                        horizontal = 8.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(
                    5.dp
                )
        ) {
            AnimatedContent(
                targetState =
                    current &&
                        playing,
                transitionSpec = {
                    fadeIn() togetherWith
                        fadeOut()
                },
                label =
                    "recentPlayState"
            ) {
                active ->

                PlaybackIcon(
                    type =
                        if (active) {
                            PlaybackIconType
                                .PAUSE
                        } else {
                            PlaybackIconType
                                .PLAY
                        },
                    color =
                        if (active) colors.primaryAccent else Color.White,
                    modifier =
                        Modifier.size(
                            14.dp
                        )
                )
            }

            if (
                current &&
                playing
            ) {
                Text(
                    text =
                        "Playing",
                    color =
                        Color.White,
                    fontSize = 9.sp
                )
            }
        }
    }
}
