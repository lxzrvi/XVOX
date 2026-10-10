package com.xvox.music.core.ui.effects

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
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
 * Genuine live backdrop blur for Blur UI and explicitly translucent Default chrome. API 31+ uses
 * Cloudy's GPU path; older devices deliberately use Cloudy's cheap scrim fallback rather than a
 * per-card CPU blur that would make library flings jank. The material alpha remains readable.
 */
@Composable
fun Modifier.xvoxLiveBackdropBlur(
    shape: Shape = RectangleShape,
    radius: Int = 18,
    /** Default UI requests Cloudy only for natural translucent chrome, never as a style rewrite. */
    applyInDefault: Boolean = false
): Modifier {
    val sky = LocalXvoxBlurSky.current
    val appearance = LocalXvoxExperimentalAppearance.current
    val hostView = LocalView.current
    // Cloudy's real RenderEffect capture is stable on API 31+ hardware-accelerated Compose
    // views. Never ask its GPU recorder to emulate itself on an older/software canvas—the source
    // readiness gate at the root leaves these frames as ordinary material instead of crashing.
    val canRenderLiveCloudy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hostView.isHardwareAccelerated
    // Cloudy is intentionally opt-in at the actual chrome/sheet call site. In particular, a
    // translucent palette must not turn every repeated song/playlist card into a live recorder
    // target: that was both visually noisy and the source of Home instability while Blur was on.
    // Eligible surfaces still use Cloudy's real moving capture in both modes; this is not a
    // synthetic fallback or static snapshot.
    val explicitlyEligible = applyInDefault
    val enabled = sky != null && canRenderLiveCloudy && explicitlyEligible && (
        appearance == XvoxExperimentalAppearance.BLUR ||
            appearance == XvoxExperimentalAppearance.DEFAULT
    )
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
