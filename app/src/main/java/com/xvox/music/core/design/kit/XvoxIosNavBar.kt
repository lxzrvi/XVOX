package com.xvox.music.core.design.kit

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.navigation.XvoxNavTab
import io.github.neoruaa.glasmio.core.GlasmioMotion
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop

/**
 * SF Symbols geometry used by the iOS navigation bar, shipped as vector resources so the bar never
 * depends on a bundled Apple font: house.fill, music.mic, rectangle.stack.fill, music.note.list.
 */
val XvoxNavTab.sfIconRes: Int
    get() = when (this) {
        XvoxNavTab.HOME -> R.drawable.ic_xvox_sf_house
        XvoxNavTab.ARTIST -> R.drawable.ic_xvox_sf_mic
        XvoxNavTab.ALBUM -> R.drawable.ic_xvox_sf_stack
        XvoxNavTab.PLAYLIST -> R.drawable.ic_xvox_sf_note_list
    }

private val BarHeight = 64.dp
private val SlotWidth = 62.dp
private val LensSize = 52.dp

/**
 * iOS navigation bar drawn with Glasmio's liquid-glass material.
 *
 * One floating glass capsule carries the music destinations (Home, Artists, Albums, Playlists) and
 * a separate glass circle carries search, exactly like the split-search iOS tab bar. The selected
 * lens slides between slots with Glasmio's native spring response, so switching destinations is a
 * continuous movement instead of a cross-fade.
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

    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)
    val lensOffset by animateDpAsState(
        targetValue = SlotWidth * selectedIndex,
        animationSpec = GlasmioMotion.spring(),
        label = "iosNavLens"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassSurface(
            modifier = Modifier.height(BarHeight),
            backdrop = backdrop,
            shape = CircleShape,
            emphasis = GlassEmphasis.Regular
        ) {
            Box(Modifier.fillMaxHeight().padding(horizontal = 5.dp)) {
                // The travelling selection lens.
                Box(
                    modifier = Modifier
                        .offset(x = lensOffset, y = 6.dp)
                        .size(LensSize)
                        .clip(CircleShape)
                        .background(colors.cardElevated.copy(alpha = .38f))
                )
                Row(Modifier.fillMaxHeight()) {
                    tabs.forEach { tab ->
                        val active = tab == selected
                        val iconScale by animateFloatAsState(
                            targetValue = if (active) 1f else .92f,
                            animationSpec = GlasmioMotion.spring(),
                            label = "iosNavIconScale"
                        )
                        Box(
                            modifier = Modifier
                                .width(SlotWidth)
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
                                painter = painterResource(tab.sfIconRes),
                                contentDescription = tab.label,
                                tint = if (active) colors.primaryText else colors.mutedText.copy(alpha = .78f),
                                modifier = Modifier
                                    .size(25.dp)
                                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                            )
                        }
                    }
                }
            }
        }

        // Split search control: its own glass circle, like the iOS tab bar.
        GlassSurface(
            modifier = Modifier.size(BarHeight),
            backdrop = backdrop,
            shape = CircleShape,
            emphasis = GlassEmphasis.Regular,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSearch()
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_sf_search),
                contentDescription = "Search",
                tint = colors.primaryText,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}
