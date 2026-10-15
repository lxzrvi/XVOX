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
    /** Established default timing; retained legacy values normalize to this behavior. */
    const val Duration = 300
    const val MinDuration = 120
    const val MaxDuration = 900

    const val DefaultStyle = "default"
    const val ScaleStyle = "scale"

    /**
     * The card gets enough real travel time to remain visibly behind the navbar during a fast
     * downward handoff. The next surface still releases on this exact terminal frame.
     */
    const val HandoffDuration = 320
    /** The full player uses one uniform 300 ms physical slide in both directions. */
    const val NowPlayingDuration = 300

    // Both surfaces use one continuous curve rather than a mechanical linear descent. The
    // terminal callback still happens on the very last frame, so easing introduces no gap.
    val easing: Easing = CubicBezierEasing(0.22f, 0f, 0f, 1f)

    /**
     * Compatibility helper for older persisted motion values. The active Mini Player UI exposes
     * only Default and Scale, both of which use the shipped .55 curve without overshoot.
     */
    fun easingFor(control: Float): Easing {
        val amount = control.coerceIn(0f, 1f)
        // .55 is the shipped curve exactly (.22, 0, 0, 1); legacy values simply map around it.
        val softer = ((.55f - amount) / .55f).coerceIn(0f, 1f)
        val quicker = ((amount - .55f) / .45f).coerceIn(0f, 1f)
        return CubicBezierEasing(
            .22f + .20f * softer - .16f * quicker,
            0f,
            .20f * softer,
            1f
        )
    }

    /** Old Scale variants migrate to Scale; every other retired experimental variant becomes Default. */
    fun normalizedStyle(value: String): String = when {
        value == ScaleStyle || value.startsWith("scale_") -> ScaleStyle
        else -> DefaultStyle
    }

    /** Kept for older callers while the live UI uses the fixed surface timing directly. */
    fun durationFor(value: Int): Int = value.coerceIn(MinDuration, MaxDuration)

    /** Legacy callers retain the no-dwell handoff; the active Mini Player has no delay control. */
    fun handoffDelayFor(@Suppress("UNUSED_PARAMETER") durationMillis: Int): Long = 0L

    fun spec(durationMillis: Int = Duration, easeControl: Float = .55f): AnimationSpec<Float> = tween(
        durationMillis = durationMillis.coerceIn(MinDuration, MaxDuration),
        easing = easingFor(easeControl)
    )

    fun handoffSpec(
        durationMillis: Int = HandoffDuration,
        easeControl: Float = .55f
    ): AnimationSpec<Float> = tween(
        durationMillis = durationMillis.coerceIn(120, 900),
        easing = easingFor(easeControl)
    )

    fun nowPlayingSpec(
        durationMillis: Int = NowPlayingDuration,
        easeControl: Float = .55f
    ): AnimationSpec<Float> = tween(
        durationMillis = durationMillis.coerceIn(120, 900),
        easing = easingFor(easeControl)
    )

    /**
     * Default is a pure physical slide. Scale shrinks toward the physical Mini Player handoff:
     * no expansion, diagonal drift, fade, or overshoot is permitted at either endpoint.
     */
    fun layer(style: String, fraction: Float): XvoxPlayerTransitionLayer {
        val progress = fraction.coerceIn(0f, 1f)
        return if (normalizedStyle(style) == ScaleStyle) {
            XvoxPlayerTransitionLayer(
                scaleX = 1f - .16f * progress,
                scaleY = 1f - .16f * progress
            )
        } else {
            XvoxPlayerTransitionLayer()
        }
    }

    /** Full player stays exactly 1f at rest and contracts only while travelling down to Mini. */
    fun nowPlayingScale(style: String, fraction: Float): Float =
        if (normalizedStyle(style) == ScaleStyle) 1f - .16f * fraction.coerceIn(0f, 1f) else 1f
}
