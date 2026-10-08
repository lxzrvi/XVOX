package com.xvox.music.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

enum class XvoxThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

private val LocalXvoxPalette =
    staticCompositionLocalOf {
        XvoxDarkPalette
    }

object XvoxTheme {
    val colors: XvoxPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalXvoxPalette.current
}

/**
 * Removes the global card-transparency alpha for one subtree. Used around Now Playing so its
 * chrome (controls, options and queue sheets over the artwork surface) stays as designed while
 * Home, Liked, playlists, Search and Settings keep the transparent cards and tinted backdrop.
 */
@Composable
fun ProvideXvoxNowPlayingChrome(content: @Composable () -> Unit) {
    val palette = LocalXvoxPalette.current
    // Glass is intentionally allowed to remain translucent over Now Playing artwork. Default and
    // DP Minimal retain the established opaque player-chrome contract.
    if (LocalXvoxExperimentalAppearance.current == XvoxExperimentalAppearance.GLASS) {
        content()
    } else {
        CompositionLocalProvider(
            LocalXvoxPalette provides palette.copy(
                card = palette.card.copy(alpha = 1f),
                cardElevated = palette.cardElevated.copy(alpha = 1f),
                surface = palette.surface.copy(alpha = 1f)
            ),
            content = content
        )
    }
}

@Composable
fun XvoxTheme(
    mode: XvoxThemeMode = XvoxThemeMode.SYSTEM,
    accent: String = "Red",
    background: String = "Default",
    cardTransparency: Float = 0f,
    cardBorder: String = "",
    cardBorderAlpha: Float = 1f,
    experimentalAppearance: XvoxExperimentalAppearance = XvoxExperimentalAppearance.DEFAULT,
    content: @Composable () -> Unit
) {
    val ordinaryDark = when (mode) {
        XvoxThemeMode.LIGHT -> false
        XvoxThemeMode.DARK, XvoxThemeMode.AMOLED -> true
        XvoxThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    // DP Minimal is deliberately dark regardless of the ordinary Theme selector. It is a full
    // app-wide appearance experiment, not just a darkened card treatment.
    val dark = ordinaryDark || experimentalAppearance == XvoxExperimentalAppearance.DP_MINIMAL

    val ordinaryBasePalette = when (mode) {
        XvoxThemeMode.LIGHT -> XvoxWhitePalette
        XvoxThemeMode.DARK -> XvoxDarkPalette
        XvoxThemeMode.AMOLED -> XvoxAmoledPalette

        XvoxThemeMode.SYSTEM -> {
            if (ordinaryDark) {
                XvoxDarkPalette
            } else {
                XvoxWhitePalette
            }
        }
    }
    val basePalette = if (experimentalAppearance == XvoxExperimentalAppearance.DP_MINIMAL) {
        XvoxDarkPalette
    } else {
        ordinaryBasePalette
    }

    val palette = basePalette
        .withAccent(accent, light = !dark)
        .withBackdrop(background, light = !dark, transparency = cardTransparency)
        .withExperimentalAppearance(experimentalAppearance)
        .let { p ->
            val chosen = com.xvox.music.core.ui.chrome.parseHexColor(cardBorder) ?: p.cardBorder
            val alpha = cardBorderAlpha.coerceIn(0f, 1f)
            p.copy(cardBorder = chosen.copy(alpha = chosen.alpha * alpha))
        }

    val isLight = palette.isLight

    val materialColors = if (isLight) {
        lightColorScheme(
                primary = palette.primaryAccent,
                onPrimary = palette.background,
                background = palette.background,
                onBackground = palette.primaryText,
                surface = palette.surface,
                onSurface = palette.primaryText,
                outline = palette.cardBorder
            )
        } else {
            darkColorScheme(
                primary = palette.primaryAccent,
                onPrimary = palette.background,
                background = palette.background,
                onBackground = palette.primaryText,
                surface = palette.surface,
                onSurface = palette.primaryText,
                outline = palette.cardBorder
            )
        }

    CompositionLocalProvider(
        LocalXvoxPalette provides palette,
        LocalXvoxExperimentalAppearance provides experimentalAppearance
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = XvoxTypography,
            shapes = XvoxShapes
        ) {
            ProvideTextStyle(
                value = TextStyle(
                    fontFamily = XvoxUiFont
                ),
                content = content
            )
        }
    }
}
