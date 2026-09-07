package com.xvox.music.audio

import kotlin.math.*

data class AudioDspSettings(
    val equalizerEnabled: Boolean = false,
    val bands: List<Float> = List(5) { 0f },
    val headroomDb: Float = 3f,
    val balance: Float = 0f,
    val surroundEnabled: Boolean = false,
    val surroundDepth: Float = .65f,
    val orbitSeconds: Float = 6f,
    val masterVolume: Float = 1f
)

/**
 * Allocation-free, stereo-linked PCM DSP. Settings only change targets: no filter reset or audio-session
 * reattachment while dragging. EQ coefficients interpolate, controls ramp, and a peak guard reserves
 * consistent mix headroom. The spatial effect models ITD / head shadow; it is not personalized HRTF.
 */
class XvoxDspEngine {
    @Volatile var settings = AudioDspSettings()
    @Volatile var mixGain = 1f
    @Volatile var duckGain = 1f
    var left = 0f; private set
    var right = 0f; private set

    private var rate = 44100
    private val frequencies = doubleArrayOf(60.0, 230.0, 910.0, 3600.0, 14000.0)
    private val filters = Array(5) { SmoothPeakingFilter() }
    private val gains = DoubleArray(5)
    private var block = 0
    private var gainAlpha = 0.0
    private var controlAlpha = 0.0
    private var fastAlpha = 0.0
    private var peakReleaseAlpha = 0.0
    private var shadowAlpha = 0.0
    private var currentVolume = 1.0
    private var currentMix = 1.0
    private var currentPreamp = 1.0
    private var currentBalance = 0.0
    private var currentDepth = 0.0
    private var currentPeriod = 6.0
    private var phase = 0.0
    private var peakGain = 1.0
    private var delayLeft = DoubleArray(512)
    private var delayRight = DoubleArray(512)
    private var cursor = 0
    private var shadowLeft = 0.0
    private var shadowRight = 0.0
    private var pan = 0.0
    private var back = 0.0
    private var targetPreamp = 1.0
    private var targetDepth = 0.0
    private var targetVolume = 1.0
    private var targetBalance = 0.0
    private var targetPeriod = 6.0

    fun configure(sampleRate: Int) {
        rate = sampleRate.coerceIn(8000, 384000)
        gainAlpha = 1 - exp(-32.0 / (rate * .060))
        controlAlpha = 1 - exp(-1.0 / (rate * .045))
        fastAlpha = 1 - exp(-1.0 / (rate * .006))
        peakReleaseAlpha = 1 - exp(-1.0 / (rate * .120))
        delayLeft = DoubleArray((rate * .004).toInt().coerceAtLeast(64) + 8)
        delayRight = DoubleArray(delayLeft.size)
        reset()
    }

    fun reset() {
        filters.forEach { it.reset() }
        gains.fill(0.0)
        delayLeft.fill(0.0); delayRight.fill(0.0)
        cursor = 0; phase = 0.0; block = 0; peakGain = 1.0
        shadowLeft = 0.0; shadowRight = 0.0
        currentVolume = settings.masterVolume.toDouble().coerceIn(0.0, 1.0)
        currentMix = mixGain.toDouble().coerceIn(0.0, 1.0)
        currentBalance = settings.balance.toDouble().coerceIn(-1.0, 1.0)
        currentDepth = 0.0
        currentPreamp = 10.0.pow(-settings.headroomDb.coerceIn(0f, 18f) / 20.0)
        currentPeriod = settings.orbitSeconds.toDouble().coerceIn(2.0, 10.0)
    }

