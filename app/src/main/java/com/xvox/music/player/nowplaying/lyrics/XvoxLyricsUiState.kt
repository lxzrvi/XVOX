package com.xvox.music.player.nowplaying.lyrics

data class XvoxLyricsUiState(
    val loading: Boolean = false,
    val lyrics: XvoxLyrics? = null,
    val fullscreen: Boolean = false
)
