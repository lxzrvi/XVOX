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
    val softenHighs: Float = 0f,
    val surroundWidth: Float = .78f,
    val surroundPosition: Float = 0f,
    val roomAmount: Float = .5f,
    val reverbAmount: Float = 0f,
    val hrtf: Float = .6f,
    val centerPreservation: Float = 0f,
    /** The selected room character; its wet amount is [reverbAmount]. */
    val reverbPreset: String = if (reverbAmount > .001f) "Medium Room" else ReverbPresets.OFF,
    val noiseReductionEnabled: Boolean = noiseReduction > .001f,
    val grainControlEnabled: Boolean = softenHighs > .001f
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
    @Volatile var splitStems = false
    private var lastStemFlag = false
    private var splitPhase = 0.0
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
    private var currentWidth = 0.78
    private var currentPosition = 0.0
    private var currentRoom = 1.0
    private var currentReverb = 0.0
    private var currentReverbFeedbackBase = 0.0
    private var currentReverbFeedbackDepth = 0.0
    private var currentReverbWetBase = 0.0
    private var currentReverbWetDepth = 0.0
    private var currentReverbDelayScale = 1.0
    private var currentReverbDamping = 0.0
    private var currentReverbStereoWidth = 0.0
    private var currentHrtf = 0.6
    private var currentCenter = 0.0
    private var targetWidth = 0.78
    private var targetPosition = 0.0
    private var targetRoom = 1.0
    private var targetReverb = 0.0
    private var targetReverbFeedbackBase = 0.0
    private var targetReverbFeedbackDepth = 0.0
    private var targetReverbWetBase = 0.0
    private var targetReverbWetDepth = 0.0
    private var targetReverbDelayScale = 1.0
    private var targetReverbDamping = 0.0
    private var targetReverbStereoWidth = 0.0
    private var targetHrtf = 0.6
    private var targetCenter = 0.0
    private var phase = 0.0
    private var pan = 0.0
    private var rear = 0.0
    private var shadowAlpha = 0.0
    private var nearLeft = 1.0
    private var nearRight = 1.0
    private var delayLeft = DoubleArray(1600)
    private var delayRight = DoubleArray(1600)
    private var cursor = 0
    private val reverb = StereoReverbNetwork()
    private var shadowLeft = 0.0
    private var shadowRight = 0.0
    private var softL = 0.0; private var softR = 0.0
    private var bassL = 0.0; private var bassR = 0.0
    private var spatialBassL = 0.0; private var spatialBassR = 0.0
    private var toneAlpha = 0.0; private var noiseToneAlpha = 0.0; private var bassAlpha = 0.0; private var spatialBassAlpha = 0.0
    private var noiseToneL = 0.0; private var noiseToneR = 0.0
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
        // Noise reduction works on the high-frequency residual and a stronger quiet-level gate.
        noiseToneAlpha = 1 - exp(-2 * PI * minOf(6200.0, rate * .36) / rate)
        bassAlpha = 1 - exp(-2 * PI * 180 / rate)
        spatialBassAlpha = 1 - exp(-2 * PI * 700 / rate)
        pinna.configure(minOf(6800.0, rate * .40), rate, 1.15)
        delayLeft = DoubleArray((rate * .024).toInt() + 8)
        delayRight = DoubleArray(delayLeft.size)
        reverb.configure(rate)
        peakGuard.configure(rate)
        reset()
    }

    fun reset() {
        filters.forEach { it.reset() }; pinna.reset(); peakGuard.reset()
        currentBand.fill(0.0); targetBand.fill(0.0)
        delayLeft.fill(0.0); delayRight.fill(0.0)
        reverb.reset()
        cursor = 0; phase = 0.0; splitPhase = 0.0; block = 0
        shadowLeft = 0.0; shadowRight = 0.0
        softL = 0.0; softR = 0.0; noiseToneL = 0.0; noiseToneR = 0.0
        bassL = 0.0; bassR = 0.0; spatialBassL = 0.0; spatialBassR = 0.0
        noiseEnvelope = 0.0; noiseGain = 1.0; currentNoise = 0.0; currentSoftHighs = 1.0
        currentBassGain = transitionBassGain.toDouble().coerceIn(0.0, 1.0)
        currentVolume = settings.masterVolume.toDouble().coerceIn(0.0, 2.0)
        currentMix = mixGain.toDouble().coerceIn(0.0, 1.0)
        currentBalance = settings.balance.toDouble().coerceIn(-1.0, 1.0)
        currentDepth = 0.0
        currentPeriod = settings.orbitSeconds.toDouble().coerceIn(2.0, 10.0)
        lastSettings = null
        updateTargets()
        targetPreamp = targetHeadroom
        currentPreamp = targetHeadroom
        currentWidth = targetWidth
        currentPosition = targetPosition
        currentRoom = targetRoom
        currentReverb = targetReverb
        currentReverbFeedbackBase = targetReverbFeedbackBase
        currentReverbFeedbackDepth = targetReverbFeedbackDepth
        currentReverbWetBase = targetReverbWetBase
        currentReverbWetDepth = targetReverbWetDepth
        currentReverbDelayScale = targetReverbDelayScale
        currentReverbDamping = targetReverbDamping
        currentReverbStereoWidth = targetReverbStereoWidth
        currentHrtf = targetHrtf
        currentCenter = targetCenter
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
        targetSoftHighs = if (s.grainControlEnabled) {
            10.0.pow(-s.softenHighs.coerceIn(0f, 1f) * 18.0 / 20.0)
        } else {
            1.0
        }
        targetNoise = if (s.noiseReductionEnabled) s.noiseReduction.toDouble().coerceIn(0.0, 1.0) else 0.0
        val spatialActive = s.surroundEnabled && !splitStems
        targetDepth = if (spatialActive) s.surroundDepth.toDouble().coerceIn(0.0, 1.0) else 0.0
        targetWidth = if (spatialActive) s.surroundWidth.toDouble().coerceIn(0.05, 1.0) else 0.78
        targetPosition = if (spatialActive) s.surroundPosition.toDouble() else 0.0
        targetRoom = if (spatialActive) s.roomAmount.toDouble().coerceIn(0.0, 1.0) * 2.0 else 1.0
        targetHrtf = if (spatialActive) s.hrtf.toDouble().coerceIn(0.0, 1.0) else 0.6
        targetCenter = if (spatialActive) s.centerPreservation.toDouble().coerceIn(0.0, 1.0) else 0.0
        val reverbProfile = ReverbPresets.profile(s.reverbPreset)
        targetReverb = s.reverbAmount.toDouble().coerceIn(0.0, 1.0)
        targetReverbFeedbackBase = reverbProfile.feedbackBase
        targetReverbFeedbackDepth = reverbProfile.feedbackDepth
        targetReverbWetBase = reverbProfile.wetBase
        targetReverbWetDepth = reverbProfile.wetDepth
        targetReverbDelayScale = reverbProfile.delayScale
        targetReverbDamping = reverbProfile.damping
        targetReverbStereoWidth = reverbProfile.stereoWidth
        targetVolume = s.masterVolume.toDouble().coerceIn(0.0, 2.0)
        targetBalance = s.balance.toDouble().coerceIn(-1.0, 1.0)
        targetPeriod = if (s.orbitSeconds > 0f) s.orbitSeconds.toDouble().coerceIn(1.0, 20.0) else 100000.0
    }

    fun process(inputLeft: Float, inputRight: Float) {
        if (block == 0) {
            if (lastStemFlag != splitStems) { lastStemFlag = splitStems; lastSettings = null }
            updateTargets()
            // User-controlled headroom only: raising a band must not secretly turn the entire track down.
            targetPreamp = targetHeadroom
            // Quiet passages contain the most perceptible floor; this adaptive threshold gives
            // the control a genuinely audible gate without flattening normal music dynamics.
            noiseThreshold = .012 + currentNoise * .086
            pan = sin(phase + currentPosition) * currentWidth
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
        currentWidth += (targetWidth - currentWidth) * controlAlpha
        currentPosition += (targetPosition - currentPosition) * controlAlpha
        currentRoom += (targetRoom - currentRoom) * controlAlpha
        currentHrtf += (targetHrtf - currentHrtf) * controlAlpha
        currentCenter += (targetCenter - currentCenter) * controlAlpha
        currentReverb += (targetReverb - currentReverb) * controlAlpha
        currentReverbFeedbackBase += (targetReverbFeedbackBase - currentReverbFeedbackBase) * controlAlpha
        currentReverbFeedbackDepth += (targetReverbFeedbackDepth - currentReverbFeedbackDepth) * controlAlpha
        currentReverbWetBase += (targetReverbWetBase - currentReverbWetBase) * controlAlpha
        currentReverbWetDepth += (targetReverbWetDepth - currentReverbWetDepth) * controlAlpha
        currentReverbDelayScale += (targetReverbDelayScale - currentReverbDelayScale) * controlAlpha
        currentReverbDamping += (targetReverbDamping - currentReverbDamping) * controlAlpha
        currentReverbStereoWidth += (targetReverbStereoWidth - currentReverbStereoWidth) * controlAlpha
        currentPeriod += (targetPeriod - currentPeriod) * controlAlpha
        if (settings.orbitSeconds > 0f) {
            phase += 2 * PI / (rate * currentPeriod)
            if (phase >= 2 * PI) phase -= 2 * PI
        }

        var dryL = inputLeft.toDouble()
        var dryR = inputRight.toDouble()
        if (splitStems) {
            val instrumental = dryL * 2.0
            val vocal = dryR * 2.0
            val vocalPan = cos(splitPhase)
            val vocalAngle = (vocalPan + 1) * PI / 4
            val musicAngle = (-vocalPan + 1) * PI / 4
            dryL = instrumental * cos(musicAngle) + vocal * cos(vocalAngle)
            dryR = instrumental * sin(musicAngle) + vocal * sin(vocalAngle)
            splitPhase += 2 * PI / (rate * 12.0)
            if (splitPhase > 2 * PI) splitPhase -= 2 * PI
        }
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
        // Suppress high-frequency residual (where hiss/grain lives) as well as low-level noise.
        // The residual path is active only in proportion to the selected control, so an off
        // setting remains bit-for-bit close to the dry path.
        noiseToneL += (l - noiseToneL) * noiseToneAlpha
        noiseToneR += (r - noiseToneR) * noiseToneAlpha
        l = noiseToneL + (l - noiseToneL) * (1 - currentNoise * .66)
        r = noiseToneR + (r - noiseToneR) * (1 - currentNoise * .66)
        val envelopeInput = max(abs(l), abs(r))
        noiseEnvelope += (envelopeInput - noiseEnvelope) * if (envelopeInput > noiseEnvelope) fastAlpha else controlAlpha
        val ratio = (noiseEnvelope / noiseThreshold.coerceAtLeast(1e-8)).coerceIn(0.0, 1.0)
        val desiredNoiseGain = 1 - currentNoise * .95 * (1 - ratio * ratio)
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
        // HRTF amount drives both the head shadow and the pinna glare that make the image sit
        // outside the ears (defaults equal the original fixed strengths).
        val headShadow = currentHrtf
        val pinnaShadow = currentHrtf * .5
        val filteredL = (earL * (1 - farL * headShadow) + shadowLeft * farL * headShadow) - pinna.left * rear * pinnaShadow
        val filteredR = (earR * (1 - farR * headShadow) + shadowRight * farR * headShadow) - pinna.right * rear * pinnaShadow
        spatialBassL += (filteredL - spatialBassL) * spatialBassAlpha
        spatialBassR += (filteredR - spatialBassR) * spatialBassAlpha
        // Room is the level of the early reflections that suggest an enclosed space.
        val spatialL = (spatialBassL * (.90 + .10 * nearLeft) + (filteredL - spatialBassL) * nearLeft) * .94 + (delayed(delayRight, rate * .011) * .04 + delayed(delayLeft, rate * .017) * .02) * currentRoom
        val spatialR = (spatialBassR * (.90 + .10 * nearRight) + (filteredR - spatialBassR) * nearRight) * .94 + (delayed(delayLeft, rate * .013) * .04 + delayed(delayRight, rate * .019) * .02) * currentRoom
        l += (spatialL - l) * currentDepth
        r += (spatialR - r) * currentDepth
        // A decorrelated multi-line feedback network replaces the old single delayed copy.
        // The preset sets tail length/damping/stereo spread; Amount only changes its wet mix.
        if (currentReverb > .001) {
            val feedback = (currentReverbFeedbackBase + currentReverbFeedbackDepth * currentReverb)
                .coerceIn(0.0, .86)
            val wet = (currentReverb * (currentReverbWetBase + currentReverbWetDepth * currentReverb))
                .coerceIn(0.0, .62)
            reverb.process(
                inputLeft = l,
                inputRight = r,
                feedback = feedback,
                delayScale = currentReverbDelayScale,
                damping = currentReverbDamping,
                stereoWidth = currentReverbStereoWidth
            )
            l += reverb.left * wet
            r += reverb.right * wet
        }
        // Center preservation keeps the phantom centre glued to the original mix while the
        // sides fan out; at the default (0) the behaviour is exactly the old widening.
        if (currentCenter > .001) {
            val dryMid = (dryL + dryR) * .5
            val processedMid = (l + r) * .5
            l += (dryMid - processedMid) * currentCenter
            r += (dryMid - processedMid) * currentCenter
        }
        cursor = (cursor + 1) % delayLeft.size
        if (currentBalance > 0) l *= 1 - currentBalance else r *= 1 + currentBalance
        if (!l.isFinite() || !r.isFinite()) {
            l = 0.0; r = 0.0
            filters.forEach { it.reset() }; pinna.reset()
        }
        // App volume is part of the PCM gain path. Above 100%, use a gentle soft-drive before
        // the transparent look-ahead guard: a peak-only limiter would otherwise cancel nearly
        // all of a 101–200% boost on mastered tracks. At and below 100% this remains exact linear
        // gain; above it, quieter detail and average loudness genuinely rise while output peaks
        // stay protected for PCM conversion.
        val mixedL = l * currentMix
        val mixedR = r * currentMix
        val boostedL = if (currentVolume <= 1.0001) mixedL * currentVolume else tanh(mixedL * currentVolume)
        val boostedR = if (currentVolume <= 1.0001) mixedR * currentVolume else tanh(mixedR * currentVolume)
        peakGuard.process(boostedL, boostedR)
        left = peakGuard.left.toFloat()
        right = peakGuard.right.toFloat()
    }

    private fun delayed(buffer: DoubleArray, samples: Double): Double {
        val offset = samples.coerceIn(0.0, buffer.size - 2.0)
        val whole = offset.toInt(); val fraction = offset - whole
        val a = (cursor - whole + buffer.size) % buffer.size
        val b = (a - 1 + buffer.size) % buffer.size
        return buffer[a] * (1 - fraction) + buffer[b] * fraction
    }
}

