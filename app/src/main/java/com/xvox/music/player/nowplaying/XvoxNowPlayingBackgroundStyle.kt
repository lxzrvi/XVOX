package com.xvox.music.player.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.xvox.music.core.design.theme.XvoxTheme

/** Now Playing intentionally has one faithful, adaptive-cover Default backdrop. */
data class XvoxNowPlayingBackgroundOption(
    val key: String,
    val title: String
)

object XvoxNowPlayingBackgroundStyles {
    const val DEFAULT = "default"
    val options = listOf(XvoxNowPlayingBackgroundOption(DEFAULT, "Default"))
    fun normalize(value: String?): String = DEFAULT
}

/** Theme-safe fallback only when a cover cannot provide a palette result. */
@Composable
private fun backgroundSource(dominant: Color): Color {
    val colors = XvoxTheme.colors
    return dominant.takeIf { it != Color.Unspecified && it != Color.Transparent } ?: colors.primaryAccent
}

/**
 * [dominant] is already the cover-derived, clustered and HSL-adapted colour supplied by
 * XvoxArtworkPaletteLoader. This function deliberately performs no normalisation or preset hue
 * shift: red stays red, and each cover keeps its own close background.
 */
@Composable
fun xvoxNowPlayingBackgroundColor(style: String, dominant: Color): Color = backgroundSource(dominant)

/** Retained for source compatibility; Default uses a single adaptive colour surface. */
@Composable
fun xvoxNowPlayingBackgroundBrush(style: String, dominant: Color): Brush? = null
