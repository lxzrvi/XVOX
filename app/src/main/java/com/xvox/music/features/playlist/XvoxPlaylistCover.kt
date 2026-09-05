package com.xvox.music.features.playlist

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
import com.xvox.music.features.home.XvoxSongArtwork

@Composable
fun XvoxPlaylistCover(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    coverSongIds: List<Long> =
        emptyList(),
    customCoverUri: String? =
        null,
    requestSize: Int = 192,
) {
    if (
        !customCoverUri
            .isNullOrBlank()
    ) {
        AsyncImage(
            model =
                Uri.parse(
                    customCoverUri,
                ),
            contentDescription =
            null,
            contentScale =
                ContentScale.Crop,
            modifier =
            modifier,
        )

        return
    }

    val byId =
        songs.associateBy {
            it.id
        }

    // Single cover mode: if user selected exactly 1 cover, show it full (not 2x2 grid)
    if (coverSongIds.size == 1) {
        val singleSong = byId[coverSongIds.first()]
        if (singleSong != null) {
            XvoxSongArtwork(
                artwork = singleSong.artworkUri,
                requestSize = requestSize,
                modifier = modifier,
            )
            return
        }
    }

    val coverSongs =
        buildList {
            coverSongIds
                .mapNotNull {
                    byId[it]
                }.forEach {
                    add(it)
                }

            songs.forEach { song ->

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

    // If only one song available after fallback (e.g., playlist has 1 song), show single full as well
    if (coverSongs.size == 1) {
        XvoxSongArtwork(
            artwork = coverSongs.first().artworkUri,
            requestSize = requestSize,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier =
        modifier,
    ) {
        repeat(2) { row ->

            Row(
                modifier =
                    Modifier.weight(1f),
            ) {
                repeat(2) { column ->

                    val index =
                        row * 2 +
                            column

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize(),
                    ) {
                        XvoxSongArtwork(
                            artwork =
                                coverSongs
                                    .getOrNull(
                                        index,
                                    )?.artworkUri,
                            requestSize =
                            requestSize,
                            modifier =
                                Modifier
                                    .fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
