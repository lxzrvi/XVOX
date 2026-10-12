package com.xvox.music

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.design.theme.XvoxThemeMode
import com.xvox.music.core.ui.XvoxStartupLoadingScreen
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.haptics.rememberXvoxHaptics
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
    val minimumReady by viewModel.minimumReady.collectAsState()
    val startupProgress by viewModel.progress.collectAsState()
    val preparing = state == AppUiState.Preparing || state == AppUiState.Home
    val homeVm: com.xvox.music.features.home.HomeViewModel? =
        if (preparing) androidx.lifecycle.viewmodel.compose.viewModel() else null
    val playerVm: com.xvox.music.player.playback.MainPlayerViewModel? =
        if (preparing) androidx.lifecycle.viewmodel.compose.viewModel() else null
    val library = homeVm?.state?.collectAsState()?.value
    val player = playerVm?.state?.collectAsState()?.value

    // Progress remains tied to useful work, but its visual release never waits indefinitely for a
    // media/provider callback. Home itself continues to populate reactively after it is visible.
    LaunchedEffect(library?.songs?.isNotEmpty()) {
        if (library?.songs?.isNotEmpty() == true) viewModel.report(0.42f, "Sorting your songs")
    }
    LaunchedEffect(library?.startupReady) {
        if (library?.startupReady == true) viewModel.report(0.68f, "Warming up artwork")
    }
    LaunchedEffect(player?.connected) {
        if (player?.connected == true) viewModel.report(0.80f, "Connecting playback")
    }

    // Mount the real shell behind the loader as soon as the minimum startup gate opens. Waiting
    // for a second, decorative delay was the source of the perceptible release lag and could pin
    // first launch on a slow provider. One rendered frame is enough to prevent a half-built reveal.
    var shellMounted by remember { mutableStateOf(false) }
    LaunchedEffect(minimumReady, preparing) {
        if (preparing && minimumReady && !shellMounted) {
            shellMounted = true
            withFrameNanos { }
        }
    }
    var startupSequenceReleased by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == AppUiState.Setup) {
            shellMounted = false
            startupSequenceReleased = false
        }
    }

    val overlays = remember { XvoxOverlayController() }
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context.applicationContext) }
    val themeStr by prefs.theme.collectAsState(initial = "System")
    val accentStr by prefs.accentColor.collectAsState(initial = "Red")
    val accentPreview = com.xvox.music.core.design.theme.XvoxAccentPreview.value
    val backgroundStr by prefs.themeBackground.collectAsState(initial = "Default")
    val backgroundImage by prefs.themeBackgroundImage.collectAsState(initial = "")
    val cardTransparency by prefs.cardTransparency.collectAsState(initial = 0f)
    val fontScale by prefs.fontSizeScale.collectAsState(initial = .90f)
    val fontFamilyKey by prefs.fontFamily.collectAsState(initial = "inter")
    val chrome by prefs.chromeStyle.collectAsState(initial = com.xvox.music.core.ui.chrome.XvoxChromeStyle())
    val chromePreview = com.xvox.music.core.ui.chrome.XvoxChromePreview.value
    LaunchedEffect(chrome, chromePreview) {
        if (chromePreview != null && chromePreview == chrome) {
            com.xvox.music.core.ui.chrome.XvoxChromePreview.clearWhenPersisted(chrome)
        }
    }
    val effectiveChrome = chromePreview ?: chrome
    val backgroundBrightness by prefs.backgroundBrightness.collectAsState(initial = 0.8f)
    val hapticFeedbackEnabled by prefs.hapticFeedbackEnabled.collectAsState(initial = true)
    val hapticIntensity by prefs.hapticIntensity.collectAsState(initial = "medium")

    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        onDispose { }
    }

    LaunchedEffect(accentStr, accentPreview) {
        if (accentPreview != null && accentPreview == accentStr) {
            com.xvox.music.core.design.theme.XvoxAccentPreview.clearWhenPersisted(accentStr)
        }
    }
    val effectiveAccent = accentPreview ?: accentStr
    val mode = when (themeStr) {
        "Light" -> XvoxThemeMode.LIGHT
        "Dark" -> XvoxThemeMode.DARK
        "AMOLED" -> XvoxThemeMode.AMOLED
        else -> XvoxThemeMode.SYSTEM
    }

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity.density, fontScale) {
        Density(density = currentDensity.density, fontScale = fontScale)
    }
    val selectedUiFont = remember(fontFamilyKey) {
        com.xvox.music.core.design.theme.xvoxUiFontFor(fontFamilyKey)
    }

    XvoxTheme(
        mode = mode,
        accent = effectiveAccent,
        background = backgroundStr,
        cardTransparency = cardTransparency,
        cardBorder = effectiveChrome.cardBorder,
        cardBorderAlpha = effectiveChrome.cardBorderAlpha,
        uiFontFamily = selectedUiFont
    ) {
        val haptics = rememberXvoxHaptics(enabled = hapticFeedbackEnabled, strength = hapticIntensity)
        CompositionLocalProvider(
            LocalDensity provides customDensity,
            LocalXvoxOverlayController provides overlays,
            com.xvox.music.core.ui.chrome.LocalXvoxBaseChromeStyle provides effectiveChrome,
            com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle provides effectiveChrome,
            LocalXvoxHaptics provides haptics
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(XvoxTheme.colors.background)
            ) {
                // The ordinary user-selected theme image remains a normal background with no
                // root capture or app-wide effect processing.
                if (backgroundImage.isNotBlank()) {
                    AsyncImage(
                        model = Uri.parse(backgroundImage),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

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
                        XvoxStartupLoadingScreen(
                            readyToEnter = shellMounted,
                            progress = startupProgress,
                            onSequenceComplete = {
                                if (!startupSequenceReleased && shellMounted) {
                                    startupSequenceReleased = true
                                    viewModel.report(1f, "Ready")
                                    viewModel.onHomeReady()
                                }
                            }
                        )
                    }
                }

                XvoxOverlayHost(controller = overlays, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
