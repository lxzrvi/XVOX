package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    Column {
        Text(
            text =
                "Recently Played",
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

        LazyRow(
            contentPadding =
                PaddingValues(
                    top = 10.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {
            items(
                items = songs,
                key = { it.id }
            ) { song ->

                Column(
                    modifier = Modifier
                        .width(205.dp)
                        .clip(
                            RoundedCornerShape(
                                15.dp
                            )
                        )
                        .background(
                            colors.card
                        )
                        .xvoxPressScale {
                            onSongClick(song)
                        }
                        .padding(6.dp)
                ) {
                    SongArtwork(
                        artwork =
                            song.artworkUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(82.dp)
                            .clip(
                                RoundedCornerShape(
                                    11.dp
                                )
                            )
                    )

                    Text(
                        text =
                            song.title,
                        color =
                            colors.primaryText,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis,
                        modifier =
                            Modifier.padding(
                                top = 5.dp
                            )
                    )

                    Text(
                        text =
                            song.artist,
                        color =
                            colors.secondaryText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
