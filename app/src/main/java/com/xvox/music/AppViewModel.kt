package com.xvox.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Startup is gated on real work, never on a stopwatch.
 *
 * Loading stays up until the whole Home layout is genuinely ready, but it can never look stuck:
 * every prepared stage moves a determinate bar, an idle creep keeps the bar alive while a slow
 * stage runs, and a hard ceiling releases the UI even if one stage never reports back.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        /** Nothing may hold the loading screen longer than this, whatever happens. */
        const val HARD_CEILING_MS = 9000L

        /** Enough for the shell to lay out once, so Home never appears half-drawn. */
        const val MIN_VISIBLE_MS = 450L
    }

    private val prefs = UserPreferencesRepository(application)
    private val _state = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private val _minimumReady = MutableStateFlow(false)
    val minimumReady: StateFlow<Boolean> = _minimumReady.asStateFlow()

    /** 0..1, monotonic. Drives the determinate startup bar. */
    private val _progress = MutableStateFlow(0.04f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _stage = MutableStateFlow("Starting XVOX")
    val stage: StateFlow<String> = _stage.asStateFlow()

    private var released = false

    init {
        viewModelScope.launch { delay(MIN_VISIBLE_MS); _minimumReady.value = true }

        // Idle creep: the bar always advances towards, but never reaches, the next milestone.
        viewModelScope.launch {
            while (isActive && !released) {
                delay(90)
                val target = 0.94f
                val current = _progress.value
                if (current < target) _progress.value = current + (target - current) * 0.012f
            }
        }

        // Absolute ceiling: a stage that never answers cannot freeze the app on the logo.
        viewModelScope.launch {
            delay(HARD_CEILING_MS)
            if (!released && _state.value == AppUiState.Preparing) {
                _stage.value = "Almost there"
                _minimumReady.value = true
                forceHome()
            }
        }

        viewModelScope.launch {
            val isCompleted = runCatching { prefs.preferences.first().setupCompleted }.getOrDefault(false)
            if (isCompleted) {
                report(0.12f, "Reading your library")
                _state.value = AppUiState.Preparing
            } else {
                released = true
                _state.value = AppUiState.Setup
            }
        }
    }

    /** Called as each startup stage completes; progress only ever moves forward. */
    fun report(value: Float, text: String? = null) {
        if (released) return
        if (value > _progress.value) _progress.value = value.coerceIn(0f, 1f)
        if (text != null) _stage.value = text
    }

    /** Every stage reported ready and the shell has had a frame to lay out. */
    fun onHomeReady() {
        if (_state.value == AppUiState.Preparing && _minimumReady.value) forceHome()
    }

    private fun forceHome() {
        if (_state.value != AppUiState.Preparing) return
        released = true
        _progress.value = 1f
        _stage.value = "Ready"
        _state.value = AppUiState.Home
    }

    fun onSetupFinished() {
        released = false
        _progress.value = 0.1f
        _stage.value = "Preparing your library"
        _minimumReady.value = false
        viewModelScope.launch { delay(MIN_VISIBLE_MS); _minimumReady.value = true }
        viewModelScope.launch {
            delay(HARD_CEILING_MS)
            if (!released) { _minimumReady.value = true; forceHome() }
        }
        _state.value = AppUiState.Preparing
    }
}
