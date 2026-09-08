package com.xvox.music.audio

import kotlin.math.*

data class AudioDspSettings(
    val equalizerEnabled: Boolean = false,
    val bands: List<Float> = List(5) { 0f },
    val headroomDb: Float = 0f,
    val balance: Float = 0f,
    val surroundEnabled: Boolean = false,
    val surroundDepth: Float = .65f,
    val orbitSeconds: Float = 6f,
    val masterVolume: Float = 1f,
    val bandCount: Int = 5,
    val noiseReduction: Float = 0f,
    val softenHighs: Float = 0f
)

/**
 * Fixed-pole parallel graphic EQ: filters stay warm even when bypassed. Only output gains ramp.
 * No feedback-coefficient interpolation, filter resets or audio-session recreation on slider moves.
 * All expensive target calculations happen on a settings change, not for every sample block.
 */
class XvoxDspEngine {
    @Volatile var settings = AudioDspSettings()
    @Volatile var mixGain = 1f
    @Volatile var duckGain = 1f
    @Volatile var transitionBassGain = 1f
    var left = 0f
        private set
    var right = 0f
        private set

    private val peakGuard = StereoPeakGuard()
    val latencyFrames: Int get() = peakGuard.latencyFrames
    private var noiseThreshold = .0003
    private var rate = 44100
    private val frequencies = EqBands.five + EqBands.ten
    private val filters = Array(15) { FixedStereoBandpass() }
    private val pinna = FixedStereoBandpass()
    private val currentBand = DoubleArray(15)
    private val targetBand = DoubleArray(15)
    private var targetHeadroom = 1.0
    private var lastSettings: AudioDspSettings? = null
    private var block = 0
    private var eqAlpha = 0.0
    private var controlAlpha = 0.0
    private var reductionAlpha = 0.0
    private var fastAlpha = 0.0
    private var currentVolume = 1.0
    private var currentMix = 1.0
    private var currentPreamp = 1.0
    private var currentBalance = 0.0
    private var currentDepth = 0.0
    private var currentPeriod = 6.0
    private var targetPreamp = 1.0
    private var targetDepth = 0.0
    private var targetVolume = 1.0
    private var targetBalance = 0.0
    private var targetPeriod = 6.0
    private var phase = 0.0
    private var pan = 0.0
    private var rear = 0.0
    private var shadowAlpha = 0.0
    private var nearLeft = 1.0
    private var nearRight = 1.0
    private var delayLeft = DoubleArray(1600)
    private var delayRight = DoubleArray(1600)
    private var cursor = 0
    private var shadowLeft = 0.0
    private var shadowRight = 0.0
    private var softL = 0.0; private var softR = 0.0
    private var bassL = 0.0; private var bassR = 0.0
    private var spatialBassL = 0.0; private var spatialBassR = 0.0
    private var toneAlpha = 0.0; private var bassAlpha = 0.0; private var spatialBassAlpha = 0.0
    private var currentSoftHighs = 1.0; private var targetSoftHighs = 1.0
    private var currentNoise = 0.0; private var targetNoise = 0.0; private var noiseEnvelope = 0.0; private var noiseGain = 1.0
    private var currentBassGain = 1.0

    fun configure(sampleRate: Int) {
        rate = sampleRate.coerceIn(8000, 384000)
        eqAlpha = 1 - exp(-1.0 / (rate * .085))
        controlAlpha = 1 - exp(-1.0 / (rate * .080))
        reductionAlpha = 1 - exp(-1.0 / (rate * .008))
        fastAlpha = 1 - exp(-1.0 / (rate * .008))
        for (i in filters.indices) filters[i].configure(frequencies[i].coerceAtMost(rate * .43), rate, .82)
        toneAlpha = 1 - exp(-2 * PI * minOf(5500.0, rate * .35) / rate)
        bassAlpha = 1 - exp(-2 * PI * 180 / rate)
        spatialBassAlpha = 1 - exp(-2 * PI * 700 / rate)
        pinna.configure(minOf(6800.0, rate * .40), rate, 1.15)
        delayLeft = DoubleArray((rate * .024).toInt() + 8)
        delayRight = DoubleArray(delayLeft.size)
        peakGuard.configure(rate)
        reset()
    }

