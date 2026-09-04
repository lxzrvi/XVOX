package com.xvox.music.features.home

data class LibraryRefreshResult(
    val totalSongs: Int,
    val addedSongs: Int,
    val removedSongs: Int
)
