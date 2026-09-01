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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import kotlin.math.roundToInt

@Composable
fun RecentlyPlayedSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val colors = XvoxTheme.colors

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
                color = colors.primaryText,
                fontSize = 20.sp,
                lineHeight = 23.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text = "${songs.size} played",
                color = colors.mutedText,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }

        if (songs.isEmpty()) {
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

            return@Column
        }

        RecentCarousel(
            songs = songs,
            onSongClick = onSongClick
        )
    }
}

@Composable
private fun RecentCarousel(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val colors = XvoxTheme.colors

    val state =
        rememberLazyListState()

    val fling =
        rememberSnapFlingBehavior(state)

    var currentIndex by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(songs.first().id) {
        state.animateScrollToItem(0)
    }

    LaunchedEffect(state) {
        snapshotFlow {
            state.firstVisibleItemIndex
        }.collect {
            currentIndex =
                it.coerceIn(
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
        LazyRow(
            state = state,
            flingBehavior = fling,
            contentPadding =
                PaddingValues(
                    horizontal = 6.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = songs,
                key = { it.id },
                contentType = {
                    "recent"
                }
            ) { song ->

                BoxWithConstraints(
                    modifier =
                        Modifier.fillParentMaxWidth()
                ) {
                    RecentArtwork(
                        song = song,
                        onClick = {
                            onSongClick(song)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(122.dp)
                    )
                }
            }
        }

        val dots =
            minOf(7, songs.size)

        val selected =
            if (songs.size <= 1) {
                0
            } else {
                (
                    currentIndex.toFloat() /
                        (songs.size - 1) *
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
            repeat(dots) { index ->
                val active =
                    index == selected

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
                            color =
                                if (active) {
                                    colors.progressActive
                                } else {
                                    colors.progressTrack
                                },
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun RecentArtwork(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.cardElevated)
            .xvoxPressScale(
                pressedScale = 0.975f,
                onClick = onClick
            )
    ) {
        SongArtwork(
            artwork = song.artworkUri,
            requestSize = 720,
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
                                alpha = 0.8f
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
                .fillMaxWidth(0.8f)
                .padding(12.dp)
        )
    }
}