    fun reset() {
        filters.forEach { it.reset() }; pinna.reset(); peakGuard.reset()
        currentBand.fill(0.0); targetBand.fill(0.0)
        delayLeft.fill(0.0); delayRight.fill(0.0)
        cursor = 0; phase = 0.0; block = 0
        shadowLeft = 0.0; shadowRight = 0.0
        softL = 0.0; softR = 0.0; bassL = 0.0; bassR = 0.0; spatialBassL = 0.0; spatialBassR = 0.0
        noiseEnvelope = 0.0; noiseGain = 1.0; currentNoise = 0.0; currentSoftHighs = 1.0
        currentBassGain = transitionBassGain.toDouble().coerceIn(0.0, 1.0)
        currentVolume = settings.masterVolume.toDouble().coerceIn(0.0, 1.0)
        currentMix = mixGain.toDouble().coerceIn(0.0, 1.0)
        currentBalance = settings.balance.toDouble().coerceIn(-1.0, 1.0)
        currentDepth = 0.0
        currentPeriod = settings.orbitSeconds.toDouble().coerceIn(2.0, 10.0)
        lastSettings = null
        updateTargets()
        targetPreamp = targetHeadroom
        currentPreamp = targetHeadroom
    }

    private fun updateTargets() {
        val s = settings
        if (lastSettings == s) return
        lastSettings = s
        for (i in targetBand.indices) {
            val selectedBank = if (s.bandCount == 10) i >= 5 else i < 5
            val bandIndex = if (s.bandCount == 10) i - 5 else i
            val db = if (s.equalizerEnabled && selectedBank) s.bands.getOrElse(bandIndex) { 0f }.toDouble().coerceIn(-12.0, 12.0) else 0.0
            targetBand[i] = 10.0.pow(db / 20.0) - 1.0
        }
        targetHeadroom = 10.0.pow(-s.headroomDb.coerceIn(0f, 18f) / 20.0)
        targetSoftHighs = 10.0.pow(-s.softenHighs.coerceIn(0f, 1f) * 9.0 / 20.0)
        targetNoise = s.noiseReduction.toDouble().coerceIn(0.0, 1.0)
        targetDepth = if (s.surroundEnabled) s.surroundDepth.toDouble().coerceIn(0.0, 1.0) else 0.0
        targetVolume = s.masterVolume.toDouble().coerceIn(0.0, 1.0)
        targetBalance = s.balance.toDouble().coerceIn(-1.0, 1.0)
        targetPeriod = s.orbitSeconds.toDouble().coerceIn(2.0, 10.0)
    }

    fun process(inputLeft: Float, inputRight: Float) {
        if (block == 0) {
            updateTargets()
            // User-controlled headroom only: raising a band must not secretly turn the entire track down.
            targetPreamp = targetHeadroom
            noiseThreshold = 10.0.pow((-70 + currentNoise * 20) / 20)
            pan = sin(phase) * .78
            rear = (1 - cos(phase)) * .5
            shadowAlpha = 1 - exp(-2 * PI * (12000 - rear * 7000) / rate)
            nearLeft = cos((pan + 1) * PI / 4) * sqrt(2.0)
            nearRight = sin((pan + 1) * PI / 4) * sqrt(2.0)
        }
        block = (block + 1) and 31
        currentPreamp += (targetPreamp - currentPreamp) * reductionAlpha
        currentVolume += (targetVolume * duckGain - currentVolume) * controlAlpha
        currentSoftHighs += (targetSoftHighs - currentSoftHighs) * controlAlpha
        currentNoise += (targetNoise - currentNoise) * controlAlpha
        currentBassGain += (transitionBassGain.coerceIn(0f, 1f) - currentBassGain) * fastAlpha
        currentMix += (mixGain.toDouble().coerceIn(0.0, 1.0) - currentMix) * fastAlpha
        currentBalance += (targetBalance - currentBalance) * controlAlpha
        currentDepth += (targetDepth - currentDepth) * controlAlpha
        currentPeriod += (targetPeriod - currentPeriod) * controlAlpha
        phase += 2 * PI / (rate * currentPeriod)
        if (phase >= 2 * PI) phase -= 2 * PI

        val dryL = inputLeft.toDouble()
        val dryR = inputRight.toDouble()
        var l = dryL; var r = dryR
        for (i in filters.indices) {
            currentBand[i] += (targetBand[i] - currentBand[i]) * eqAlpha
            filters[i].process(dryL, dryR) // Always primed; bypass is a gain, not a filter reset.
            l += filters[i].left * currentBand[i]
            r += filters[i].right * currentBand[i]
        }
        // Float-domain filtering cannot clip internally; apply the smooth protection envelope here
        // so changing headroom does not perturb filter histories.
        l *= currentPreamp; r *= currentPreamp
        softL += (l - softL) * toneAlpha; softR += (r - softR) * toneAlpha
        l = softL + (l - softL) * currentSoftHighs
        r = softR + (r - softR) * currentSoftHighs
        val envelopeInput = max(abs(l), abs(r))
        noiseEnvelope += (envelopeInput - noiseEnvelope) * if (envelopeInput > noiseEnvelope) fastAlpha else controlAlpha
        val ratio = (noiseEnvelope / noiseThreshold.coerceAtLeast(1e-8)).coerceIn(0.0, 1.0)
        val desiredNoiseGain = 1 - currentNoise * .92 * (1 - ratio * ratio)
        noiseGain += (desiredNoiseGain - noiseGain) * controlAlpha
        l *= noiseGain; r *= noiseGain
        bassL += (l - bassL) * bassAlpha; bassR += (r - bassR) * bassAlpha
        l += bassL * (currentBassGain - 1); r += bassR * (currentBassGain - 1)
        delayLeft[cursor] = l; delayRight[cursor] = r
        // Headphone orbit: fractional ear delay, equal-power position, rear pinna shadow and quiet early reflections.
        // Preserve the stereo side information rather than folding the recording to mono.
        val itd = abs(pan) * .00068 * rate
        val earL = delayed(delayLeft, if (pan > 0) itd else 0.0)
        val earR = delayed(delayRight, if (pan < 0) itd else 0.0)
        shadowLeft += (earL - shadowLeft) * shadowAlpha
        shadowRight += (earR - shadowRight) * shadowAlpha
        pinna.process(earL, earR)
        val farL = max(pan, 0.0)
        val farR = max(-pan, 0.0)
        val filteredL = (earL * (1 - farL * .60) + shadowLeft * farL * .60) - pinna.left * rear * .30
        val filteredR = (earR * (1 - farR * .60) + shadowRight * farR * .60) - pinna.right * rear * .30
        spatialBassL += (filteredL - spatialBassL) * spatialBassAlpha
        spatialBassR += (filteredR - spatialBassR) * spatialBassAlpha
        val spatialL = (spatialBassL * (.90 + .10 * nearLeft) + (filteredL - spatialBassL) * nearLeft) * .94 + delayed(delayRight, rate * .011) * .04 + delayed(delayLeft, rate * .017) * .02
        val spatialR = (spatialBassR * (.90 + .10 * nearRight) + (filteredR - spatialBassR) * nearRight) * .94 + delayed(delayLeft, rate * .013) * .04 + delayed(delayRight, rate * .019) * .02
        l += (spatialL - l) * currentDepth
        r += (spatialR - r) * currentDepth
        cursor = (cursor + 1) % delayLeft.size
        if (currentBalance > 0) l *= 1 - currentBalance else r *= 1 + currentBalance
        if (!l.isFinite() || !r.isFinite()) {
            l = 0.0; r = 0.0
            filters.forEach { it.reset() }; pinna.reset()
        }
        peakGuard.process(l, r)
        val output = currentVolume * currentMix
        left = (peakGuard.left * output).toFloat()
        right = (peakGuard.right * output).toFloat()
    }

