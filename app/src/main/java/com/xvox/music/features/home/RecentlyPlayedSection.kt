package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalConfiguration
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

    val listState =
        rememberLazyListState()

    val newestId =
        songs.firstOrNull()?.id

    LaunchedEffect(newestId) {
        if (newestId != null) {
            listState.animateScrollToItem(0)
        }
    }

    Column {
        Text(
            text = "Recently Played",
            color = colors.primaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "${songs.size} played",
            color = colors.mutedText,
            fontSize = 11.sp
        )

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nothing played yet",
                    color = colors.secondaryText,
                    fontSize = 13.sp
                )
            }

            return@Column
        }

        val screenWidth =
            LocalConfiguration.current
                .screenWidthDp.dp

        val itemWidth =
            screenWidth - 20.dp

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp),
            contentPadding = PaddingValues(
                horizontal = 10.dp
            ),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = songs.size,
                key = { songs[it].id }
            ) { index ->
                val song = songs[index]

                RecentArtwork(
                    song = song,
                    onClick = {
                        onSongClick(song)
                    },
                    modifier =
                        Modifier.width(itemWidth)
                )
            }
        }

        RecentDots(
            songCount = songs.size,
            listState = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

@Composable
private fun RecentArtwork(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = modifier
            .aspectRatio(2.15f)
            .clip(
                RoundedCornerShape(15.dp)
            )
            .background(colors.cardElevated)
            .xvoxPressScale(
                pressedScale = 0.975f,
                onClick = onClick
            )
    ) {
        SongArtwork(
            artwork = song.artworkUri,
            requestSize = 720,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(
                                alpha = 0.70f
                            )
                        )
                    )
                )
        )

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 13.dp,
                    end = 13.dp,
                    bottom = 10.dp
                )
        )
    }
}

@Composable
private fun RecentDots(
    songCount: Int,
    listState:
        androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    if (songCount <= 0) {
        return
    }

    val colors = XvoxTheme.colors

    val dotCount =
        songCount.coerceAtMost(7)

    val currentSongIndex by remember(
        listState,
        songCount
    ) {
        derivedStateOf {
            listState.firstVisibleItemIndex
                .coerceIn(
                    0,
                    songCount - 1
                )
        }
    }

    val activeDot =
        if (songCount <= 1) {
            0
        } else {
            (
                currentSongIndex.toFloat() /
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
        modifier = modifier,
        horizontalArrangement =
            Arrangement.Center,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        repeat(dotCount) { index ->
            val active =
                index == activeDot

            Box(
                modifier = Modifier
                    .padding(
                        horizontal = 3.dp
                    )
                    .size(
                        if (active) {
                            7.dp
                        } else {
                            5.dp
                        }
                    )
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            colors.progressActive
                        } else {
                            colors.progressTrack
                        }
                    )
            )
        }
    }
}
