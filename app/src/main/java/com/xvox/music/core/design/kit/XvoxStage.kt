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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.kit.icons.XvoxSfIcons
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxGeneratedBackground
import com.xvox.music.core.ui.navigation.XvoxNavTab
import com.xvox.music.data.preferences.UserPreferencesRepository
import io.github.neoruaa.glasmio.glass.glassBackdropSource
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.icons.GlasmioIconProvider
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.scaffold.GlasmioFloatingSearch
import io.github.neoruaa.glasmio.scaffold.GlasmioNavigationBarWithTrailingItem
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioSearchTransition
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import io.github.neoruaa.glasmio.ui.GlasmioTabItem

/**
 * Rebuild staging.
 *
 * The old application surface stays in the source tree but is not composed: only the gradient
 * backdrop, the Glasmio navigation bar, the always-visible profile control and the Glasmio boxes
 * opened from it are live. Nothing is deleted; every surface is switched back on as it is rebuilt.
 */
object XvoxStage {

    /** Stage 2 - gradient backdrop, Glasmio navbar, profile menu, settings and edit profile. */
    const val OnlyBackgroundAndNavBar = true
}

private const val SearchKey = 9

/** The three destinations the bar carries, in the requested order. */
private val StageTabs = listOf(XvoxNavTab.HOME, XvoxNavTab.PLAYLIST, XvoxNavTab.ALBUM)

private fun stageIcon(tab: XvoxNavTab): SfSymbol = when (tab) {
    XvoxNavTab.HOME -> SfSymbol.HouseFilled
    XvoxNavTab.PLAYLIST -> SfSymbol.MusicNoteList
    XvoxNavTab.ALBUM -> SfSymbol.MusicNoteSquareStackFilled
    XvoxNavTab.ARTIST -> SfSymbol.Microphone
}

/** Each destination owns its gradient accent, so the glass behind it re-samples a new backdrop. */
internal fun xvoxTabAccent(tab: XvoxNavTab, themeAccent: Color): Color = when (tab) {
    XvoxNavTab.HOME -> themeAccent
    XvoxNavTab.ARTIST -> Color(0xFFFF9F0A)
    XvoxNavTab.ALBUM -> Color(0xFF0A84FF)
    XvoxNavTab.PLAYLIST -> Color(0xFF30D158)
}

@Composable
fun XvoxStageScaffold(modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferencesRepository(context.applicationContext) }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    var searchBounds by remember { mutableStateOf<Rect?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }

    val compact = remember { mutableFloatStateOf(0f) }
    val backdrop = rememberLayerBackdrop()
    val searchTransition = rememberGlasmioSearchTransition(searchActive)

    val selectedTab = StageTabs[selectedIndex.coerceIn(0, StageTabs.lastIndex)]
    val gradientAccent by animateColorAsState(
        targetValue = xvoxTabAccent(selectedTab, colors.primaryAccent),
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "stageAccent"
    )

    val items = remember {
        StageTabs.mapIndexed { index, tab ->
            GlasmioTabItem(
                key = index,
                label = tab.label,
                symbol = stageIcon(tab)
            )
        }
    }
    val searchItem = remember { GlasmioTabItem(key = SearchKey, label = "Search", symbol = SfSymbol.Search) }

    XvoxGlassEnvironment(backdrop = backdrop) {
            GlasmioIconProvider(resolver = XvoxSfIcons.Resolver) {
                Box(modifier.fillMaxSize()) {
                    // Same layer is both the visible gradient and the glass source: bar, pill and
                    // search capsule reflect real pixels, live, on every animated frame.
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

                    // Glasmio's own floating search owns the bar-to-search morph. autoFocus stays
                    // false, so tapping search never raises the keyboard.
                    GlasmioFloatingSearch(
                        active = searchActive,
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = { searchActive = false },
                        onDismiss = {
                            searchActive = false
                            query = TextFieldValue()
                        },
                        placeholder = "Search",
                        cancelLabel = "Cancel",
                        autoFocus = false,
                        backdrop = backdrop,
                        transition = searchTransition,
                        sourceBoundsInWindow = searchBounds
                    ) {
                        Box(Modifier.fillMaxSize()) {
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
                                        if (key == SearchKey) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            searchActive = true
                                        } else {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedIndex = key
                                        }
                                    },
                                    onExpand = { compact.floatValue = 0f },
                                    compactProgress = compact,
                                    modifier = Modifier.fillMaxWidth(),
                                    backdrop = backdrop,
                                    onItemBoundsChanged = { key, bounds ->
                                        if (key == SearchKey) searchBounds = bounds
                                    }
                                )
                            }
                        }
                    }

                    // Always-on profile control, top right.
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 8.dp, end = 16.dp)
                    ) {
                        GlasmioMenu(
                            expanded = menuOpen,
                            onExpandedChange = { menuOpen = it },
                            itemCount = 2,
                            backdrop = backdrop,
                            anchor = { open ->
                                GlasmioIconButton(
                                    onClick = open,
                                    backdrop = backdrop,
                                    size = 44.dp
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_xvox_user),
                                        contentDescription = "Profile",
                                        tint = colors.primaryText,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        ) { menuBackdrop, close ->
                            GlasmioMenuItem(
                                title = "Edit Profile",
                                onClick = {
                                    close()
                                    showProfile = true
                                },
                                systemName = "person",
                                backdrop = menuBackdrop
                            )
                            GlasmioMenuItem(
                                title = "Settings",
                                onClick = {
                                    close()
                                    showSettings = true
                                },
                                systemName = "gear",
                                backdrop = menuBackdrop
                            )
                        }
                    }

                    if (showProfile) {
                        XvoxStageEditProfileSheet(
                            prefs = prefs,
                            backdrop = backdrop,
                            onDismiss = { showProfile = false }
                        )
                    }

                    if (showSettings) {
                        XvoxStageSettingsPage(
                            prefs = prefs,
                            backdrop = backdrop,
                            onBack = { showSettings = false }
                        )
                    }
                }
            }
    }
}
