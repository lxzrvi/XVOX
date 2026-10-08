package com.xvox.music.features.home.recent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxHomeSectionHeading

@Composable
fun XvoxRecentlyPlayedSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    transition: RecentTransitionRequest,
    onSongClick: (Song) -> Unit,
    onSongOptions: (Song) -> Unit,
    selectedSongIds: Set<Long> = emptySet(),
    sources: Map<Long, String> = emptyMap(),
    onSourceClick: (Song) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        XvoxHomeSectionHeading(
            title = "Recently Played",
            subtitle = "Total ${songs.size} played"
        )

        XvoxRecentCarousel(
            songs = songs,
            currentSongId = currentSongId,
            isPlaying = isPlaying,
            transition = transition,
            onSongClick = onSongClick,
            onSongOptions = onSongOptions,
            selectedSongIds = selectedSongIds,
            sources = sources,
            onSourceClick = onSourceClick,
        )
    }
}
