package com.xvox.music

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.design.theme.XvoxThemeMode
import com.xvox.music.core.design.theme.forExperimentalAppearance
import com.xvox.music.core.ui.XvoxStartupLoadingScreen
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.haptics.rememberXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayHost
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.setup.SetupScreen
import coil3.compose.AsyncImage
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import kotlinx.coroutines.delay

private fun Color.xvoxMix(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = alpha + (other.alpha - alpha) * t
    )
}

/** A colourful but theme-tethered source for Prismal's app-wide Blur appearance. */
private fun xvoxLiquidAppBrush(
    background: Color,
    accent: Color,
    phase: Float,
    alpha: Float = 1f
): Brush {
    val violetAccent = accent.xvoxMix(Color(0xFF7867FF), .38f)
    val coralAccent = accent.xvoxMix(Color(0xFFFF7A9D), .22f)
    val travel = phase.coerceIn(0f, 1f)
    return Brush.linearGradient(
        colors = listOf(
            background.xvoxMix(coralAccent, .56f).copy(alpha = alpha),
            background.xvoxMix(violetAccent, .48f).copy(alpha = alpha),
            background.xvoxMix(accent, .36f).copy(alpha = alpha)
        ),
        // An unhurried diagonal drift keeps the source visibly liquid without a high-frequency
        // animation. Prismal samples each rendered frame, so every glass layer follows it live.
        start = Offset(-420f + 780f * travel, 120f - 80f * travel),
        end = Offset(1240f - 500f * travel, 2140f + 190f * travel)
    )
}

