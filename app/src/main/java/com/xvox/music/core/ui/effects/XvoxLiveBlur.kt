package com.xvox.music.core.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.skydoves.cloudy.Sky
import com.skydoves.cloudy.cloudy
import com.xvox.music.core.design.theme.LocalXvoxExperimentalAppearance
import com.xvox.music.core.design.theme.XvoxExperimentalAppearance

/**
 * One shared Cloudy backdrop source is installed at the app root.  Surfaces opt into this modifier
 * rather than painting a translucent tint that merely imitates glass.  Cloudy samples the actual
 * content behind each surface and follows nested scrolling while it is moving.
 */
val LocalXvoxBlurSky = staticCompositionLocalOf<Sky?> { null }

/**
 * Genuine live backdrop blur for the Blur appearance. API 31+ uses Cloudy's GPU path; older
 * devices deliberately use Cloudy's cheap scrim fallback rather than a per-card CPU blur that
 * would make library flings jank. The material alpha remains a readable overlay, not the blur.
 */
@Composable
fun Modifier.xvoxLiveBackdropBlur(
    shape: Shape = RectangleShape,
    radius: Int = 18
): Modifier {
    val sky = LocalXvoxBlurSky.current
    val enabled = LocalXvoxExperimentalAppearance.current == XvoxExperimentalAppearance.BLUR && sky != null
    return if (enabled) {
        cloudy(
            sky = sky!!,
            radius = radius,
            tint = Color.Transparent,
            cpuBlurEnabled = false,
            shape = shape
        )
    } else {
        this
    }
}
