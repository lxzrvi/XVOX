package com.xvox.music.core.design.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xvox.music.core.ui.chrome.XvoxChromeStyle

/**
 * App-wide UI experiments. Blur is intentionally the only experimental treatment: it uses the
 * live, GPU-blurred Home backdrop rather than a painted gradient, artificial reflection, or
 * decorative outline.
 */
enum class XvoxExperimentalAppearance(val storageValue: String) {
    DEFAULT("default"),
    BLUR("blur");

    companion object {
        /** Glass is a migration alias for the replacement real Blur UI; retired modes fall back. */
        fun fromStorage(value: String?): XvoxExperimentalAppearance = when (value?.trim()?.lowercase()) {
            BLUR.storageValue, "glass", "live_blur", "live blur" -> BLUR
            else -> DEFAULT
        }
    }
}

val LocalXvoxExperimentalAppearance = staticCompositionLocalOf { XvoxExperimentalAppearance.DEFAULT }

/**
 * Blur surfaces are translucent materials with no synthetic border or reflection. The content
 * behind them is the same live background image that [TabSurface] renders through a native GPU
 * blur, so cards, navigation, Mini Player and sheets reveal a genuine moving backdrop.
 */
fun XvoxPalette.withExperimentalAppearance(mode: XvoxExperimentalAppearance): XvoxPalette = when (mode) {
    XvoxExperimentalAppearance.DEFAULT -> this
    XvoxExperimentalAppearance.BLUR -> {
        val material = if (isLight) Color.White else Color(0xFF141414)
        copy(
            surface = material.copy(alpha = .72f),
            card = material.copy(alpha = .70f),
            cardElevated = material.copy(alpha = .80f),
            cardBorder = Color.Transparent,
            accentSoft = primaryAccent.copy(alpha = .16f),
            progressTrack = primaryText.copy(alpha = .22f)
        )
    }
}

/**
 * Keep stored chrome durable, but remove all artificial outlines while Blur UI is active. The
 * shared palette carries the material alpha; no gradient/reflection/border is layered over it.
 */
fun XvoxChromeStyle.forExperimentalAppearance(mode: XvoxExperimentalAppearance): XvoxChromeStyle = when (mode) {
    XvoxExperimentalAppearance.DEFAULT -> this
    XvoxExperimentalAppearance.BLUR -> copy(
        optionBoxBgAlpha = optionBoxBgAlpha.coerceAtMost(.92f),
        optionBoxBorderAlpha = 0f,
        headerBgAlpha = headerBgAlpha.coerceAtMost(.78f),
        headerBorderAlpha = 0f,
        miniBgAlpha = miniBgAlpha.coerceAtMost(.78f),
        miniBorderAlpha = 0f,
        navBgAlpha = navBgAlpha.coerceAtMost(.78f),
        navBorderAlpha = 0f,
        cardBorderAlpha = 0f
    )
}

/**
 * Compatibility hook retained at existing surface call sites. Blur UI deliberately draws no
 * fake reflection, gradient, or outline; the real blurred backdrop is provided behind it.
 */
fun Modifier.xvoxGlassReflection(): Modifier = this
