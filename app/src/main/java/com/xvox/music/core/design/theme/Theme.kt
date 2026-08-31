package com.xvox.music.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class XvoxThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

private val LightColors = lightColorScheme(
    primary = XvoxPurple,
    onPrimary = XvoxLightSurface,
    background = XvoxLightBackground,
    onBackground = XvoxLightText,
    surface = XvoxLightSurface,
    onSurface = XvoxLightText,
    surfaceVariant = XvoxLightSurfaceVariant,
    onSurfaceVariant = XvoxLightSecondaryText,
    error = XvoxError
)

private val DarkColors = darkColorScheme(
    primary = XvoxPurpleLight,
    onPrimary = XvoxDarkBackground,
    background = XvoxDarkBackground,
    onBackground = XvoxDarkText,
    surface = XvoxDarkSurface,
    onSurface = XvoxDarkText,
    surfaceVariant = XvoxDarkSurfaceVariant,
    onSurfaceVariant = XvoxDarkSecondaryText,
    error = XvoxError
)

private val AmoledColors = darkColorScheme(
    primary = XvoxPurpleLight,
    onPrimary = XvoxAmoledBackground,
    background = XvoxAmoledBackground,
    onBackground = XvoxDarkText,
    surface = XvoxAmoledSurface,
    onSurface = XvoxDarkText,
    surfaceVariant = XvoxDarkSurfaceVariant,
    onSurfaceVariant = XvoxDarkSecondaryText,
    error = XvoxError
)

@Composable
fun XvoxTheme(
    mode: XvoxThemeMode = XvoxThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val colors = when (mode) {
        XvoxThemeMode.LIGHT -> LightColors
        XvoxThemeMode.DARK -> DarkColors
        XvoxThemeMode.AMOLED -> AmoledColors
        XvoxThemeMode.SYSTEM ->
            if (isSystemInDarkTheme()) DarkColors else LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = XvoxTypography,
        shapes = XvoxShapes,
        content = content
    )
}