/**
 * Four decorrelated damped feedback lines. Unlike a single slap-back delay, their uneven lengths
 * build a dense tail with stereo cross-feed. It allocates only when the audio format changes.
 */
private class StereoReverbNetwork {
    private val baseSeconds = doubleArrayOf(.0297, .0371, .0411, .0437)
    private var rate = 44100
    private var leftLines: Array<DoubleArray> = emptyArray()
    private var rightLines: Array<DoubleArray> = emptyArray()
    private var cursors = IntArray(0)
    private var dampedLeft = DoubleArray(0)
    private var dampedRight = DoubleArray(0)
    var left = 0.0
        private set
    var right = 0.0
        private set

    fun configure(sampleRate: Int) {
        rate = sampleRate.coerceAtLeast(8000)
        // The largest profile is Cathedral. Keep headroom for profile interpolation.
        leftLines = Array(baseSeconds.size) { index ->
            DoubleArray((baseSeconds[index] * rate * 1.85).toInt().coerceAtLeast(32) + 4)
        }
        rightLines = Array(baseSeconds.size) { index -> DoubleArray(leftLines[index].size) }
        cursors = IntArray(baseSeconds.size)
        dampedLeft = DoubleArray(baseSeconds.size)
        dampedRight = DoubleArray(baseSeconds.size)
        reset()
    }

