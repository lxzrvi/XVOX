package com.xvox.music.features.home

import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferences
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.features.home.recent.RecentTransitionRequest
import com.xvox.music.features.home.recent.RecentTransitionMode

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val songs: List<Song> =
        emptyList(),
    val recentlyPlayed: List<Song> =
        emptyList(),
    val profile: UserPreferences =
        UserPreferences(),
    val libraryMode:
        XvoxHomeLibraryMode =
        XvoxHomeLibraryMode.ALL_SONGS,
    val recentTransition:
        RecentTransitionRequest =
        RecentTransitionRequest(),
    val likedSongIds: Set<Long> =
        emptySet(),
    val hiddenSongIds: Set<Long> =
        emptySet(),
    val playlists: List<XvoxPlaylist> =
        emptyList()
)
