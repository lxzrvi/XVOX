package com.xvox.music.features.home

enum class RecentTransitionMode {
    NONE,
    ADJACENT,
    FAR,
    LIBRARY
}

data class RecentTransitionEvent(
    val id: Long = 0L,
    val songId: Long? = null,
    val mode: RecentTransitionMode =
        RecentTransitionMode.NONE
)
