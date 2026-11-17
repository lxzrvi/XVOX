package com.xvox.music.core.design.kit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.kit.icons.XvoxSfIcons
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxGeneratedBackground
import com.xvox.music.core.ui.navigation.XvoxNavTab
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.setup.PfpIcon
import com.xvox.music.features.setup.PfpType
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.glassBackdropSource
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.icons.GlasmioIconProvider
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.scaffold.GlasmioFloatingSearch
import io.github.neoruaa.glasmio.scaffold.GlasmioNavigationBarWithTrailingItem
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioSearchTransition
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioAlertTextField
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import io.github.neoruaa.glasmio.ui.GlasmioTabItem
import kotlinx.coroutines.launch

/**
 * Rebuild staging. The previous application surface stays in the source tree but is not composed.
 */
object XvoxStage {

    /** Stage 3 - gradient backdrop, compact Glasmio bar, profile box, settings, edit profile. */
    const val OnlyBackgroundAndNavBar = true
}

private const val SearchKey = 9
private const val SettingsKey = 2

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

    val tab = StageTabs.getOrNull(selectedIndex) ?: XvoxNavTab.HOME
    val gradientAccent by animateColorAsState(
        targetValue = if (selectedIndex == SettingsKey) colors.primaryAccent
        else xvoxTabAccent(tab, colors.primaryAccent),
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "stageAccent"
    )

    val items = remember {
        StageTabs.mapIndexed { index, t -> GlasmioTabItem(index, t.label, stageIcon(t)) } +
            GlasmioTabItem(SettingsKey, "Settings", SfSymbol.Gearshape)
    }
    // The trailing capsule swaps to a close glyph while the search bar is open.
    val trailingSymbol = if (searchActive) SfSymbol.Close else SfSymbol.Search
    val searchItem = remember(trailingSymbol) {
        GlasmioTabItem(SearchKey, if (searchActive) "Close search" else "Search", trailingSymbol)
    }

    XvoxGlassEnvironment(backdrop = backdrop) {
        GlasmioIconProvider(resolver = XvoxSfIcons.Resolver) {
            Box(modifier.fillMaxSize()) {
                // Visible gradient is also the glass source: the bar, pill and search capsule
                // reflect real pixels, live, on every animated frame.
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
                    // The keyboard never opens from the bar; the field stays passive.
                    autoFocus = false,
                    backdrop = backdrop,
                    transition = searchTransition,
                    sourceBoundsInWindow = searchBounds
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Content-width bar: Glasmio's own sizing, deliberately not stretched.
                            GlasmioNavigationBarWithTrailingItem(
                                items = items,
                                trailingItem = searchItem,
                                selectedKey = selectedIndex,
                                onSelected = { key ->
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    when (key) {
                                        SearchKey -> searchActive = !searchActive
                                        SettingsKey -> {
                                            selectedIndex = SettingsKey
                                            showSettings = true
                                        }
                                        else -> {
                                            selectedIndex = key
                                            showSettings = false
                                        }
                                    }
                                },
                                onExpand = { compact.floatValue = 0f },
                                compactProgress = compact,
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
                                selectedIndex = SettingsKey
                                showSettings = true
                            },
                            systemName = "gear",
                            backdrop = menuBackdrop
                        )
                    }
                }

                if (showSettings) {
                    XvoxStageSettingsPage(
                        prefs = prefs,
                        backdrop = backdrop,
                        onBack = {
                            showSettings = false
                            if (selectedIndex == SettingsKey) selectedIndex = 0
                        }
                    )
                }

                if (showProfile) {
                    XvoxEditProfileBox(
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
 * Edit Profile as a centred Glasmio alert: the same clear material as the system alert, text-only
 * buttons, a name field and the built-in avatar set.
 */
@Composable
private fun XvoxEditProfileBox(
    prefs: UserPreferencesRepository,
    backdrop: io.github.neoruaa.glasmio.glass.render.backdrops.LayerBackdrop,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val colors = XvoxTheme.colors
    val saved by prefs.preferences.collectAsState(initial = null)
    var name by remember { mutableStateOf(TextFieldValue(saved?.username.orEmpty())) }
    var pfp by remember { mutableStateOf(saved?.selectedPfp ?: PfpType.DEFAULT.name) }

    GlasmioAlertDialog(
        onDismissRequest = onDismiss,
        title = "Edit Profile",
        message = "Change your name and avatar",
        backdrop = backdrop,
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = "Cancel",
                onClick = onDismiss,
                role = GlasmioAlertButtonRole.Cancel
            ),
            GlasmioAlertButtonSpec(
                label = "Save",
                onClick = {
                    scope.launch {
                        prefs.saveProfile(name.text, pfp, null)
                        onDismiss()
                    }
                },
                enabled = name.text.isNotBlank()
            )
        ),
        visible = true
    ) {
        GlasmioAlertTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Your name",
            singleLine = true
        )
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PfpType.entries.filter { it != PfpType.CUSTOM }.forEach { type ->
                val selected = pfp == type.name
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { pfp = type.name },
                    contentAlignment = Alignment.Center
                ) {
                    PfpIcon(
                        type = type,
                        color = if (selected) colors.primaryAccent else colors.mutedText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Text(
            text = if (pfp == PfpType.DEFAULT.name) "Default avatar" else PfpType.valueOf(pfp).label,
            color = colors.mutedText,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
