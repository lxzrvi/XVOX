package com.xvox.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayHost
import com.xvox.music.features.setup.SetupScreen

@Composable
fun XvoxAppRoot(
    viewModel: AppViewModel = viewModel()
) {
    val state by
        viewModel.state.collectAsState()

    val overlays =
        remember {
            XvoxOverlayController()
        }

    CompositionLocalProvider(
        LocalXvoxOverlayController
            provides overlays
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {
            when (state) {
                AppUiState.Loading ->
                    Unit

                AppUiState.Setup -> {
                    SetupScreen(
                        onSetupComplete = {}
                    )
                }

                AppUiState.Home -> {
                    XvoxMainShell()
                }
            }

            XvoxOverlayHost(
                controller = overlays,
                modifier =
                    Modifier.fillMaxSize()
            )
        }
    }
}
