package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
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
fun AllSongsSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val colors = XvoxTheme.colors

    Column {
        Text(
            text = "All Songs",
            color = colors.primaryText,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Total ${songs.size} songs",
            color = colors.mutedText,
            fontSize = 12.sp
        )

        LazyHorizontalGrid(
            rows = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .height(505.dp)
                .padding(top = 12.dp),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = songs,
                key = { it.id }
            ) { song ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.24f)
                        .clip(
                            RoundedCornerShape(14.dp)
                        )
                        .background(colors.card)
                        .padding(6.dp)
                        .xvoxPressScale {
                            onSongClick(song)
                        }
                ) {
                    SongArtwork(
                        artwork = song.artworkUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(
                                RoundedCornerShape(
                                    10.dp
                                )
                            )
                    )

                    Text(
                        text = song.title,
                        color = colors.primaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Text(
                        text = song.artist,
                        color = colors.secondaryText,
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
