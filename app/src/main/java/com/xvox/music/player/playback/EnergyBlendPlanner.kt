package com.xvox.music.player.playback

import kotlin.math.*

data class EnergyEnvelope(val startMs: Long, val hopMs: Long, val levels: List<Float>) {
    fun at(position: Long): Float {
        val f = ((position - startMs).toDouble() / hopMs).coerceIn(0.0, (levels.size - 1).coerceAtLeast(0).toDouble())
        val i = f.toInt(); val a = levels.getOrElse(i) { 0f }; val b = levels.getOrElse(i + 1) { a }
        return a + (b - a) * (f - i).toFloat()
    }
    companion object {
        fun fromEnergy(values: DoubleArray, startMs: Long): EnergyEnvelope {
            val rms = values.map { sqrt((it.takeIf { v -> v.isFinite() } ?: 0.0).coerceAtLeast(0.0)).toFloat() }
            val sorted = rms.sorted()
            val reference = sorted.getOrElse((sorted.size * .92).toInt().coerceAtMost(sorted.lastIndex)) { 1f }.coerceAtLeast(.001f)
            return EnergyEnvelope(startMs, 10, rms.map { (it / reference).coerceIn(0f, 1f) })
        }
    }
}
data class TrackBlendProfile(val beats: BeatGrid?, val energy: EnergyEnvelope)
data class EnergyBlendPlan(val startPositionMs: Long, val overlapMs: Long, val handoff: Float = .5f, val beatAligned: Boolean = false)

/** Match quiet/high-energy regions locally, then hand the bass from one track to the other. */
object EnergyBlendPlanner {
    fun plan(duration: Long, window: Long, out: TrackBlendProfile?, incoming: TrackBlendProfile?, alignBeats: Boolean): EnergyBlendPlan? {
        if (out == null || incoming == null || window < 500) return null
        val normalStart = duration - window
        val beat = if (alignBeats) BeatAlignment.plan(duration, window, out.beats, incoming.beats) else null
        val starts = (0L..minOf(900L, window / 3) step 30L).map { normalStart + it }.toMutableList()
        beat?.let { starts.add(it.startPositionMs) }
        var best: EnergyBlendPlan? = null; var bestCost = Double.MAX_VALUE
        for (start in starts) {
            val length = duration - start
            if (length < 500) continue
            var handoff = .5f; var quiet = Float.MAX_VALUE
            for (step in 30..70) {
                val t = step / 100f
                val a = out.energy.at(start + (length * t).toLong())
                val b = incoming.energy.at((length * t).toLong())
                val cost = a + b + abs(t - .5f) * .35f
                if (cost < quiet) { quiet = cost; handoff = t }
            }
            val beatAligned = beat != null && abs(beat.startPositionMs - start) < 15
            var collision = 0.0
            repeat(48) { i ->
                val t = i / 47f
                val a = out.energy.at(start + (length * t).toLong())
                val b = incoming.energy.at((length * t).toLong())
                val gains = gains(t, handoff, true)
                collision += (if (beatAligned) abs(a - b) else a * b) * gains.incoming * gains.outgoing
            }
            val cost = collision / 48 + quiet * .16 + (start - normalStart).toDouble() / window * .22 - if (beatAligned) .12 else 0.0
            if (cost < bestCost) { bestCost = cost; best = EnergyBlendPlan(start, length, handoff, beatAligned) }
        }
        return best
    }
    fun gains(progress: Float, handoff: Float, smart: Boolean): CrossfadeGains {
        val p = progress.coerceIn(0f, 1f)
        val h = handoff.coerceIn(.3f, .7f)
        val warped = if (!smart) p else if (p <= h) .5f * p / h else .5f + .5f * (p - h) / (1 - h)
        val t = if (smart) warped * warped * (3 - 2 * warped) else warped
        return CrossfadeMath.gains(t)
    }
    fun bassGains(progress: Float, handoff: Float, strength: Float, master: CrossfadeGains): CrossfadeGains {
        val width = .65f - .47f * strength.coerceIn(0f, 1f)
        val x = ((progress - handoff + width / 2) / width).coerceIn(0f, 1f)
        val incoming = x * x * (3 - 2 * x)
        return CrossfadeGains(((1 - incoming) / master.outgoing.coerceAtLeast(.001f)).coerceIn(0f, 1f),
            (incoming / master.incoming.coerceAtLeast(.001f)).coerceIn(0f, 1f))
    }
}
