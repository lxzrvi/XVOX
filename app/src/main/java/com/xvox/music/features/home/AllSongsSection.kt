package com.xvox.music.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

private const val Rows = 3
private const val Columns = 4
private const val SongsPerPage = 12

@Composable
fun AllSongsSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val colors =
        XvoxTheme.colors

    val pageWidth =
        LocalConfiguration.current
            .screenWidthDp.dp - 36.dp

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
                    14.dp
                )
        ) {
            itemsIndexed(
                items = pages,
                key = { index, _ ->
                    index
                }
            ) { _, page ->
                AllSongsPage(
                    songs = page,
                    width = pageWidth,
                    onSongClick =
                        onSongClick
                )
            }
        }
    }
}

@Composable
private fun AllSongsPage(
    songs: List<Song>,
    width: Dp,
    onSongClick: (Song) -> Unit
) {
    val gap =
        7.dp

    val cardWidth =
        (
            width -
                gap * (Columns - 1)
            ) / Columns

    Column(
        modifier =
            Modifier.width(width),
        verticalArrangement =
            Arrangement.spacedBy(9.dp)
    ) {
        repeat(Rows) { row ->

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        gap
                    )
            ) {
                repeat(Columns) {
                    column ->

                    val index =
                        row *
                            Columns +
                            column

                    if (index < songs.size) {
                        val song =
                            songs[index]

                        AllSongCard(
                            song = song,
                            onClick = {
                                onSongClick(
                                    song
                                )
                            },
                            modifier =
                                Modifier.width(
                                    cardWidth
                                )
                        )
                    } else {
                        Spacer(
                            modifier =
                                Modifier.width(
                                    cardWidth
                                )
                        )
                    }
                }
            }
        }
    }
}
