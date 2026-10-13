package com.xvox.music.features.playlist

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork

@Composable
fun XvoxPlaylistCover(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    coverSongIds: List<Long> = emptyList(),
    customCoverUri: String? = null,
    requestSize: Int = 192,
    /** Must match the immediate cover container; callers with a non-card preview pass theirs. */
    coverShape: Shape = RoundedCornerShape(16.dp),
) {
    // Each rendering root owns the same shape as the immediate containing cover box. This keeps
    // custom art, a single fallback, and mosaic tiles aligned even in compact picker/editor rows.
    val clippedModifier = modifier.clip(coverShape)

    if (!customCoverUri.isNullOrBlank()) {
        // Route custom art through the shared renderer too: a bad/deleted URI falls back to the
        // centered XVOX mark rather than leaving an unbranded blank playlist card.
        XvoxSongArtwork(
            artwork = Uri.parse(customCoverUri),
            fallbackSeed = customCoverUri.hashCode(),
            requestSize = requestSize,
            modifier = clippedModifier,
        )
        return
    }

    val byId = songs.associateBy { it.id }

    val selectedCovers = coverSongIds.mapNotNull { byId[it] }

    if (selectedCovers.size == 1) {
        XvoxSongArtwork(
            artwork = selectedCovers.first().artworkUri,
            fallbackSeed = selectedCovers.first().id.hashCode(),
            requestSize = requestSize,
            modifier = clippedModifier,
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
        XvoxSongArtwork(
            artwork = null,
            fallbackSeed = (coverSongIds.hashCode() * 31) + songs.hashCode(),
            requestSize = requestSize,
            modifier = clippedModifier,
        )
        return
    }

    if (coverSongs.size == 1) {
        XvoxSongArtwork(
            artwork = coverSongs.first().artworkUri,
            fallbackSeed = coverSongs.first().id.hashCode(),
            requestSize = requestSize,
            modifier = clippedModifier,
        )
        return
    }

    Column(modifier = clippedModifier) {
        repeat(2) { row ->
            Row(modifier = Modifier.weight(1f)) {
                repeat(2) { column ->
                    val index = row * 2 + column
                    val coverSong = coverSongs.getOrNull(index) ?: coverSongs.firstOrNull()
                    val art = coverSong?.artworkUri
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    ) {
                        XvoxSongArtwork(
                            artwork = art,
                            fallbackSeed = coverSong?.id?.hashCode(),
                            requestSize = requestSize,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
