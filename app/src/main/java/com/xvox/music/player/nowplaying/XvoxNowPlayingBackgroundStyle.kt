package com.xvox.music.player.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.xvox.music.core.design.theme.XvoxTheme

/** A normal adaptive player canvas or the current song's locally blurred cover background. */
data class XvoxNowPlayingBackgroundOption(
    val key: String,
    val title: String
)

object XvoxNowPlayingBackgroundStyles {
    const val DEFAULT = "default"
    const val COVER = "cover"
    val options = listOf(
        XvoxNowPlayingBackgroundOption(DEFAULT, "Default"),
        XvoxNowPlayingBackgroundOption(COVER, "Cover")
    )

    fun normalize(value: String?): String =
        if (value.equals(COVER, ignoreCase = true)) COVER else DEFAULT
}

/** Preserve Default's original adaptive-cover source; missing analysis falls back to accent. */
@Composable
fun xvoxNowPlayingBackgroundColor(
    @Suppress("UNUSED_PARAMETER") style: String,
    dominant: Color
): Color {
    val colors = XvoxTheme.colors
    return dominant.takeIf { it != Color.Unspecified && it != Color.Transparent } ?: colors.primaryAccent
}
