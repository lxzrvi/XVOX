package com.xvox.music.core.design.kit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.GlassSurfaceStyle
import io.github.neoruaa.glasmio.glass.render.backdrops.layerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberCombinedBackdrop
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.State
import io.github.neoruaa.glasmio.glass.render.Backdrop
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.xvox.music.core.ui.icons.painterResource
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
import io.github.neoruaa.glasmio.scaffold.glasmioNavigationScrollHint
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioNavigationScrollState
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioSearchTransition
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioMenu
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

/** Expanded bar width: centred and narrower than the screen. */
private val StageBarMaxWidth = 320.dp

/** Glasmio's navigation surface height; the compact circles and the mini player match it. */
private val StageControlHeight = 62.dp
private val StageSideMargin = 16.dp
private val StageControlGap = 8.dp

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
    // Settings is opened only once the profile popover has finished collapsing into its button,
    // so the two transitions never overlap.
    var openSettingsAfterClose by remember { mutableStateOf(false) }

    // One list state per root page. The navigation bar follows whichever page is on screen: it
    // shrinks to its compact capsule while scrolling away and springs back on tap or at the top.
    val pageListStates = StageTabs.indices.map { rememberLazyListState() } + rememberLazyListState()
    val navigationScroll = rememberGlasmioNavigationScrollState(
        listState = pageListStates[if (selectedIndex == SettingsIndex) pageListStates.lastIndex else selectedIndex],
        enabled = !searchActive
    )
    // The stage owns no background: it samples the application's real backdrop layer, which the
    // root registered from the live wallpaper, gradient or theme fill.
    val engineBackdrop = LocalXvoxGlassBackdrop.current
    val ownBackdrop = rememberLayerBackdrop()
    val backdrop = engineBackdrop ?: ownBackdrop
    // Floating glass (bar, search, profile, mini player) samples the app backdrop AND the page
    // content that scrolls beneath it, not just the wallpaper.
    val pageBackdrop = rememberLayerBackdrop()
    val floatingBackdrop = rememberCombinedBackdrop(backdrop, pageBackdrop)
    // Preferences are read here, once, so Settings opens with every control already in place.
    val settings = rememberXvoxStageSettings(prefs)
    var playing by remember { mutableStateOf(false) }
    val onExpand = remember(navigationScroll) { { navigationScroll.expand() } }
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
                    backdrop = floatingBackdrop,
                    transition = searchTransition,
                    sourceBoundsInWindow = searchBounds
                ) {
                    Box(Modifier.fillMaxSize().nestedScroll(navigationScroll.nestedScrollConnection)) {
                        // The page is its own recorded layer: every floating glass element samples
                        // it together with the wallpaper. The bar below is a sibling, never inside.
                        Box(Modifier.fillMaxSize().layerBackdrop(pageBackdrop)) {
                            AnimatedContent(
                                targetState = selectedIndex,
                                transitionSpec = {
                                    (fadeIn(tween(280)) + scaleIn(tween(320), initialScale = .94f)) togetherWith
                                        (fadeOut(tween(200)) + scaleOut(tween(260), targetScale = .97f))
                                },
                                label = "stageRoot"
                            ) { index ->
                                when (index) {
                                    SettingsIndex -> XvoxStageSettingsPage(
                                        prefs = prefs,
                                        values = settings,
                                        // Controls live inside the recorded page, so they sample
                                        // the wallpaper only (never the layer they are drawn in).
                                        backdrop = backdrop,
                                        onBack = { selectedIndex = 0 },
                                        listState = pageListStates.last()
                                    )
                                    else -> XvoxStagePage(
                                        title = StageTabs[index].label,
                                        listState = pageListStates[index]
                                    )
                                }
                            }
                        }

                        // The bar and the mini player retire into the search field together: one
                        // smooth fade across the first part of Glasmio's morph clock, with no
                        // scaling, so bar and search capsule overlap and read as one blending shape.
                        if (morph < .999f) {
                            val fade = (morph * 1.25f).coerceIn(0f, 1f)
                            val barAlpha = 1f - fade * fade * (3f - 2f * fade)
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(bottom = 16.dp)
                                    .graphicsLayer {
                                        // ModulateAlpha fades each draw call instead of rendering into
                                        // an offscreen layer, which would crop the glass shadow and rim.
                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                        alpha = barAlpha
                                    }
                            ) {
                                StageMiniPlayer(
                                    compactProgress = navigationScroll.compactProgress,
                                    playing = playing,
                                    onToggle = { playing = !playing },
                                    backdrop = floatingBackdrop
                                )
                                // Width follows the scroll: 320 dp while expanded, the full row once
                                // compact, so the two remaining circles sit far apart (left and right)
                                // and Glasmio's own drop-separation animation plays between them.
                                Box(
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .layout { measurable, c ->
                                            val margin = StageSideMargin.roundToPx() * 2
                                            val full = (c.maxWidth - margin).coerceAtLeast(0)
                                            val expanded = StageBarMaxWidth.roundToPx().coerceAtMost(full)
                                            val w = lerp(
                                                expanded,
                                                full,
                                                navigationScroll.compactProgress.value.coerceIn(0f, 1f)
                                            )
                                            val placeable = measurable.measure(Constraints(w, w, 0, c.maxHeight))
                                            layout(c.maxWidth, placeable.height) {
                                                placeable.place((c.maxWidth - placeable.width) / 2, 0)
                                            }
                                        }
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
                                        onExpand = onExpand,
                                        compactProgress = navigationScroll.compactProgress,
                                        compactSize = StageControlHeight,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .glasmioNavigationScrollHint(navigationScroll.scrollHintProgress),
                                        backdrop = floatingBackdrop,
                                        onItemBoundsChanged = { key, bounds ->
                                            if (key == SearchKey) searchBounds = bounds
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 8.dp, end = 16.dp)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                            alpha = 1f - morph
                        }
                ) {
                    // The profile button morphs into the panel and the panel collapses back into the
                    // same button (Glasmio's source-preview morph), instead of a separate dialog.
                    GlasmioMenu(
                        expanded = menuOpen,
                        onExpandedChange = { menuOpen = it },
                        itemCount = 1,
                        menuWidth = XvoxProfilePanelWidth,
                        heightOverride = XvoxProfilePanelHeight,
                        forceBelowAnchor = true,
                        backdrop = floatingBackdrop,
                        onClosed = {
                            if (openSettingsAfterClose) {
                                openSettingsAfterClose = false
                                selectedIndex = SettingsIndex
                            }
                        },
                        anchor = { open ->
                            GlasmioIconButton(
                                onClick = open,
                                backdrop = floatingBackdrop,
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
                        XvoxProfilePanel(
                            prefs = prefs,
                            menuBackdrop = menuBackdrop,
                            onClose = close,
                            onOpenSettings = {
                                openSettingsAfterClose = true
                                close()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dummy mini player. Expanded it floats above the navigation bar; as the bar compacts it slides
 * down and narrows into the gap between the two remaining circles, growing to the same height.
 */
@Composable
private fun BoxScope.StageMiniPlayer(
    compactProgress: State<Float>,
    playing: Boolean,
    onToggle: () -> Unit,
    backdrop: Backdrop
) {
    val glass = LocalGlassColors.current
    val colors = XvoxTheme.colors
    GlassSurface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .layout { measurable, c ->
                val p = compactProgress.value.coerceIn(0f, 1f)
                val margin = StageSideMargin.roundToPx() * 2
                val full = (c.maxWidth - margin).coerceAtLeast(0)
                val expandedWidth = StageBarMaxWidth.roundToPx().coerceAtMost(full)
                // Compact: the row minus the two circles and the gap on each side.
                val compactWidth = (full - 2 * (StageControlHeight + StageControlGap).roundToPx()).coerceAtLeast(0)
                val w = lerp(expandedWidth, compactWidth, p)
                val h = lerp(54.dp.roundToPx(), StageControlHeight.roundToPx(), p)
                val rise = ((StageControlHeight + 10.dp).roundToPx() * (1f - p)).toInt()
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(c.maxWidth, StageControlHeight.roundToPx() + rise) {
                    placeable.place(
                        (c.maxWidth - w) / 2,
                        StageControlHeight.roundToPx() + rise - h - rise
                    )
                }
            },
        backdrop = backdrop,
        shape = RoundedCornerShape(50),
        style = GlassSurfaceStyle.Navigation,
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(colors.primaryAccent, colors.primaryAccent.copy(alpha = .35f))
                        )
                    )
            )
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    "Midnight Drive",
                    color = glass.content,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Dummy artist",
                    color = glass.secondaryContent,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painter = painterResource(if (playing) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play),
                contentDescription = if (playing) "Pause" else "Play",
                tint = glass.content,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * A page surface for the rebuild: the Glasmio header only, so the page name sits top-left and the
 * top strip picks up its blurred material as soon as the page scrolls.
 */
@Composable
private fun XvoxStagePage(title: String, listState: LazyListState) {
    GlasmioPinnedListPage(
        title = title,
        listState = listState,
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
