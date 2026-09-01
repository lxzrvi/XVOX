package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun RecentlyPlayedSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val colors =
        XvoxTheme.colors

    val listState =
        rememberLazyListState()

    val newestSongId =
        songs.firstOrNull()?.id

    LaunchedEffect(newestSongId) {
        if (
            newestSongId != null &&
            songs.isNotEmpty()
        ) {
            listState.animateScrollToItem(0)
        }
    }

    Column {
        Text(
            text = "Recently Played",
            color =
                colors.primaryText,
            fontSize = 20.sp,
            fontWeight =
                FontWeight.SemiBold
        )

        Text(
            text =
                "${songs.size} played",
            color =
                colors.mutedText,
            fontSize = 11.sp
        )

        if (songs.isEmpty()) {
            Text(
                text =
                    "Nothing played yet",
                color =
                    colors.secondaryText,
                fontSize = 13.sp,
                modifier =
                    Modifier.padding(
                        top = 18.dp,
                        bottom = 18.dp
                    )
            )

            return@Column
        }

        val screenWidth =
            LocalConfiguration.current
                .screenWidthDp.dp

        val artworkWidth =
            screenWidth - 52.dp

        LazyRow(
            state = listState,
            contentPadding =
                PaddingValues(
                    top = 10.dp,
                    end = 10.dp
                )
        ) {
            items(
                count = songs.size,
                key = {
                    songs[it].id
                }
            ) { index ->
                val song =
                    songs[index]

                RecentArtwork(
                    song = song,
                    onClick = {
                        onSongClick(song)
                    },
                    modifier = Modifier
                        .width(
                            artworkWidth
                        )
                        .padding(
                            end = 10.dp
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
    val colors =
        XvoxTheme.colors

    Box(
        modifier = modifier
            .aspectRatio(1.85f)
            .clip(
                RoundedCornerShape(
                    16.dp
                )
            )
            .background(
                colors.cardElevated
            )
            .xvoxPressScale(
                pressedScale = 0.975f,
                onClick = onClick
            )
    ) {
        SongArtwork(
            artwork =
                song.artworkUri,
            modifier =
                Modifier.fillMaxWidth()
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
                                alpha = 0.72f
                            )
                        )
                    )
                )
        )

        Text(
            text =
                song.title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight =
                FontWeight.SemiBold,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis,
            modifier = Modifier
                .align(
                    Alignment.BottomStart
                )
                .padding(
                    start = 13.dp,
                    end = 13.dp,
                    bottom = 11.dp
                )
        )
    }
}
