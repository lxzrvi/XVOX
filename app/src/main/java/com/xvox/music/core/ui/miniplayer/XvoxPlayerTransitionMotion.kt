package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/** A named test variant shown in the Mini Player's long-press Settings surface. */
data class XvoxPlayerTransitionPreset(
    val id: String,
    val family: String,
    val name: String,
    val description: String
)

/**
 * Small visual offsets layered on top of the physical Mini Player / Now Playing handoff. The
 * physical surfaces always clear one another; these are deliberately conservative so every one
 * of the 30 experiments remains usable with screen readers, short displays, and fast swipes.
 */
data class XvoxPlayerTransitionLayer(
    val alpha: Float = 1f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val xFraction: Float = 0f,
    val yFraction: Float = 0f,
    val rotationZ: Float = 0f,
    val rotationY: Float = 0f
)

object XvoxPlayerTransitionMotion {

    /** Faster shared default than the former 320ms handoff. */
    const val Duration = 220
    const val MinDuration = 120
    const val MaxDuration = 420

    val easing: Easing =
        CubicBezierEasing(
            0.22f,
            0.0f,
            0.0f,
            1.0f
        )

    /** Six readable families × five curated variants = 30 testable handoffs. */
    val presets: List<XvoxPlayerTransitionPreset> = listOf(
        XvoxPlayerTransitionPreset("glide_1", "Glide", "Straight", "Quiet straight glide"),
        XvoxPlayerTransitionPreset("glide_2", "Glide", "Drift", "Gentle side drift"),
        XvoxPlayerTransitionPreset("glide_3", "Glide", "Arc", "Short curved drift"),
        XvoxPlayerTransitionPreset("glide_4", "Glide", "Rush", "Crisp forward glide"),
        XvoxPlayerTransitionPreset("glide_5", "Glide", "Soft", "Softened glide"),

        XvoxPlayerTransitionPreset("scale_1", "Scale", "Expand", "Subtle expand"),
        XvoxPlayerTransitionPreset("scale_2", "Scale", "Lens", "Lens-like focus"),
        XvoxPlayerTransitionPreset("scale_3", "Scale", "Snap", "Quick compact scale"),
        XvoxPlayerTransitionPreset("scale_4", "Scale", "Breathe", "Gentle breathing scale"),
        XvoxPlayerTransitionPreset("scale_5", "Scale", "Fold", "Horizontal fold"),

        XvoxPlayerTransitionPreset("reveal_1", "Reveal", "Curtain", "Side curtain reveal"),
        XvoxPlayerTransitionPreset("reveal_2", "Reveal", "Wipe", "Light diagonal wipe"),
        XvoxPlayerTransitionPreset("reveal_3", "Reveal", "Dawn", "Upward dawn reveal"),
        XvoxPlayerTransitionPreset("reveal_4", "Reveal", "Focus", "Focused fade reveal"),
        XvoxPlayerTransitionPreset("reveal_5", "Reveal", "Veil", "Soft veil reveal"),

        XvoxPlayerTransitionPreset("card_1", "Card", "Tilt", "Small card tilt"),
        XvoxPlayerTransitionPreset("card_2", "Card", "Stack", "Stacked-card lift"),
        XvoxPlayerTransitionPreset("card_3", "Card", "Lift", "Lift and settle"),
        XvoxPlayerTransitionPreset("card_4", "Card", "Drop", "Counter-drop card"),
        XvoxPlayerTransitionPreset("card_5", "Card", "Corner", "Corner pivot"),

        XvoxPlayerTransitionPreset("orbit_1", "Orbit", "Flip", "Soft Y flip"),
        XvoxPlayerTransitionPreset("orbit_2", "Orbit", "Turn", "Quarter turn"),
        XvoxPlayerTransitionPreset("orbit_3", "Orbit", "Pivot", "Pivot rotation"),
        XvoxPlayerTransitionPreset("orbit_4", "Orbit", "Sway", "Swaying arc"),
        XvoxPlayerTransitionPreset("orbit_5", "Orbit", "Sweep", "Wide soft sweep"),

        XvoxPlayerTransitionPreset("prism_1", "Prism", "Bloom", "Prismatic bloom"),
        XvoxPlayerTransitionPreset("prism_2", "Prism", "Glass", "Glass shimmer"),
        XvoxPlayerTransitionPreset("prism_3", "Prism", "Aurora", "Aurora drift"),
        XvoxPlayerTransitionPreset("prism_4", "Prism", "Pulse", "Single prism pulse"),
        XvoxPlayerTransitionPreset("prism_5", "Prism", "Facet", "Faceted settle")
    )

