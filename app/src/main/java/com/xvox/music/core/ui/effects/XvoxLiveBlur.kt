package com.xvox.music.core.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.xvox.music.core.design.theme.LocalXvoxExperimentalAppearance
import com.xvox.music.core.design.theme.XvoxExperimentalAppearance

/**
 * The single, live Prismal backdrop captured at the application root. Eligible glass controls
 * sample this real rendered layer; no card creates a fake/static bitmap blur of its own.
 */
val LocalXvoxPrismalBackdrop = staticCompositionLocalOf<PrismalBackdrop?> { null }

/**
 * Genuine PrismalAGSL liquid glass for app chrome, sheets, player surfaces, and intentionally
 * translucent controls. The backdrop is recorded from the real XVOX background before glass is
 * drawn, then Prismal's Android 13+ AGSL chain applies blur, vibrancy, refraction, and specular
 * edges to the surface above it.
 */
@Composable
fun Modifier.xvoxLiveBackdropBlur(
    shape: Shape = RectangleShape,
    radius: Int = 18,
    /** Default UI opts in only where an intentionally translucent chrome surface exists. */
    applyInDefault: Boolean = false
): Modifier {
    val backdrop = LocalXvoxPrismalBackdrop.current
    val appearance = LocalXvoxExperimentalAppearance.current
    val shouldApply = backdrop != null && (
        appearance == XvoxExperimentalAppearance.BLUR || applyInDefault
    )
    if (!shouldApply) return this

    val glassRadius = radius.coerceIn(8, 36).dp
    // Prismal's rounded shapes expose the edge geometry needed by its AGSL lens. We retain the
    // caller's clip separately, so existing circles and custom rounded surfaces keep their exact
    // visual containment while the liquid layer receives a physically meaningful shape.
    val prismalShape: Shape = when (shape) {
        RectangleShape -> PrismalRoundedRectangle(0.dp)
        androidx.compose.foundation.shape.CircleShape -> PrismalCapsule()
        else -> PrismalRoundedRectangle(glassRadius)
    }

    return clip(shape).drawPrismalGlass(
        backdrop = backdrop,
        shape = { prismalShape },
        effects = {
            applyPrismalGlassEffects(
                density = this,
                adaptiveLuminance = false,
                luminance = .5f,
                blurRadiusPx = glassRadius.toPx(),
                refractionHeightPx = (glassRadius * .72f).toPx(),
                refractionAmountPx = (glassRadius * 1.5f).toPx(),
                saturation = 1.35f,
                useVibrancy = true
            )
        }
    )
}