    fun process(inputLeft: Float, inputRight: Float) {
        if (block == 0) {
            val s = settings
            var boost = 0.0
            for (i in filters.indices) {
                val target = if (s.equalizerEnabled) s.bands.getOrElse(i) { 0f }.toDouble().coerceIn(-12.0, 12.0) else 0.0
                gains[i] += (target - gains[i]) * gainAlpha
                boost = max(boost, max(target, gains[i]))
                filters[i].setTarget(frequencies[i].coerceAtMost(rate * .44), rate, gains[i])
            }
            // Automatic boost compensation is applied before the filters; user headroom adds safety.
            targetPreamp = 10.0.pow(-(s.headroomDb.coerceIn(0f, 18f) + boost) / 20.0)
            targetDepth = if (s.surroundEnabled) s.surroundDepth.toDouble().coerceIn(0.0, 1.0) else 0.0
            targetVolume = (s.masterVolume * duckGain).toDouble().coerceIn(0.0, 1.0)
            targetBalance = s.balance.toDouble().coerceIn(-1.0, 1.0)
            targetPeriod = s.orbitSeconds.toDouble().coerceIn(2.0, 10.0)
            pan = sin(phase)
            back = (1 - cos(phase)) * .5
            shadowAlpha = 1 - exp(-2 * PI * (8500 - back * 4500) / rate)
        }
        block = (block + 1) and 31
        currentPreamp += (targetPreamp - currentPreamp) * controlAlpha
        currentVolume += (targetVolume - currentVolume) * controlAlpha
        currentMix += (mixGain.toDouble().coerceIn(0.0, 1.0) - currentMix) * fastAlpha
        currentBalance += (targetBalance - currentBalance) * controlAlpha
        currentDepth += (targetDepth - currentDepth) * controlAlpha
        currentPeriod += (targetPeriod - currentPeriod) * controlAlpha
        phase += 2 * PI / (rate * currentPeriod)
        if (phase >= 2 * PI) phase -= 2 * PI

        var l = inputLeft.toDouble() * currentPreamp
        var r = inputRight.toDouble() * currentPreamp
        for (filter in filters) {
            filter.process(l, r)
            l = filter.left; r = filter.right
        }
        // Preserve the original stereo image; the orbit adds ear delay and directional high-frequency shadow.
        delayLeft[cursor] = l; delayRight[cursor] = r
        val depth = currentDepth
        if (depth > .0001) {
            val delay = abs(pan) * .00055 * rate * depth // up to 0.55 ms interaural delay
            val dl = if (pan > 0) delayed(delayLeft, delay) else l
            val dr = if (pan < 0) delayed(delayRight, delay) else r
            shadowLeft += (dl - shadowLeft) * shadowAlpha
            shadowRight += (dr - shadowRight) * shadowAlpha
            val farLeft = max(pan, 0.0) * depth
            val farRight = max(-pan, 0.0) * depth
            val spatialL = (dl * (1 - farLeft * .50) + shadowLeft * farLeft * .50) * (1 - farLeft * .30)
            val spatialR = (dr * (1 - farRight * .50) + shadowRight * farRight * .50) * (1 - farRight * .30)
            val reflectionL = delayed(delayRight, rate * .0018)
            val reflectionR = delayed(delayLeft, rate * .0018)
            l = spatialL * (1 - .08 * depth) + reflectionL * .08 * depth
            r = spatialR * (1 - .08 * depth) + reflectionR * .08 * depth
        }
        cursor = (cursor + 1) % delayLeft.size
        if (currentBalance > 0) l *= 1 - currentBalance else r *= 1 + currentBalance
        if (!l.isFinite() || !r.isFinite()) { l = 0.0; r = 0.0; filters.forEach { it.reset() } }
        // Stereo-linked fast attack / slow release avoids hard PCM clipping and image shifts.
        // A constant 0.70 ceiling also leaves room for two equal-power decks (max sum < 1).
        val peak = max(abs(l), abs(r))
        val needed = if (peak > .70) .70 / peak else 1.0
        peakGain = if (needed < peakGain) needed else min(needed, peakGain + (1 - peakGain) * peakReleaseAlpha)
        val output = currentVolume * currentMix * peakGain
        left = (l * output).toFloat().coerceIn(-.70f, .70f)
        right = (r * output).toFloat().coerceIn(-.70f, .70f)
    }

    private fun delayed(buffer: DoubleArray, samples: Double): Double {
        val offset = samples.coerceIn(0.0, buffer.size - 2.0)
        val whole = offset.toInt()
        val frac = offset - whole
        val a = (cursor - whole + buffer.size) % buffer.size
        val b = (a - 1 + buffer.size) % buffer.size
        return buffer[a] * (1 - frac) + buffer[b] * frac
    }
}

private class SmoothPeakingFilter {
    private val c = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0)
    private val step = DoubleArray(5)
    private var z1L = 0.0; private var z2L = 0.0
    private var z1R = 0.0; private var z2R = 0.0
    var left = 0.0; private set
    var right = 0.0; private set
    fun reset() { c.fill(0.0); c[0] = 1.0; step.fill(0.0); z1L = 0.0; z2L = 0.0; z1R = 0.0; z2R = 0.0 }
    fun setTarget(frequency: Double, sampleRate: Int, db: Double) {
        val a = 10.0.pow(db / 40)
        val w = 2 * PI * frequency / sampleRate
        val alpha = sin(w) / (2 * .80)
        val a0 = 1 + alpha / a
        step[0] = ((1 + alpha * a) / a0 - c[0]) / 32
        step[1] = (-2 * cos(w) / a0 - c[1]) / 32
        step[2] = ((1 - alpha * a) / a0 - c[2]) / 32
        step[3] = (-2 * cos(w) / a0 - c[3]) / 32
        step[4] = ((1 - alpha / a) / a0 - c[4]) / 32
    }
    fun process(l: Double, r: Double) {
        for (i in c.indices) c[i] += step[i]
        left = c[0] * l + z1L; right = c[0] * r + z1R
        z1L = c[1] * l - c[3] * left + z2L; z2L = c[2] * l - c[4] * left
        z1R = c[1] * r - c[3] * right + z2R; z2R = c[2] * r - c[4] * right
    }
}
