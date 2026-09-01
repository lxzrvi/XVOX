package com.xvox.music.features.home

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
    val colors = XvoxTheme.colors

    Column {
        Text(
            text = "Recently Played",
            color = colors.primaryText,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "${songs.size} played",
            color = colors.mutedText,
            fontSize = 12.sp
        )

        if (songs.isNotEmpty()) {
            LazyRow(
                contentPadding =
                    PaddingValues(top = 12.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = songs,
                    key = { it.id }
                ) { song ->
                    Column(
                        modifier = Modifier
                            .width(210.dp)
                            .xvoxPressScale {
                                onSongClick(song)
                            }
                    ) {
                        SongArtwork(
                            artwork = song.artworkUri,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .clip(
                                    RoundedCornerShape(
                                        14.dp
                                    )
                                )
                        )

                        Text(
                            text = song.title,
                            color = colors.primaryText,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis,
                            modifier =
                                Modifier.padding(
                                    top = 6.dp
                                )
                        )

                        Text(
                            text = song.artist,
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
