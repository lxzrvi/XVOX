package com.xvox.music.core.design.kit

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.zIndex
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.kit.icons.XvoxSfIcons
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.blur.xvoxGlassRim
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.core.ui.icons.painterResource
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.home.HomeScreen
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.player.playback.MainPlayerViewModel
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.GlassSurfaceStyle
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.layerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberCombinedBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.icons.GlasmioIconProvider
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.scaffold.GlasmioFloatingSearch
import io.github.neoruaa.glasmio.scaffold.GlasmioNavigationBarWithTrailingItem
import io.github.neoruaa.glasmio.scaffold.glasmioNavigationScrollHint
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioNavigationScrollState
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioSearchTransition
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioTabItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Rebuild staging. The previous application surface stays in the source tree but is not composed.
 */
object XvoxStage {

    /** Stage 5 - the stage draws no background: the app backdrop is the live glass source. */
    const val OnlyBackgroundAndNavBar = true
}

private const val SearchKey = 9
private const val HomeIndex = 0
private const val LibraryIndex = 1
private const val ArtistsIndex = 2
private const val SettingsIndex = 3

/** Expanded navigation bar width: centred and narrower than the screen. */
private val StageBarMaxWidth = 368.dp

/** Glasmio's navigation surface height; the compact circles and the mini player match it. */
private val StageControlHeight = 62.dp
private val StageSideMargin = 16.dp
private val StageControlGap = 8.dp

/** Library filters, in the order they appear on the Library page. */
private val LibraryModes = listOf(
    XvoxHomeLibraryMode.RECENT to "Recent",
    XvoxHomeLibraryMode.ARTISTS to "Artists",
    XvoxHomeLibraryMode.PLAYLISTS to "Playlists",
    XvoxHomeLibraryMode.LIKED to "Liked",
    XvoxHomeLibraryMode.ALBUMS to "Albums"
)

