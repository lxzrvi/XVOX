package com.xvox.music.audio

import android.content.Context
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveEqState(
    val enabled: Boolean,
    val preset: String,
    val bands: List<Int>,
    val headroomDb: Float,
    val balance: Float,
    val surroundEnabled: Boolean,
    val surroundDepth: Float,
    val orbitSeconds: Int,
    val appVolume: Float,
    val volumeLimit: Float,
    val bandCount: Int = 5,
    val noiseReduction: Float = 0f,
    val softenHighs: Float = 0f,
    val surroundWidth: Float = .78f,
    val surroundPosition: Float = 0f,
    val roomAmount: Float = .5f,
    val reverbAmount: Float = 0f,
    val hrtf: Float = .6f,
    val centerPreservation: Float = 0f,
    val revision: Long = 0,
    /** Room identity is independent from the wet amount below it. */
    val reverbPreset: String = if (reverbAmount > .001f) "Medium Room" else ReverbPresets.OFF,
    val noiseReductionEnabled: Boolean = noiseReduction > .001f,
    val grainControlEnabled: Boolean = softenHighs > .001f,
    /** The separately retained manual curve, independent from [bands] while presets are active. */
    val customBands: List<Int> = bands
) {
    fun applyTo(saved: AudioDspSettings) = saved.copy(
        equalizerEnabled = enabled,
        bands = bands.map { it.toFloat() },
        bandCount = bandCount,
        noiseReduction = noiseReduction,
        softenHighs = softenHighs,
        headroomDb = headroomDb,
        balance = balance,
        surroundEnabled = surroundEnabled,
        surroundDepth = surroundDepth,
        orbitSeconds = orbitSeconds.toFloat(),
        masterVolume = (appVolume * volumeLimit).coerceIn(0f, 2f),
        surroundWidth = surroundWidth,
        surroundPosition = surroundPosition,
        roomAmount = roomAmount,
        reverbAmount = reverbAmount,
        hrtf = hrtf,
        centerPreservation = centerPreservation,
        reverbPreset = ReverbPresets.normalize(reverbPreset),
        noiseReductionEnabled = noiseReductionEnabled,
        grainControlEnabled = grainControlEnabled
    )
}

/** Preset catalogue. DSP lives in the PCM pipeline, never in a repeatedly re-created hardware EQ. */
object AudioEffectsManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _liveEq = MutableStateFlow<LiveEqState?>(null)
    val liveEq = _liveEq.asStateFlow()
    private val _persistenceError = MutableStateFlow<String?>(null)
    val persistenceError = _persistenceError.asStateFlow()
    private var writer: Job? = null
    private var revision = 0L

    /** Immediate in-memory audio update; coalesce slider persistence instead of writing per pixel. */
    @Synchronized
    fun submit(context: Context, controls: LiveEqState) {
        val state = controls.copy(
            preset = normalizeEqPreset(controls.preset),
            reverbPreset = ReverbPresets.normalize(controls.reverbPreset),
            bands = List(EqBands.count(controls.bandCount)) { controls.bands.getOrElse(it) { 0 }.coerceIn(-12, 12) },
            customBands = EqBands.convert(controls.customBands, 5),
            revision = ++revision
        )
        _liveEq.value = state
        _persistenceError.value = null
        writer?.cancel()
        val appContext = context.applicationContext
        writer = scope.launch {
            delay(140)
            try {
                UserPreferencesRepository(appContext).setAudioState(state)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: java.io.IOException) {
                _persistenceError.value = "Audio changes are active, but could not be saved. Check device storage."
            }
        }
    }

    fun clearIfPersisted(saved: AudioDspSettings) {
        val pending = _liveEq.value ?: return
        if (pending.applyTo(saved) == saved) {
            _liveEq.compareAndSet(pending, null)
        }
    }

    /** The compact equalizer's visible preset order. Custom is selected when a band is edited. */
    val EQ_PRESET_NAMES: List<String> = listOf(
        "Custom",
        "Flat",
        "Bass Boost",
        "Bass Reducer",
        "Treble Boost",
        "Treble Reducer",
        "Vocal Boost",
        "Rock",
        "Pop",
        "Jazz",
        "Electronic",
        "Dance",
        "Acoustic"
    )

    val PRESETS: Map<String, List<Int>> = linkedMapOf(
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Bass Boost" to listOf(7, 5, 2, 0, -1),
        "Bass Reducer" to listOf(-7, -5, -2, 0, 1),
        "Treble Boost" to listOf(-2, 0, 2, 5, 7),
        "Treble Reducer" to listOf(2, 0, -2, -5, -7),
        "Vocal Boost" to listOf(-3, 1, 6, 4, 1),
        "Rock" to listOf(5, 3, -1, 3, 5),
        "Pop" to listOf(-1, 3, 5, 3, -1),
        "Jazz" to listOf(4, 2, -1, 2, 4),
        "Electronic" to listOf(6, 4, 0, 3, 5),
        "Dance" to listOf(6, 4, 1, 5, 6),
        "Acoustic" to listOf(3, 2, 0, 2, 3)
    )

    /** Maps previous shipped labels without exposing duplicate chips in the redesigned UI. */
    fun normalizeEqPreset(value: String?): String = when (value) {
        "Treble" -> "Treble Boost"
        "Vocal" -> "Vocal Boost"
        "Custom" -> "Custom"
        else -> value?.takeIf { it in PRESETS } ?: "Flat"
    }
}
