package com.xvox.music.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxBottomBar(
    selected: XvoxDestination,
    onSelected: (XvoxDestination) -> Unit,
    /** Retained for call-site compatibility; the Bottom Bar owns long-hold drag selection. */
    @Suppress("UNUSED_PARAMETER") onLongPressSettings: () -> Unit = {},
    /** Navigation artwork comes only from Chrome's navigationImageUri, never the Header. */
    modifier: Modifier = Modifier
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val colors = XvoxTheme.colors
    val chrome = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current
    val navBarHeight = chrome.navigationBarHeight.coerceIn(52f, 88f).dp
    val availableWidth = (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp - 24.dp)
        .coerceAtLeast(190.dp)
    val navBarWidth = chrome.navigationBarWidth.coerceIn(190f, 380f).dp.coerceAtMost(availableWidth)
    val slotWidth = navBarWidth / 3f
    val selectorWidth = (slotWidth - 8.dp).coerceAtLeast(52.dp)
    val selectorHeight = (navBarHeight - 8.dp).coerceAtLeast(44.dp)
    val destinations = XvoxDestination.entries
    val selectedIndex = destinations.indexOf(selected)

    var position by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    val latestSelect by rememberUpdatedState(onSelected)
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)

    LaunchedEffect(selectedIndex, dragging) {
        if (!dragging) position = selectedIndex.toFloat()
    }

    val motion = rememberXvoxNavigationMotion(position = position)
    val parentShape = RoundedCornerShape(navBarHeight / 2f)
    val selectorShape = RoundedCornerShape(selectorHeight / 2f)

    // Navbar and Mini Player retain independent transparency sliders in both themes. Coupling the
    // dark navbar to miniBgAlpha made the navbar slider appear broken and kept its selector heavy.
    val navSurfaceAlpha = chrome.navBgAlpha.coerceIn(0f, 1f)
    val navEdgeBase = com.xvox.music.core.ui.chrome.parseHexColor(chrome.navBorder) ?: colors.cardBorder
    val navBorderAlpha = chrome.navBorderAlpha.coerceIn(.42f, 1f)
    val navEdge = navEdgeBase.copy(
        alpha = navEdgeBase.alpha * navBorderAlpha * navSurfaceAlpha
    )
    // Match Settings' Appearance option-pill material (for example the Font choice box), not
    // the larger section-card material. The selector deliberately follows the existing navbar
    // transparency control without adding a separate slider: its transparency is always one half
    // of the navbar transparency (6% -> 3%, 100% -> 50%).
    val navbarTransparency = 1f - navSurfaceAlpha
    val selectorSurfaceAlpha = (1f - navbarTransparency * .5f).coerceIn(.5f, 1f)
    val pillEdgeBase = com.xvox.music.core.ui.chrome.parseHexColor(chrome.pillBorder) ?: navEdgeBase
    // The active selector has the same option-pill surface as Settings, while its edge remains a
    // quiet outline.
    val pillEdgeStrength = if (!colors.isLight && chrome.pillBorder.isBlank()) .14f else 1f
    val pillEdge = pillEdgeBase.copy(
        alpha = pillEdgeBase.alpha * navBorderAlpha * selectorSurfaceAlpha * pillEdgeStrength
    )
    val defaultNavSurface = colors.background
    val appliedNavSurfaceAlpha = navSurfaceAlpha
    // Override only the material's opacity here. `colors.cardElevated` is the Settings option
    // pill material in both themes, while its stored alpha can reflect the unrelated page-card
    // transparency preference. Replacing—not multiplying—its alpha preserves the exact required
    // half-navbar relationship for the selector.
    val pillFill = colors.cardElevated.copy(alpha = selectorSurfaceAlpha)

    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // Device-orientation baselines requested for the chrome remain separate from the editable
    // offset below, so a user's custom placement is always additive.
    val defaultPlacementY = if (isLandscape) 31.5.dp else 10.5.dp
    val actualHostHeight = if (isLandscape) navBarHeight else navBarHeight + 20.dp
    val topOffset = if (isLandscape) 0.dp else XvoxNavigationGeometry.hostOverflow

    Box(
        modifier = modifier
            // Move the entire touch and paint surface together, so controls stay where they appear.
            .offset(
                x = chrome.navigationBarOffsetX.coerceIn(-220f, 220f).dp,
                y = defaultPlacementY + chrome.navigationBarOffsetY.coerceIn(-260f, 260f).dp
            )
            .width(navBarWidth)
            .height(actualHostHeight)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = topOffset)
                .size(navBarWidth, navBarHeight)
                .clip(parentShape)
                .border(
                    width = chrome.navBorderWidth.coerceIn(0f, 4f).dp,
                    color = navEdge,
                    shape = parentShape
                )
        ) {
            if (chrome.navigationImageUri.isNotBlank()) {
                AsyncImage(
                    model = chrome.navigationImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = navSurfaceAlpha }
                )
                // A fully opaque custom image is the surface itself.  Do not put a theme wash
                // over it at 0% transparency; the image should be seen exactly as chosen.
            } else {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(defaultNavSurface.copy(alpha = appliedNavSurfaceAlpha))
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    y = topOffset + navBarHeight / 2 - selectorHeight / 2
                )
                .graphicsLayer {
                    // Derive directly from each equal slot's centre, rather than a nominal travel
                    // constant. The selected icon and pill therefore share one exact centre for
                    // Home, Search, and Settings at every allowed navbar width.
                    translationX = (
                        (slotWidth - selectorWidth) / 2f + slotWidth * motion.position
                    ).toPx()
                    shape = selectorShape
                    clip = true
                }
                .size(selectorWidth, selectorHeight)
                .clip(selectorShape)
                .background(pillFill)
                .border(
                    width = chrome.pillBorderWidth.coerceIn(0f, 4f).dp,
                    color = pillEdge,
                    shape = selectorShape
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = topOffset)
                .size(navBarWidth, navBarHeight)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val slotPx = size.width / destinations.size.toFloat()
                        fun indexAt(x: Float) = (x / slotPx).toInt().coerceIn(0, destinations.lastIndex)

                        val longPress = awaitLongPressOrCancellation(down.id)
                        if (longPress == null) {
                            // No long press: a released finger is an ordinary tap.
                            val change = currentEvent.changes.firstOrNull { it.id == down.id }
                            if (change != null && !change.pressed) {
                                val index = indexAt(down.position.x)
                                position = index.toFloat()
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                latestSelect(destinations[index])
                            }
                            return@awaitEachGesture
                        }

                        // A long hold only previews the destination. It commits after a normal
                        // drag release, so routes do not change while the finger is travelling.
                        dragging = true
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            var hoverIndex = latestSelectedIndex

                            fun follow(x: Float) {
                                // During the drag only the selector pill follows the finger.
                                position = (x / slotPx - 0.5f).coerceIn(0f, destinations.lastIndex.toFloat())
                                val index = indexAt(x)
                                if (index != hoverIndex) {
                                    hoverIndex = index
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }

                            follow(longPress.position.x)
                            val finished = drag(down.id) { change ->
                                change.consume()
                                follow(change.position.x)
                            }

                            // Commit only on an uncancelled finger release.
                            if (finished) {
                                position = hoverIndex.toFloat()
                                if (hoverIndex != latestSelectedIndex) {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                    latestSelect(destinations[hoverIndex])
                                }
                            }
                        } finally {
                            dragging = false
                        }
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEachIndexed { index, destination ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Active icon colour: pill icon setting wins, otherwise the accent.
                    val iconAccent = com.xvox.music.core.ui.chrome.parseHexColor(chrome.pillIconColor)
                        ?: colors.primaryAccent
                    XvoxNavigationItem(
                        destination = destination,
                        proximity = navigationProximity(position = motion.position, index = index),
                        dragging = dragging,
                        inactiveColor = colors.mutedText.copy(alpha = 0.76f),
                        activeColor = iconAccent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
