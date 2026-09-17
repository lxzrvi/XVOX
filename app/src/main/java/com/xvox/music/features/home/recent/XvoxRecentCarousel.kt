package com.xvox.music.features.home.recent

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

/**
 * Recently played carousel with snap fling behavior.
 * Adapts to 3 columns visible at once in landscape mode.
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
    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(state)
    val click by rememberUpdatedState(onSongClick)
    val options by rememberUpdatedState(onSongOptions)
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val edge = 6.dp
        val gap = 10.dp
        val itemWidth = if (isLandscape) {
            ((maxWidth - edge * 2 - gap * 2) / 3).coerceAtLeast(180.dp)
        } else {
            maxWidth - edge * 2
        }

        if (songs.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(122.dp), contentAlignment = Alignment.Center) {
                Text("Nothing played yet", color = XvoxTheme.colors.secondaryText, fontSize = 12.sp)
            }
        } else {
            LazyRow(
                state = state,
                flingBehavior = fling,
                modifier = Modifier.fillMaxWidth().height(122.dp),
                contentPadding = PaddingValues(horizontal = edge),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                items(songs, key = { it.id }, contentType = { "recent_song" }) { song ->
                    XvoxRecentArtwork(
                        song = song,
                        current = song.id == currentSongId,
                        playing = song.id == currentSongId && isPlaying,
                        selected = song.id in selectedSongIds,
                        onClick = { click(song) },
                        onLongClick = { options(song) },
                        source = sources[song.id] ?: song.source,
                        modifier = Modifier.width(itemWidth).height(122.dp)
                    )
                }
            }
        }
    }
}
