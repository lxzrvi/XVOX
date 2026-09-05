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
        "Bass Boost" to listOf(6, 4, 1, 0, -1),
        "Treble" to listOf(-1, 0, 1, 4, 6),
        "Rock" to listOf(4, 2, -1, 2, 4),
        "Pop" to listOf(-1, 2, 4, 2, -1),
        "Jazz" to listOf(3, 1, -1, 2, 3),
        "Electronic" to listOf(5, 3, 0, 2, 4),
        "Vocal" to listOf(-2, 1, 5, 3, 0)
    )

    fun attachAudioSession(sessionId: Int, context: Context) {
        if (sessionId <= 0) return
        if (currentSessionId == sessionId && equalizer != null) return

        releaseEffects()
        currentSessionId = sessionId

        runCatching {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
            }
        }.onFailure { Log.e(TAG, "Failed to init Equalizer: ${it.message}") }

        runCatching {
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
            }
        }.onFailure { Log.e(TAG, "Failed to init BassBoost: ${it.message}") }

        runCatching {
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = true
            }
        }.onFailure { Log.e(TAG, "Failed to init Virtualizer: ${it.message}") }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            runCatching {
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                    enabled = true
                }
            }.onFailure { Log.e(TAG, "Failed to init LoudnessEnhancer: ${it.message}") }
        }

        startObservingPreferences(context)
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

            // Bass Boost observer
            launch {
                combine(
                    prefs.bassBoost,
                    prefs.bassBoostStrength
                ) { enabled, strength ->
                    Pair(enabled, strength)
                }.collect { (enabled, strength) ->
                    applyBassBoost(enabled, strength)
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

            // Loudness Enhancer observer
            launch {
                combine(
                    prefs.loudnessEnhancer,
                    prefs.loudnessGainMb
                ) { enabled, gain ->
                    Pair(enabled, gain)
                }.collect { (enabled, gain) ->
                    applyLoudnessEnhancer(enabled, gain)
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
                        // 1 dB = 100 mB
                        val mbVal = (dbVal * 100).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                        eq.setBandLevel(i.toShort(), mbVal)
                    }
                }
            }.onFailure { Log.w(TAG, "Error applying Equalizer: ${it.message}") }
        }
    }

    private fun applyBassBoost(enabled: Boolean, strength: Int) {
        bassBoost?.let { bb ->
            runCatching {
                bb.enabled = enabled
                if (enabled && bb.strengthSupported) {
                    bb.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }.onFailure { Log.w(TAG, "Error applying BassBoost: ${it.message}") }
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

    private fun applyLoudnessEnhancer(enabled: Boolean, gainMb: Int) {
        loudnessEnhancer?.let { le ->
            runCatching {
                le.enabled = enabled
                if (enabled) {
                    le.setTargetGain(gainMb.coerceIn(0, 2000))
                }
            }.onFailure { Log.w(TAG, "Error applying LoudnessEnhancer: ${it.message}") }
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
