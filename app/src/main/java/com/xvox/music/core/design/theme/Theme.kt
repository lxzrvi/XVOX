package com.xvox.music.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

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

@Composable
fun XvoxTheme(
    mode: XvoxThemeMode = XvoxThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val palette = when (mode) {
        XvoxThemeMode.LIGHT -> XvoxWhitePalette
        XvoxThemeMode.DARK -> XvoxDarkPalette
        XvoxThemeMode.AMOLED -> XvoxAmoledPalette

        XvoxThemeMode.SYSTEM -> {
            if (isSystemInDarkTheme()) {
                XvoxDarkPalette
            } else {
                XvoxWhitePalette
            }
        }
    }

    val materialColors =
        if (
            mode == XvoxThemeMode.LIGHT ||
            (
                mode == XvoxThemeMode.SYSTEM &&
                !isSystemInDarkTheme()
            )
        ) {
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

    androidx.compose.runtime.CompositionLocalProvider(
        LocalXvoxPalette provides palette
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = XvoxTypography,
            shapes = XvoxShapes,
            content = content
        )
    }
}
