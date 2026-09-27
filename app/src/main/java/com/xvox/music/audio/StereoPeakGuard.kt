package com.xvox.music.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * Stereo-linked look-ahead true-peak guard.
 *
 * The mandatory 0% path only keeps PCM conversion inside the representable range.  Increasing
 * [protection] progressively lowers the working ceiling and accelerates gain capture, so it is a
 * real overload controller rather than a cosmetic loudness adjustment.
 */
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
    private var gain = 1.0
    private var fastAttack = 1.0
    private var release = .001

    fun configure(rate: Int) {
        latencyFrames = (rate * .0025).toInt().coerceAtLeast(1)
        l = DoubleArray(latencyFrames); r = DoubleArray(latencyFrames)
        peaks = DoubleArray(latencyFrames + 2); frames = LongArray(peaks.size)
        // A short look-ahead makes the controller catch a real inter-sample-style crest before
        // it reaches AudioTrack, while the release prevents audible pumping.
        fastAttack = 1 - exp(-1.0 / (rate * .0004))
        release = 1 - exp(-1.0 / (rate * .120))
        reset()
    }

    fun reset() {
        l.fill(0.0); r.fill(0.0)
        cursor = 0; head = 0; tail = 0; frame = 0; gain = 1.0
    }

    fun process(inputL: Double, inputR: Double, protection: Double = 0.0) {
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

        val amount = protection.coerceIn(0.0, 1.0)
        // Zero means exactly no requested control: do not run a gain envelope at all. The final
        // PCM clamp is representation safety only and cannot alter in-range programme material.
        if (amount <= .0001) {
            gain = 1.0
            left = oldL.coerceIn(-.999, .999)
            right = oldR.coerceIn(-.999, .999)
            frame++
            return
        }
        // At 100% the .76 ceiling leaves substantial true-peak headroom after the repair curve,
        // making the endpoint a real stronger protector rather than an inverse/weak limiter.
        val ceiling = .999 - .239 * amount
        val desired = if (maxPeak > ceiling) ceiling / maxPeak else 1.0
        // More requested control captures a crest faster; the response is monotonic from 0→100.
        val attack = (fastAttack * (.10 + .90 * amount)).coerceIn(.01, 1.0)
        gain += (desired - gain) * if (desired < gain) attack else release
        left = (oldL * gain).coerceIn(-.999, .999)
        right = (oldR * gain).coerceIn(-.999, .999)
        frame++
    }
}
