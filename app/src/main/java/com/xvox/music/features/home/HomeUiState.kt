package com.xvox.music.features.home

import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferences

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val songs: List<Song> =
        emptyList(),
    val recentlyPlayed: List<Song> =
        emptyList(),
    val profile: UserPreferences =
        UserPreferences(),
    val currentSongId: Long? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val playbackPosition: Long = 0L,
    val playbackDuration: Long = 0L,
    val miniPlayerVisible: Boolean = true,
    val showPlaylists: Boolean = false
)
