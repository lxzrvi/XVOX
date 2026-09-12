package com.xvox.music.features.home.recent

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

/** Keep the LazyRow mounted, including during updates. Never replace it with an inert transition card. */
@Composable
fun XvoxRecentCarousel(
    songs: List<Song>, currentSongId: Long?, isPlaying: Boolean, transition: RecentTransitionRequest,
    onSongClick: (Song) -> Unit, onSongOptions: (Song) -> Unit,
    sources: Map<Long, String> = emptyMap(), onSourceClick: (Song) -> Unit = {}
) {
    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(state)
    val click by rememberUpdatedState(onSongClick)
    val options by rememberUpdatedState(onSongOptions)
    val sourceClick by rememberUpdatedState(onSourceClick)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val edge = 6.dp
        val itemWidth = maxWidth - edge * 2
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
                horizontalArrangement = Arrangement.spacedBy(edge * 2)
            ) {
                items(songs, key = { it.id }, contentType = { "recent_song" }) { song ->
                    XvoxRecentArtwork(
                        song = song,
                        current = song.id == currentSongId,
                        playing = song.id == currentSongId && isPlaying,
                        onClick = { click(song) },
                        onLongClick = { options(song) },
                        source = sources[song.id],
                        onSourceClick = { sourceClick(song) },
                        modifier = Modifier.width(itemWidth).height(122.dp)
                    )
                }
            }
        }
    }
}
