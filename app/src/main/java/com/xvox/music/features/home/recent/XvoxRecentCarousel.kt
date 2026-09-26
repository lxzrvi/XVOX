package com.xvox.music.features.home.recent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

/**
 * Recent history is intentionally a vertical list rather than a sideways carousel. It stays part
 * of the parent Home list, so its scroll motion and the shell header move as one surface.
 */
@Composable
fun XvoxRecentCarousel(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    transition: RecentTransitionRequest,
    onSongClick: (Song) -> Unit,
    onSongOptions: (Song) -> Unit,
    selectedSongIds: Set<Long> = emptySet(),
    sources: Map<Long, String> = emptyMap(),
    onSourceClick: (Song) -> Unit = {}
) {
    val click by rememberUpdatedState(onSongClick)
    val options by rememberUpdatedState(onSongOptions)

    if (songs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Nothing played yet", color = XvoxTheme.colors.secondaryText, fontSize = 12.sp)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            songs.forEach { song ->
                XvoxRecentArtwork(
                    song = song,
                    current = song.id == currentSongId,
                    playing = song.id == currentSongId && isPlaying,
                    selected = song.id in selectedSongIds,
                    onClick = { click(song) },
                    onLongClick = { options(song) },
                    source = sources[song.id] ?: song.source,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                )
            }
        }
    }
}
