package com.xvox.music.features.home

enum class RecentTransitionMode {
    NONE,
    ADJACENT_SWAP,
    FRONT_REPLACE
}

data class RecentTransitionRequest(
    val id: Long = 0L,
    val songId: Long? = null,
    val mode: RecentTransitionMode =
        RecentTransitionMode.NONE
)