    val families: List<String> = presets.map(XvoxPlayerTransitionPreset::family).distinct()

    fun presetsInFamily(family: String): List<XvoxPlayerTransitionPreset> =
        presets.filter { it.family == family }

    fun normalizedStyle(value: String): String =
        value.takeIf { candidate -> presets.any { it.id == candidate } } ?: "glide_1"

    fun durationFor(value: Int): Int = value.coerceIn(MinDuration, MaxDuration)

    fun handoffDelayFor(durationMillis: Int): Long =
        (durationFor(durationMillis) / 12).toLong().coerceIn(12L, 30L)

    fun spec(durationMillis: Int = Duration): AnimationSpec<Float> = tween(
        durationMillis = durationFor(durationMillis),
        easing = easing
    )

    /**
     * A zero fraction is the settled surface; one is the offscreen endpoint. The physical slide
     * still owns the primary route, so these visual values never make a card linger onscreen.
     */
    fun layer(style: String, fraction: Float): XvoxPlayerTransitionLayer {
        val p = fraction.coerceIn(0f, 1f)
        return when (normalizedStyle(style)) {
            "glide_1" -> XvoxPlayerTransitionLayer(alpha = 1f - .05f * p)
            "glide_2" -> XvoxPlayerTransitionLayer(alpha = 1f - .10f * p, xFraction = .045f * p)
            "glide_3" -> XvoxPlayerTransitionLayer(alpha = 1f - .08f * p, xFraction = -.06f * p, yFraction = -.025f * p)
            "glide_4" -> XvoxPlayerTransitionLayer(alpha = 1f - .18f * p, xFraction = .025f * p, scaleX = 1f - .025f * p, scaleY = 1f - .025f * p)
            "glide_5" -> XvoxPlayerTransitionLayer(alpha = 1f - .14f * p, xFraction = -.025f * p, scaleX = 1f - .045f * p, scaleY = 1f - .045f * p)

            "scale_1" -> XvoxPlayerTransitionLayer(alpha = 1f - .10f * p, scaleX = 1f - .10f * p, scaleY = 1f - .10f * p)
            "scale_2" -> XvoxPlayerTransitionLayer(alpha = 1f - .15f * p, scaleX = 1f - .15f * p, scaleY = 1f - .08f * p)
            "scale_3" -> XvoxPlayerTransitionLayer(alpha = 1f - .20f * p, scaleX = 1f - .075f * p, scaleY = 1f - .12f * p)
            "scale_4" -> XvoxPlayerTransitionLayer(alpha = 1f - .12f * p, scaleX = 1f - .055f * p, scaleY = 1f - .055f * p, yFraction = -.018f * p)
            "scale_5" -> XvoxPlayerTransitionLayer(alpha = 1f - .18f * p, scaleX = 1f - .19f * p, scaleY = 1f - .035f * p)

            "reveal_1" -> XvoxPlayerTransitionLayer(alpha = 1f - .34f * p, xFraction = -.10f * p, scaleX = 1f - .035f * p)
            "reveal_2" -> XvoxPlayerTransitionLayer(alpha = 1f - .30f * p, xFraction = .075f * p, yFraction = -.04f * p)
            "reveal_3" -> XvoxPlayerTransitionLayer(alpha = 1f - .26f * p, yFraction = -.09f * p, scaleX = 1f - .04f * p, scaleY = 1f - .04f * p)
            "reveal_4" -> XvoxPlayerTransitionLayer(alpha = 1f - .42f * p, scaleX = 1f - .11f * p, scaleY = 1f - .11f * p)
            "reveal_5" -> XvoxPlayerTransitionLayer(alpha = 1f - .38f * p, xFraction = -.035f * p, scaleX = 1f - .08f * p, scaleY = 1f - .05f * p)

            "card_1" -> XvoxPlayerTransitionLayer(alpha = 1f - .12f * p, scaleX = 1f - .06f * p, scaleY = 1f - .06f * p, rotationZ = 4f * p)
            "card_2" -> XvoxPlayerTransitionLayer(alpha = 1f - .18f * p, xFraction = -.03f * p, yFraction = -.055f * p, scaleX = 1f - .09f * p, scaleY = 1f - .09f * p, rotationZ = -3f * p)
            "card_3" -> XvoxPlayerTransitionLayer(alpha = 1f - .13f * p, yFraction = -.09f * p, scaleX = 1f - .055f * p, scaleY = 1f - .055f * p)
            "card_4" -> XvoxPlayerTransitionLayer(alpha = 1f - .14f * p, yFraction = .04f * p, scaleX = 1f - .08f * p, scaleY = 1f - .08f * p, rotationZ = -4f * p)
            "card_5" -> XvoxPlayerTransitionLayer(alpha = 1f - .18f * p, xFraction = .06f * p, scaleX = 1f - .10f * p, scaleY = 1f - .065f * p, rotationZ = 5f * p)

            "orbit_1" -> XvoxPlayerTransitionLayer(alpha = 1f - .18f * p, scaleX = 1f - .055f * p, scaleY = 1f - .055f * p, rotationY = 13f * p)
            "orbit_2" -> XvoxPlayerTransitionLayer(alpha = 1f - .15f * p, xFraction = -.045f * p, rotationY = -17f * p)
            "orbit_3" -> XvoxPlayerTransitionLayer(alpha = 1f - .18f * p, xFraction = .04f * p, scaleX = 1f - .07f * p, scaleY = 1f - .07f * p, rotationZ = -5f * p)
            "orbit_4" -> XvoxPlayerTransitionLayer(alpha = 1f - .16f * p, xFraction = .075f * p, yFraction = -.025f * p, rotationZ = 3f * p)
            "orbit_5" -> XvoxPlayerTransitionLayer(alpha = 1f - .22f * p, xFraction = -.09f * p, scaleX = 1f - .075f * p, scaleY = 1f - .075f * p, rotationY = 10f * p)

            "prism_1" -> XvoxPlayerTransitionLayer(alpha = 1f - .26f * p, scaleX = 1f - .13f * p, scaleY = 1f - .13f * p, rotationZ = 2.5f * p)
            "prism_2" -> XvoxPlayerTransitionLayer(alpha = 1f - .28f * p, xFraction = .035f * p, scaleX = 1f - .095f * p, scaleY = 1f - .06f * p, rotationY = -9f * p)
            "prism_3" -> XvoxPlayerTransitionLayer(alpha = 1f - .25f * p, xFraction = -.055f * p, yFraction = -.06f * p, scaleX = 1f - .08f * p, scaleY = 1f - .08f * p, rotationZ = -3.5f * p)
            "prism_4" -> XvoxPlayerTransitionLayer(alpha = 1f - .34f * p, scaleX = 1f - .16f * p, scaleY = 1f - .10f * p)
            "prism_5" -> XvoxPlayerTransitionLayer(alpha = 1f - .30f * p, xFraction = .06f * p, scaleX = 1f - .14f * p, scaleY = 1f - .075f * p, rotationZ = 4.5f * p, rotationY = 8f * p)
            else -> XvoxPlayerTransitionLayer()
        }
    }
}
