package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
            .padding(top = 26.dp)
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = 12.dp
                )
        ) {
            Text(
                text = "Recently Played",
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
                fontSize = 10.sp
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

    val state =
        rememberLazyListState()

    val fling =
        rememberSnapFlingBehavior(
            state
        )

    var centeredIndex by
        remember {
            mutableIntStateOf(0)
        }

    LaunchedEffect(
        songs.first().id
    ) {
        state.animateScrollToItem(0)
    }

    LaunchedEffect(state) {
        snapshotFlow {
            val layout =
                state.layoutInfo

            val center =
                (
                    layout.viewportStartOffset +
                        layout.viewportEndOffset
                    ) / 2

            layout.visibleItemsInfo
                .minByOrNull {
                    abs(
                        (
                            it.offset +
                                it.size / 2
                            ) -
                            center
                    )
                }
                ?.index
                ?: 0
        }.collect {
            centeredIndex =
                it.coerceIn(
                    0,
                    songs.lastIndex
                )
        }
    }

    Column(
        modifier =
            Modifier.padding(
                top = 10.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            val itemWidth =
                maxWidth - 24.dp

            LazyRow(
                state = state,
                flingBehavior = fling,
                contentPadding =
                    PaddingValues(
                        horizontal = 12.dp
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        1.dp
                    )
            ) {
                items(
                    items = songs,
                    key = { it.id }
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
                            onSongClick(song)
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

        val dots =
            minOf(
                7,
                songs.size
            )

        val selectedDot =
            if (
                songs.size <= 1
            ) {
                0
            } else {
                (
                    centeredIndex.toFloat() /
                        songs.lastIndex *
                        (dots - 1)
                    )
                    .roundToInt()
                    .coerceIn(
                        0,
                        dots - 1
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
            repeat(dots) {
                index ->

                val active =
                    index ==
                        selectedDot

                Box(
                    modifier = Modifier
                        .size(
                            if (active) {
                                7.dp
                            } else {
                                5.dp
                            }
                        )
                        .background(
                            if (active) {
                                colors.progressActive
                            } else {
                                colors.progressTrack
                            },
                            CircleShape
                        )
                )
            }
        }
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
    Box(
        modifier = modifier
            .background(
                XvoxTheme.colors
                    .cardElevated
            )
            .xvoxRecentClick(
                onClick
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
            modifier = Modifier
                .align(
                    Alignment.BottomStart
                )
                .fillMaxWidth(0.72f)
                .padding(12.dp)
        )

        Box(
            modifier = Modifier
                .align(
                    Alignment.TopEnd
                )
                .padding(10.dp)
                .size(30.dp)
                .background(
                    Color.Black.copy(
                        alpha = 0.55f
                    ),
                    CircleShape
                ),
            contentAlignment =
                Alignment.Center
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
                    Modifier.size(14.dp)
            )
        }
    }
}

private fun Modifier.xvoxRecentClick(
    onClick: () -> Unit
): Modifier {
    return this.then(
        Modifier
            .clickable(
                indication = null,
                interactionSource =
                    androidx.compose.foundation
                        .interaction
                        .MutableInteractionSource(),
                onClick = onClick
            )
    )
}
