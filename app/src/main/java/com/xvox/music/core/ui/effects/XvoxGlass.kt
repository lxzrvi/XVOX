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

/*
 * ============================================================
 * XVOX GLASS LENS
 * ============================================================
 *
 * Kept intentionally subtle.
 *
 * Strong refraction / dispersion can make small UI elements
 * feel wobbly or delayed, especially while dragging.
 */
object XvoxGlassLens {

    const val NavigationRefraction = 0.055f
    const val NavigationCurve = 0.075f
    const val NavigationDispersion = 0.0f

    const val NavigationSaturation = 1.0f
    const val NavigationContrast = 1.0f

    const val NavigationEdge = 0.075f
}


/*
 * ============================================================
 * SHARED SKY
 * ============================================================
 *
 * One Sky instance is shared by the content and all glass
 * overlays.
 */
@Composable
fun rememberXvoxSky(): Sky {
    return rememberSky()
}


/*
 * ============================================================
 * GLASS SOURCE
 * ============================================================
 *
 * Apply this to the background/content that should be visible
 * through the glass.
 */
@Composable
fun Modifier.xvoxGlassSource(
    sky: Sky
): Modifier {
    return this.sky(sky)
}


/*
 * ============================================================
 * XVOX BACKDROP GLASS
 * ============================================================
 */
@Composable
fun Modifier.xvoxGlass(
    sky: Sky,
    style: XvoxGlassStyle
): Modifier {

    val colors = XvoxTheme.colors

    return when (style) {

        /*
         * ----------------------------------------------------
         * HOME HEADER
         * ----------------------------------------------------
         *
         * Light blur so scrolling content remains visible.
         * Avoid heavy tint because it makes the header feel
         * like a solid panel.
         */
        XvoxGlassStyle.HEADER -> {
            cloudy(
                sky = sky,
                radius = 12,
                tint = colors.surface.copy(
                    alpha = 0.18f
                ),
                enabled = true
            )
        }


        /*
         * ----------------------------------------------------
         * HEADER ACTIONS
         * ----------------------------------------------------
         */
        XvoxGlassStyle.HEADER_ACTIONS -> {
            cloudy(
                sky = sky,
                radius = 15,
                tint = colors.card.copy(
                    alpha = 0.22f
                ),
                enabled = true
            )
        }


        /*
         * ----------------------------------------------------
         * MINI PLAYER
         * ----------------------------------------------------
         */
        XvoxGlassStyle.MINI_PLAYER -> {
            cloudy(
                sky = sky,
                radius = 18,
                tint = colors.surface.copy(
                    alpha = 0.32f
                ),
                enabled = true
            )
        }


        /*
         * ----------------------------------------------------
         * MINI PLAYER CONTROL
         * ----------------------------------------------------
         */
        XvoxGlassStyle.MINI_CONTROL -> {
            cloudy(
                sky = sky,
                radius = 14,
                tint = colors.cardElevated.copy(
                    alpha = 0.28f
                ),
                enabled = true
            )
        }


        /*
         * ----------------------------------------------------
         * BOTTOM NAVIGATION
         * ----------------------------------------------------
         *
         * The selected pill gets the actual Liquid Glass
         * lens separately below.
         */
        XvoxGlassStyle.NAVIGATION -> {
            cloudy(
                sky = sky,
                radius = 16,
                tint = colors.surface.copy(
                    alpha = 0.24f
                ),
                enabled = true
            )
        }
    }
}


/*
 * ============================================================
 * XVOX NAVIGATION LIQUID GLASS
 * ============================================================
 *
 * This is applied to the selected navigation pill.
 *
 * Backdrop blur + lens refraction gives the pill the
 * transparent "glass over content" appearance.
 */
@Composable
fun Modifier.xvoxNavigationLens(
    sky: Sky,
    lensCenter: Offset,
    lensSize: Size,
    cornerRadius: Float
): Modifier {

    val colors = XvoxTheme.colors

    return this

        /*
         * First blur the content behind the lens.
         */
        .cloudy(
            sky = sky,
            radius = 16,
            tint = colors.cardElevated.copy(
                alpha = 0.12f
            ),
            enabled = true
        )

        /*
         * Then apply the subtle optical lens.
         */
        .liquidGlass(
            lensCenter = lensCenter,
            lensSize = lensSize,
            cornerRadius = cornerRadius,

            refraction = XvoxGlassLens.NavigationRefraction,
            curve = XvoxGlassLens.NavigationCurve,
            dispersion = XvoxGlassLens.NavigationDispersion,

            saturation = XvoxGlassLens.NavigationSaturation,
            contrast = XvoxGlassLens.NavigationContrast,

            tint = colors.cardElevated.copy(
                alpha = 0.065f
            ),

            edge = XvoxGlassLens.NavigationEdge,

            enabled = true
        )
}
