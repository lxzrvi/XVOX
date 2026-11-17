package com.xvox.music.core.design.kit

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.kit.icons.XvoxSfIcons
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.blur.XvoxGpuGuard
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.chrome.XvoxGeneratedBackground
import com.xvox.music.core.ui.navigation.XvoxNavTab
import com.xvox.music.data.preferences.UserPreferencesRepository
import io.github.neoruaa.glasmio.glass.glassBackdropSource
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

    /** Stage 4 - live glass bar, Glasmio pages, settings tree and the profile box. */
    const val OnlyBackgroundAndNavBar = true
}

private const val SearchKey = 9
private const val SettingsIndex = 2

/** Compact bar width: deliberately narrower than the screen, like the Glasmio bar. */
private val StageBarMaxWidth = 286.dp

private val StageTabs = listOf(XvoxNavTab.HOME, XvoxNavTab.ALBUM)

private fun stageIcon(tab: XvoxNavTab): SfSymbol = when (tab) {
    XvoxNavTab.HOME -> SfSymbol.HouseFilled
    XvoxNavTab.ALBUM -> SfSymbol.MusicNoteSquareStackFilled
    XvoxNavTab.PLAYLIST -> SfSymbol.MusicNoteList
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
    val prefs = remember { UserPreferencesRepository(context.applicationContext) }

    val backgroundImage by prefs.themeBackgroundImage.collectAsState(initial = "")
    val chrome by prefs.chromeStyle.collectAsState(initial = XvoxChromeStyle())
    val backgroundBrightness by prefs.backgroundBrightness.collectAsState(initial = .8f)

    var selectedIndex by remember { mutableIntStateOf(0) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    var searchBounds by remember { mutableStateOf<Rect?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }

    val compact = remember { mutableFloatStateOf(0f) }
    val backdrop = rememberLayerBackdrop()
    val searchTransition = rememberGlasmioSearchTransition(searchActive)
    // 0 = bar expanded, 1 = fully morphed into the search field. Glasmio owns the curve.
    val morph by searchTransition.progressState

    val tab = StageTabs.getOrNull(selectedIndex) ?: XvoxNavTab.HOME
    val gradientAccent by animateColorAsState(
        targetValue = if (selectedIndex == SettingsIndex) colors.primaryAccent
        else xvoxTabAccent(tab, colors.primaryAccent),
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "stageAccent"
    )

    val items = remember {
        StageTabs.mapIndexed { index, t -> GlasmioTabItem(index, t.label, stageIcon(t)) } +
            GlasmioTabItem(SettingsIndex, "Settings", SfSymbol.Gearshape)
    }
    val searchItem = remember {
        GlasmioTabItem(SearchKey, "Search", SfSymbol.Search)
    }

    XvoxGlassEnvironment(backdrop = backdrop) {
        GlasmioIconProvider(resolver = XvoxSfIcons.Resolver) {
            Box(modifier.fillMaxSize()) {
                // One layer is the visible backdrop and the glass source at the same time, so the
                // bar, its lens and the search capsule sample real, live pixels every frame.
                Box(Modifier.fillMaxSize().glassBackdropSource(backdrop)) {
                    if (backgroundImage.isNotBlank()) {
                        val blurDp = if (XvoxGpuGuard.safeMode) 0.dp else (chrome.bgImageBlur * 36f).dp
                        AsyncImage(
                            model = Uri.parse(backgroundImage),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (blurDp > 0.dp) Modifier
                                        .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }
                                        .blur(blurDp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
                                    else Modifier
                                )
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Color.Black.copy(alpha = (1f - backgroundBrightness).coerceIn(0f, .95f))
                                )
                        )
                    } else {
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
                }

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
                    // The field stays passive: tapping search never raises the keyboard.
                    autoFocus = false,
                    backdrop = backdrop,
                    transition = searchTransition,
                    sourceBoundsInWindow = searchBounds
                ) {
                    Box(Modifier.fillMaxSize()) {
                        // Pages: every page is a Glasmio page, so the title sits top-left and the
                        // header materialises and blurs as the page scrolls.
                        when (selectedIndex) {
                            SettingsIndex -> XvoxStageSettingsPage(
                                prefs = prefs,
                                backdrop = backdrop,
                                onBack = { selectedIndex = 0 }
                            )
                            else -> XvoxStagePage(title = tab.label)
                        }

                        // The bar itself retires into the search field: it fades and scales from
                        // its trailing corner on Glasmio's morph clock, then stops composing.
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

                // Always-on profile control, top right. It also retires during search.
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
 * A page surface for the rebuild: only the Glasmio header exists for now, so the title sits at the
 * top-left and the top strip picks up its blurred material as soon as the page scrolls.
 */
@Composable
private fun XvoxStagePage(title: String) {
    GlasmioPinnedListPage(
        title = title,
        bottomPadding = 132.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        // Invisible scroll runway; real content replaces these once a page is rebuilt.
        items(14) {
            Spacer(Modifier.fillMaxWidth().height(78.dp))
        }
    }
}
