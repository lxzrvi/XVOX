package com.xvox.music.core.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.skydoves.cloudy.Sky
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import com.xvox.music.core.design.theme.XvoxTheme

enum class XvoxGlassStyle {
    HEADER,
    HEADER_ACTIONS,
    MINI_PLAYER,
    NAVIGATION,
    NAVIGATION_SELECTOR
}

@Composable
fun rememberXvoxSky(): Sky {
    return rememberSky()
}

fun Modifier.xvoxGlassSource(
    sky: Sky
): Modifier {
    return this.sky(sky)
}

@Composable
fun Modifier.xvoxGlass(
    sky: Sky,
    style: XvoxGlassStyle
): Modifier {
    val colors =
        XvoxTheme.colors

    return when (style) {
        XvoxGlassStyle.HEADER -> {
            cloudy(
                sky = sky,
                radius = 24,
                tint =
                    colors.surface.copy(
                        alpha = 0.38f
                    )
            )
        }

        XvoxGlassStyle.HEADER_ACTIONS -> {
            cloudy(
                sky = sky,
                radius = 20,
                tint =
                    colors.card.copy(
                        alpha = 0.28f
                    )
            )
        }

        XvoxGlassStyle.MINI_PLAYER -> {
            cloudy(
                sky = sky,
                radius = 24,
                tint =
                    colors.surface.copy(
                        alpha = 0.38f
                    )
            )
        }

        XvoxGlassStyle.NAVIGATION -> {
            cloudy(
                sky = sky,
                radius = 24,
                tint =
                    colors.surface.copy(
                        alpha = 0.36f
                    )
            )
        }

        XvoxGlassStyle.NAVIGATION_SELECTOR -> {
            cloudy(
                sky = sky,
                radius = 18,
                tint =
                    colors.cardElevated.copy(
                        alpha = 0.26f
                    )
            )
        }
    }
}
