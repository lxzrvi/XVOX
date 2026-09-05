package com.xvox.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.design.theme.XvoxThemeMode
import com.xvox.music.core.ui.XvoxStartupLoadingScreen
import com.xvox.music.core.ui.effects.LocalLiveBlurEnabled
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayHost
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.setup.SetupScreen

@Composable
fun XvoxAppRoot(
    viewModel: AppViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val overlays = remember { XvoxOverlayController() }
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context.applicationContext) }
    val themeStr by prefs.theme.collectAsState(initial = "System")
    val accentStr by prefs.accentColor.collectAsState(initial = "Default")
    val liveBlur by prefs.liveBlur.collectAsState(initial = true)
    val fontScale by prefs.fontSizeScale.collectAsState(initial = 1.0f)

    val mode = when (themeStr) {
        "Light" -> XvoxThemeMode.LIGHT
        "Dark" -> XvoxThemeMode.DARK
        "AMOLED" -> XvoxThemeMode.AMOLED
        else -> XvoxThemeMode.SYSTEM
    }

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity.density, fontScale) {
        Density(
            density = currentDensity.density,
            fontScale = fontScale
        )
    }

    XvoxTheme(mode = mode, accent = accentStr) {
        CompositionLocalProvider(
            LocalDensity provides customDensity,
            LocalXvoxOverlayController provides overlays,
            LocalLiveBlurEnabled provides liveBlur
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(320)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "app_root_state_transition",
                    modifier = Modifier.fillMaxSize()
                ) { targetState ->
                    when (targetState) {
                        AppUiState.Loading -> {
                            XvoxStartupLoadingScreen()
                        }
                        AppUiState.Setup -> {
                            SetupScreen(onSetupComplete = {})
                        }
                        AppUiState.Home -> {
                            XvoxMainShell()
                        }
                    }
                }
                XvoxOverlayHost(controller = overlays, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
