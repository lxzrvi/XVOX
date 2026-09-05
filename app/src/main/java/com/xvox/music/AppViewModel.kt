package com.xvox.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val preferencesRepository =
        UserPreferencesRepository(application)

    private val _state =
        MutableStateFlow<AppUiState>(
            AppUiState.Loading
        )

    val state: StateFlow<AppUiState> =
        _state.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            // Settle time for complete UI initialization before fading in
            delay(1200L)
            _state.value = if (prefs.setupCompleted) AppUiState.Home else AppUiState.Setup
        }
    }
}
