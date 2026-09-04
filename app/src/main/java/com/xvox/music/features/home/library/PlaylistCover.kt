package com.xvox.music.features.home.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.SongArtwork

@Composable
fun PlaylistCover(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    requestSize: Int = 192
) {
    Column(
        modifier = modifier
    ) {
        repeat(2) {
            row ->

            Row(
                modifier =
                    Modifier.weight(1f)
            ) {
                repeat(2) {
                    column ->

                    val index =
                        row * 2 +
                            column

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                    ) {
                        SongArtwork(
                            artwork =
                                songs
                                    .getOrNull(
                                        index
                                    )
                                    ?.artworkUri,
                            requestSize =
                                requestSize,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