    private fun delayed(buffer: DoubleArray, samples: Double): Double {
        val offset = samples.coerceIn(0.0, buffer.size - 2.0)
        val whole = offset.toInt(); val fraction = offset - whole
        val a = (cursor - whole + buffer.size) % buffer.size
        val b = (a - 1 + buffer.size) % buffer.size
        return buffer[a] * (1 - fraction) + buffer[b] * fraction
    }
}

private class FixedStereoBandpass {
    private var b0 = 0.0; private var b2 = 0.0; private var a1 = 0.0; private var a2 = 0.0
    private var z1L = 0.0; private var z2L = 0.0; private var z1R = 0.0; private var z2R = 0.0
    var left = 0.0
        private set
    var right = 0.0
        private set
    var responseReal = 0.0
        private set
    var responseImaginary = 0.0
        private set
    fun configure(frequency: Double, rate: Int, q: Double) {
        val w = 2 * PI * frequency / rate
        val alpha = sin(w) / (2 * q)
        b0 = alpha / (1 + alpha); b2 = -b0
        a1 = -2 * cos(w) / (1 + alpha); a2 = (1 - alpha) / (1 + alpha)
        reset()
    }
    fun reset() { z1L = 0.0; z2L = 0.0; z1R = 0.0; z2R = 0.0 }
    fun process(l: Double, r: Double) {
        left = b0 * l + z1L; right = b0 * r + z1R
        z1L = -a1 * left + z2L; z2L = b2 * l - a2 * left
        z1R = -a1 * right + z2R; z2R = b2 * r - a2 * right
    }
    fun response(w: Double) {
        val nr = b0 + b2 * cos(2 * w); val ni = -b2 * sin(2 * w)
        val dr = 1 + a1 * cos(w) + a2 * cos(2 * w); val di = -a1 * sin(w) - a2 * sin(2 * w)
        val denominator = (dr * dr + di * di).coerceAtLeast(1e-18)
        responseReal = (nr * dr + ni * di) / denominator
        responseImaginary = (ni * dr - nr * di) / denominator
    }
}
