package com.xvox.music.core.design.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.kit.icons.XvoxSfIcons
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.navigation.XvoxNavTab
import io.github.neoruaa.glasmio.core.GlasmioMotion
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import kotlin.math.roundToInt

/** Real SF Symbols geometry (house.fill, music.microphone, opticaldisc.fill, music.note.list). */
val XvoxNavTab.sfIcon: ImageVector
    get() = when (this) {
        XvoxNavTab.HOME -> XvoxSfIcons.HouseFill
        XvoxNavTab.ARTIST -> XvoxSfIcons.MusicMicrophone
        XvoxNavTab.ALBUM -> XvoxSfIcons.OpticaldiscFill
        XvoxNavTab.PLAYLIST -> XvoxSfIcons.MusicNoteList
    }

internal val NavBarHeight = 64.dp
internal val NavSlotWidth = 62.dp
private val PillWidth = 54.dp
private val PillHeight = 54.dp

/**
 * iOS navigation bar on Glasmio liquid glass.
 *
 *  - The selection pill is draggable: your finger carries it, the pill rubber-bands at both ends,
 *    and on release it settles onto the nearest destination with Glasmio's native spring while the
 *    destination is confirmed live (so the backdrop gradient starts changing mid-drag).
 *  - The whole bar is real glass sampling the live gradient layer, so the blur updates every frame
 *    instead of being a static frost.
 *  - Search lives in its own glass circle, like the split-search iOS tab bar.
 */
@Composable
fun XvoxIosNavBar(
    selected: XvoxNavTab,
    onSelect: (XvoxNavTab) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<XvoxNavTab> = listOf(
        XvoxNavTab.HOME,
        XvoxNavTab.ARTIST,
        XvoxNavTab.ALBUM,
        XvoxNavTab.PLAYLIST
    )
) {
    val colors = XvoxTheme.colors
    val haptics = LocalHapticFeedback.current
    val backdrop = LocalXvoxGlassBackdrop.current ?: rememberLayerBackdrop()
    val slotInteraction = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    val slotPx = with(density) { NavSlotWidth.toPx() }
    val maxIndex = (tabs.size - 1).coerceAtLeast(0)
    val maxPx = slotPx * maxIndex

    val pill = remember { Animatable(0f) }
    var dragging by remember { mutableFloatStateOf(0f) }
    var dragPx by remember { mutableFloatStateOf(0f) }
    var lastTicked by remember { mutableIntStateOf(-1) }

    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)
    val currentOnSelect by rememberUpdatedState(onSelect)

    // Settle whenever the selection changes from anywhere else (tap, back stack, deep link).
    LaunchedEffect(selectedIndex) {
        if (dragging == 0f) pill.animateTo(slotPx * selectedIndex, GlasmioMotion.spring())
    }

    val offsetPx = if (dragging == 1f) dragPx else pill.value
    val pressedScale by animateFloatAsState(
        targetValue = if (dragging == 1f) 1.06f else 1f,
        animationSpec = GlasmioMotion.spring(),
        label = "iosNavPillPress"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassSurface(
            modifier = Modifier.height(NavBarHeight),
            backdrop = backdrop,
            shape = CircleShape,
            emphasis = GlassEmphasis.Regular
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 5.dp)
                    // Finger-driven pill: drag anywhere on the bar and the selection follows.
                    .pointerInput(tabs.size, slotPx) {
                        detectDragGestures(
                            onDragStart = {
                                dragging = 1f
                                dragPx = pill.value
                                lastTicked = -1
                            },
                            onDragEnd = {
                                val target = (dragPx / slotPx).roundToInt().coerceIn(0, maxIndex)
                                dragging = 0f
                                currentOnSelect(tabs[target])
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDragCancel = {
                                dragging = 0f
                                dragPx = pill.value
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            val raw = dragPx + dragAmount.x
                            // Rubber band outside the first/last slot instead of hard-clamping.
                            dragPx = when {
                                raw < 0f -> raw * .32f
                                raw > maxPx -> maxPx + (raw - maxPx) * .32f
                                else -> raw
                            }
                            val hovered = (dragPx / slotPx).roundToInt().coerceIn(0, maxIndex)
                            if (hovered != lastTicked) {
                                if (lastTicked >= 0) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                lastTicked = hovered
                                // Confirm live so the gradient behind the glass starts moving
                                // while the finger is still down.
                                currentOnSelect(tabs[hovered])
                            }
                        }
                    }
            ) {
                // The pill itself: same geometry as the app's navigation selector.
                Box(
                    modifier = Modifier
                        .offset(x = with(density) { offsetPx.toDp() }, y = 5.dp)
                        .size(PillWidth, PillHeight)
                        .graphicsLayer {
                            scaleX = pressedScale
                            scaleY = pressedScale
                        }
                        .clip(CircleShape)
                        .background(colors.cardElevated.copy(alpha = .42f))
                )
                Row(Modifier.fillMaxHeight()) {
                    tabs.forEach { tab ->
                        val active = tab == selected
                        Box(
                            modifier = Modifier
                                .width(NavSlotWidth)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = slotInteraction,
                                    indication = null
                                ) {
                                    if (!active) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelect(tab)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.sfIcon,
                                contentDescription = tab.label,
                                tint = if (active) colors.primaryText else colors.mutedText.copy(alpha = .76f),
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                }
            }
        }

        GlassSurface(
            modifier = Modifier.size(NavBarHeight),
            backdrop = backdrop,
            shape = CircleShape,
            emphasis = GlassEmphasis.Regular,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSearch()
            }
        ) {
            Icon(
                imageVector = XvoxSfIcons.Magnifyingglass,
                contentDescription = "Search",
                tint = colors.primaryText,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}
