package com.xvox.music.player.nowplaying.lyrics

data class XvoxLyricLine(
    val timeMs: Long?,
    val text: String
)

data class XvoxLyrics(
    val lines: List<XvoxLyricLine>,
    val synchronized: Boolean,
    val source: XvoxLyricsSource
)

enum class XvoxLyricsSource {
    EMBEDDED,
    USER_LRC,
    USER_TEXT
}
