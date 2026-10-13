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

/**
 * Default keeps its adaptive cover source.  When artwork analysis is unavailable, callers supply
 * the theme-derived default-cover palette; the final guard remains theme-neutral rather than
 * falling back to the fresh-install red accent.
 */
@Composable
fun xvoxNowPlayingBackgroundColor(
    @Suppress("UNUSED_PARAMETER") style: String,
    dominant: Color
): Color {
    val colors = XvoxTheme.colors
    val neutralFallback = if (colors.isLight) colors.cardElevated else colors.background
    return dominant.takeIf { it != Color.Unspecified && it != Color.Transparent } ?: neutralFallback
}
