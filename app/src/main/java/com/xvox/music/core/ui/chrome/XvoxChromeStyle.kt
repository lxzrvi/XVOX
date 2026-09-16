package com.xvox.music.core.ui.chrome

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Per-surface chrome knobs from Settings (Appearance). Everything is stored as one encoded
 * preference so the whole set travels through [SettingsState] in a single field and is offered
 * to every composable through [LocalXvoxChromeStyle].
 *
 * Border colour fields are hex like "#7A5CFF" or empty when the user wants the theme's own
 * border colour; each border/alpha pair lets them tune fills and outlines separately.
 */
data class XvoxChromeStyle(
    // Option boxes (every options popup opened over the app).
    val optionBoxBgAlpha: Float = 1f,
    val optionBoxBorder: String = "",
    val optionBoxBorderAlpha: Float = 1f,
    // Home / top header strip.
    val headerBgAlpha: Float = 1f,
    val headerBorder: String = "",
    val headerBorderAlpha: Float = 0f,
    // Mini player bar.
    val miniBgAlpha: Float = 1f,
    val miniBorder: String = "",
    val miniBorderAlpha: Float = 0.62f,
    // Floating navigation bar.
    val navBgAlpha: Float = 0.88f,
    val navBorder: String = "",
    val navBorderAlpha: Float = 0.62f,
    // Navigation selector "pill".
    val pillColor: String = "",
    val pillAlpha: Float = 1f,
    // Colour of the icon sitting inside the pill ("" = the theme accent).
    val pillIconColor: String = "",
    // Cards everywhere (border only; the card fill transparency lives in Theme's card alpha).
    val cardBorder: String = "",
    val cardBorderAlpha: Float = 1f,
    // Mini player cover mode ("default" or "full").
    val miniCoverStyle: String = "default"
) {
    fun encode(): String = listOf(
        optionBoxBgAlpha, optionBoxBorder, optionBoxBorderAlpha,
        headerBgAlpha, headerBorder, headerBorderAlpha,
        miniBgAlpha, miniBorder, miniBorderAlpha,
        navBgAlpha, navBorder, navBorderAlpha,
        pillColor, pillAlpha,
        pillIconColor,
        cardBorder, cardBorderAlpha,
        miniCoverStyle
    ).joinToString("|")

    companion object {
        fun decode(raw: String): XvoxChromeStyle {
            val parts = raw.split("|")
            fun str(i: Int): String = parts.getOrNull(i).orEmpty().trim()
            fun flt(i: Int, fallback: Float): Float =
                parts.getOrNull(i)?.trim()?.toFloatOrNull()?.coerceIn(0f, 1f) ?: fallback
            if (parts.size == 16) {
                return XvoxChromeStyle(
                    optionBoxBgAlpha = flt(0, 1f), optionBoxBorder = str(1), optionBoxBorderAlpha = flt(2, 1f),
                    headerBgAlpha = flt(3, 1f), headerBorder = str(4), headerBorderAlpha = flt(5, 0f),
                    miniBgAlpha = flt(6, 1f), miniBorder = str(7), miniBorderAlpha = flt(8, 0.62f),
                    navBgAlpha = flt(9, 0.88f), navBorder = str(10), navBorderAlpha = flt(11, 0.62f),
                    pillColor = str(12), pillAlpha = flt(13, 1f),
                    cardBorder = str(14), cardBorderAlpha = flt(15, 1f)
                )
            }
            if (parts.size == 17) {
                return XvoxChromeStyle(
                    optionBoxBgAlpha = flt(0, 1f), optionBoxBorder = str(1), optionBoxBorderAlpha = flt(2, 1f),
                    headerBgAlpha = flt(3, 1f), headerBorder = str(4), headerBorderAlpha = flt(5, 0f),
                    miniBgAlpha = flt(6, 1f), miniBorder = str(7), miniBorderAlpha = flt(8, 0.62f),
                    navBgAlpha = flt(9, 0.88f), navBorder = str(10), navBorderAlpha = flt(11, 0.62f),
                    pillColor = str(12), pillAlpha = flt(13, 1f),
                    pillIconColor = str(14),
                    cardBorder = str(15), cardBorderAlpha = flt(16, 1f)
                )
            }
            if (parts.size < 18) return XvoxChromeStyle()
            return XvoxChromeStyle(
                optionBoxBgAlpha = flt(0, 1f), optionBoxBorder = str(1), optionBoxBorderAlpha = flt(2, 1f),
                headerBgAlpha = flt(3, 1f), headerBorder = str(4), headerBorderAlpha = flt(5, 0f),
                miniBgAlpha = flt(6, 1f), miniBorder = str(7), miniBorderAlpha = flt(8, 0.62f),
                navBgAlpha = flt(9, 0.88f), navBorder = str(10), navBorderAlpha = flt(11, 0.62f),
                pillColor = str(12), pillAlpha = flt(13, 1f),
                pillIconColor = str(14),
                cardBorder = str(15), cardBorderAlpha = flt(16, 1f),
                miniCoverStyle = if (str(17).isNotBlank()) str(17) else "default"
            )
        }
    }
}

val LocalXvoxChromeStyle = staticCompositionLocalOf { XvoxChromeStyle() }

/** Parses "#RRGGBB" (or short "#RGB") into a colour; null when blank/invalid. */
fun parseHexColor(hex: String): Color? {
    var value = hex.trim().removePrefix("#")
    if (value.length == 3) value = value.map { "$it$it" }.joinToString("")
    if (value.length != 6) return null
    val rgb = value.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or rgb)
}
