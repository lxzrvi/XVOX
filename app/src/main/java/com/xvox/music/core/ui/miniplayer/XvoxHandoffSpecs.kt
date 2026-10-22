package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import kotlin.math.roundToInt

/** A light ease-in lets the outgoing card leave quickly without a hard first frame. */
val XvoxHandoffExitEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.6f)

/** The incoming player still decelerates cleanly into its resting position. */
val XvoxHandoffEnterEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

const val XvoxHandoffExitMs = 160
const val XvoxHandoffEnterMs = 260
private const val XvoxHandoffMinimumMs = 120
private const val XvoxHandoffMaximumMs = 900

// Now Playing opens and closes with one shared curve and full-distance timing.
val XvoxNowPlayingEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
const val XvoxNowPlayingMs = 340

fun xvoxHandoffMs(baseMs: Int, travel: Float): Int =
    (baseMs * travel.coerceIn(0f, 1f))
        .roundToInt()
        .coerceIn(XvoxHandoffMinimumMs, XvoxHandoffMaximumMs)

fun xvoxNowPlayingSpec(ms: Int = XvoxNowPlayingMs): FiniteAnimationSpec<Float> =
    tween(ms, easing = XvoxNowPlayingEasing)

/** Fixed-duration compatibility specs used by callers that do not know their travel distance. */
fun xvoxHandoffExitSpec(durationMillis: Int = XvoxHandoffExitMs): TweenSpec<Float> = tween(
    durationMillis = durationMillis.coerceIn(XvoxHandoffMinimumMs, XvoxHandoffMaximumMs),
    easing = XvoxHandoffExitEasing
)

fun xvoxHandoffEnterSpec(durationMillis: Int = XvoxHandoffEnterMs): TweenSpec<Float> = tween(
    durationMillis = durationMillis.coerceIn(XvoxHandoffMinimumMs, XvoxHandoffMaximumMs),
    easing = XvoxHandoffEnterEasing
)

/**
 * Motion shared by the physical Mini Player <-> Now Playing handoff.
 *
 * Full-distance travel uses the short fixed handoff timings above. Interrupted or reversed
 * handoffs scale that time to the actual segment, while retaining a visible lower bound.
 */
object XvoxHandoffSpecs {
    const val ExitBaseDurationMillis = XvoxHandoffExitMs
    const val EnterBaseDurationMillis = XvoxHandoffEnterMs
    const val MinimumDurationMillis = XvoxHandoffMinimumMs
    const val MaximumDurationMillis = XvoxHandoffMaximumMs

    val ExitEasing get() = XvoxHandoffExitEasing
    val EnterEasing get() = XvoxHandoffEnterEasing

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
    ): TweenSpec<Float> = xvoxHandoffExitSpec(
        distanceProportionalDuration(
            distancePx = distancePx,
            fullDistancePx = fullDistancePx,
            baseDurationMillis = ExitBaseDurationMillis
        )
    )

    fun enterSpec(
        distancePx: Float,
        fullDistancePx: Float
    ): TweenSpec<Float> = xvoxHandoffEnterSpec(
        distanceProportionalDuration(
            distancePx = distancePx,
            fullDistancePx = fullDistancePx,
            baseDurationMillis = EnterBaseDurationMillis
        )
    )
}
