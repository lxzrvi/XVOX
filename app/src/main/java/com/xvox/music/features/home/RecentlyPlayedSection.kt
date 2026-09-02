package com.xvox.music.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun RecentlyPlayedSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit
) {
    val colors =
        XvoxTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 7.dp
                ),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = "Recently Played",
                color = colors.primaryText,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "${songs.size} played",
                color = colors.mutedText,
                fontSize = 9.sp
            )
        }

        AnimatedContent(
            targetState =
                songs.isEmpty(),
            transitionSpec = {
                fadeIn() togetherWith
                    fadeOut()
            },
            label =
                "recentAvailability"
        ) { empty ->

            if (empty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text =
                            "Nothing played yet",
                        color =
                            colors.secondaryText,
                        fontSize = 12.sp
                    )
                }
            } else {
                RecentCarousel(
                    songs = songs,
                    currentSongId =
                        currentSongId,
                    isPlaying =
                        isPlaying,
                    onSongClick =
                        onSongClick
                )
            }
        }
    }
}

@Composable
private fun RecentCarousel(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit
) {
    val listState =
        rememberLazyListState()

    val fling =
        rememberSnapFlingBehavior(
            listState
        )

    var centeredIndex by
        remember {
            mutableIntStateOf(0)
        }

    LaunchedEffect(
        songs.firstOrNull()?.id
    ) {
        if (songs.isNotEmpty()) {
            listState
                .animateScrollToItem(0)
        }
    }

    LaunchedEffect(
        listState,
        songs.size
    ) {
        snapshotFlow {
            val layout =
                listState.layoutInfo

            val center =
                (
                    layout.viewportStartOffset +
                        layout.viewportEndOffset
                    ) / 2

            layout.visibleItemsInfo
                .minByOrNull {
                    item ->

                    val itemCenter =
                        item.offset +
                            item.size / 2

                    abs(
                        itemCenter -
                            center
                    )
                }
                ?.index
                ?: 0
        }.collect { index ->
            centeredIndex =
                index.coerceIn(
                    0,
                    songs.lastIndex
                )
        }
    }

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            val edge = 6.dp

            val itemWidth =
                maxWidth -
                    edge * 2

            val railWidth =
                itemWidth * 0.22f

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                LazyRow(
                    state = listState,
                    flingBehavior = fling,
                    modifier =
                        Modifier.fillMaxWidth(),
                    contentPadding =
                        PaddingValues(
                            horizontal = edge
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            1.dp
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

                RecentPositionRail(
                    songCount =
                        songs.size,
                    centeredIndex =
                        centeredIndex,
                    railWidth =
                        railWidth,
                    modifier =
                        Modifier.padding(
                            top = 8.dp
                        )
                )
            }
        }

        Box(
            modifier =
                Modifier.height(8.dp)
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
    val interaction =
        remember {
            MutableInteractionSource()
        }

    Box(
        modifier = modifier
            .background(
                XvoxTheme.colors
                    .cardElevated
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
            requestSize = 512,
            modifier =
                Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
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
                .animateContentSize()
                .clickable(
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
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
                color = Color.White,
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
                    color = Color.White,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun RecentPositionRail(
    songCount: Int,
    centeredIndex: Int,
    railWidth: Dp,
    modifier: Modifier = Modifier
) {
    if (songCount <= 0) {
        return
    }

    val colors =
        XvoxTheme.colors

    val buckets =
        minOf(
            songCount,
            7
        )

    val indicatorWidth =
        railWidth /
            buckets

    val bucketIndex =
        if (
            songCount <= 1
        ) {
            0
        } else {
            (
                centeredIndex
                    .toFloat() /
                    (songCount - 1) *
                    (buckets - 1)
                )
                .roundToInt()
                .coerceIn(
                    0,
                    buckets - 1
                )
        }

    val targetX =
        indicatorWidth *
            bucketIndex

    val animatedX by
        animateDpAsState(
            targetValue =
                targetX,
            animationSpec =
                spring(
                    dampingRatio = 0.82f,
                    stiffness = 500f
                ),
            label =
                "recentRailPosition"
        )

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
                    x = animatedX
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
