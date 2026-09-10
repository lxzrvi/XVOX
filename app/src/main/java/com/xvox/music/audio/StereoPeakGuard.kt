package com.xvox.music.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/** Stereo-linked look-ahead limiting. Only real peaks reduce gain; no whole-preset normalization. */
class StereoPeakGuard {
    var latencyFrames = 1
        private set
    var left = 0.0
        private set
    var right = 0.0
        private set
    private var l = DoubleArray(1); private var r = DoubleArray(1)
    private var peaks = DoubleArray(3); private var frames = LongArray(3)
    private var cursor = 0; private var head = 0; private var tail = 0; private var frame = 0L
    private var gain = 1.0; private var attack = 1.0; private var release = .001
    fun configure(rate: Int) {
        latencyFrames = (rate * .0025).toInt().coerceAtLeast(1)
        l = DoubleArray(latencyFrames); r = DoubleArray(latencyFrames)
        peaks = DoubleArray(latencyFrames + 2); frames = LongArray(peaks.size)
        attack = 1 - exp(-1.0 / (rate * .0004))
        release = 1 - exp(-1.0 / (rate * .120))
        reset()
    }
    fun reset() { l.fill(0.0); r.fill(0.0); cursor = 0; head = 0; tail = 0; frame = 0; gain = 1.0 }
    fun process(inputL: Double, inputR: Double) {
        val oldL = l[cursor]; val oldR = r[cursor]
        l[cursor] = inputL; r[cursor] = inputR; cursor = (cursor + 1) % l.size
        val peak = max(abs(inputL), abs(inputR))
        // Expire before inserting: the circular deque intentionally reserves one empty slot.
        while (head != tail && frames[head] < frame - latencyFrames) head = (head + 1) % peaks.size
        while (head != tail) {
            val last = (tail - 1 + peaks.size) % peaks.size
            if (peaks[last] > peak) break
            tail = last
        }
        peaks[tail] = peak; frames[tail] = frame; tail = (tail + 1) % peaks.size
        val maxPeak = if (head != tail) peaks[head] else 0.0
        // Protection only starts at the very top of the range (≈0.98), so boosting EQ bands
        // raises them without quietly turning the whole track down. Only real overshoot is caught.
        val desired = if (maxPeak > .98) .98 / maxPeak else 1.0
        gain += (desired - gain) * if (desired < gain) attack else release
        left = (oldL * gain).coerceIn(-.98, .98)
        right = (oldR * gain).coerceIn(-.98, .98)
        frame++
    }
}
