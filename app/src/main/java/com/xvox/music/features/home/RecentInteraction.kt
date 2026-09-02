package com.xvox.music.features.home

enum class RecentTransitionMode {
    NONE,
    LIBRARY
}

data class RecentTransitionRequest(
    val id: Long = 0L,
    val songId: Long? = null,
    val mode: RecentTransitionMode =
        RecentTransitionMode.NONE
)
