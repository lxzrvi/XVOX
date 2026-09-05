package com.xvox.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {

    private val _state =
        MutableStateFlow<AppUiState>(
            AppUiState.Loading,
        )

    val state: StateFlow<AppUiState> =
        _state.asStateFlow()

    init {
        viewModelScope.launch {
            delay(5000L)

            _state.value =
                AppUiState.Home
        }
    }
}
