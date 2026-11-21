package com.xvox.music.core.design.kit

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxFontOptions
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferencesRepository
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceGroupTitle
import io.github.neoruaa.glasmio.preference.GlasmioSwitchPreference
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
import kotlinx.coroutines.launch

/**
 * Home layout, deliberately NOT a plain list: every option is its own small glass card in a
 * two-column grid, and the choices unfold inside the card as Glasmio segmented controls. Tapping a
 * card collapses the others, so the page stays short while every value is visible.
 */
@Composable
fun XvoxStageHomeLayoutPage(
    prefs: UserPreferencesRepository,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val config by prefs.homePresentation.collectAsState(
        initial = com.xvox.music.features.home.HomePresentation()
    )
    val hideRecents by prefs.hideRecentlyPlayed.collectAsState(initial = false)
    val glass = LocalGlassColors.current

    GlasmioPinnedListPage(
        title = "Home layout",
        onNavigateBack = onBack,
        bottomPadding = 132.dp,
        backgroundColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LayoutCard(
                    title = "All songs",
                    value = if (config.style == "mosaic1") "Mosaic" else "List",
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f)
                ) {
                    GlasmioSegmentedControl(
                        items = listOf("uniform" to "List", "mosaic1" to "Mosaic"),
                        selected = config.style,
                        onSelected = { scope.launch { prefs.setHomeLayoutStyle(it) } },
                        backdrop = backdrop,
                        height = 32.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LayoutCard(
                    title = "Scrolling",
                    value = if (config.direction == "horizontal") "Pages" else "Vertical",
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f)
                ) {
                    GlasmioSegmentedControl(
                        items = listOf("vertical" to "V", "horizontal" to "Pages"),
                        selected = config.direction,
                        onSelected = { scope.launch { prefs.setHomeScrollDirection(it) } },
                        backdrop = backdrop,
                        height = 32.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LayoutCard(
                    title = "Portrait columns",
                    value = "${config.portraitColumns}",
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f)
                ) {
                    GlasmioSegmentedControl(
                        items = (3..6).map { it to "$it" },
                        selected = config.portraitColumns,
                        onSelected = { scope.launch { prefs.setHomePortraitColumns(it) } },
                        backdrop = backdrop,
                        height = 32.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LayoutCard(
                    title = "Landscape columns",
                    value = "${config.landscapeColumns}",
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f)
                ) {
                    GlasmioSegmentedControl(
                        items = (8..10).map { it to "$it" },
                        selected = config.landscapeColumns,
                        onSelected = { scope.launch { prefs.setHomeLandscapeColumns(it) } },
                        backdrop = backdrop,
                        height = 32.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        if (config.direction == "horizontal") {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    LayoutCard(
                        title = "Rows per page",
                        value = "${config.rows}",
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f)
                    ) {
                        GlasmioSegmentedControl(
                            items = listOf(3, 4, 5, 6).map { it to "$it" },
                            selected = config.rows,
                            onSelected = { scope.launch { prefs.setHomeHorizontalRows(it) } },
                            backdrop = backdrop,
                            height = 32.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        item {
            GlasmioPreferenceGroupTitle("Recently played")
            CardGroup(backdrop) { cardBackdrop ->
                GlasmioSwitchPreference(
                    title = { Text("Hide the row") },
                    description = "Keeps Home focused on the mood cards and All songs",
                    checked = hideRecents,
                    onCheckedChange = { scope.launch { prefs.setHideRecentlyPlayed(it) } },
                    showTopSeparator = false
                )
            }
        }
    }
}

/** One option as a small clear-glass card: label, current value, and its choices underneath. */
@Composable
private fun LayoutCard(
    title: String,
    value: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glass = LocalGlassColors.current
    GlassSurface(
        backdrop = backdrop,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.animateContentSize(tween(240))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = title,
                color = glass.secondaryContent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = glass.content,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/**
 * Library and scanning: sort order, ignored material and the density switches. Same Glasmio card
 * language, one page per group.
 */
@Composable
fun XvoxStageLibraryPage(
    prefs: UserPreferencesRepository,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sortOrder by prefs.sortOrder.collectAsState(initial = "A-Z")
    val hideRecents by prefs.hideRecentlyPlayed.collectAsState(initial = false)
    val fourRows by prefs.fourRowsGrid.collectAsState(initial = false)
    val ignoreBelowSec by prefs.ignoreBelowSec.collectAsState(initial = 0)

    GlasmioPinnedListPage(
        title = "Library & scanning",
        onNavigateBack = onBack,
        bottomPadding = 132.dp,
        backgroundColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GlasmioPreferenceGroupTitle("Sorting")
            CardGroup(backdrop) { cardBackdrop ->
                PickerRow(
                    title = "Default sort",
                    options = listOf("A-Z" to "Title A–Z", "Z-A" to "Title Z–A", "Newest" to "Newest first", "Oldest" to "Oldest first"),
                    selected = sortOrder,
                    onSelected = { scope.launch { prefs.setSortOrder(it) } },
                    backdrop = cardBackdrop,
                    showTopSeparator = false
                )
            }
        }
        item {
            GlasmioPreferenceGroupTitle("Scanning")
            CardGroup(backdrop) { cardBackdrop ->
                PickerRow(
                    title = "Ignore songs shorter than",
                    options = listOf(0 to "Off", 30 to "30 s", 60 to "60 s", 90 to "90 s"),
                    selected = ignoreBelowSec,
                    onSelected = { scope.launch { prefs.setIgnoreBelowSec(it) } },
                    backdrop = cardBackdrop,
                    showTopSeparator = false
                )
            }
        }
        item {
            GlasmioPreferenceGroupTitle("List density")
            CardGroup(backdrop) { cardBackdrop ->
                GlasmioSwitchPreference(
                    title = { Text("Four rows per grid row") },
                    description = "Tighter artwork grid",
                    checked = fourRows,
                    onCheckedChange = { scope.launch { prefs.setFourRowsGrid(it) } },
                    showTopSeparator = false
                )
                GlasmioSwitchPreference(
                    title = { Text("Hide recently played") },
                    description = "Also hides the Home row",
                    checked = hideRecents,
                    onCheckedChange = { scope.launch { prefs.setHideRecentlyPlayed(it) } }
                )
            }
        }
    }
}

/**
 * Interface: the typeface, display size, orientation lock and status-bar policy, all as Glasmio
 * controls with their current value shown on the row.
 */
@Composable
fun XvoxStageInterfacePage(
    prefs: UserPreferencesRepository,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val fontFamily by prefs.fontFamily.collectAsState(initial = "inter")
    val displaySize by prefs.displaySize.collectAsState(initial = "medium")
    val orientation by prefs.appOrientation.collectAsState(initial = "auto")
    val hideStatusBar by prefs.hideStatusBar.collectAsState(initial = false)
    val context = androidx.compose.ui.platform.LocalContext.current
    val safeGraphics = com.xvox.music.core.XvoxGraphicsGuard.safe.value

    GlasmioPinnedListPage(
        title = "Interface",
        onNavigateBack = onBack,
        bottomPadding = 132.dp,
        backgroundColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GlasmioPreferenceGroupTitle("Type")
            CardGroup(backdrop) { cardBackdrop ->
                PickerRow(
                    title = "Typeface",
                    options = XvoxFontOptions.map { it.key to it.label },
                    selected = fontFamily,
                    onSelected = { scope.launch { prefs.setFontFamily(it) } },
                    backdrop = cardBackdrop,
                    showTopSeparator = false
                )
                PickerRow(
                    title = "Display size",
                    options = listOf("small" to "Small", "medium" to "Medium", "large" to "Large"),
                    selected = displaySize,
                    onSelected = { scope.launch { prefs.setDisplaySize(it) } },
                    backdrop = cardBackdrop
                )
            }
        }
        item {
            GlasmioPreferenceGroupTitle("Graphics")
            CardGroup(backdrop) { cardBackdrop ->
                GlasmioSwitchPreference(
                    title = { Text("Safe graphics") },
                    description = "Skips the capture, blur and refraction layers. Turn this on if " +
                        "the app closes itself on this device; turn it off to get liquid glass back.",
                    checked = safeGraphics,
                    onCheckedChange = { com.xvox.music.core.XvoxGraphicsGuard.set(context, it) },
                    showTopSeparator = false
                )
            }
        }
        item {
            GlasmioPreferenceGroupTitle("Window")
            CardGroup(backdrop) { cardBackdrop ->
                PickerRow(
                    title = "Orientation",
                    options = listOf("auto" to "Auto", "portrait" to "Portrait", "landscape" to "Landscape"),
                    selected = orientation,
                    onSelected = { scope.launch { prefs.setAppOrientation(it) } },
                    backdrop = cardBackdrop,
                    showTopSeparator = false
                )
                GlasmioSwitchPreference(
                    title = { Text("Hide the status bar") },
                    description = "Immersive, edge-to-edge pages",
                    checked = hideStatusBar,
                    onCheckedChange = { scope.launch { prefs.setHideStatusBar(it) } }
                )
            }
        }
    }
}
