package com.xvox.music.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AudioEffectsManager {
    private const val TAG = "AudioEffectsManager"

    private var currentSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // Standard preset band levels in dB (-15 to +15)
    val PRESETS = mapOf(
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Bass Boost" to listOf(7, 5, 2, 0, -1),
        "Treble" to listOf(-2, 0, 2, 5, 7),
        "Rock" to listOf(5, 3, -1, 3, 5),
        "Pop" to listOf(-1, 3, 5, 3, -1),
        "Jazz" to listOf(4, 2, -1, 2, 4),
        "Electronic" to listOf(6, 4, 0, 3, 5),
        "Vocal" to listOf(-3, 1, 6, 4, 1)
    )

    fun attachAudioSession(sessionId: Int, context: Context) {
        if (sessionId <= 0) return
        if (currentSessionId == sessionId && equalizer != null) {
            applyAllCurrent(context)
            return
        }

        releaseEffects()
        currentSessionId = sessionId

        runCatching {
            equalizer = Equalizer(1000, sessionId).apply {
                enabled = true
            }
        }.onFailure {
            runCatching {
                equalizer = Equalizer(0, sessionId).apply {
                    enabled = true
                }
            }.onFailure { Log.e(TAG, "Failed to init Equalizer: ${it.message}") }
        }

        runCatching {
            bassBoost = BassBoost(1000, sessionId).apply {
                enabled = true
            }
        }.onFailure {
            runCatching {
                bassBoost = BassBoost(0, sessionId).apply {
                    enabled = true
                }
            }.onFailure { Log.e(TAG, "Failed to init BassBoost: ${it.message}") }
        }

        runCatching {
            virtualizer = Virtualizer(1000, sessionId).apply {
                enabled = true
            }
        }.onFailure {
            runCatching {
                virtualizer = Virtualizer(0, sessionId).apply {
                    enabled = true
                }
            }.onFailure { Log.e(TAG, "Failed to init Virtualizer: ${it.message}") }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            runCatching {
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                    enabled = true
                }
            }.onFailure { Log.e(TAG, "Failed to init LoudnessEnhancer: ${it.message}") }
        }

        startObservingPreferences(context)
    }

    private fun applyAllCurrent(context: Context) {
        scope.launch {
            val prefs = UserPreferencesRepository(context)
            val eqEn = prefs.equalizerEnabled.first()
            val eqPr = prefs.eqPreset.first()
            val eqB = prefs.eqBands.first()
            applyEqualizer(eqEn, eqPr, eqB)

            val bbEn = prefs.bassBoost.first()
            val bbSt = prefs.bassBoostStrength.first()
            val leEn = prefs.loudnessEnhancer.first()
            val leG = prefs.loudnessGainMb.first()

            applyBassBoostWithCompensation(bbEn, bbSt, leEn, leG)

            val virtEn = prefs.virtualizerEnabled.first()
            val virtSt = prefs.virtualizerStrength.first()
            applyVirtualizer(virtEn, virtSt)
        }
    }

    private fun startObservingPreferences(context: Context) {
        syncJob?.cancel()
        val prefs = UserPreferencesRepository(context)

        syncJob = scope.launch {
            // Equalizer observer
            launch {
                combine(
                    prefs.equalizerEnabled,
                    prefs.eqPreset,
                    prefs.eqBands
                ) { enabled, preset, bands ->
                    Triple(enabled, preset, bands)
                }.collect { (enabled, preset, bands) ->
                    applyEqualizer(enabled, preset, bands)
                }
            }

            // Bass Boost & Loudness Compensation observer
            launch {
                combine(
                    prefs.bassBoost,
                    prefs.bassBoostStrength,
                    prefs.loudnessEnhancer,
                    prefs.loudnessGainMb
                ) { bbEn, bbSt, leEn, leG ->
                    listOf(bbEn, bbSt, leEn, leG)
                }.collect { params ->
                    val bbEn = params[0] as Boolean
                    val bbSt = params[1] as Int
                    val leEn = params[2] as Boolean
                    val leG = params[3] as Int
                    applyBassBoostWithCompensation(bbEn, bbSt, leEn, leG)
                }
            }

            // Virtualizer observer
            launch {
                combine(
                    prefs.virtualizerEnabled,
                    prefs.virtualizerStrength
                ) { enabled, strength ->
                    Pair(enabled, strength)
                }.collect { (enabled, strength) ->
                    applyVirtualizer(enabled, strength)
                }
            }
        }
    }

    private fun applyEqualizer(enabled: Boolean, preset: String, bands: List<Int>) {
        equalizer?.let { eq ->
            runCatching {
                eq.enabled = enabled
                if (enabled) {
                    val targetBands = if (preset != "Custom" && PRESETS.containsKey(preset)) {
                        PRESETS[preset] ?: bands
                    } else {
                        bands
                    }

                    val numBands = eq.numberOfBands.toInt()
                    val range = eq.bandLevelRange
                    val minLevel = range?.getOrNull(0) ?: -1500
                    val maxLevel = range?.getOrNull(1) ?: 1500

                    for (i in 0 until numBands) {
                        val dbVal = targetBands.getOrNull(i) ?: 0
                        val mbVal = (dbVal * 100).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                        eq.setBandLevel(i.toShort(), mbVal)
                    }
                }
            }.onFailure { Log.w(TAG, "Error applying Equalizer: ${it.message}") }
        }
    }

    private fun applyBassBoostWithCompensation(
        bassEnabled: Boolean,
        bassStrength: Int,
        loudnessEnabled: Boolean,
        loudnessGainMb: Int
    ) {
        bassBoost?.let { bb ->
            runCatching {
                bb.enabled = bassEnabled
                if (bassEnabled && bb.strengthSupported) {
                    bb.setStrength(bassStrength.coerceIn(0, 1000).toShort())
                }
            }.onFailure { Log.w(TAG, "Error applying BassBoost: ${it.message}") }
        }

        // Automatic volume makeup compensation to prevent Android AGC from crushing the sound
        loudnessEnhancer?.let { le ->
            runCatching {
                val effectiveGain = if (bassEnabled && bassStrength > 0) {
                    // Makeup gain between 150mB and 450mB based on bass strength
                    val makeup = (bassStrength * 0.45f).toInt()
                    if (loudnessEnabled) loudnessGainMb + makeup else makeup
                } else if (loudnessEnabled) {
                    loudnessGainMb
                } else {
                    0
                }

                if (effectiveGain > 0) {
                    le.enabled = true
                    le.setTargetGain(effectiveGain.coerceIn(0, 2000))
                } else {
                    le.enabled = false
                }
            }.onFailure { Log.w(TAG, "Error applying LoudnessEnhancer compensation: ${it.message}") }
        }
    }

    private fun applyVirtualizer(enabled: Boolean, strength: Int) {
        virtualizer?.let { virt ->
            runCatching {
                virt.enabled = enabled
                if (enabled && virt.strengthSupported) {
                    virt.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }.onFailure { Log.w(TAG, "Error applying Virtualizer: ${it.message}") }
        }
    }

    fun releaseEffects() {
        syncJob?.cancel()
        syncJob = null

        runCatching { equalizer?.release() }
        equalizer = null

        runCatching { bassBoost?.release() }
        bassBoost = null

        runCatching { virtualizer?.release() }
        virtualizer = null

        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null

        currentSessionId = 0
    }
}
