package com.xvox.music.player.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.xvox.music.core.design.theme.XvoxTheme

/** Now Playing intentionally has one adaptive, cover-matched background treatment. */
data class XvoxNowPlayingBackgroundOption(
    val key: String,
    val title: String
)

object XvoxNowPlayingBackgroundStyles {
    const val DEFAULT = "default"
    val options = listOf(XvoxNowPlayingBackgroundOption(DEFAULT, "Default"))
    fun normalize(value: String?): String = DEFAULT
}

private fun Color.xvoxBlendTo(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = alpha + (other.alpha - alpha) * t
    )
}

/**
 * Theme-safe fallback for very dark/near-neutral artwork. A black cover used to make the whole
 * player look like a black sheet; switching to the user's accent retains cover affinity without
 * losing the colourful liquid background.
 */
@Composable
private fun backgroundSource(dominant: Color): Color {
    val colors = XvoxTheme.colors
    val maximumChannel = maxOf(dominant.red, dominant.green, dominant.blue)
    val minimumChannel = minOf(dominant.red, dominant.green, dominant.blue)
    val hasUsefulCoverColor =
        dominant != Color.Unspecified && dominant != Color.Transparent &&
            maximumChannel > .16f && (maximumChannel - minimumChannel > .035f || maximumChannel > .38f)
    return if (hasUsefulCoverColor) dominant else colors.primaryAccent
}

/**
 * [dominant] is the live clustered palette color for the cover. The palette produces two related
 * but distinct colour stops, so song changes animate the entire player and lyrics canvas as a
 * smooth cover-matched gradient rather than a flat/black fill.
 */
@Composable
fun xvoxNowPlayingBackgroundColor(style: String, dominant: Color): Color = backgroundSource(dominant)

@Composable
fun xvoxNowPlayingBackgroundBrush(style: String, dominant: Color): Brush {
    val colors = XvoxTheme.colors
    val base = backgroundSource(dominant)
    val first = base.xvoxBlendTo(colors.primaryAccent, .20f)
    val companion = base.xvoxBlendTo(colors.primaryAccent.xvoxBlendTo(Color(0xFF7768FF), .42f), .54f)
    val depth = base.xvoxBlendTo(colors.background, .34f)
    return Brush.linearGradient(colors = listOf(first, companion, depth))
}
