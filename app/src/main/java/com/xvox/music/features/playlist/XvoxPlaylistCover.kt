package com.xvox.music.features.playlist

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork

@Composable
fun XvoxPlaylistCover(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    coverSongIds: List<Long> = emptyList(),
    customCoverUri: String? = null,
    requestSize: Int = 192,
) {
    val colors = XvoxTheme.colors

    if (!customCoverUri.isNullOrBlank()) {
        AsyncImage(
            model = Uri.parse(customCoverUri),
            contentDescription = "Playlist cover",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        return
    }

    val byId = songs.associateBy { it.id }

    val selectedCovers = coverSongIds.mapNotNull { byId[it] }

    if (selectedCovers.size == 1) {
        XvoxSongArtwork(
            artwork = selectedCovers.first().artworkUri,
            requestSize = requestSize,
            modifier = modifier,
        )
        return
    }

    val coverSongs = buildList {
        addAll(selectedCovers)
        songs.forEach { song ->
            if (none { it.id == song.id } && size < 4) {
                add(song)
            }
        }
    }.take(4)

    if (coverSongs.isEmpty()) {
        Box(
            modifier = modifier.background(colors.cardElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_playlist),
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(24.dp)
            )
        }
        return
    }

    if (coverSongs.size == 1) {
        XvoxSongArtwork(
            artwork = coverSongs.first().artworkUri,
            requestSize = requestSize,
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier) {
        repeat(2) { row ->
            Row(modifier = Modifier.weight(1f)) {
                repeat(2) { column ->
                    val index = row * 2 + column
                    val art = coverSongs.getOrNull(index)?.artworkUri ?: coverSongs.firstOrNull()?.artworkUri
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    ) {
                        XvoxSongArtwork(
                            artwork = art,
                            requestSize = requestSize,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
