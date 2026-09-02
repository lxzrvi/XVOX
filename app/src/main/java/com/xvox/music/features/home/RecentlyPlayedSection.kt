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

private data class FrontPresentation(
    val key: Long,
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
            if (songs.isEmpty()) {
                AnimatedContent(
                    targetState =
                        FrontPresentation(
                            key =
                                frontTransitionKey,
                            song = null
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(122.dp),
                    label =
                        "emptyRecent"
                ) {
                    EmptyRecent(
                        modifier =
                            Modifier.fillMaxSize()
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
                        items =
                            songs,
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

                        if (index == 0) {
                            AnimatedContent(
                                targetState =
                                    FrontPresentation(
                                        key =
                                            frontTransitionKey,
                                        song =
                                            song
                                    ),
                                contentKey = {
                                    it.key
                                },
                                transitionSpec = {
                                    (
                                        slideInHorizontally(
                                            animationSpec =
                                                tween(
                                                    250
                                                ),
                                            initialOffsetX = {
                                                -it
                                            }
                                        ) +
                                            fadeIn(
                                                tween(
                                                    150
                                                )
                                            )
                                        )
                                        .togetherWith(
                                            slideOutHorizontally(
                                                animationSpec =
                                                    tween(
                                                        250
                                                    ),
                                                targetOffsetX = {
                                                    it
                                                }
                                            ) +
                                                fadeOut(
                                                    tween(
                                                        150
                                                    )
                                                )
                                        )
                                },
                                modifier = Modifier
                                    .width(
                                        itemWidth
                                    )
                                    .height(
                                        122.dp
                                    ),
                                label =
                                    "recentFront"
                            ) { front ->
                                front.song
                                    ?.let {
                                        RecentArtwork(
                                            song = it,
                                            current =
                                                it.id ==
                                                    currentSongId,
                                            playing =
                                                it.id ==
                                                    currentSongId &&
                                                    isPlaying,
                                            onClick = {
                                                onSongClick(
                                                    it
                                                )
                                            },
                                            modifier =
                                                Modifier
                                                    .fillMaxSize()
                                        )
                                    }
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
                                    onSongClick(
                                        song
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

@Composable
private fun EmptyRecent(
    modifier: Modifier =
        Modifier
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
            .clip(
                shape
            )
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

    if (
        songCount <= 0
    ) {
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
                )
                .toPx()
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