@Composable
fun XvoxAppRoot(
    viewModel: AppViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val minimumReady by viewModel.minimumReady.collectAsState()
    val startupProgress by viewModel.progress.collectAsState()
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
        // Keep the real preparation work under the staged loading surface long enough for the
        // shell to compose, measure, and prime its first artwork/layout frame.
        repeat(3) { withFrameNanos { } }
        delay(60)
    }
    var startupSequenceReleased by remember { mutableStateOf(false) }
    var startupFallbackRelease by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == AppUiState.Setup) {
            startupSequenceReleased = false
            startupFallbackRelease = false
            shellMounted = false
        }
    }
    // A slow/misbehaving media scan must not pin a finished shell behind a decorative loader
    // forever. Once the shell is safe to compose, reveal it after a bounded grace period and let
    // the existing reactive data flows finish in place.
    LaunchedEffect(state, shellMounted, dataReady) {
        if (state != AppUiState.Home && shellMounted && !dataReady) {
            delay(6_000)
            if (state != AppUiState.Home && shellMounted && !dataReady) {
                startupFallbackRelease = true
            }
        } else if (dataReady) {
            startupFallbackRelease = false
        }
    }

    val overlays = remember { XvoxOverlayController() }
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context.applicationContext) }
    val themeStr by prefs.theme.collectAsState(initial = "System")
    val accentStr by prefs.accentColor.collectAsState(initial = "Red")
    // Snapshot-backed preview updates in the pointer frame; it avoids Flow collection latency
    // while the HSV wheel or brightness rail is being dragged.
    val accentPreview = com.xvox.music.core.design.theme.XvoxAccentPreview.value
    val backgroundStr by prefs.themeBackground.collectAsState(initial = "Default")
    val backgroundImage by prefs.themeBackgroundImage.collectAsState(initial = "")
    val cardTransparency by prefs.cardTransparency.collectAsState(initial = 0f)
    val experimentalAppearanceRaw by prefs.experimentalAppearance.collectAsState(initial = "default")
    val experimentalAppearance = com.xvox.music.core.design.theme.XvoxExperimentalAppearance.fromStorage(experimentalAppearanceRaw)
    val fontScale by prefs.fontSizeScale.collectAsState(initial = .90f)
    val fontFamilyKey by prefs.fontFamily.collectAsState(initial = "inter")
    val chrome by prefs.chromeStyle.collectAsState(initial = com.xvox.music.core.ui.chrome.XvoxChromeStyle())
    val chromePreview = com.xvox.music.core.ui.chrome.XvoxChromePreview.value
    LaunchedEffect(chrome, chromePreview) {
        if (chromePreview != null && chromePreview == chrome) {
            com.xvox.music.core.ui.chrome.XvoxChromePreview.clearWhenPersisted(chrome)
        }
    }
    // Keep a raw local for editors/persistence and derive the temporary Blur UI chrome only at
    // the root, so changing visual treatment never overwrites Default-mode knobs.
    val baseChrome = chromePreview ?: chrome
    val effectiveChrome = baseChrome.forExperimentalAppearance(experimentalAppearance)
    val backgroundBrightness by prefs.backgroundBrightness.collectAsState(initial = 0.8f)
    val hapticFeedbackEnabled by prefs.hapticFeedbackEnabled.collectAsState(initial = true)
    val hapticIntensity by prefs.hapticIntensity.collectAsState(initial = "medium")

    val view = androidx.compose.ui.platform.LocalView.current
    // Status chrome returns to the ordinary app baseline. The Settings switch was removed, so
    // page-specific fullscreen surfaces may manage their temporary state without a global lock.
    androidx.compose.runtime.DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        onDispose { }
    }

    LaunchedEffect(accentStr, accentPreview) {
        // Once DataStore catches up, let its persisted value become the source of truth again.
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
    // Text size is the visible typography scale control; the retired Display size preference is
    // retained only for compatibility records.
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
        experimentalAppearance = experimentalAppearance,
        uiFontFamily = selectedUiFont
    ) {
        val haptics = rememberXvoxHaptics(enabled = hapticFeedbackEnabled, strength = hapticIntensity)
        val liquidBlurEnabled = experimentalAppearance ==
            com.xvox.music.core.design.theme.XvoxExperimentalAppearance.BLUR
        // Blur is not a black fallback: Prismal samples this live, colourful app gradient (or
        // the user's image plus a translucent colour wash) behind every eligible glass surface.
        val liquidPhase by rememberInfiniteTransition(label = "xvoxLiquidBackdrop").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 10_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "xvoxLiquidBackdropPhase"
        )
        val liquidAppBrush = remember(
            XvoxTheme.colors.background,
            XvoxTheme.colors.primaryAccent,
            liquidPhase
        ) {
            xvoxLiquidAppBrush(XvoxTheme.colors.background, XvoxTheme.colors.primaryAccent, liquidPhase)
        }
        val liquidImageWash = remember(
            XvoxTheme.colors.background,
            XvoxTheme.colors.primaryAccent,
            liquidPhase
        ) {
            xvoxLiquidAppBrush(
                XvoxTheme.colors.background,
                XvoxTheme.colors.primaryAccent,
                phase = liquidPhase,
                alpha = .34f
            )
        }
        // Prismal samples one root-recorded layer behind the app chrome. Recording only mounts
        // when a translucent surface can actually reveal it, avoiding a per-card capture cost and
        // the old Home source-readiness race.
        val defaultBackdropCanShowThrough = backgroundImage.isNotBlank() || cardTransparency > .005f
        val prismalSourceEnabled = liquidBlurEnabled || defaultBackdropCanShowThrough
        val prismalBackdrop = rememberPrismalGlassLayer()

        CompositionLocalProvider(
            LocalDensity provides customDensity,
            LocalXvoxOverlayController provides overlays,
            com.xvox.music.core.ui.chrome.LocalXvoxBaseChromeStyle provides baseChrome,
            com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle provides effectiveChrome,
            com.xvox.music.core.ui.effects.LocalXvoxPrismalBackdrop provides if (prismalSourceEnabled) prismalBackdrop else null,
            LocalXvoxHaptics provides haptics
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (liquidBlurEnabled) Modifier.background(liquidAppBrush)
                        else Modifier.background(XvoxTheme.colors.background)
                    )
            ) {
                // Prismal follows its documented backdrop/effect sibling arrangement. This layer
                // records the actual wallpaper/theme background only; sheets and controls draw
                // above it, so liquid glass cannot recursively sample itself.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (prismalSourceEnabled) Modifier.prismalGlassLayer(prismalBackdrop) else Modifier)
                        .then(
                            if (liquidBlurEnabled) Modifier.background(liquidAppBrush)
                            else Modifier.background(XvoxTheme.colors.background)
                        )
                ) {
                    if (backgroundImage.isNotBlank()) {
                        AsyncImage(
                            model = Uri.parse(backgroundImage),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (liquidBlurEnabled) {
                            // Preserve the selected image while bathing it in the same animated-
                            // feeling palette that supplies Prismal's live glass source.
                            Box(Modifier.fillMaxSize().background(liquidImageWash))
                        }
                    }
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
                            readyToEnter = shellMounted && (dataReady || startupFallbackRelease),
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
