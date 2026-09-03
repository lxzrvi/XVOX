package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Size
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

    val cardShape =
        RoundedCornerShape(
            15.dp
        )

    val artworkShape =
        RoundedCornerShape(
            11.dp
        )

    val controlInteraction =
        remember {
            MutableInteractionSource()
        }

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(cardShape)
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
                    cardShape
            )
    ) {
        /*
         * Progress starts at the horizontal center of
         * the 50dp artwork:
         *
         * artwork left = 4dp
         * artwork half = 25dp
         * start/end inset = 29dp
         *
         * This keeps progress completely away from the
         * rounded card sides.
         */
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .align(
                    Alignment.TopStart
                )
        ) {
            val sideInset =
                29.dp.toPx()

            val availableWidth =
                (
                    size.width -
                        sideInset * 2f
                    )
                    .coerceAtLeast(0f)

            if (
                progress > 0f &&
                availableWidth > 0f
            ) {
                drawRect(
                    color =
                        colors.progressActive,
                    topLeft =
                        Offset(
                            x = sideInset,
                            y = 0f
                        ),
                    size =
                        Size(
                            width =
                                availableWidth *
                                    progress,
                            height =
                                size.height
                        )
                )
            }
        }

        Row(
            modifier = Modifier
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
            /*
             * ARTWORK:
             *
             * Song changes use only a clean fade.
             */
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(
                        artworkShape
                    )
            ) {
                AnimatedContent(
                    targetState =
                        song,
                    contentKey = {
                        it.id
                    },
                    transitionSpec = {
                        fadeIn(
                            animationSpec =
                                tween(180)
                        ).togetherWith(
                            fadeOut(
                                animationSpec =
                                    tween(140)
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxSize(),
                    label =
                        "miniArtworkFade"
                ) { visualSong ->
                    SongArtwork(
                        artwork =
                            visualSong
                                .artworkUri,
                        requestSize = 160,
                        modifier =
                            Modifier.fillMaxSize()
                    )
                }
            }

            /*
             * METADATA VIEWPORT:
             *
             * It is clipped so title/artist can never be
             * visible outside their allocated MiniPlayer area.
             */
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(
                        RoundedCornerShape(
                            1.dp
                        )
                    )
            ) {
                AnimatedContent(
                    targetState =
                        song,
                    contentKey = {
                        it.id
                    },
                    transitionSpec = {
                        when {
                            /*
                             * NEXT:
                             *
                             * old center -> UP
                             * new BOTTOM -> center
                             */
                            direction > 0 -> {
                                (
                                    fadeIn(
                                        animationSpec =
                                            tween(150)
                                    ) +
                                        slideInVertically(
                                            animationSpec =
                                                tween(200),
                                            initialOffsetY = {
                                                height ->
                                                height
                                            }
                                        )
                                    )
                                    .togetherWith(
                                        fadeOut(
                                            animationSpec =
                                                tween(120)
                                        ) +
                                            slideOutVertically(
                                                animationSpec =
                                                    tween(180),
                                                targetOffsetY = {
                                                    height ->
                                                    -height
                                                }
                                            )
                                    )
                            }

                            /*
                             * PREVIOUS:
                             *
                             * old center -> BOTTOM
                             * previous TOP -> center
                             */
                            direction < 0 -> {
                                (
                                    fadeIn(
                                        animationSpec =
                                            tween(150)
                                    ) +
                                        slideInVertically(
                                            animationSpec =
                                                tween(200),
                                            initialOffsetY = {
                                                height ->
                                                -height
                                            }
                                        )
                                    )
                                    .togetherWith(
                                        fadeOut(
                                            animationSpec =
                                                tween(120)
                                        ) +
                                            slideOutVertically(
                                                animationSpec =
                                                    tween(180),
                                                targetOffsetY = {
                                                    height ->
                                                    height
                                                }
                                            )
                                    )
                            }

                            else -> {
                                fadeIn(
                                    animationSpec =
                                        tween(140)
                                ).togetherWith(
                                    fadeOut(
                                        animationSpec =
                                            tween(100)
                                    )
                                )
                            }
                        }
                    },
                    modifier =
                        Modifier.fillMaxSize(),
                    label =
                        "miniMetadataSlide"
                ) { visualSong ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = 9.dp,
                                end = 5.dp
                            ),
                        verticalArrangement =
                            Arrangement.Center
                    ) {
                        Text(
                            text =
                                visualSong.title,
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
                                visualSong.artist,
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
            }
        }

        Box(
            modifier = Modifier
                .align(
                    Alignment.CenterEnd
                )
                .padding(
                    end = 7.dp
                )
                .size(38.dp)
                .clip(
                    CircleShape
                )
                .background(
                    colors.cardElevated.copy(
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
                        controlInteraction,
                    indication = null,
                    onClick =
                        togglePlay
                ),
            contentAlignment =
                Alignment.Center
        ) {
            XvoxMiniPlayerIcon(
                icon =
                    if (isPlaying) {
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
    }
}
