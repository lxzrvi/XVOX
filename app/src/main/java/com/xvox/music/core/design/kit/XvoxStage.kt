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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.kit.icons.XvoxSfIcons
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxGeneratedBackground
import com.xvox.music.core.ui.navigation.XvoxNavTab
import io.github.neoruaa.glasmio.glass.GlasmioBackdropProvider
import io.github.neoruaa.glasmio.glass.glassBackdropSource
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.icons.GlasmioIconProvider
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.scaffold.GlasmioNavigationBarWithTrailingItem
import io.github.neoruaa.glasmio.ui.GlasmioTabItem

/**
 * Rebuild staging.
 *
 * The application surface is being rebuilt from the navigation bar outward, so the old screens stay
 * in the source tree but are not composed: only the gradient backdrop and the Glasmio navigation
 * bar are visible. Nothing is deleted, so each surface can be switched back on later.
 */
object XvoxStage {

    /** Stage 1 - only the gradient background and the Glasmio navigation bar are visible. */
    const val OnlyBackgroundAndNavBar = true
}

/** Search lives in the trailing capsule, so it needs a key outside the destination range. */
private const val SearchKey = 9

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

/**
 * The visible stage.
 *
 * The bar itself is Glasmio's own `GlasmioNavigationBarWithTrailingItem`: identical shape, layout,
 * capsule-to-circle morph, drag contact, press optics and spring animation to the upstream demo.
 * Nothing is re-implemented here. The only thing carried over from the previous XVOX bar is the
 * material hook-up: the animated gradient layer is registered as the glass source, so the pill and
 * the bar reflect the live backdrop behind them every frame instead of a frozen frost.
 */
@Composable
fun XvoxStageScaffold(modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    val haptics = LocalHapticFeedback.current
    var selectedIndex by remember { mutableIntStateOf(0) }
    val compact = remember { mutableFloatStateOf(0f) }
    val backdrop = rememberLayerBackdrop()

    val tabs = XvoxNavTab.entries
    val selectedTab = tabs[selectedIndex.coerceIn(0, tabs.lastIndex)]
    val gradientAccent by animateColorAsState(
        targetValue = xvoxTabAccent(selectedTab, colors.primaryAccent),
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "stageAccent"
    )

    val items = remember {
        listOf(
            GlasmioTabItem(0, "Home", SfSymbol.HouseFilled),
            GlasmioTabItem(1, "Artists", SfSymbol.Microphone),
            GlasmioTabItem(2, "Albums", SfSymbol.MusicNoteSquareStackFilled),
            GlasmioTabItem(3, "Playlists", SfSymbol.MusicNoteList)
        )
    }
    val searchItem = remember { GlasmioTabItem(SearchKey, "Search", SfSymbol.Search) }

    GlasmioBackdropProvider(backdrop = backdrop) {
        CompositionLocalProvider(LocalXvoxGlassBackdrop provides backdrop) {
            GlasmioIconProvider(resolver = XvoxSfIcons.Resolver) {
                Box(modifier.fillMaxSize()) {
                    // Same layer is both the visible gradient and the glass source: the pill and the
                    // bar sample real pixels, live, on every animated frame.
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
                            .padding(bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GlasmioNavigationBarWithTrailingItem(
                            items = items,
                            trailingItem = searchItem,
                            selectedKey = selectedIndex,
                            onSelected = { key ->
                                if (key != SearchKey) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedIndex = key
                                }
                            },
                            onExpand = { compact.floatValue = 0f },
                            compactProgress = compact,
                            modifier = Modifier.fillMaxWidth(),
                            backdrop = backdrop
                        )
                    }
                }
            }
        }
    }
}
