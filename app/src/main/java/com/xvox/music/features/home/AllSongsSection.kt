package com.xvox.music.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import kotlinx.coroutines.flow.distinctUntilChanged

private const val Columns = 4
private const val Rows = 3
private const val SongsPerPage = 12

@Composable
fun AllSongsSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPrefetch: (Int) -> Unit
) {
    val colors =
        XvoxTheme.colors

    val gridState =
        rememberLazyGridState()

    LaunchedEffect(
        gridState,
        songs.size
    ) {
        snapshotFlow {
            gridState
                .layoutInfo
                .visibleItemsInfo
                .maxOfOrNull {
                    it.index
                } ?: 0
        }
            .distinctUntilChanged()
            .collect {
                visibleSlot ->

                onPrefetch(
                    visibleSlot + 12
                )
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp
            )
        ) {
            Text(
                text = "All Songs",
                color =
                    colors.primaryText,
                fontSize = 20.sp,
                lineHeight = 23.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "Total ${songs.size} songs",
                color =
                    colors.mutedText,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            val edge = 6.dp
            val gap = 6.dp

            val cardWidth =
                (
                    maxWidth -
                        edge * 2 -
                        gap * 3
                    ) / 4

            val cardHeight =
                cardWidth + 34.dp

            val gridHeight =
                cardHeight * Rows +
                    gap * (Rows - 1)

            val slots =
                remember(songs.size) {
                    if (songs.isEmpty()) {
                        0
                    } else {
                        (
                            (
                                songs.size +
                                    SongsPerPage -
                                    1
                                ) /
                                SongsPerPage
                            ) *
                            SongsPerPage
                    }
                }

            LazyHorizontalGrid(
                rows =
                    GridCells.Fixed(Rows),
                state = gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
                contentPadding =
                    PaddingValues(
                        horizontal = edge
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        gap
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        gap
                    )
            ) {
                items(
                    count = slots,
                    key = {
                        "song_slot_$it"
                    },
                    contentType = {
                        "song_slot"
                    }
                ) { slot ->

                    val page =
                        slot /
                            SongsPerPage

                    val local =
                        slot %
                            SongsPerPage

                    val row =
                        local % Rows

                    val column =
                        local / Rows

                    val sourceIndex =
                        page *
                            SongsPerPage +
                            row *
                            Columns +
                            column

                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                    ) {
                        songs
                            .getOrNull(
                                sourceIndex
                            )
                            ?.let {
                                song ->

                                AllSongCard(
                                    song = song,
                                    current =
                                        currentSongId ==
                                            song.id,
                                    playing =
                                        currentSongId ==
                                            song.id &&
                                            isPlaying,
                                    onClick = {
                                        onSongClick(
                                            song
                                        )
                                    },
                                    modifier =
                                        Modifier
                                            .width(
                                                cardWidth
                                            )
                                            .height(
                                                cardHeight
                                            )
                                )
                            }
                    }
                }
            }
        }
    }
}
