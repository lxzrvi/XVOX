package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/**
 * A small visual layer for the Mini Player only. The full player keeps its own stable bottom
 * travel; this layer never lets the Mini Player overshoot, rotate, or leak above the handoff.
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
    /** Product timing is fixed at 300 ms; the former slider and experimental timing range retired. */
    const val Duration = 300
    const val MinDuration = Duration
    const val MaxDuration = Duration

    const val DefaultStyle = "default"
    const val ScaleStyle = "scale"

    val easing: Easing = CubicBezierEasing(0.22f, 0f, 0f, 1f)

    /** Old Scale variants migrate to Scale; every other retired experimental variant becomes Default. */
    fun normalizedStyle(value: String): String = when {
        value == ScaleStyle || value.startsWith("scale_") -> ScaleStyle
        else -> DefaultStyle
    }

    /** Existing persisted slider values remain readable, but rendering is intentionally fixed. */
    fun durationFor(@Suppress("UNUSED_PARAMETER") value: Int): Int = Duration

    /** Once the Mini Player has fully crossed the bottom edge, Now Playing starts with no pause. */
    fun handoffDelayFor(@Suppress("UNUSED_PARAMETER") durationMillis: Int): Long = 0L

    fun spec(@Suppress("UNUSED_PARAMETER") durationMillis: Int = Duration): AnimationSpec<Float> = tween(
        durationMillis = Duration,
        easing = easing
    )

    /**
     * Default is a pure physical slide. Scale is a visibly distinct but locked compact shrink:
     * it never expands, fades, drifts diagonally, or overshoots the stable bottom handoff.
     */
    fun layer(style: String, fraction: Float): XvoxPlayerTransitionLayer {
        val progress = fraction.coerceIn(0f, 1f)
        return if (normalizedStyle(style) == ScaleStyle) {
            XvoxPlayerTransitionLayer(
                scaleX = 1f - .10f * progress,
                scaleY = 1f - .10f * progress
            )
        } else {
            XvoxPlayerTransitionLayer()
        }
    }
}
