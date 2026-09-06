package com.xvox.music.core.design.theme

import androidx.compose.ui.graphics.Color

data class XvoxPalette(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val cardBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val primaryAccent: Color,
    val accentSoft: Color,
    val progressTrack: Color,
    val progressActive: Color
) {
    fun withAccent(accentName: String): XvoxPalette {
        val accentColor = when (accentName) {
            "XVOX Red", "Red" -> Color(0xFFFA2D48)
            "XVOX Blue", "Blue" -> Color(0xFF007AFF)
            else -> return this
        }
        return this.copy(
            primaryAccent = accentColor,
            progressActive = accentColor,
            accentSoft = accentColor.copy(alpha = 0.18f)
        )
    }
}

val XvoxWhitePalette = XvoxPalette(
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF2F2F2),
    cardBorder = Color(0xFFE2E2E2),
    primaryText = Color(0xFF111111),
    secondaryText = Color(0xFF2E2E2E),
    mutedText = Color(0xFF555555),
    primaryAccent = Color(0xFF171717),
    accentSoft = Color(0xFFE8E8E8),
    progressTrack = Color(0xFFD9D9D9),
    progressActive = Color(0xFF171717)
)

val XvoxDarkPalette = XvoxPalette(
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF111111),
    card = Color(0xFF171717),
    cardElevated = Color(0xFF1E1E1E),
    cardBorder = Color(0xFF292929),
    primaryText = Color(0xFFFFFFFF),
    secondaryText = Color(0xFFEDEDED),
    mutedText = Color(0xFFCCCCCC),
    primaryAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFF292929),
    progressTrack = Color(0xFF363636),
    progressActive = Color(0xFFFFFFFF)
)

val XvoxAmoledPalette = XvoxPalette(
    background = Color(0xFF000000),
    surface = Color(0xFF050505),
    card = Color(0xFF0B0B0B),
    cardElevated = Color(0xFF121212),
    cardBorder = Color(0xFF202020),
    primaryText = Color(0xFFFFFFFF),
    secondaryText = Color(0xFFEDEDED),
    mutedText = Color(0xFFCCCCCC),
    primaryAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFF181818),
    progressTrack = Color(0xFF303030),
    progressActive = Color(0xFFFFFFFF)
)

val XvoxSuccess = Color(0xFF45B97C)
