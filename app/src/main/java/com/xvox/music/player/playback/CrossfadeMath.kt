package com.xvox.music.player.playback

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

data class CrossfadeGains(val outgoing: Float, val incoming: Float)

/**
 * Gain laws shared by the playback engine and its preview. Curve selection changes timing, not
 * the safety envelope: every non-linear style remains bounded, monotonic, and reaches exact
 * endpoints, while the default retains the original equal-power pairing.
 */
object CrossfadeMath {
    const val CURVE_EQUAL_POWER = 0
    const val CURVE_LATE = 1
    const val CURVE_SOFT = 2

    fun normalizeCurveStyle(style: Int): Int = style.coerceIn(CURVE_EQUAL_POWER, CURVE_SOFT)

    /** Maps raw transition progress to the selected curve's perceptual timing. */
    fun shape(progress: Float, curveStyle: Int = CURVE_EQUAL_POWER): Float {
        val p = progress.coerceIn(0f, 1f)
        return when (normalizeCurveStyle(curveStyle)) {
            // Holds the outgoing track a little longer, then completes a more decisive hand-off.
            CURVE_LATE -> p * p
            // A gentle zero-slope start/end that keeps resume and quiet intros unhurried.
            CURVE_SOFT -> p * p * (3f - 2f * p)
            else -> p
        }
    }

    /** Equal-power gains after [shape] has applied the chosen timing family. */
    fun gains(progress: Float, curveStyle: Int = CURVE_EQUAL_POWER): CrossfadeGains {
        val p = shape(progress, curveStyle)
        val angle = p * (PI / 2.0)
        return CrossfadeGains(cos(angle).toFloat(), sin(angle).toFloat())
    }

    fun progress(positionMs: Long, startMs: Long, windowMs: Long): Float =
        if (windowMs <= 0L) 1f else ((positionMs - startMs).toDouble() / windowMs).toFloat().coerceIn(0f, 1f)

    fun windowMs(requestedSeconds: Int, durationMs: Long, nextDurationMs: Long = Long.MAX_VALUE): Long {
        // Preserve the established short-track guard and service-side 12 second maximum; curve
        // selection changes only the gain law, never when a deck is eligible to overlap.
        if (durationMs < 600L || nextDurationMs < 600L) return 0L
        return minOf(requestedSeconds.coerceIn(1, 12) * 1000L, durationMs / 2, nextDurationMs / 2)
    }
}
