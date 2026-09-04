package com.xvox.music.features.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xvox.music.core.model.Song

@Composable
fun RecentlyPlayedSection(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    transition: RecentTransitionRequest,
    onSongClick: (Song) -> Unit,
    onSongOptions: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        return
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        RecentCarousel(
            songs = songs,
            currentSongId =
                currentSongId,
            isPlaying =
                isPlaying,
            transition =
                transition,
            onSongClick =
                onSongClick,
            onSongOptions =
                onSongOptions
        )
    }
}
