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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.xvox.music.artwork.XvoxArtworkPreloader
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.design.theme.XvoxThemeMode
import com.xvox.music.core.ui.XvoxStartupLoadingScreen
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.haptics.rememberXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayHost
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.home.XvoxGridArtworkSize
import com.xvox.music.features.home.XvoxRecentArtworkSize
import com.xvox.music.features.setup.SetupScreen

@Composable
fun XvoxAppRoot(
    viewModel: AppViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val minimumReady by viewModel.minimumReady.collectAsState()
    val startupProgress by viewModel.progress.collectAsState()
    // Home and playback work begin during Preparing, but the shell itself is mounted only at the
    // loader's terminal frame. That places the ready Home surface underneath the unchanged loader
    // before it fades, so the reveal never exposes a black/blank compositor frame.
    var shellMounted by remember { mutableStateOf(false) }
    var startupSequenceReleased by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        when (state) {
            // This is also a safety net for process/state restoration. The usual handoff mounts
            // the shell just before onHomeReady below, while the loader is still fully visible.
            AppUiState.Home -> shellMounted = true
            AppUiState.Setup -> {
                shellMounted = false
                startupSequenceReleased = false
            }
            else -> shellMounted = false
        }
    }
    // ViewModels begin their IO/background work as soon as bootstrap enters Preparing. The loader
    // observes only this one terminal Home-ready flag, never the song/artwork stream, so MediaStore
    // and cover work can run concurrently without invalidating its animation frames.
    val bootstrapModels = state == AppUiState.Preparing || state == AppUiState.Home
    val homeVm: com.xvox.music.features.home.HomeViewModel? =
        if (bootstrapModels) androidx.lifecycle.viewmodel.compose.viewModel() else null
    val playerVm: com.xvox.music.player.playback.MainPlayerViewModel? =
        if (bootstrapModels) androidx.lifecycle.viewmodel.compose.viewModel() else null
    val homeStartupReady by remember(homeVm) {
        homeVm?.state
            ?.map { it.startupReady }
            ?.distinctUntilChanged()
            ?: flowOf(false)
    }.collectAsState(initial = false)

    val overlays = remember { XvoxOverlayController() }
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context.applicationContext) }
    val rootScope = rememberCoroutineScope()
    var artworkWarmDone by remember { mutableStateOf(false) }
    val artworkPreloader = remember(context) { XvoxArtworkPreloader(context) }
    LaunchedEffect(homeStartupReady, homeVm) {
        artworkWarmDone = false
        val vm = homeVm
        if (homeStartupReady && vm != null) {
            val snapshot = vm.state.value
            // Start warming the Home runway while the completed loader remains opaque. The
            // handoff below waits only briefly, so slow/missing cover files never block Home.
            coroutineScope {
                launch {
                    artworkPreloader.warm(
                        songs = snapshot.songs,
                        fromIndex = 0,
                        count = 36,
                        requestSize = XvoxGridArtworkSize
                    )
                }
                launch {
                    artworkPreloader.warm(
                        songs = snapshot.recentlyPlayed,
                        fromIndex = 0,
                        count = 12,
                        requestSize = XvoxRecentArtworkSize
                    )
                }
            }
            artworkWarmDone = true
        }
    }
    val themeStr by prefs.theme.collectAsState(initial = "System")
    val accentStr by prefs.accentColor.collectAsState(initial = "Red")
    val accentPreview = com.xvox.music.core.design.theme.XvoxAccentPreview.value
    val backgroundStr by prefs.themeBackground.collectAsState(initial = "Default")
    val backgroundPreview = com.xvox.music.core.design.theme.XvoxBackgroundPreview.value
    val backgroundImage by prefs.themeBackgroundImage.collectAsState(initial = "")
    val cardTransparency by prefs.cardTransparency.collectAsState(initial = 0f)
    val cardTransparencyPreview = com.xvox.music.core.design.theme.XvoxCardTransparencyPreview.value
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
    LaunchedEffect(cardTransparency, cardTransparencyPreview) {
        if (cardTransparencyPreview != null) {
            com.xvox.music.core.design.theme.XvoxCardTransparencyPreview.clearWhenPersisted(cardTransparency)
        }
    }
    LaunchedEffect(backgroundStr, backgroundPreview) {
        if (backgroundPreview != null && backgroundPreview == backgroundStr) {
            com.xvox.music.core.design.theme.XvoxBackgroundPreview.clearWhenPersisted(backgroundStr)
        }
    }
    val effectiveAccent = accentPreview ?: accentStr
    val effectiveBackground = backgroundPreview ?: backgroundStr
    val effectiveCardTransparency = cardTransparencyPreview ?: cardTransparency
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
        background = effectiveBackground,
        cardTransparency = effectiveCardTransparency,
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
                        val loaderSequenceReady = state == AppUiState.Preparing && minimumReady
                        XvoxStartupLoadingScreen(
                            // The ring-to-bar clock starts on the original lightweight bootstrap
                            // signal. Home is already loading behind it; only the final Home reveal
                            // waits for that terminal readiness flag, never for noisy Home frames.
                            readyToEnter = loaderSequenceReady,
                            canEnterHome = loaderSequenceReady && homeStartupReady,
                            progress = startupProgress,
                            onSequenceComplete = {
                                if (
                                    !startupSequenceReleased &&
                                    state == AppUiState.Preparing &&
                                    minimumReady &&
                                    homeStartupReady
                                ) {
                                    startupSequenceReleased = true
                                    rootScope.launch {
                                        // Give the first Home runway a short opportunity to warm,
                                        // but always reveal the application promptly when artwork
                                        // is unavailable or the cache is cold.
                                        withTimeoutOrNull(700) {
                                            snapshotFlow { artworkWarmDone }.first { it }
                                        }
                                        // Mount Home while the loader is still a static opaque
                                        // frame, then wait for a few compose frames before the
                                        // normal loader fade begins.
                                        shellMounted = true
                                        repeat(3) { withFrameNanos { } }
                                        viewModel.report(1f, "Ready")
                                        viewModel.onHomeReady()
                                    }
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
