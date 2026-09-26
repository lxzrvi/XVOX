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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxBottomBar(
    selected: XvoxDestination,
    onSelected: (XvoxDestination) -> Unit,
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
    val selectorTravel = navBarWidth - selectorWidth - 8.dp
    val selectorHeight = (navBarHeight - 8.dp).coerceAtLeast(44.dp)
    val destinations = XvoxDestination.entries
    val selectedIndex = destinations.indexOf(selected)

    var position by remember { mutableFloatStateOf(selectedIndex.toFloat()) }

    LaunchedEffect(selectedIndex) {
        position = selectedIndex.toFloat()
    }

    val motion = rememberXvoxNavigationMotion(position = position)
    val parentShape = RoundedCornerShape(navBarHeight / 2f)
    val selectorShape = RoundedCornerShape(selectorHeight / 2f)

    // Appearance › Home chrome overrides for the floating nav bar and its travelling pill.
    val navEdgeBase = com.xvox.music.core.ui.chrome.parseHexColor(chrome.navBorder) ?: colors.cardBorder
    val navEdge = navEdgeBase.copy(alpha = navEdgeBase.alpha * chrome.navBorderAlpha.coerceIn(0f, 1f))
    val pillBase = com.xvox.music.core.ui.chrome.parseHexColor(chrome.pillColor)
        ?: colors.cardElevated.copy(alpha = 0.42f)
    val pillFill = if (chrome.pillColor.isBlank()) pillBase
        else pillBase.copy(alpha = pillBase.alpha * chrome.pillAlpha.coerceIn(0f, 1f))

    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val actualHostHeight = if (isLandscape) navBarHeight else navBarHeight + 20.dp
    val topOffset = if (isLandscape) 0.dp else XvoxNavigationGeometry.hostOverflow

    Box(
        modifier = modifier
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
                    width = XvoxNavigationGeometry.barBorderWidth,
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
                        .graphicsLayer { alpha = chrome.navBgAlpha.coerceIn(0f, 1f) }
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .background(colors.cardElevated.copy(alpha = .42f * chrome.navBgAlpha.coerceIn(0f, 1f)))
                )
            } else {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(colors.cardElevated.copy(alpha = chrome.navBgAlpha.coerceIn(0f, 1f)))
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
                    // Three destinations remain fixed; only their shared bar width is adjustable.
                    translationX = (4.dp + selectorTravel * (motion.position / 2f)).toPx()
                    shape = selectorShape
                    clip = true
                }
                .size(selectorWidth, selectorHeight)
                .clip(selectorShape)
                .background(pillFill)
                .border(
                    width = XvoxNavigationGeometry.selectorBorderWidth,
                    color = navEdge,
                    shape = selectorShape
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = topOffset)
                .size(navBarWidth, navBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEachIndexed { index, destination ->
                val interaction = remember(destination) { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) {
                            position = index.toFloat()
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            onSelected(destination)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Active icon colour: pill icon setting wins, otherwise the accent.
                    val iconAccent = com.xvox.music.core.ui.chrome.parseHexColor(chrome.pillIconColor)
                        ?: colors.primaryAccent
                    XvoxNavigationItem(
                        destination = destination,
                        proximity = navigationProximity(position = motion.position, index = index),
                        dragging = false,
                        inactiveColor = colors.mutedText.copy(alpha = 0.76f),
                        activeColor = iconAccent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
