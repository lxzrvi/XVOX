package com.xvox.music.player.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class CrossfadeGains(val outgoing: Float, val incoming: Float)

object CrossfadeMath {
    /** Constant power for uncorrelated tracks; master volume is not dipped between them. */
    fun gains(progress: Float): CrossfadeGains {
        val t = progress.coerceIn(0f, 1f)
        return CrossfadeGains(cos(t * PI / 2).toFloat(), sin(t * PI / 2).toFloat())
    }
    fun windowMs(requestedSeconds: Int, durationMs: Long, nextDurationMs: Long = Long.MAX_VALUE): Long {
        if (durationMs < 600 || nextDurationMs < 600) return 0
        return minOf(requestedSeconds.coerceIn(1, 12) * 1000L, durationMs / 2, nextDurationMs / 2)
    }
    fun progress(positionMs: Long, startMs: Long, windowMs: Long): Float =
        if (windowMs <= 0) 1f else ((positionMs - startMs).toDouble() / windowMs).toFloat().coerceIn(0f, 1f)
}
