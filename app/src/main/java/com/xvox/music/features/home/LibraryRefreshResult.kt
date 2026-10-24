package com.xvox.music.features.home

/** One completed user-requested MediaStore scan; never a simulated loading result. */
data class LibraryRefreshResult(
    val totalSongs: Int,
    val addedSongs: Int,
    val removedSongs: Int,
    val unavailableRowsSkipped: Int = 0,
    val duplicateRowsSkipped: Int = 0,
    val scanFailed: Boolean = false
)
