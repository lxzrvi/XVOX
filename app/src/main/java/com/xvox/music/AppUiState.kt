package com.xvox.music

sealed interface AppUiState {

    data object Loading : AppUiState

    data object Setup : AppUiState

    data object Home : AppUiState
}
