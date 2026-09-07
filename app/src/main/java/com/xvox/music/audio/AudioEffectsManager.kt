package com.xvox.music.audio

import android.content.Context
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveEqState(
    val enabled: Boolean, val preset: String, val bands: List<Int>,
    val headroomDb: Float, val balance: Float, val surroundEnabled: Boolean,
    val surroundDepth: Float, val orbitSeconds: Int, val appVolume: Float, val volumeLimit: Float,
    val revision: Long = 0
) {
    fun applyTo(saved: AudioDspSettings) = saved.copy(equalizerEnabled = enabled, bands = bands.map { it.toFloat() },
        headroomDb = headroomDb, balance = balance, surroundEnabled = surroundEnabled, surroundDepth = surroundDepth,
        orbitSeconds = orbitSeconds.toFloat(), masterVolume = (appVolume * volumeLimit).coerceIn(0f, 1f))
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
        val state = controls.copy(bands = List(5) { controls.bands.getOrElse(it) { 0 }.coerceIn(-12, 12) }, revision = ++revision)
        _liveEq.value = state
        _persistenceError.value = null
        writer?.cancel()
        val appContext = context.applicationContext
        writer = scope.launch {
            delay(140)
            try {
                UserPreferencesRepository(appContext).setAudioState(state)
            } catch (cancelled: CancellationException) { throw cancelled } catch (_: java.io.IOException) { _persistenceError.value = "Audio changes are active, but could not be saved. Check device storage." }
        }
    }

    fun clearIfPersisted(saved: AudioDspSettings) {
        val pending = _liveEq.value ?: return
        if (pending.applyTo(saved) == saved) {
            _liveEq.compareAndSet(pending, null)
        }
    }

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
}
