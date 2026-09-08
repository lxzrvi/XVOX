package com.xvox.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferencesRepository(application)
    private val _state = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private val _minimumReady = MutableStateFlow(false)
    val minimumReady: StateFlow<Boolean> = _minimumReady.asStateFlow()
    init {
        viewModelScope.launch { delay(5000L); _minimumReady.value = true }
        viewModelScope.launch {
            val isCompleted = runCatching { prefs.preferences.first().setupCompleted }.getOrDefault(false)
            if (isCompleted) {
                _state.value = AppUiState.Preparing
            } else {
                _state.value = AppUiState.Setup
            }
        }
    }

    fun onHomeReady() { if (_state.value == AppUiState.Preparing && _minimumReady.value) _state.value = AppUiState.Home }

    fun onSetupFinished() {
        _state.value = AppUiState.Preparing
    }
}
