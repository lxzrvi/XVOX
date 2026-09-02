package com.xvox.music.core.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.skydoves.cloudy.Sky
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.liquidGlass
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import com.xvox.music.core.design.theme.XvoxTheme

enum class XvoxGlassStyle {
    HEADER,
    HEADER_ACTIONS,
    MINI_PLAYER,
    MINI_CONTROL,
    NAVIGATION
}

object XvoxGlassLens {

    const val NavigationRefraction =
        0.10f

    const val NavigationCurve =
        0.12f

    const val NavigationDispersion =
        0.0f

    const val NavigationSaturation =
        1.0f

    const val NavigationContrast =
        1.0f

    const val NavigationEdge =
        0.10f
}

@Composable
fun rememberXvoxSky(): Sky {
    return rememberSky()
}

fun Modifier.xvoxGlassSource(
    sky: Sky
): Modifier {
    return sky(sky)
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
                        alpha = 0.26f
                    )
            )
        }

        XvoxGlassStyle.MINI_PLAYER -> {
            cloudy(
                sky = sky,
                radius = 24,
                tint =
                    colors.surface.copy(
                        alpha = 0.40f
                    )
            )
        }

        XvoxGlassStyle.MINI_CONTROL -> {
            cloudy(
                sky = sky,
                radius = 18,
                tint =
                    colors.cardElevated.copy(
                        alpha = 0.34f
                    )
            )
        }

        XvoxGlassStyle.NAVIGATION -> {
            cloudy(
                sky = sky,
                radius = 24,
                tint =
                    colors.surface.copy(
                        alpha = 0.38f
                    )
            )
        }
    }
}

@Composable
fun Modifier.xvoxNavigationLens(
    lensCenter: Offset,
    lensSize: Size,
    cornerRadius: Float
): Modifier {
    val colors =
        XvoxTheme.colors

    return liquidGlass(
        lensCenter =
            lensCenter,
        lensSize =
            lensSize,
        cornerRadius =
            cornerRadius,
        refraction =
            XvoxGlassLens
                .NavigationRefraction,
        curve =
            XvoxGlassLens
                .NavigationCurve,
        dispersion =
            XvoxGlassLens
                .NavigationDispersion,
        saturation =
            XvoxGlassLens
                .NavigationSaturation,
        contrast =
            XvoxGlassLens
                .NavigationContrast,
        tint =
            colors.cardElevated.copy(
                alpha = 0.18f
            ),
        edge =
            XvoxGlassLens
                .NavigationEdge,
        enabled = true
    )
}
