package com.xvox.music.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

private const val Columns = 4
private const val Rows = 3
private const val SongsPerPage = Columns * Rows

@Composable
fun AllSongsSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val colors = XvoxTheme.colors

    androidx.compose.foundation.layout.Column {
        Text(
            text = "All Songs",
            color = colors.primaryText,
            fontSize = 20.sp
        )

        Text(
            text = "Total ${songs.size} songs",
            color = colors.mutedText,
            fontSize = 11.sp
        )

        val gridState =
            rememberLazyGridState()

        val slots =
            remember(songs.size) {
                if (songs.isEmpty()) {
                    0
                } else {
                    (
                        (songs.size + SongsPerPage - 1) /
                            SongsPerPage
                        ) * SongsPerPage
                }
            }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val edge = 4.dp
            val gap = 7.dp

            val cardWidth =
                (
                    maxWidth -
                        edge * 2 -
                        gap * (Columns - 1)
                    ) / Columns

            val cardHeight =
                cardWidth + 34.dp

            val gridHeight =
                cardHeight * Rows +
                    gap * (Rows - 1)

            LazyHorizontalGrid(
                rows = GridCells.Fixed(Rows),
                state = gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
                contentPadding =
                    PaddingValues(
                        horizontal = edge
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(gap),
                verticalArrangement =
                    Arrangement.spacedBy(gap)
            ) {
                items(
                    count = slots,
                    key = {
                        "all_song_slot_$it"
                    },
                    contentType = {
                        "song_slot"
                    }
                ) { slot ->

                    val page =
                        slot / SongsPerPage

                    val local =
                        slot % SongsPerPage

                    val row =
                        local % Rows

                    val column =
                        local / Rows

                    val sourceIndex =
                        page * SongsPerPage +
                            row * Columns +
                            column

                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                    ) {
                        songs
                            .getOrNull(sourceIndex)
                            ?.let { song ->
                                AllSongCard(
                                    song = song,
                                    onClick = {
                                        onSongClick(song)
                                    },
                                    modifier = Modifier
                                        .width(cardWidth)
                                        .height(cardHeight)
                                )
                            }
                    }
                }
            }
        }
    }
}
