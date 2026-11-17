package com.xvox.music.core.design.kit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.kit.icons.XvoxSfIcons
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.navigation.XvoxNavTab
import com.xvox.music.data.preferences.UserPreferencesRepository
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.icons.GlasmioIconProvider
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.scaffold.GlasmioFloatingSearch
import io.github.neoruaa.glasmio.scaffold.GlasmioNavigationBarWithTrailingItem
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioSearchTransition
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import io.github.neoruaa.glasmio.ui.GlasmioTabItem

/**
 * Rebuild staging. The previous application surface stays in the source tree but is not composed.
 */
object XvoxStage {

    /** Stage 5 - the stage draws no background: the app backdrop is the live glass source. */
    const val OnlyBackgroundAndNavBar = true
}

private const val SearchKey = 9
private const val SettingsIndex = 2

/** Bar width: wider again, still centred and narrower than the screen. */
private val StageBarMaxWidth = 320.dp

private val StageTabs = listOf(XvoxNavTab.HOME, XvoxNavTab.ALBUM)

private fun stageIcon(tab: XvoxNavTab): SfSymbol = when (tab) {
    XvoxNavTab.HOME -> SfSymbol.HouseFilled
    XvoxNavTab.ALBUM -> SfSymbol.MusicNoteSquareStackFilled
    XvoxNavTab.PLAYLIST -> SfSymbol.MusicNoteList
    XvoxNavTab.ARTIST -> SfSymbol.Microphone
}

@Composable
fun XvoxStageScaffold(modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val prefs = remember { UserPreferencesRepository(context.applicationContext) }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    var searchBounds by remember { mutableStateOf<Rect?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }

    val compact = remember { mutableFloatStateOf(0f) }
    // The stage owns no background: it samples the application's real backdrop layer, which the
    // root registered from the live wallpaper, gradient or theme fill.
    val engineBackdrop = LocalXvoxGlassBackdrop.current
    val ownBackdrop = rememberLayerBackdrop()
    val backdrop = engineBackdrop ?: ownBackdrop
    val searchTransition = rememberGlasmioSearchTransition(searchActive)
    val morph by searchTransition.progressState

    val items = remember {
        StageTabs.mapIndexed { index, t -> GlasmioTabItem(index, t.label, stageIcon(t)) } +
            GlasmioTabItem(SettingsIndex, "Settings", SfSymbol.Gearshape)
    }
    val searchItem = remember { GlasmioTabItem(SearchKey, "Search", SfSymbol.Search) }

    XvoxGlassEnvironment(backdrop = backdrop) {
        GlasmioIconProvider(resolver = XvoxSfIcons.Resolver) {
            Box(modifier.fillMaxSize()) {
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
                    // Tapping search never raises the keyboard.
                    autoFocus = false,
                    backdrop = backdrop,
                    transition = searchTransition,
                    sourceBoundsInWindow = searchBounds
                ) {
                    Box(Modifier.fillMaxSize()) {
                        when (selectedIndex) {
                            SettingsIndex -> XvoxStageSettingsPage(
                                prefs = prefs,
                                backdrop = backdrop,
                                onBack = { selectedIndex = 0 }
                            )
                            else -> XvoxStagePage(title = StageTabs[selectedIndex].label)
                        }

                        // The bar retires into the search field on Glasmio's own morph clock.
                        if (morph < .999f) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(bottom = 16.dp)
                            ) {
                                GlasmioNavigationBarWithTrailingItem(
                                    items = items,
                                    trailingItem = searchItem,
                                    selectedKey = selectedIndex,
                                    onSelected = { key ->
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        when (key) {
                                            SearchKey -> searchActive = true
                                            else -> selectedIndex = key
                                        }
                                    },
                                    onExpand = { compact.floatValue = 0f },
                                    compactProgress = compact,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .widthIn(max = StageBarMaxWidth)
                                        .graphicsLayer {
                                            alpha = 1f - morph
                                            val scale = 1f - .06f * morph
                                            scaleX = scale
                                            scaleY = scale
                                            transformOrigin = TransformOrigin(1f, 1f)
                                        },
                                    backdrop = backdrop,
                                    onItemBoundsChanged = { key, bounds ->
                                        if (key == SearchKey) searchBounds = bounds
                                    }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 8.dp, end = 16.dp)
                        .graphicsLayer { alpha = 1f - morph }
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
                                selectedIndex = SettingsIndex
                            },
                            systemName = "gear",
                            backdrop = menuBackdrop
                        )
                    }
                }

                if (showProfile) {
                    XvoxEditProfileGlassBox(
                        prefs = prefs,
                        backdrop = backdrop,
                        onDismiss = { showProfile = false }
                    )
                }
            }
        }
    }
}

/**
 * A page surface for the rebuild: the Glasmio header only, so the page name sits top-left and the
 * top strip picks up its blurred material as soon as the page scrolls.
 */
@Composable
private fun XvoxStagePage(title: String) {
    GlasmioPinnedListPage(
        title = title,
        bottomPadding = 132.dp,
        // Transparent: the page must not paint over the live app backdrop, which is exactly what
        // the bar, the lens and every glass box sample.
        backgroundColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) {
        // Invisible scroll runway; real content replaces these once a page is rebuilt.
        items(14) {
            Spacer(Modifier.fillMaxWidth().height(78.dp))
        }
    }
}
