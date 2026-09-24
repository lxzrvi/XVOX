package com.xvox.music.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.playlist.XvoxHomeLibraryMode

private enum class XvoxBottomEntryKind { DESTINATION, LIKED, PLAYLISTS }

private data class XvoxBottomEntry(
    val id: String,
    val label: String,
    val icon: Int,
    val kind: XvoxBottomEntryKind,
    val destination: XvoxDestination? = null
)

private fun xvoxBottomEntries(extendedSlots: Int): List<XvoxBottomEntry> = buildList {
    add(XvoxBottomEntry("home", "Home", R.drawable.ic_xvox_home, XvoxBottomEntryKind.DESTINATION, XvoxDestination.HOME))
    add(XvoxBottomEntry("search", "Search", R.drawable.ic_xvox_search, XvoxBottomEntryKind.DESTINATION, XvoxDestination.SEARCH))
    if (extendedSlots >= 4) {
        add(XvoxBottomEntry("liked", "Liked Songs", R.drawable.ic_xvox_heart, XvoxBottomEntryKind.LIKED))
    }
    if (extendedSlots >= 5) {
        add(XvoxBottomEntry("playlists", "Playlists", R.drawable.ic_xvox_playlist, XvoxBottomEntryKind.PLAYLISTS))
    }
    add(XvoxBottomEntry("settings", "Settings", R.drawable.ic_xvox_settings, XvoxBottomEntryKind.DESTINATION, XvoxDestination.SETTINGS))
}

/**
 * Floating navigation that retains the original 3-item geometry and grows to balanced 4/5-item
 * slots for Extended layouts. Liked/Playlists are first-class nav actions rather than fake tabs.
 */
@Composable
fun XvoxBottomBar(
    selected: XvoxDestination,
    libraryMode: XvoxHomeLibraryMode,
    extendedSlots: Int = 3,
    onSelected: (XvoxDestination) -> Unit,
    onLikedClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val colors = XvoxTheme.colors
    val chrome = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current
    val slots = extendedSlots.coerceIn(3, 5)
    val entries = xvoxBottomEntries(slots)

    val selectedLibraryKind = if (selected == XvoxDestination.HOME) when (libraryMode) {
        XvoxHomeLibraryMode.LIKED -> XvoxBottomEntryKind.LIKED
        XvoxHomeLibraryMode.PLAYLISTS -> XvoxBottomEntryKind.PLAYLISTS
        else -> null
    } else null
    val selectedIndex = entries.indexOfFirst { it.kind == selectedLibraryKind }
        .takeIf { it >= 0 }
        ?: entries.indexOfFirst { it.destination == selected }.coerceAtLeast(0)

    var position by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex, slots) { position = selectedIndex.toFloat() }

    val motion = rememberXvoxNavigationMotion(position = position)
    val parentShape = RoundedCornerShape(XvoxNavigationGeometry.barRadius)
    val selectorShape = RoundedCornerShape(XvoxNavigationGeometry.selectorBaseRadius)

    val navEdgeBase = com.xvox.music.core.ui.chrome.parseHexColor(chrome.navBorder) ?: colors.cardBorder
    val navEdge = navEdgeBase.copy(alpha = navEdgeBase.alpha * chrome.navBorderAlpha.coerceIn(0f, 1f))
    val pillBase = com.xvox.music.core.ui.chrome.parseHexColor(chrome.pillColor)
        ?: colors.cardElevated.copy(alpha = .42f)
    val pillFill = if (chrome.pillColor.isBlank()) pillBase
        else pillBase.copy(alpha = pillBase.alpha * chrome.pillAlpha.coerceIn(0f, 1f))

    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val actualHostHeight = if (isLandscape) XvoxNavigationGeometry.barHeight else XvoxNavigationGeometry.hostHeight
    val topOffset = if (isLandscape) 0.dp else XvoxNavigationGeometry.hostOverflow
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val barWidth = XvoxNavigationGeometry.barWidth(entries.size)
        .coerceAtMost((screenWidth - 24.dp).coerceAtLeast(180.dp))
    val slotWidth = barWidth / entries.size.toFloat()
    val selectorWidth = XvoxNavigationGeometry.selectorWidth(barWidth, entries.size)
    val selectorStart = slotWidth / 2f - selectorWidth / 2f
    val iconAccent = com.xvox.music.core.ui.chrome.parseHexColor(chrome.pillIconColor) ?: colors.primaryAccent

    Box(
        modifier = modifier
            .width(barWidth)
            .height(actualHostHeight)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = topOffset)
                .size(barWidth, XvoxNavigationGeometry.barHeight)
                .clip(parentShape)
                .background(colors.surface.copy(alpha = chrome.navBgAlpha.coerceIn(0f, 1f)))
                .border(XvoxNavigationGeometry.barBorderWidth, navEdge, parentShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(y = topOffset + XvoxNavigationGeometry.barHeight / 2 - XvoxNavigationGeometry.selectorRestHeight / 2)
                .graphicsLayer {
                    translationX = (selectorStart + slotWidth * motion.position).toPx()
                    shape = selectorShape
                    clip = true
                }
                .size(selectorWidth, XvoxNavigationGeometry.selectorRestHeight)
                .clip(selectorShape)
                .background(pillFill)
                .border(XvoxNavigationGeometry.selectorBorderWidth, navEdge, selectorShape)
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = topOffset)
                .size(barWidth, XvoxNavigationGeometry.barHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            entries.forEachIndexed { index, entry ->
                val interaction = remember(entry.id) { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(interactionSource = interaction, indication = null) {
                            position = index.toFloat()
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            when (entry.kind) {
                                XvoxBottomEntryKind.DESTINATION -> entry.destination?.let { onSelected(it) }
                                XvoxBottomEntryKind.LIKED -> onLikedClick()
                                XvoxBottomEntryKind.PLAYLISTS -> onPlaylistClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    XvoxNavigationItem(
                        icon = entry.icon,
                        label = entry.label,
                        proximity = navigationProximity(position = motion.position, index = index),
                        dragging = false,
                        inactiveColor = colors.mutedText.copy(alpha = .76f),
                        activeColor = iconAccent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
