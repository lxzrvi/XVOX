package com.xvox.music.player.playback

import com.xvox.music.core.model.Song

data class MainPlayerUiState(
    val connected: Boolean = false,
    val queue: List<Song> =
        emptyList(),
    val currentSongId: Long? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val miniPlayerVisible: Boolean = false,
    val miniPlayerRiseKey: Int = 0,
    val nowPlayingVisible: Boolean = false
)
