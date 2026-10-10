package com.xvox.music.core.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.LocalXvoxExperimentalAppearance
import com.xvox.music.core.design.theme.XvoxExperimentalAppearance
import com.xvox.music.core.design.theme.XvoxTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * The shared genuine Haze source installed at the application root. Every eligible glass surface
 * reads from this one state rather than allocating a per-card recorder or painting a synthetic
 * blur snapshot.
 */
val LocalXvoxHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Genuine Chris Banes Haze backdrop blur for chrome, sheets, player surfaces, and other explicit
 * glass targets. Haze owns platform capability handling itself: supported devices render its live
 * backdrop blur; unsupported devices receive Haze's own material fallback rather than an XVOX
 * static or fake blur implementation.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.xvoxLiveBackdropBlur(
    shape: Shape = RectangleShape,
    radius: Int = 18,
    /** Default UI opts in only where an intentionally translucent chrome surface exists. */
    applyInDefault: Boolean = false
): Modifier {
    val hazeState = LocalXvoxHazeState.current
    val appearance = LocalXvoxExperimentalAppearance.current
    val colors = XvoxTheme.colors
    val shouldApply = hazeState != null && (
        appearance == XvoxExperimentalAppearance.BLUR || applyInDefault
    )

    return if (shouldApply) {
        // HazeMaterials supplies the genuine material/tint treatment while the modifier samples
        // the live Haze source. Clipping sits outside the effect so no rectangular blur bleeds
        // beyond a pill, circle, or rounded sheet edge.
        clip(shape).hazeEffect(
            state = hazeState,
            style = HazeMaterials.ultraThin(containerColor = colors.surface.copy(alpha = 1f))
        ) {
            blurRadius = radius.coerceIn(8, 36).dp
            noiseFactor = .08f
        }
    } else {
        this
    }
}
