package com.xvox.music.core.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

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

    val scope = rememberCoroutineScope()
    val latestSelect by rememberUpdatedState(onSelected)
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)

    // Outside a drag, the selector settles to the active tab with a restrained bounce. During a
    // drag, `dragPos` is written straight from the finger so the pill never lags behind it.
    val restPos = remember { Animatable(selectedIndex.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    val trail = remember { Animatable(selectedIndex.toFloat()) }

    fun shown(): Float = if (dragging) dragPos else restPos.value

    LaunchedEffect(selectedIndex) {
        if (!dragging && abs(restPos.value - selectedIndex) > 0.001f) {
            restPos.animateTo(
                selectedIndex.toFloat(),
                spring(dampingRatio = 0.62f, stiffness = 520f)
            )
        }
    }
    // The delayed trail adds a brief stretch while the selector is moving, then settles cleanly.
    LaunchedEffect(Unit) {
        snapshotFlow { shown() }.collectLatest { target ->
            trail.animateTo(target, spring(dampingRatio = 0.45f, stiffness = 300f))
        }
    }
    val shownPos = shown()
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
                    val p = if (dragging) dragPos else restPos.value
                    translationX = ((slotWidth - selectorWidth) / 2f + slotWidth * p).toPx()
                    // Motion stretches the pill in travel direction while the delayed trail
                    // catches up, then it returns to its normal rounded geometry at rest.
                    val stretch = abs(p - trail.value).coerceAtMost(0.6f)
                    scaleX = 1f + stretch * 0.45f
                    scaleY = 1f - stretch * 0.22f
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
                            // A simple press/release still selects immediately.
                            val change = currentEvent.changes.firstOrNull { it.id == down.id }
                            if (change != null && !change.pressed) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                latestSelect(destinations[indexAt(down.position.x)])
                            }
                            return@awaitEachGesture
                        }

                        // A long hold previews under the finger; the route commits only on release.
                        dragPos = restPos.value
                        dragging = true
                        var hoverIndex = latestSelectedIndex
                        var finished = false
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            fun follow(x: Float) {
                                // Direct finger mapping deliberately bypasses the resting spring.
                                dragPos = (x / slotPx - 0.5f).coerceIn(0f, destinations.lastIndex.toFloat())
                                val index = indexAt(x)
                                if (index != hoverIndex) {
                                    hoverIndex = index
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                            follow(longPress.position.x)
                            finished = drag(down.id) { change ->
                                change.consume()
                                follow(change.position.x)
                            }
                        } finally {
                            val from = dragPos
                            dragging = false
                            val target = if (finished) hoverIndex else latestSelectedIndex
                            // Leave from the physical finger position, then bounce into the tab.
                            scope.launch {
                                restPos.snapTo(from)
                                restPos.animateTo(
                                    target.toFloat(),
                                    spring(dampingRatio = 0.55f, stiffness = 480f)
                                )
                            }
                            if (finished && target != latestSelectedIndex) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                latestSelect(destinations[target])
                            }
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
                        proximity = navigationProximity(position = shownPos, index = index),
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
