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

    /*
     * Keep refraction LOW.
     *
     * This makes the pill behave like glass,
     * but doesn't make the content behind it
     * look like it's jumping around.
     */
    const val NavigationRefraction = 0.055f

    /*
     * Gentle optical curvature.
     */
    const val NavigationCurve = 0.075f

    /*
     * No rainbow / RGB splitting.
     */
    const val NavigationDispersion = 0.0f

    const val NavigationSaturation = 1.0f

    const val NavigationContrast = 1.0f

    /*
     * Small glass rim.
     */
    const val NavigationEdge = 0.075f
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

        XvoxGlassStyle.HEADER -> {

            cloudy(
                sky = sky,
                radius = 18,
                tint = colors.surface.copy(
                    alpha = 0.30f
                ),
                enabled = true
            )
        }

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

        XvoxGlassStyle.MINI_CONTROL -> {

            cloudy(
                sky = sky,
                radius = 14,
                tint = colors.cardElevated.copy(
                    alpha = 0.26f
                ),
                enabled = true
            )
        }

        XvoxGlassStyle.NAVIGATION -> {

            /*
             * Main navbar glass.
             *
             * Soft enough that the background
             * remains visible.
             */
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
 * Selected navigation pill.
 *
 * IMPORTANT:
 *
 * cloudy()
 *      ↓
 * liquidGlass()
 *
 * Both are applied to the SAME pill.
 *
 * cloudy  = real backdrop blur
 * liquidGlass = optical lens/refraction
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
         * FIRST:
         *
         * Live backdrop blur.
         *
         * Very transparent so things behind
         * the pill remain recognizable.
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
         * SECOND:
         *
         * Actual Cloudy Liquid Glass lens.
         */
        .liquidGlass(
            lensCenter = lensCenter,

            lensSize = lensSize,

            cornerRadius = cornerRadius,

            /*
             * Subtle optical distortion.
             */
            refraction =
                XvoxGlassLens.NavigationRefraction,

            curve =
                XvoxGlassLens.NavigationCurve,

            /*
             * No chromatic aberration.
             */
            dispersion =
                XvoxGlassLens.NavigationDispersion,

            saturation =
                XvoxGlassLens.NavigationSaturation,

            contrast =
                XvoxGlassLens.NavigationContrast,

            /*
             * Almost transparent glass body.
             *
             * This is important:
             * high alpha makes the pill look
             * like a solid grey capsule.
             */
            tint = colors.cardElevated.copy(
                alpha = 0.065f
            ),

            /*
             * Soft optical rim.
             */
            edge =
                XvoxGlassLens.NavigationEdge,

            enabled = true
        )
}
