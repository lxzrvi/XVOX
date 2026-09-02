package com.xvox.music.features.home

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlin.math.abs

@Composable
fun RecentlyPlayedSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    transition: RecentTransitionEvent,
    onSongClick:
        (
            Song,
            RecentTransitionMode
        ) -> Unit
) {
    val colors =
        XvoxTheme.colors

    val listState =
        rememberLazyListState()

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top =
                            HomeGeometry
                                .sectionGap,
                        bottom =
                            HomeGeometry
                                .sectionGap
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
            transition =
                transition,
            listState =
                listState,
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
    transition: RecentTransitionEvent,
    listState: LazyListState,
    onSongClick:
        (
            Song,
            RecentTransitionMode
        ) -> Unit
) {
    val fling =
        rememberSnapFlingBehavior(
            listState
        )

    LaunchedEffect(
        transition.id
    ) {
        when (
            transition.mode
        ) {
            RecentTransitionMode
                .ADJACENT -> {
                listState
                    .animateScrollToItem(
                        0
                    )
            }

            RecentTransitionMode.FAR,
            RecentTransitionMode.LIBRARY -> {
                listState
                    .scrollToItem(
                        0
                    )
            }

            RecentTransitionMode.NONE ->
                Unit
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
            if (
                songs.isEmpty()
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                122.dp
                            ),
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
                    itemsIndexed(
                        items = songs,
                        key = {
                            _,
                            song ->

                            song.id
                        },
                        contentType = {
                            _,
                            _ ->

                            "recent_song"
                        }
                    ) {
                        index,
                        song ->

                        val animateFront =
                            index == 0 &&
                                transition.songId ==
                                    song.id &&
                                (
                                    transition.mode ==
                                        RecentTransitionMode
                                            .LIBRARY ||
                                    transition.mode ==
                                        RecentTransitionMode
                                            .FAR
                                    )

                        if (
                            animateFront
                        ) {
                            AnimatedContent(
                                targetState =
                                    transition.id,
                                transitionSpec = {
                                    (
                                        slideInHorizontally(
                                            animationSpec =
                                                tween(
                                                    260
                                                ),
                                            initialOffsetX = {
                                                -it
                                            }
                                        ) +
                                            fadeIn(
                                                tween(
                                                    160
                                                )
                                            )
                                        )
                                        .togetherWith(
                                            slideOutHorizontally(
                                                animationSpec =
                                                    tween(
                                                        260
                                                    ),
                                                targetOffsetX = {
                                                    it
                                                }
                                            ) +
                                                fadeOut(
                                                    tween(
                                                        160
                                                    )
                                                )
                                        )
                                },
                                modifier =
                                    Modifier
                                        .width(
                                            itemWidth
                                        )
                                        .height(
                                            122.dp
                                        ),
                                label =
                                    "recentFront"
                            ) {
                                RecentArtwork(
                                    song =
                                        song,
                                    current =
                                        song.id ==
                                            currentSongId,
                                    playing =
                                        song.id ==
                                            currentSongId &&
                                            isPlaying,
                                    onClick = {
                                        val mode =
                                            recentMode(
                                                selectedIndex =
                                                    index,
                                                listState =
                                                    listState
                                            )

                                        onSongClick(
                                            song,
                                            mode
                                        )
                                    },
                                    modifier =
                                        Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            RecentArtwork(
                                song =
                                    song,
                                current =
                                    song.id ==
                                        currentSongId,
                                playing =
                                    song.id ==
                                        currentSongId &&
                                        isPlaying,
                                onClick = {
                                    val mode =
                                        recentMode(
                                            selectedIndex =
                                                index,
                                            listState =
                                                listState
                                        )

                                    onSongClick(
                                        song,
                                        mode
                                    )
                                },
                                modifier =
                                    Modifier
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
                        top =
                            HomeGeometry
                                .sectionGap
                    )
            )
        }
    }
}

private fun recentMode(
    selectedIndex: Int,
    listState: LazyListState
): RecentTransitionMode {
    val visible =
        listState
            .layoutInfo
            .visibleItemsInfo

    if (
        visible.isEmpty()
    ) {
        return RecentTransitionMode
            .NONE
    }

    val viewportCenter =
        (
            listState
                .layoutInfo
                .viewportStartOffset +
                listState
                    .layoutInfo
                    .viewportEndOffset
            ) / 2

    val focusedIndex =
        visible
            .minByOrNull {
                abs(
                    (
                        it.offset +
                            it.size / 2
                        ) -
                        viewportCenter
                )
            }
            ?.index
            ?: selectedIndex

    val distance =
        abs(
            selectedIndex -
                focusedIndex
        )

    return when {
        distance == 0 ->
            RecentTransitionMode.NONE

        distance == 1 ->
            RecentTransitionMode.ADJACENT

        else ->
            RecentTransitionMode.FAR
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
        modifier =
            modifier
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
                    onClick =
                        onClick
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
            modifier =
                Modifier
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
            modifier =
                Modifier
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
                        onClick =
                            onClick
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
                        PlaybackIconType
                            .PAUSE
                    } else {
                        PlaybackIconType
                            .PLAY
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

@Composable
private fun RecentPositionRail(
    songCount: Int,
    listState: LazyListState,
    itemWidth: Dp,
    itemGap: Dp,
    railWidth: Dp,
    modifier: Modifier =
        Modifier
) {
    val colors =
        XvoxTheme.colors

    val density =
        LocalDensity.current

    val indicatorWidth =
        if (
            songCount > 0
        ) {
            railWidth /
                songCount
        } else {
            railWidth
        }

    val itemStridePx =
        with(density) {
            (
                itemWidth +
                    itemGap
                ).toPx()
        }

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
                val index =
                    listState
                        .firstVisibleItemIndex +
                        (
                            listState
                                .firstVisibleItemScrollOffset /
                                itemStridePx
                            )

                (
                    index /
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

    val travel =
        railWidth -
            indicatorWidth

    Box(
        modifier =
            modifier
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
    ) {
        Box(
            modifier =
                Modifier
                    .offset(
                        x =
                            travel *
                                progress
                    )
                    .width(
                        indicatorWidth
                    )
                    .height(
                        3.dp
                    )
                    .background(
                        colors.progressActive,
                        CircleShape
                    )
        )
    }
}
