package com.xvox.music.features.home.library

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.SongArtwork

@Composable
fun PlaylistCover(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    coverSongIds: List<Long> =
        emptyList(),
    customCoverUri: String? =
        null,
    requestSize: Int = 192
) {
    if (
        !customCoverUri
            .isNullOrBlank()
    ) {
        AsyncImage(
            model =
                Uri.parse(
                    customCoverUri
                ),
            contentDescription =
                null,
            contentScale =
                ContentScale.Crop,
            modifier =
                modifier
        )

        return
    }

    val byId =
        songs.associateBy {
            it.id
        }

    val coverSongs =
        buildList {
            coverSongIds
                .mapNotNull {
                    byId[it]
                }
                .forEach {
                    add(it)
                }

            songs.forEach {
                song ->

                if (
                    none {
                        it.id ==
                            song.id
                    } &&
                    size < 4
                ) {
                    add(song)
                }
            }
        }.take(4)

    Column(
        modifier =
            modifier
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
                                coverSongs
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
