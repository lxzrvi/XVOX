package com.xvox.music.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

private data class RecentFrontTarget(
    val transitionKey: Long,
    val song: Song?
)

@Composable
fun RecentlyPlayedSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    frontTransitionKey: Long,
    onSongClick: (Song) -> Unit
) {
    val colors =
        XvoxTheme.colors

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    "Recently Played",
                color =
                    colors.primaryText,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "${songs.size} played",
                color =
                    colors.mutedText,
                fontSize = 9.sp
            )
        }

        RecentCarousel(
            songs = songs,
            currentSongId =
                currentSongId,
            isPlaying =
                isPlaying,
            frontTransitionKey =
                frontTransitionKey,
            onSongClick =
                onSongClick
        )
    }
}

@Composable
private fun RecentCarousel(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    frontTransitionKey: Long,
    onSongClick: (Song) -> Unit
) {
    val listState =
        rememberLazyListState()

    val fling =
        rememberSnapFlingBehavior(
            listState
        )

    /*
     * Only an explicit external/library event is
     * allowed to visually pull Recent to its front.
     *
     * Silent history reorder from Recent taps does
     * not trigger this effect.
     */
    var handledTransitionKey by remember {
        mutableLongStateOf(
            frontTransitionKey
        )
    }

    val showFrontTransition =
        frontTransitionKey >
            handledTransitionKey

    LaunchedEffect(
        frontTransitionKey
    ) {
        if (
            frontTransitionKey >
            handledTransitionKey
        ) {
            /*
             * Prepare the real row underneath at index 0.
             * The AnimatedContent below visually covers this
             * change, so intermediate history items don't
             * animate through the viewport.
             */
            listState.scrollToItem(0)

            handledTransitionKey =
                frontTransitionKey
        }
    }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        val edge = 6.dp

        val itemWidth =
            maxWidth -
                edge * 2

        val itemGap =
            edge * 2

        val railWidth =
            itemWidth * 0.22f

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            /*
             * Normal history row.
             *
             * NO animateItem.
             * NO list-wide reorder animation.
             */
            if (songs.isEmpty()) {
                EmptyRecent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(122.dp)
                )
            } else {
                LazyRow(
                    state =
                        listState,
                    flingBehavior =
                        fling,
                    modifier =
                        Modifier.fillMaxWidth(),
                    contentPadding =
                        PaddingValues(
                            horizontal =
                                edge
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            itemGap
                        )
                ) {
                    items(
                        items = songs,
                        key = {
                            it.id
                        },
                        contentType = {
                            "recent_song"
                        }
                    ) { song ->
                        RecentArtwork(
                            song = song,
                            current =
                                song.id ==
                                    currentSongId,
                            playing =
                                song.id ==
                                    currentSongId &&
                                    isPlaying,
                            onClick = {
                                onSongClick(
                                    song
                                )
                            },
                            modifier = Modifier
                                .width(
                                    itemWidth
                                )
                                .height(
                                    122.dp
                                )
                        )
                    }
                }
            }

            RecentPositionRail(
                songCount =
                    songs.size,
                listState =
                    listState,
                itemWidth =
                    itemWidth,
                itemGap =
                    itemGap,
                railWidth =
                    railWidth,
                modifier =
                    Modifier.padding(
                        top = 8.dp
                    )
            )
        }

        /*
         * Front transition presentation.
         *
         * AnimatedContent identifies every explicit external
         * front event with its own key, so replaying the same
         * song can still animate.
         */
        RecentFrontTransition(
            target =
                RecentFrontTarget(
                    transitionKey =
                        frontTransitionKey,
                    song =
                        songs.firstOrNull()
                ),
            currentSongId =
                currentSongId,
            isPlaying =
                isPlaying,
            onSongClick =
                onSongClick,
            edge =
                edge,
            modifier =
                Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RecentFrontTransition(
    target: RecentFrontTarget,
    currentSongId: Long?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    edge: Dp,
    modifier: Modifier = Modifier
) {
    /*
     * AnimatedContent itself retains the outgoing target
     * long enough for:
     *
     * outgoing -> RIGHT
     * incoming <- LEFT
     *
     * When there has never been an explicit front event,
     * target key 0 acts as the resting state.
     */
    AnimatedContent(
        targetState =
            target,
        contentKey = {
            it.transitionKey
        },
        modifier =
            modifier.height(
                122.dp
            ),
        transitionSpec = {
            (
                slideInHorizontally(
                    animationSpec =
                        tween(260),
                    initialOffsetX = {
                        -it
                    }
                ) +
                    fadeIn(
                        animationSpec =
                            tween(180)
                    )
                )
                .togetherWith(
                    slideOutHorizontally(
                        animationSpec =
                            tween(260),
                        targetOffsetX = {
                            it
                        }
                    ) +
                        fadeOut(
                            animationSpec =
                                tween(180)
                        )
                )
        },
        label =
            "recentFront"
    ) { state ->

        /*
         * Key 0 is transparent so normal LazyRow remains
         * interactive. Subsequent explicit front events draw
         * the transition card over it.
         */
        if (
            state.transitionKey > 0L
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal =
                            edge
                    )
            ) {
                val song =
                    state.song

                if (song == null) {
                    EmptyRecent(
                        modifier =
                            Modifier.fillMaxSize()
                    )
                } else {
                    RecentArtwork(
                        song = song,
                        current =
                            song.id ==
                                currentSongId,
                        playing =
                            song.id ==
                                currentSongId &&
                                isPlaying,
                        onClick = {
                            onSongClick(song)
                        },
                        modifier =
                            Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRecent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier,
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text =
                "Nothing played yet",
            color =
                XvoxTheme.colors
                    .secondaryText,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RecentArtwork(
    song: Song,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    val interaction =
        remember {
            MutableInteractionSource()
        }

    val controlInteraction =
        remember {
            MutableInteractionSource()
        }

    val shape =
        RoundedCornerShape(
            3.dp
        )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                colors.cardElevated
            )
            .border(
                width = 0.7.dp,
                color =
                    colors.cardBorder,
                shape =
                    shape
            )
            .clickable(
                interactionSource =
                    interaction,
                indication = null,
                onClick = onClick
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
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
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
            text =
                song.title,
            color =
                Color.White,
            fontSize = 14.sp,
            fontWeight =
                FontWeight.SemiBold,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis,
            modifier = Modifier
                .align(
                    Alignment.BottomStart
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
            modifier = Modifier
                .align(
                    Alignment.TopEnd
                )
                .padding(9.dp)
                .height(30.dp)
                .background(
                    Color.Black.copy(
                        alpha = 0.58f
                    ),
                    CircleShape
                )
                .clickable(
                    interactionSource =
                        controlInteraction,
                    indication = null,
                    onClick = onClick
                )
                .padding(
                    horizontal =
                        if (
                            current &&
                            playing
                        ) {
                            9.dp
                        } else {
                            8.dp
                        }
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(
                    5.dp
                )
        ) {
            PlaybackIcon(
                type =
                    if (
                        current &&
                        playing
                    ) {
                        PlaybackIconType.PAUSE
                    } else {
                        PlaybackIconType.PLAY
                    },
                color =
                    Color.White,
                modifier =
                    Modifier.size(
                        14.dp
                    )
            )

            if (
                current &&
                playing
            ) {
                Text(
                    text = "Playing",
                    color =
                        Color.White,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun RecentPositionRail(
    songCount: Int,
    listState: LazyListState,
    itemWidth: Dp,
    itemGap: Dp,
    railWidth: Dp,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    if (
        songCount <= 0
    ) {
        Box(
            modifier = modifier
                .width(
                    railWidth
                )
                .height(
                    3.dp
                )
                .background(
                    colors.progressTrack,
                    CircleShape
                )
        )

        return
    }

    val density =
        LocalDensity.current

    val itemStridePx =
        with(density) {
            (
                itemWidth +
                    itemGap
                ).toPx()
        }

    val indicatorWidth =
        railWidth /
            songCount

    val maxTravel =
        railWidth -
            indicatorWidth

    val progress by remember(
        listState,
        songCount,
        itemStridePx
    ) {
        derivedStateOf {
            if (
                songCount <= 1 ||
                itemStridePx <= 0f
            ) {
                0f
            } else {
                val continuousIndex =
                    listState
                        .firstVisibleItemIndex +
                        (
                            listState
                                .firstVisibleItemScrollOffset /
                                itemStridePx
                            )

                (
                    continuousIndex /
                        (songCount - 1)
                            .toFloat()
                    )
                    .coerceIn(
                        0f,
                        1f
                    )
            }
        }
    }

    Box(
        modifier = modifier
            .width(
                railWidth
            )
            .height(3.dp)
            .background(
                colors.progressTrack,
                CircleShape
            )
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x =
                        maxTravel *
                            progress
                )
                .width(
                    indicatorWidth
                )
                .height(3.dp)
                .background(
                    colors.progressActive,
                    CircleShape
                )
        )
    }
}
