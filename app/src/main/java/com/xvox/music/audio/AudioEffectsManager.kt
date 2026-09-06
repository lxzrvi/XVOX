package com.xvox.music.audio

import android.content.Context
import android.media.audiofx.Equalizer
import android.util.Log
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
object AudioEffectsManager {
    private const val TAG = "AudioEffectsManager"

    private var currentSessionId: Int = 0
    private var equalizer: Equalizer? = null

    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

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

        startObservingPreferences(context)
    }

    private fun applyAllCurrent(context: Context) {
        scope.launch {
            val prefs = UserPreferencesRepository(context)
            val eqEn = prefs.equalizerEnabled.first()
            val eqPr = prefs.eqPreset.first()
            val eqB = prefs.eqBands.first()
            applyEqualizer(eqEn, eqPr, eqB)
        }
    }

    private fun startObservingPreferences(context: Context) {
        syncJob?.cancel()
        val prefs = UserPreferencesRepository(context)

        syncJob = scope.launch {
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

    fun releaseEffects() {
        syncJob?.cancel()
        syncJob = null

        runCatching { equalizer?.release() }
        equalizer = null

        currentSessionId = 0
    }
}
