package com.xvox.music.player.playback

import kotlin.math.*

data class BeatGrid(val beatsMs: List<Long>, val periodMs: Double, val confidence: Double)
data class BeatBlendPlan(val startPositionMs: Long, val overlapMs: Long, val confidence: Double)

/** Conservative onset alignment. Never skips the next intro or forces unrelated tempos to match. */
object BeatAlignment {
    fun plan(durationMs: Long, overlapMs: Long, outgoing: BeatGrid?, incoming: BeatGrid?): BeatBlendPlan? {
        if (outgoing == null || incoming == null || min(outgoing.confidence, incoming.confidence) < .42) return null
        val ratio = outgoing.periodMs / incoming.periodMs
        val mismatch = minOf(abs(1 - ratio), abs(1 - ratio * 2), abs(1 - ratio / 2))
        if (!ratio.isFinite() || mismatch > .045) return null
        val firstBeat = incoming.beatsMs.firstOrNull { it >= 0 } ?: return null
        if (firstBeat > minOf(1200L, overlapMs / 2)) return null
        val normalStart = durationMs - overlapMs
        val maxShift = minOf(500L, overlapMs / 5)
        val start = outgoing.beatsMs.asSequence().map { it - firstBeat }
            .filter { it in normalStart..(normalStart + maxShift) && durationMs - it >= 500 }
            .minOrNull() ?: return null
        return BeatBlendPlan(start, durationMs - start, min(outgoing.confidence, incoming.confidence))
    }

    /** 10-ms energy bins -> tempo + real detected beat onsets. Silence / weak rhythms return null. */
    fun detect(energy: DoubleArray, startMs: Long, hopMs: Long = 10): BeatGrid? {
        if (energy.size < 400) return null
        val envelope = DoubleArray(energy.size) { sqrt(energy[it].coerceAtLeast(0.0)) }
        val novelty = DoubleArray(energy.size)
        var history = 0.0
        for (i in envelope.indices) {
            novelty[i] = max(0.0, envelope[i] - history * 1.06)
            history += (envelope[i] - history) * .10
        }
        val power = novelty.sumOf { it * it }
        if (power < 1e-8) return null
        val minLag = (285 / hopMs).toInt().coerceAtLeast(1)
        val maxLag = (1000 / hopMs).toInt().coerceAtMost(novelty.size / 3)
        var bestLag = minLag
        var bestScore = 0.0
        for (lag in minLag..maxLag) {
            var dot = 0.0; var a = 0.0; var b = 0.0
            for (i in lag until novelty.size) {
                dot += novelty[i] * novelty[i - lag]
                a += novelty[i] * novelty[i]; b += novelty[i - lag] * novelty[i - lag]
            }
            val score = dot / sqrt((a * b).coerceAtLeast(1e-20))
            // Near-equal double-period peaks should not turn a clear pulse train into half-tempo.
            if (score > bestScore + .005) { bestScore = score; bestLag = lag }
        }
        if (bestScore < .42) return null
        val mean = novelty.average()
        val peaks = mutableListOf<Int>()
        for (i in 2 until novelty.size - 2) {
            if (novelty[i] < mean * 1.65 || novelty[i] <= novelty[i - 1] || novelty[i] < novelty[i + 1]) continue
            if (peaks.isEmpty() || i - peaks.last() >= bestLag * .60) peaks += i
            else if (novelty[i] > novelty[peaks.last()]) peaks[peaks.lastIndex] = i
        }
        if (peaks.size < 5) return null
        val periods = peaks.zipWithNext().map { (a, b) -> (b - a).toDouble() }
            .filter { it in bestLag * .70..bestLag * 1.30 }
        if (periods.size < 3) return null
        val period = periods.sorted()[periods.size / 2]
        val consistency = periods.count { abs(it - period) <= period * .12 }.toDouble() / periods.size
        val confidence = min(bestScore, consistency)
        if (confidence < .42) return null
        return BeatGrid(peaks.map { startMs + it * hopMs }, period * hopMs, confidence)
    }
}
