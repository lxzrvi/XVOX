package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import kotlin.math.roundToInt

/**
 * Motion shared by the physical Mini Player <-> Now Playing handoff.
 *
 * The outgoing surface starts deliberately gently and gathers speed as it leaves, while the
 * incoming surface reaches its resting position quickly and eases into it. Both legs retain their
 * established full-distance timings, then shorten proportionally when a reversal resumes from an
 * already-travelled position. A lower bound keeps interrupted handoffs perceptible instead of
 * turning into a one-frame jump.
 */
object XvoxHandoffSpecs {
    const val ExitBaseDurationMillis = 360
    const val EnterBaseDurationMillis = 320
    const val MinimumDurationMillis = 120
    const val MaximumDurationMillis = 900

    /** The leaving surface accelerates toward the bottom boundary. */
    val ExitEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /** The arriving surface decelerates cleanly into its settled position. */
    val EnterEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    /**
     * Scales a full-distance timing by the untravelled/traversed segment and applies the shared
     * interruption bounds. `fullDistancePx` is guarded so an early pre-layout gesture remains
     * deterministic rather than producing an invalid duration.
     */
    fun distanceProportionalDuration(
        distancePx: Float,
        fullDistancePx: Float,
        baseDurationMillis: Int
    ): Int {
        val safeDistance = distancePx.coerceAtLeast(0f)
        val safeFullDistance = fullDistancePx.coerceAtLeast(1f)
        val fraction = safeDistance / safeFullDistance
        return (baseDurationMillis * fraction)
            .roundToInt()
            .coerceIn(MinimumDurationMillis, MaximumDurationMillis)
    }

    fun exitSpec(
        distancePx: Float,
        fullDistancePx: Float
    ): TweenSpec<Float> = tween(
        durationMillis = distanceProportionalDuration(
            distancePx = distancePx,
            fullDistancePx = fullDistancePx,
            baseDurationMillis = ExitBaseDurationMillis
        ),
        easing = ExitEasing
    )

    fun enterSpec(
        distancePx: Float,
        fullDistancePx: Float
    ): TweenSpec<Float> = tween(
        durationMillis = distanceProportionalDuration(
            distancePx = distancePx,
            fullDistancePx = fullDistancePx,
            baseDurationMillis = EnterBaseDurationMillis
        ),
        easing = EnterEasing
    )
}
