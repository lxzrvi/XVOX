package com.xvox.music

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.features.setup.SetupScreen

@Composable
fun XvoxAppRoot(
    viewModel: AppViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    when (state) {
        AppUiState.Loading -> Unit

        AppUiState.Setup -> {
            SetupScreen(
                onSetupComplete = {
                    // DataStore emission changes
                    // AppUiState automatically.
                }
            )
        }

        AppUiState.Home -> {
            XvoxMainShell()
        }
    }
}
