package com.xvox.music.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

private const val SongsPerPage = 20
private const val RowsPerPage = 5
private const val ColumnsPerPage = 4

@Composable
fun AllSongsSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val colors =
        XvoxTheme.colors

    val screenWidth =
        LocalConfiguration.current
            .screenWidthDp.dp

    val pageWidth =
        screenWidth - 36.dp

    Column {
        Text(
            text = "All Songs",
            color =
                colors.primaryText,
            fontSize = 20.sp,
            fontWeight =
                FontWeight.SemiBold
        )

        Text(
            text =
                "Total ${songs.size} songs",
            color =
                colors.mutedText,
            fontSize = 11.sp
        )

        val pages =
            songs.chunked(
                SongsPerPage
            )

        LazyRow(
            modifier =
                Modifier.padding(
                    top = 10.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            itemsIndexed(
                items = pages,
                key = { index, _ ->
                    index
                }
            ) { _, page ->

                SongPage(
                    songs = page,
                    pageWidth =
                        pageWidth,
                    onSongClick =
                        onSongClick
                )
            }
        }
    }
}

@Composable
private fun SongPage(
    songs: List<Song>,
    pageWidth: androidx.compose.ui.unit.Dp,
    onSongClick: (Song) -> Unit
) {
    val horizontalGap =
        7.dp

    val verticalGap =
        8.dp

    val cardWidth =
        (
            pageWidth -
                horizontalGap *
                (ColumnsPerPage - 1)
            ) / ColumnsPerPage

    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                verticalGap
            )
    ) {
        repeat(RowsPerPage) { row ->

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        horizontalGap
                    )
            ) {
                repeat(
                    ColumnsPerPage
                ) { column ->

                    val index =
                        row *
                            ColumnsPerPage +
                            column

                    if (
                        index <
                        songs.size
                    ) {
                        val song =
                            songs[index]

                        AllSongCard(
                            song = song,
                            onClick = {
                                onSongClick(
                                    song
                                )
                            },
                            modifier = Modifier
                                .width(
                                    cardWidth
                                )
                                .height(
                                    104.dp
                                )
                        )
                    } else {
                        androidx.compose.foundation.layout.Spacer(
                            modifier =
                                Modifier
                                    .width(
                                        cardWidth
                                    )
                                    .height(
                                        104.dp
                                    )
                        )
                    }
                }
            }
        }
    }
}
