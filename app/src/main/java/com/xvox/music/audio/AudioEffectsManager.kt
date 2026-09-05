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

    // Standard default bands in mB (-1500 to +1500)
    val PRESETS = mapOf(
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Bass Boost" to listOf(600, 400, 100, 0, -100),
        "Treble" to listOf(-100, 0, 100, 400, 600),
        "Rock" to listOf(400, 200, -100, 200, 400),
        "Pop" to listOf(-100, 200, 400, 200, -100),
        "Jazz" to listOf(300, 100, -100, 200, 300),
        "Electronic" to listOf(500, 300, 0, 200, 400),
        "Vocal" to listOf(-200, 100, 500, 300, 0)
    )

    fun attachAudioSession(sessionId: Int, context: Context) {
        if (sessionId <= 0) return
        if (currentSessionId == sessionId && equalizer != null) return

        releaseEffects()
        currentSessionId = sessionId

        runCatching {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = false
            }
        }.onFailure { Log.e(TAG, "Failed to init Equalizer: ${it.message}") }

        runCatching {
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = false
            }
        }.onFailure { Log.e(TAG, "Failed to init BassBoost: ${it.message}") }

        runCatching {
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = false
            }
        }.onFailure { Log.e(TAG, "Failed to init Virtualizer: ${it.message}") }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            runCatching {
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                    enabled = false
                }
            }.onFailure { Log.e(TAG, "Failed to init LoudnessEnhancer: ${it.message}") }
        }

        startObservingPreferences(context)
    }

    private fun startObservingPreferences(context: Context) {
        syncJob?.cancel()
        val prefs = UserPreferencesRepository(context)

        syncJob = scope.launch {
            combine(
                prefs.equalizerEnabled,
                prefs.eqPreset,
                prefs.eqBands,
                prefs.bassBoost,
                prefs.bassBoostStrength,
                prefs.virtualizerEnabled,
                prefs.virtualizerStrength,
                prefs.loudnessEnhancer,
                prefs.loudnessGainMb
            ) { eqEnabled, preset, bands, bbEnabled, bbStrength, virtEnabled, virtStrength, leEnabled, leGain ->
                AudioEffectParams(
                    eqEnabled = eqEnabled,
                    preset = preset,
                    bands = bands,
                    bbEnabled = bbEnabled,
                    bbStrength = bbStrength,
                    virtEnabled = virtEnabled,
                    virtStrength = virtStrength,
                    leEnabled = leEnabled,
                    leGain = leGain
                )
            }.collect { params ->
                applyParams(params)
            }
        }
    }

    private fun applyParams(params: AudioEffectParams) {
        // Equalizer
        equalizer?.let { eq ->
            runCatching {
                eq.enabled = params.eqEnabled
                if (params.eqEnabled) {
                    val targetBands = if (params.preset != "Custom" && PRESETS.containsKey(params.preset)) {
                        PRESETS[params.preset] ?: params.bands
                    } else {
                        params.bands
                    }

                    val numBands = eq.numberOfBands.toInt()
                    val range = eq.bandLevelRange // shortArray [min, max]
                    val minLevel = range?.getOrNull(0) ?: -1500
                    val maxLevel = range?.getOrNull(1) ?: 1500

                    for (i in 0 until numBands) {
                        val level = targetBands.getOrNull(i) ?: 0
                        val clamped = (level * 100).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                        eq.setBandLevel(i.toShort(), clamped)
                    }
                }
            }.onFailure { Log.w(TAG, "Error applying Equalizer: ${it.message}") }
        }

        // Bass Boost
        bassBoost?.let { bb ->
            runCatching {
                bb.enabled = params.bbEnabled
                if (params.bbEnabled && bb.strengthSupported) {
                    bb.setStrength(params.bbStrength.coerceIn(0, 1000).toShort())
                }
            }.onFailure { Log.w(TAG, "Error applying BassBoost: ${it.message}") }
        }

        // Virtualizer
        virtualizer?.let { virt ->
            runCatching {
                virt.enabled = params.virtEnabled
                if (params.virtEnabled && virt.strengthSupported) {
                    virt.setStrength(params.virtStrength.coerceIn(0, 1000).toShort())
                }
            }.onFailure { Log.w(TAG, "Error applying Virtualizer: ${it.message}") }
        }

        // Loudness Enhancer
        loudnessEnhancer?.let { le ->
            runCatching {
                le.enabled = params.leEnabled
                if (params.leEnabled) {
                    le.setTargetGain(params.leGain.coerceIn(0, 2000))
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

    private data class AudioEffectParams(
        val eqEnabled: Boolean,
        val preset: String,
        val bands: List<Int>,
        val bbEnabled: Boolean,
        val bbStrength: Int,
        val virtEnabled: Boolean,
        val virtStrength: Int,
        val leEnabled: Boolean,
        val leGain: Int
    )
}
