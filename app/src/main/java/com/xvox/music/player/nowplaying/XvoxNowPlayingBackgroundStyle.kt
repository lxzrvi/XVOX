package com.xvox.music.player.nowplaying

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xvox.music.core.design.theme.XvoxTheme

/** The background menu deliberately retains one faithful, artwork-derived Default backdrop. */
data class XvoxNowPlayingBackgroundOption(
    val key: String,
    val title: String
)

object XvoxNowPlayingBackgroundStyles {
    const val DEFAULT = "default"
    val options = listOf(XvoxNowPlayingBackgroundOption(DEFAULT, "Default"))
    fun normalize(value: String?): String = DEFAULT
}

/** A real visual handoff for every cover/palette change. */
data class XvoxBackgroundTransitionOption(
    val key: String,
    val title: String,
    val subtitle: String
)

object XvoxBackgroundTransitionStyles {
    const val DISSOLVE = "dissolve"
    val options = listOf(
        XvoxBackgroundTransitionOption(DISSOLVE, "Dissolve", "Soft grain-free dissolve"),
        XvoxBackgroundTransitionOption("crossfade", "Crossfade", "Balanced palette fade"),
        XvoxBackgroundTransitionOption("morph", "Morph", "Continuous colour interpolation"),
        XvoxBackgroundTransitionOption("drift_left", "Drift Left", "Palette arrives from the right"),
        XvoxBackgroundTransitionOption("drift_right", "Drift Right", "Palette arrives from the left"),
        XvoxBackgroundTransitionOption("rise", "Rise", "Upward colour handoff"),
        XvoxBackgroundTransitionOption("fall", "Fall", "Downward colour handoff"),
        XvoxBackgroundTransitionOption("zoom_in", "Zoom In", "Growing colour field"),
        XvoxBackgroundTransitionOption("zoom_out", "Zoom Out", "Receding colour field"),
        XvoxBackgroundTransitionOption("depth", "Depth", "Layered distant handoff"),
        XvoxBackgroundTransitionOption("sweep", "Sweep", "Fast lateral sweep"),
        XvoxBackgroundTransitionOption("curtain", "Curtain", "Tall vertical reveal"),
        XvoxBackgroundTransitionOption("pulse", "Pulse", "Brief soft pulse"),
        XvoxBackgroundTransitionOption("bloom", "Bloom", "Slow luminous bloom"),
        XvoxBackgroundTransitionOption("ripple", "Ripple", "Expanding colour ripple"),
        XvoxBackgroundTransitionOption("orbit", "Orbit", "Curved colour orbit"),
        XvoxBackgroundTransitionOption("tilt", "Tilt", "Angled depth exchange"),
        XvoxBackgroundTransitionOption("flicker", "Flicker", "Quick two-step flash"),
        XvoxBackgroundTransitionOption("breathe", "Breathe", "Long relaxed fade"),
        XvoxBackgroundTransitionOption("snap", "Snap", "Crisp compact switch")
    )
    fun normalize(value: String?): String = options.firstOrNull { it.key == value }?.key ?: DISSOLVE
}

/**
 * Twenty distinct treatments for deriving a usable background colour from the cover's dominant
 * colour.  Each preserves the cover as the source while safely blending with active theme colours
 * where contrast needs help.
 */
data class XvoxBackgroundMethodOption(
    val key: String,
    val title: String,
    val subtitle: String
)

