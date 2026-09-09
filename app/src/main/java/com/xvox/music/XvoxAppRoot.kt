package com.xvox.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.design.theme.XvoxThemeMode
import com.xvox.music.core.ui.XvoxStartupLoadingScreen
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayHost
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.setup.SetupScreen
import kotlinx.coroutines.delay

@Composable
fun XvoxAppRoot(
    viewModel: AppViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val minimumReady by viewModel.minimumReady.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val stage by viewModel.stage.collectAsState()
    val preparing = state == AppUiState.Preparing || state == AppUiState.Home
    val homeVm: com.xvox.music.features.home.HomeViewModel? = if (preparing) androidx.lifecycle.viewmodel.compose.viewModel() else null
    val playerVm: com.xvox.music.player.playback.MainPlayerViewModel? = if (preparing) androidx.lifecycle.viewmodel.compose.viewModel() else null
    val library = homeVm?.state?.collectAsState()?.value
    val player = playerVm?.state?.collectAsState()?.value

    // Report each real milestone so the bar advances for a reason, not on a timer.
    LaunchedEffect(library?.songs?.isNotEmpty()) {
        if (library?.songs?.isNotEmpty() == true) viewModel.report(0.42f, "Sorting your songs")
    }
    LaunchedEffect(library?.startupReady) {
        if (library?.startupReady == true) viewModel.report(0.68f, "Warming up artwork")
    }
    LaunchedEffect(player?.connected) {
        if (player?.connected == true) viewModel.report(0.80f, "Connecting playback")
    }

    val dataReady = minimumReady && library?.startupReady == true &&
        player?.connected == true

    // Mount the shell UNDER the loading screen first. Loading only lifts once the real layout
    // has actually been measured and drawn, so Home is never revealed half-built.
    var shellMounted by remember { mutableStateOf(false) }
    LaunchedEffect(dataReady) { if (dataReady) shellMounted = true }
    LaunchedEffect(shellMounted) {
        if (!shellMounted) return@LaunchedEffect
        viewModel.report(0.93f, "Building your Home")
        // Three frames: compose, measure/place, first draw of the mosaic pages.
        repeat(3) { withFrameNanos { } }
        delay(60)
        viewModel.report(1f, "Ready")
        viewModel.onHomeReady()
    }

    val overlays = remember { XvoxOverlayController() }
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context.applicationContext) }
    val themeStr by prefs.theme.collectAsState(initial = "System")
    val accentStr by prefs.accentColor.collectAsState(initial = "Red")
    val backgroundStr by prefs.themeBackground.collectAsState(initial = "Default")
    val cardTransparency by prefs.cardTransparency.collectAsState(initial = 0f)
    val fontScale by prefs.fontSizeScale.collectAsState(initial = 1.0f)
    val chrome by prefs.chromeStyle.collectAsState(initial = com.xvox.music.core.ui.chrome.XvoxChromeStyle())
    val backgroundBrightness by prefs.backgroundBrightness.collectAsState(initial = 0.8f)

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

    XvoxTheme(
        mode = mode,
        accent = accentStr,
        background = backgroundStr,
        cardTransparency = cardTransparency,
        cardBorder = chrome.cardBorder,
        cardBorderAlpha = chrome.cardBorderAlpha
    ) {
        CompositionLocalProvider(
            LocalDensity provides customDensity,
            LocalXvoxOverlayController provides overlays,
            com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle provides chrome
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (state == AppUiState.Setup) {
                    SetupScreen(onSetupComplete = { viewModel.onSetupFinished() })
                } else {
                    if (shellMounted && homeVm != null && playerVm != null) {
                        XvoxMainShell(homeVm, playerVm, backgroundBrightness = backgroundBrightness)
                    }

                    AnimatedVisibility(
                        visible = state != AppUiState.Home,
                        enter = fadeIn(tween(120)),
                        exit = fadeOut(tween(260))
                    ) {
                        XvoxStartupLoadingScreen(progress = progress, stage = stage)
                    }
                }

                XvoxOverlayHost(controller = overlays, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