@Composable
fun XvoxStageScaffold(modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val prefs = remember { UserPreferencesRepository(context.applicationContext) }
    val homeVm: HomeViewModel = viewModel()
    val playerVm: MainPlayerViewModel = viewModel()

    // The bar's selection moves at once so its pill animates; the (much heavier) page switch
    // follows once that release animation has had time to finish, so the pill never competes with
    // composing a whole screen on the same frames.
    var selectedIndex by remember { mutableIntStateOf(HomeIndex) }
    var pageIndex by remember { mutableIntStateOf(HomeIndex) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != pageIndex) pageIndex = selectedIndex
    }
    var libraryMode by rememberSaveable { mutableStateOf(XvoxHomeLibraryMode.RECENT) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    var searchBounds by remember { mutableStateOf<Rect?>(null) }
    // Artist / playlist / album detail pages open inside Home's list; this exits them.
    var detailMode by remember { mutableStateOf<XvoxHomeLibraryMode?>(null) }
    var detailExitKey by remember { mutableLongStateOf(0L) }

    // The list that is on screen drives the navigation bar: Home and Library report theirs, and
    // Settings owns its own.
    val settingsList = rememberLazyListState()
    val fallbackList = rememberLazyListState()
    var activeList by remember { mutableStateOf<LazyListState?>(null) }
    val navigationScroll = rememberGlasmioNavigationScrollState(
        listState = if (pageIndex == SettingsIndex) settingsList else activeList ?: fallbackList,
        enabled = !searchActive
    )
    // The stage owns no background: it samples the application's real backdrop layer, which the
    // root registered from the solid theme fill.
    val engineBackdrop = LocalXvoxGlassBackdrop.current
    val ownBackdrop = rememberLayerBackdrop()
    val backdrop = engineBackdrop ?: ownBackdrop
    // Floating glass (bar, search, profile, mini player) samples the app backdrop AND the page
    // content that scrolls beneath it, not just the wallpaper.
    val pageBackdrop = rememberLayerBackdrop()
    val floatingBackdrop = rememberCombinedBackdrop(backdrop, pageBackdrop)
    // Preferences are read here, once, so Settings opens with every control already in place.
    val settings = rememberXvoxStageSettings(prefs)
    val onExpand = remember(navigationScroll) { { navigationScroll.expand() } }
    val searchTransition = rememberGlasmioSearchTransition(searchActive)
    val morph by searchTransition.progressState

    // Only the two fields the stage needs are collected, so playback ticks never recompose it.
    val currentId by remember(playerVm) {
        playerVm.state.map { it.currentSongId }.distinctUntilChanged()
    }.collectAsState(initial = null)
    val isPlaying by remember(playerVm) {
        playerVm.state.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsState(initial = false)
    val homeSongs by remember(homeVm) { homeVm.state.map { it.songs }.distinctUntilChanged() }.collectAsState(initial = emptyList())
    val currentSong: Song? = remember(currentId) {
        currentId?.let { id ->
            playerVm.state.value.queue.firstOrNull { it.id == id }
                ?: homeVm.state.value.songs.firstOrNull { it.id == id }
        }
    }

    LaunchedEffect(pageIndex, libraryMode) {
        when (pageIndex) {
            HomeIndex -> homeVm.setLibraryMode(XvoxHomeLibraryMode.ALL_SONGS)
            LibraryIndex -> homeVm.setLibraryMode(libraryMode)
            ArtistsIndex -> homeVm.setLibraryMode(XvoxHomeLibraryMode.ARTISTS)
        }
    }

    val items = remember {
        listOf(
            GlasmioTabItem(HomeIndex, "Home", SfSymbol.HouseFilled),
            GlasmioTabItem(LibraryIndex, "Library", SfSymbol.MusicNoteList),
            GlasmioTabItem(ArtistsIndex, "Artists", SfSymbol.Microphone),
            GlasmioTabItem(SettingsIndex, "Settings", SfSymbol.Gearshape)
        )
    }
    val searchItem = remember { GlasmioTabItem(SearchKey, "Search", SfSymbol.Search) }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topInset = statusTop + if (pageIndex == LibraryIndex || pageIndex == ArtistsIndex) 60.dp else 62.dp
    val bottomInset = navBottom + 176.dp

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
                            if (pageIndex != SettingsIndex) {
                                StageAmbientGlow(
                                    accent = colors.primaryAccent,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Home / Library stay composed for the whole session. Switching tabs
                            // used to rebuild the entire (heavy) HomeScreen, which stalled the
                            // frame that was animating the navigation pill and left it frozen.
                            CompositionLocalProvider(
                                LocalXvoxTopInset provides topInset,
                                LocalXvoxBottomInset provides bottomInset
                            ) {
                                Box(Modifier.fillMaxSize()) {
                                    HomeScreen(
                                        currentSongId = currentId,
                                        isPlaying = isPlaying,
                                        detailExitKey = detailExitKey,
                                        onActiveDetailModeChange = { detailMode = it },
                                        onQueueReady = { songs ->
                                            if (songs.isNotEmpty()) playerVm.setQueue(songs)
                                        },
                                        onPlay = { song -> playerVm.play(song) },
                                        playerViewModel = playerVm,
                                        viewModel = homeVm,
                                        onActiveListState = { activeList = it }
                                    )
                                    if (pageIndex == LibraryIndex || (pageIndex == ArtistsIndex && detailMode != null)) {
                                        LibraryHeader(
                                            mode = libraryMode,
                                            detailOpen = detailMode != null,
                                            onMode = { mode ->
                                                libraryMode = mode
                                                homeVm.setLibraryMode(mode)
                                            },
                                            onBack = { detailExitKey += 1 }
                                        )
                                    }
                                }
                            }
                            // Settings is composed once, shortly after launch, and only moved on and off
                            // the screen afterwards. Opening it therefore builds nothing, so no heavy
                            // frame can land on the navigation pill's release animation.
                            var settingsReady by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                delay(1200)
                                settingsReady = true
                            }
                            val settingsShown = pageIndex == SettingsIndex
                            val settingsAlpha by animateFloatAsState(
                                if (settingsShown) 1f else 0f, tween(if (settingsShown) 220 else 180), label = "settingsAlpha"
                            )
                            if (settingsReady || settingsShown) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            alpha = settingsAlpha
                                            // Off-screen when hidden: hit-testing follows the layer, so it
                                            // can never swallow touches meant for Home.
                                            translationY = if (settingsAlpha <= 0.001f && !settingsShown) size.height else 0f
                                        }
                                        .background(colors.background)
                                        .pointerInput(Unit) { }
                                ) {
                                    XvoxStageSettingsPage(
                                        prefs = prefs,
                                        values = settings,
                                        backdrop = backdrop,
                                        onBack = {
                                            selectedIndex = HomeIndex
                                            pageIndex = HomeIndex
                                        },
                                        listState = settingsList,
                                        chromeBackdrop = floatingBackdrop
                                    )
                                }
                            }
                        }

                        // Search is its own page, over Home / Library.
                        if (searchActive) {
                            StageSearchPage(
                                query = query.text,
                                songs = homeSongs,
                                currentSongId = currentId,
                                topPadding = statusTop + 84.dp,
                                bottomPadding = navBottom + 24.dp,
                                onPlay = { song, list ->
                                    playerVm.setQueue(list)
                                    playerVm.play(song)
                                }
                            )
                        }

                        // The bar and the mini player retire into the search field together: one
                        // smooth fade spread across Glasmio's whole morph clock, with no scaling,
                        // so bar and search capsule overlap and read as one blending shape both
                        // when search opens and when it closes.
                        if (morph < .999f) {
                            val t = ((morph - .05f) / .85f).coerceIn(0f, 1f)
                            val barAlpha = 1f - t * t * (3f - 2f * t)
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
                                    song = currentSong,
                                    playing = isPlaying,
                                    onToggle = { playerVm.togglePlay() },
                                    onNext = { playerVm.playNext() },
                                    onPrevious = { playerVm.playPrevious() },
                                    onOpen = { playerVm.openNowPlaying() },
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
                                        // BitChord's rim is drawn on the bar's own node: the border
                                        // and the selection pill therefore share one geometry and
                                        // can never drift apart while the bar scrolls or compacts.
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            // The BitChord edge fades out as the bar compacts, so
                                            // no rim line can ever run across the mini player.
                                            .xvoxGlassRim(
                                                RoundedCornerShape(50),
                                                hairlineColor = Color.White.copy(
                                                    alpha = .10f * (1f - navigationScroll.compactProgress.value
                                                        .coerceIn(0f, 1f))
                                                )
                                            )
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

                // Profile button: its centre lines up with the XVOX wordmark on the left.
                XvoxProfileMenu(
                    prefs = prefs,
                    backdrop = floatingBackdrop,
                    onOpenSettings = { selectedIndex = SettingsIndex },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 11.dp, end = 16.dp)
                        // A graphics layer only exists while search is morphing: an always-on layer
                        // around the profile menu made its close animation flicker.
                        .then(
                            if (morph > 0.001f) Modifier.graphicsLayer {
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                                alpha = 1f - morph
                            } else Modifier
                        )
                )
                StageNowPlayingHost(playerVm = playerVm, homeVm = homeVm, modifier = Modifier.zIndex(20f))
            }
        }
    }
}

