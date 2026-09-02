package com.xvox.music.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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

    /*
     * Prefetch once per conceptual page rather than every
     * visible-item movement during a fast fling.
     */
    LaunchedEffect(
        gridState,
        songs.size
    ) {
        snapshotFlow {
            gridState.layoutInfo
                .visibleItemsInfo
                .maxOfOrNull {
                    it.index
                } ?: 0
        }
            .map {
                it / SongsPerPage
            }
            .distinctUntilChanged()
            .collect { page ->
                onPrefetch(
                    (page + 1) *
                        SongsPerPage
                )
            }
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = "All Songs",
                color =
                    colors.primaryText,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "Total ${songs.size} songs",
                color =
                    colors.mutedText,
                fontSize = 9.sp
            )
        }

        BoxWithConstraints(
            modifier =
                Modifier.fillMaxWidth()
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

            /*
             * Keep complete conceptual 12-item pages.
             *
             * Empty tail slots preserve the current 4x3
             * geometry without fake songs.
             */
            val slots =
                remember(
                    songs.size
                ) {
                    if (
                        songs.isEmpty()
                    ) {
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
                    GridCells.Fixed(
                        Rows
                    ),
                state =
                    gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        gridHeight
                    ),
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

                    /*
                     * Preserve custom page mapping while
                     * giving real songs stable identities.
                     */
                    key = { slot ->
                        val page =
                            slot /
                                SongsPerPage

                        val local =
                            slot %
                                SongsPerPage

                        val row =
                            local %
                                Rows

                        val column =
                            local /
                                Rows

                        val sourceIndex =
                            page *
                                SongsPerPage +
                                row *
                                Columns +
                                column

                        songs
                            .getOrNull(
                                sourceIndex
                            )
                            ?.let { song ->
                                "song_${song.id}"
                            }
                            ?: "empty_$slot"
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
                        local %
                            Rows

                    val column =
                        local /
                            Rows

                    val sourceIndex =
                        page *
                            SongsPerPage +
                            row *
                            Columns +
                            column

                    Box(
                        modifier = Modifier
                            .width(
                                cardWidth
                            )
                            .height(
                                cardHeight
                            )
                    ) {
                        songs
                            .getOrNull(
                                sourceIndex
                            )
                            ?.let { song ->
                                val current =
                                    currentSongId ==
                                        song.id

                                AllSongCard(
                                    song = song,
                                    current =
                                        current,
                                    playing =
                                        current &&
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