object XvoxBackgroundMethodStyles {
    const val DOMINANT = "dominant"
    val options = listOf(
        XvoxBackgroundMethodOption(DOMINANT, "Dominant", "Faithful dominant cover colour"),
        XvoxBackgroundMethodOption("muted", "Muted", "Reduced saturation, softer field"),
        XvoxBackgroundMethodOption("vivid", "Vivid", "Boost cover saturation"),
        XvoxBackgroundMethodOption("deep", "Deep", "Lower-value cover tone"),
        XvoxBackgroundMethodOption("light", "Light", "Lifted cover tone"),
        XvoxBackgroundMethodOption("warm", "Warm Shift", "Rotate toward warmth"),
        XvoxBackgroundMethodOption("cool", "Cool Shift", "Rotate toward coolness"),
        XvoxBackgroundMethodOption("complement", "Complement", "Opposite dominant hue"),
        XvoxBackgroundMethodOption("analogous_warm", "Analogous Warm", "Neighbouring warm hue"),
        XvoxBackgroundMethodOption("analogous_cool", "Analogous Cool", "Neighbouring cool hue"),
        XvoxBackgroundMethodOption("split_warm", "Split Warm", "Warm split-complement tone"),
        XvoxBackgroundMethodOption("split_cool", "Split Cool", "Cool split-complement tone"),
        XvoxBackgroundMethodOption("accent", "Accent Blend", "Cover mixed with app accent"),
        XvoxBackgroundMethodOption("surface", "Surface Blend", "Cover mixed with theme surface"),
        XvoxBackgroundMethodOption("card", "Card Blend", "Cover mixed with theme card"),
        XvoxBackgroundMethodOption("monochrome", "Monochrome", "Low-saturation tonal cover"),
        XvoxBackgroundMethodOption("pastel", "Pastel", "Soft high-value cover treatment"),
        XvoxBackgroundMethodOption("neon", "Neon", "Full-saturation cover treatment"),
        XvoxBackgroundMethodOption("shadow", "Shadow", "Cover settled into the theme background"),
        XvoxBackgroundMethodOption("highlight", "Highlight", "Cover lifted toward primary text")
    )
    fun normalize(value: String?): String = options.firstOrNull { it.key == value }?.key ?: DOMINANT
}

/** Theme-safe fallback when artwork has not produced a palette colour yet. */
@Composable
private fun backgroundSource(dominant: Color): Color {
    val colors = XvoxTheme.colors
    return dominant.takeIf { it != Color.Unspecified && it != Color.Transparent } ?: colors.primaryAccent
}

/** Default remains the original single dominant-colour backdrop. */
@Composable
fun xvoxNowPlayingBackgroundColor(style: String, dominant: Color): Color = backgroundSource(dominant)

/** Retained for source compatibility; Default intentionally has no alternate gradient treatment. */
@Composable
fun xvoxNowPlayingBackgroundBrush(style: String, dominant: Color): Brush? = null

@Composable
fun xvoxNowPlayingBackgroundMethodColor(method: String, dominant: Color): Color {
    val colors = XvoxTheme.colors
    val source = backgroundSource(dominant)
    val hsv = source.asHsv()
    val hue = hsv[0]
    val saturation = hsv[1]
    val value = hsv[2]
    fun hsvColor(h: Float = hue, s: Float = saturation, v: Float = value): Color =
        Color(AndroidColor.HSVToColor(floatArrayOf(wrapHue(h), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))

    return when (XvoxBackgroundMethodStyles.normalize(method)) {
        "muted" -> hsvColor(s = saturation * .54f, v = value * .86f)
        "vivid" -> hsvColor(s = (saturation * 1.30f).coerceAtMost(1f), v = (value * 1.06f).coerceAtMost(1f))
        "deep" -> hsvColor(s = (saturation * 1.08f).coerceAtMost(1f), v = value * .42f)
        "light" -> hsvColor(s = saturation * .64f, v = value + (1f - value) * .45f)
        "warm" -> hsvColor(h = hue + 18f)
        "cool" -> hsvColor(h = hue - 22f)
        "complement" -> hsvColor(h = hue + 180f)
        "analogous_warm" -> hsvColor(h = hue + 32f)
        "analogous_cool" -> hsvColor(h = hue - 32f)
        "split_warm" -> hsvColor(h = hue + 145f, s = saturation * .82f)
        "split_cool" -> hsvColor(h = hue - 145f, s = saturation * .82f)
        "accent" -> mix(source, colors.primaryAccent, .30f)
        "surface" -> mix(source, colors.background, .34f)
        "card" -> mix(source, colors.cardElevated, .32f)
        "monochrome" -> hsvColor(s = .10f, v = value * .76f)
        "pastel" -> hsvColor(s = saturation * .36f, v = value + (1f - value) * .58f)
        "neon" -> hsvColor(s = 1f, v = 1f)
        "shadow" -> mix(source, colors.background, .62f)
        "highlight" -> mix(source, colors.primaryText, .26f)
        else -> source
    }
}

private fun Color.asHsv(): FloatArray = FloatArray(3).also { AndroidColor.colorToHSV(toArgb(), it) }
private fun wrapHue(value: Float): Float = ((value % 360f) + 360f) % 360f
private fun mix(first: Color, second: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = first.red + (second.red - first.red) * t,
        green = first.green + (second.green - first.green) * t,
        blue = first.blue + (second.blue - first.blue) * t,
        alpha = first.alpha + (second.alpha - first.alpha) * t
    )
}
