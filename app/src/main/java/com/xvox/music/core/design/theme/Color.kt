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
    /**
     * Applies the chosen accent. `light` selects the correct pair of the iOS-style palette:
     *
     *   Red    #FF3B30 (light) / #FF453A (dark, AMOLED)
     *   Blue   #007AFF (light) / #0A84FF (dark, AMOLED)
     *   White  monochrome accent (ink on light themes, pure white on dark)
     *
     * Red is the default; legacy names ("Default", "XVOX …") fold into it so existing
     * installations keep a coloured accent instead of falling back to plain text colour.
     */
    fun withAccent(accentName: String, light: Boolean): XvoxPalette {
        // A "#RRGGBB" value means the user picked a fully custom accent in Settings.
        val customAccent = if (accentName.startsWith("#")) {
            com.xvox.music.core.ui.chrome.parseHexColor(accentName)
        } else null
        val normalized = when (accentName) {
            "Default", "XVOX Red" -> "Red"
            "XVOX Blue" -> "Blue"
            else -> accentName
        }
        val accentColor = customAccent ?: when (normalized) {
            "Blue" -> if (light) Color(0xFF007AFF) else Color(0xFF0A84FF)
            "White" -> if (light) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
            else -> if (light) Color(0xFFFF3B30) else Color(0xFFFF453A) // Red / anything legacy
        }
        return this.copy(
            primaryAccent = accentColor,
            progressActive = accentColor,
            accentSoft = accentColor.copy(alpha = 0.18f)
        )
    }

    /**
     * Swaps the page background for the chosen tinted preset and applies the global card
     * transparency slider. Cards blend with whatever is behind them (theme background or a
     * custom background photo), everywhere except the Now Playing artwork surface.
     */
    fun withBackdrop(backgroundName: String, light: Boolean, transparency: Float): XvoxPalette {
        val background = when (backgroundName) {
            "Midnight" -> if (light) Color(0xFFE9EDF7) else Color(0xFF070B16)
            "Warm" -> if (light) Color(0xFFF6F0E6) else Color(0xFF16130E)
            else -> this.background
        }
        val keep = (1f - transparency.coerceIn(0f, 0.6f))
        return this.copy(
            background = background,
            card = card.copy(alpha = keep),
            cardElevated = cardElevated.copy(alpha = keep),
            surface = surface.copy(alpha = keep)
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