    fun reset() {
        leftLines.forEach { it.fill(0.0) }
        rightLines.forEach { it.fill(0.0) }
        cursors.fill(0)
        dampedLeft.fill(0.0)
        dampedRight.fill(0.0)
        left = 0.0
        right = 0.0
    }

    fun process(
        inputLeft: Double,
        inputRight: Double,
        feedback: Double,
        delayScale: Double,
        damping: Double,
        stereoWidth: Double
    ) {
        if (leftLines.isEmpty()) {
            left = 0.0
            right = 0.0
            return
        }
        val safeFeedback = feedback.coerceIn(0.0, .86)
        val safeScale = delayScale.coerceIn(.42, 1.78)
        val safeDamping = damping.coerceIn(0.0, .82)
        val cross = stereoWidth.coerceIn(0.0, .9) * .22
        var sumLeft = 0.0
        var sumRight = 0.0
        for (index in leftLines.indices) {
            val lLine = leftLines[index]
            val rLine = rightLines[index]
            val cursor = cursors[index]
            val delay = (baseSeconds[index] * rate * safeScale).toInt().coerceIn(4, lLine.size - 2)
            val read = (cursor - delay + lLine.size) % lLine.size
            val delayedLeft = lLine[read]
            val delayedRight = rLine[read]
            // Damping lives in the feedback path, preserving the initial transient while each
            // selected room gives its tail a recognisably different warmth.
            dampedLeft[index] += (delayedLeft - dampedLeft[index]) * (1 - safeDamping)
            dampedRight[index] += (delayedRight - dampedRight[index]) * (1 - safeDamping)
            val inputGain = .32 + index * .018
            lLine[cursor] = (inputLeft + inputRight * cross) * inputGain + dampedLeft[index] * safeFeedback
            rLine[cursor] = (inputRight + inputLeft * cross) * inputGain + dampedRight[index] * safeFeedback
            sumLeft += delayedLeft
            sumRight += delayedRight
            cursors[index] = (cursor + 1) % lLine.size
        }
        // Normalise the four uneven lines; their lengths and cross-feed keep comb peaks decorrelated.
        left = (sumLeft * .25).coerceIn(-4.0, 4.0)
        right = (sumRight * .25).coerceIn(-4.0, 4.0)
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
