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

    // Subtle real-lens behaviour.
    // Enough to bend the background without
    // making the navbar look distorted.

    const val NavigationRefraction = 0.08f

    const val NavigationCurve = 0.10f

    // No rainbow/chromatic separation.
    const val NavigationDispersion = 0.0f

    const val NavigationSaturation = 1.0f

    const val NavigationContrast = 1.0f

    // Very subtle glass rim.
    const val NavigationEdge = 0.08f
}

@Composable
fun rememberXvoxSky(): Sky {
    return rememberSky()
}

@Composable
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

    val colors = XvoxTheme.colors

    return when (style) {

        // ----------------------------------------------------
        // HEADER
        // ----------------------------------------------------

        XvoxGlassStyle.HEADER -> {
            cloudy(
                sky = sky,

                // Softer than before.
                radius = 18,

                // More transparent so the artwork/content
                // remains visible behind the glass.
                tint = colors.surface.copy(
                    alpha = 0.30f
                )
            )
        }

        // ----------------------------------------------------
        // HEADER ACTIONS
        // ----------------------------------------------------

        XvoxGlassStyle.HEADER_ACTIONS -> {
            cloudy(
                sky = sky,
                radius = 15,
                tint = colors.card.copy(
                    alpha = 0.22f
                )
            )
        }

        // ----------------------------------------------------
        // MINI PLAYER
        // ----------------------------------------------------

        XvoxGlassStyle.MINI_PLAYER -> {
            cloudy(
                sky = sky,
                radius = 18,
                tint = colors.surface.copy(
                    alpha = 0.32f
                )
            )
        }

        // ----------------------------------------------------
        // MINI PLAYER CONTROL
        // ----------------------------------------------------

        XvoxGlassStyle.MINI_CONTROL -> {
            cloudy(
                sky = sky,
                radius = 14,
                tint = colors.cardElevated.copy(
                    alpha = 0.28f
                )
            )
        }

        // ----------------------------------------------------
        // NAVIGATION
        //
        // Blur stays soft.
        // The actual lens is applied separately through
        // xvoxNavigationLens().
        // ----------------------------------------------------

        XvoxGlassStyle.NAVIGATION -> {
            cloudy(
                sky = sky,

                // Smooth background blur.
                radius = 18,

                // Low tint = background stays visible.
                tint = colors.surface.copy(
                    alpha = 0.30f
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

    val colors = XvoxTheme.colors

    return liquidGlass(
        lensCenter = lensCenter,

        lensSize = lensSize,

        cornerRadius = cornerRadius,

        // ------------------------------------------------
        // REAL LENS CHARACTER
        // ------------------------------------------------

        // Very subtle background bending.
        refraction =
            XvoxGlassLens.NavigationRefraction,

        // Slight optical curvature.
        curve =
            XvoxGlassLens.NavigationCurve,

        // No RGB splitting.
        dispersion =
            XvoxGlassLens.NavigationDispersion,

        // Keep original artwork colours.
        saturation =
            XvoxGlassLens.NavigationSaturation,

        contrast =
            XvoxGlassLens.NavigationContrast,

        // Very subtle glass body.
        // Don't make it opaque.
        tint = colors.cardElevated.copy(
            alpha = 0.12f
        ),

        // Subtle edge highlight.
        edge =
            XvoxGlassLens.NavigationEdge,

        enabled = true
    )
}
