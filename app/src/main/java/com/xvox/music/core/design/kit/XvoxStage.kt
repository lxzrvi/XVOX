package com.xvox.music.core.design.kit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxGeneratedBackground
import com.xvox.music.core.ui.navigation.XvoxNavTab
import io.github.neoruaa.glasmio.glass.GlasmioBackdropProvider
import io.github.neoruaa.glasmio.glass.glassBackdropSource
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop

/**
 * Rebuild staging.
 *
 * The application surface is being rebuilt from the navigation bar outward, so the old screens stay
 * in the source tree but are not composed: only the gradient backdrop and the new iOS navigation
 * bar are visible. Nothing is deleted, so each surface can be switched back on later.
 */
object XvoxStage {

    /** Stage 1 - only the gradient background and the Glasmio navigation bar are visible. */
    const val OnlyBackgroundAndNavBar = true
}

/**
 * Each destination owns its gradient accent, so moving between tabs repaints the backdrop and the
 * liquid glass above it re-samples that new backdrop live.
 */
internal fun xvoxTabAccent(tab: XvoxNavTab, themeAccent: Color): Color = when (tab) {
    XvoxNavTab.HOME -> themeAccent
    XvoxNavTab.ARTIST -> Color(0xFFFF9F0A)
    XvoxNavTab.ALBUM -> Color(0xFF0A84FF)
    XvoxNavTab.PLAYLIST -> Color(0xFF30D158)
}

/** The visible stage: animated gradient backdrop plus the Glasmio iOS navigation bar. */
@Composable
fun XvoxStageScaffold(modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    var selected by remember { mutableStateOf(XvoxNavTab.HOME) }
    val backdrop = rememberLayerBackdrop()

    val gradientAccent by animateColorAsState(
        targetValue = xvoxTabAccent(selected, colors.primaryAccent),
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "stageAccent"
    )

    GlasmioBackdropProvider(backdrop = backdrop) {
        CompositionLocalProvider(LocalXvoxGlassBackdrop provides backdrop) {
            Box(modifier.fillMaxSize()) {
                // This layer is simultaneously the visible gradient and the glass source, so the
                // bar refracts the real artwork behind it and updates every animated frame.
                Box(Modifier.fillMaxSize().glassBackdropSource(backdrop)) {
                    XvoxGeneratedBackground(
                        accent = gradientAccent,
                        base = colors.background,
                        isLight = colors.isLight,
                        style = 0f,
                        blurDp = 0.dp,
                        modifier = Modifier.fillMaxSize(),
                        custom = emptyList(),
                        gradStyle = 0,
                        angle = 35f,
                        spread = .5f
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    XvoxIosNavBar(
                        selected = selected,
                        onSelect = { selected = it },
                        onSearch = { }
                    )
                }
            }
        }
    }
}