/**
 * A very slow, soft accent glow that drifts out of the top-left corner of Home and Library. It is
 * part of the recorded page layer, so the bar, search and mini player blend with it too.
 */
@Composable
private fun StageAmbientGlow(accent: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "stageGlow")
    val a = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowA"
    )
    val b = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(37_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowB"
    )
    Box(
        modifier.drawBehind {
            val w = size.width
            val h = size.height
            val pa = a.value
            val pb = b.value
            drawRect(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = .20f), accent.copy(alpha = .07f), Color.Transparent),
                    center = Offset(w * (.50f + .20f * (pa - .5f)), h * (-.04f + .10f * pb)),
                    radius = w * (.95f + .15f * pb)
                )
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = .10f), Color.Transparent),
                    center = Offset(w * (.50f - .22f * (pb - .5f)), h * (.10f + .08f * pa)),
                    radius = w * .70f
                )
            )
        }
    )
}

/** Library filter chips (or a back button while an artist / playlist / album is open). */
@Composable
private fun BoxScope.LibraryHeader(
    mode: XvoxHomeLibraryMode,
    detailOpen: Boolean,
    onMode: (XvoxHomeLibraryMode) -> Unit,
    onBack: () -> Unit
) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(top = 11.dp, start = 16.dp, end = 72.dp)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (detailOpen) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(colors.card)
                    .xvoxGlassRim(CircleShape)
                    .xvoxSongPress(onClick = onBack, pressedScale = .9f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = XvoxSfIcons.ChevronLeft,
                    contentDescription = "Back",
                    tint = colors.primaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // Plain capsules (no Glasmio component inside the scroller), so there is nothing that
            // needs a bounded width and nothing that can crash while measuring.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibraryModes.forEach { (value, label) ->
                    val on = value == mode
                    Box(
                        Modifier.height(40.dp).clip(CircleShape)
                            .background(if (on) colors.primaryAccent else colors.card)
                            .xvoxGlassRim(CircleShape)
                            .xvoxSongPress(onClick = { onMode(value) }, pressedScale = .94f)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (on) Color.White else colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

/**
 * Mini player, wired to the real player. Expanded it floats above the navigation bar at full
 * width; as the bar compacts it slides down and narrows into the gap between the two remaining
 * circles, growing to the same height. Play/pause and next sit on its right.
 */
@Composable
private fun BoxScope.StageMiniPlayer(
    compactProgress: State<Float>,
    song: Song?,
    playing: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpen: () -> Unit,
    backdrop: Backdrop
) {
    var direction by remember { mutableIntStateOf(1) }
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(50)
    var drag by remember { mutableFloatStateOf(0f) }
    val shift by animateFloatAsState(drag, tween(120), label = "miniSwipe")
    // Opening Now Playing: the mini player itself lifts, grows and hands over, instead of the
    // full screen simply appearing. Swipe up or tap both take this path.
    var opening by remember { mutableStateOf(false) }
    val openLift by animateFloatAsState(
        targetValue = if (opening) 1f else 0f,
        animationSpec = spring(dampingRatio = .78f, stiffness = Spring.StiffnessMediumLow),
        label = "miniOpenLift"
    )
    LaunchedEffect(opening) {
        if (opening) {
            kotlinx.coroutines.delay(190)
            onOpen()
            opening = false
        }
    }
    GlassSurface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .layout { measurable, c ->
                val p = compactProgress.value.coerceIn(0f, 1f)
                val margin = StageSideMargin.roundToPx() * 2
                val full = (c.maxWidth - margin).coerceAtLeast(0)
                // Compact: the row minus the two circles and the gap on each side.
                val compactWidth = (full - 2 * (StageControlHeight + StageControlGap).roundToPx()).coerceAtLeast(0)
                val w = lerp(full, compactWidth, p)
                val h = lerp(62.dp.roundToPx(), StageControlHeight.roundToPx(), p)
                val rise = ((StageControlHeight + 10.dp).roundToPx() * (1f - p)).toInt()
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(c.maxWidth, StageControlHeight.roundToPx() + rise) {
                    placeable.place((c.maxWidth - w) / 2, StageControlHeight.roundToPx() - h)
                }
            }
            .graphicsLayer {
                val l = openLift
                scaleX = 1f + .06f * l
                scaleY = 1f + .06f * l
                alpha = 1f - .85f * l
                translationY = -18.dp.toPx() * l
            }
            .xvoxGlassRim(shape),
        backdrop = backdrop,
        shape = shape,
        style = GlassSurfaceStyle.Navigation
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val showText = maxWidth > 250.dp
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { drag = 0f },
                            onVerticalDrag = { _, amount ->
                                drag = 0f
                                if (amount < -6f) opening = true
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                if (total > 70f) { direction = -1; onPrevious() }
                                else if (total < -70f) { direction = 1; onNext() }
                                total = 0f
                                drag = 0f
                            },
                            onDragCancel = { total = 0f; drag = 0f },
                            onHorizontalDrag = { _, amount ->
                                total += amount
                                drag = (total / 4f).coerceIn(-24f, 24f)
                            }
                        )
                    }
                    .graphicsLayer { translationX = shift }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { opening = true }
                    )
                    .padding(start = 7.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover and text change together. Next slides the new song in from the right,
                // Previous is the exact reverse; both fade so the swap never pops.
                AnimatedContent(
                    targetState = song,
                    contentKey = { it?.id },
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val d = direction
                        (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { d * (it / 2) } +
                            fadeIn(tween(260)) + scaleIn(tween(340), initialScale = .94f)) togetherWith
                            (slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -d * (it / 2) } +
                                fadeOut(tween(180)) + scaleOut(tween(300), targetScale = .94f)) using
                            SizeTransform(clip = true)
                    },
                    label = "miniSong"
                ) { shown ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        XvoxSongArtwork(
                            artwork = shown?.artworkUri,
                            requestSize = 160,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        )
                        if (showText) {
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(
                                    shown?.title ?: "Not playing",
                                    color = glass.content,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    shown?.artist?.ifBlank { "Unknown artist" } ?: "Pick a song",
                                    color = glass.secondaryContent,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                MiniControl(onClick = onToggle) {
                    // Play and pause swap with a quick scale-fade, like Apple Music's.
                    AnimatedContent(
                        targetState = playing,
                        transitionSpec = {
                            (fadeIn(tween(160)) + scaleIn(tween(200), initialScale = .6f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(tween(160), targetScale = .6f))
                        },
                        label = "miniPlayPause"
                    ) { isPlaying ->
                        Icon(
                            painter = painterResource(if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = glass.content,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                MiniControl(onClick = { direction = 1; onNext() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_skip_next),
                        contentDescription = "Next",
                        tint = glass.content,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniControl(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .xvoxSongPress(onClick = onClick, pressedScale = .82f),
        contentAlignment = Alignment.Center
    ) { content() }
}
