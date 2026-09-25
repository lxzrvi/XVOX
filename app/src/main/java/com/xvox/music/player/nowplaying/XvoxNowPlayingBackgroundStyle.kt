package com.xvox.music.player.nowplaying

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.xvox.music.core.design.theme.XvoxTheme

/** The ten background choices exposed from Now Playing's three-dot menu. */
data class XvoxNowPlayingBackgroundOption(
    val key: String,
    val title: String
)

object XvoxNowPlayingBackgroundStyles {
    const val DEFAULT = "default"
    val options = listOf(
        XvoxNowPlayingBackgroundOption(DEFAULT, "Default"),
        XvoxNowPlayingBackgroundOption("gradient_1", "Gradient 1"),
        XvoxNowPlayingBackgroundOption("gradient_2", "Gradient 2"),
        XvoxNowPlayingBackgroundOption("gradient_3", "Gradient 3"),
        XvoxNowPlayingBackgroundOption("solid_1", "Solid 1"),
        XvoxNowPlayingBackgroundOption("solid_2", "Solid 2"),
        XvoxNowPlayingBackgroundOption("solid_3", "Solid 3"),
        XvoxNowPlayingBackgroundOption("cool_1", "Cool 1"),
        XvoxNowPlayingBackgroundOption("cool_2", "Cool 2"),
        XvoxNowPlayingBackgroundOption("cool_3", "Cool 3")
    )

    fun normalize(value: String?): String = options.firstOrNull { it.key == value }?.key ?: DEFAULT
}

/**
 * Creates related shades from the current cover's palette. No hard-coded app-color background is
 * introduced: if artwork has not yielded a colour yet, the active theme palette is the fallback.
 */
@Composable
private fun backgroundSource(dominant: Color): Color {
    val colors = XvoxTheme.colors
    return dominant.takeIf { it != Color.Unspecified && it != Color.Transparent } ?: colors.primaryAccent
}

private fun hueShift(color: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(
        (color.red * 255).toInt().coerceIn(0, 255),
        (color.green * 255).toInt().coerceIn(0, 255),
        (color.blue * 255).toInt().coerceIn(0, 255),
        hsv
    )
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    return Color(AndroidColor.HSVToColor((color.alpha * 255).toInt().coerceIn(0, 255), hsv))
}

@Composable
private fun coverTone(source: Color, amount: Float): Color =
    lerp(source, XvoxTheme.colors.background, amount.coerceIn(0f, 1f))

/** Solid fill for Default and the three distinct Solid choices. */
@Composable
fun xvoxNowPlayingBackgroundColor(style: String, dominant: Color): Color {
    val source = backgroundSource(dominant)
    return when (XvoxNowPlayingBackgroundStyles.normalize(style)) {
        XvoxNowPlayingBackgroundStyles.DEFAULT -> source // preserves the original default backdrop exactly
        "solid_1" -> coverTone(source, .16f)
        "solid_2" -> coverTone(source, .34f)
        "solid_3" -> lerp(source, XvoxTheme.colors.cardElevated, .30f)
        "cool_1" -> coverTone(hueShift(source, 150f), .12f)
        "cool_2" -> coverTone(hueShift(source, 185f), .24f)
        "cool_3" -> coverTone(hueShift(source, 220f), .36f)
        else -> source
    }
}

/** Gradient choices are separate and deliberately use only tones derived from the cover colour. */
@Composable
fun xvoxNowPlayingBackgroundBrush(style: String, dominant: Color): Brush? {
    val source = backgroundSource(dominant)
    return when (XvoxNowPlayingBackgroundStyles.normalize(style)) {
        "gradient_1" -> Brush.linearGradient(
            listOf(coverTone(source, .55f), source, hueShift(source, 28f))
        )
        "gradient_2" -> Brush.linearGradient(
            listOf(hueShift(source, -32f), coverTone(source, .18f), coverTone(source, .64f))
        )
        "gradient_3" -> Brush.radialGradient(
            listOf(hueShift(source, 42f), source, coverTone(source, .72f))
        )
        else -> null
    }
}
