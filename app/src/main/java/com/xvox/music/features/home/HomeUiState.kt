package com.xvox.music.features.home

import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferences

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val songs: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val profile: UserPreferences =
        UserPreferences()
)
