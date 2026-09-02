package com.xvox.music.features.home

import androidx.compose.animation.animateContentSize
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
            .padding(
                top = 26.dp
            )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp
            )
        ) {
            Text(
                text =
                    "Recently Played",
                color =
                    colors.primaryText,
                fontSize = 20.sp,
                lineHeight = 23.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "${songs.size} played",
                color =
                    colors.mutedText,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(153.dp),
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

            return@Column
        }

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

@Composable
private fun RecentCarousel(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit
) {
    val colors =
        XvoxTheme.colors

    val listState =
        rememberLazyListState()

    val flingBehavior =
        rememberSnapFlingBehavior(
            listState
        )

    var centeredIndex by
        remember {
            mutableIntStateOf(0)
        }

    val newestSongId =
        songs.firstOrNull()?.id

    LaunchedEffect(
        newestSongId
    ) {
        if (
            newestSongId != null
        ) {
            listState
                .animateScrollToItem(
                    0
                )
        }
    }

    LaunchedEffect(
        listState,
        songs.size
    ) {
        snapshotFlow {
            val layout =
                listState.layoutInfo

            val viewportCenter =
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
                            viewportCenter
                    )
                }
                ?.index
                ?: 0
        }.collect {
            index ->

            centeredIndex =
                index.coerceIn(
                    0,
                    songs.lastIndex
                )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            val sideInset =
                12.dp

            val itemWidth =
                maxWidth -
                    sideInset * 2

            LazyRow(
                state = listState,
                flingBehavior =
                    flingBehavior,
                modifier =
                    Modifier.fillMaxWidth(),
                contentPadding =
                    PaddingValues(
                        horizontal =
                            sideInset
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        1.dp
                    )
            ) {
                items(
                    items = songs,
                    key = {
                        song ->
                        song.id
                    },
                    contentType = {
                        "recent_song"
                    }
                ) { song ->

                    RecentArtwork(
                        song = song,
                        current =
                            currentSongId ==
                                song.id,
                        playing =
                            currentSongId ==
                                song.id &&
                                isPlaying,
                        onClick = {
                            onSongClick(song)
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

        RecentDots(
            songCount =
                songs.size,
            centeredIndex =
                centeredIndex,
            activeColor =
                colors.progressActive,
            inactiveColor =
                colors.progressTrack
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
    val interactionSource =
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
                    interactionSource,
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
            text =
                song.title,
            color =
                Color.White,
            fontSize = 14.sp,
            lineHeight = 17.sp,
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

        RecentPlaybackControl(
            active =
                current &&
                    playing,
            onClick =
                onClick,
            modifier = Modifier
                .align(
                    Alignment.TopEnd
                )
                .padding(
                    top = 9.dp,
                    end = 9.dp
                )
        )
    }
}

@Composable
private fun RecentPlaybackControl(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    Row(
        modifier = modifier
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
                    interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal =
                    if (active) {
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
                if (active) {
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

        if (active) {
            Text(
                text =
                    "Playing",
                color =
                    Color.White,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight =
                    FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RecentDots(
    songCount: Int,
    centeredIndex: Int,
    activeColor: Color,
    inactiveColor: Color
) {
    if (
        songCount <= 0
    ) {
        return
    }

    val dotCount =
        minOf(
            7,
            songCount
        )

    val activeDot =
        if (
            songCount <= 1
        ) {
            0
        } else {
            (
                centeredIndex
                    .toFloat() /
                    (songCount - 1) *
                    (dotCount - 1)
                )
                .roundToInt()
                .coerceIn(
                    0,
                    dotCount - 1
                )
        }

    Row(
        modifier =
            Modifier.padding(
                top = 9.dp
            ),
        horizontalArrangement =
            Arrangement.spacedBy(
                6.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        repeat(
            dotCount
        ) { index ->

            val selected =
                index ==
                    activeDot

            Box(
                modifier = Modifier
                    .size(
                        if (selected) {
                            7.dp
                        } else {
                            5.dp
                        }
                    )
                    .background(
                        color =
                            if (
                                selected
                            ) {
                                activeColor
                            } else {
                                inactiveColor
                            },
                        shape =
                            CircleShape
                    )
            )
        }
    }
}
