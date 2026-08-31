package com.xvox.music

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xvox.music.features.setup.SetupScreen
import com.xvox.music.startup.StartupScreen
import com.xvox.music.startup.StartupState

@Composable
fun XvoxApp() {
    var showStartup by remember {
        mutableStateOf(
            !StartupState.animationShown
        )
    }

    if (showStartup) {
        StartupScreen(
            onFinished = {
                showStartup = false
            }
        )
    } else {
        SetupScreen(
            onSetupComplete = {
            }
        )
    }
}
