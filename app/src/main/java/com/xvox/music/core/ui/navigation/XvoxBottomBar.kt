package com.xvox.music.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxBottomBar(
    selected: XvoxDestination,
    onSelected: (XvoxDestination) -> Unit,
    /** The same transactional Mini Player / Navbar editor is available from a navbar long press. */
    onLongPressSettings: () -> Unit = {},
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
    val navSurfaceAlpha = chrome.navBgAlpha.coerceIn(0f, 1f)
    val navEdgeBase = com.xvox.music.core.ui.chrome.parseHexColor(chrome.navBorder) ?: colors.cardBorder
    // At 100% transparency the bar's surface chrome disappears too; controls remain usable.
    val navEdge = navEdgeBase.copy(alpha = navEdgeBase.alpha * chrome.navBorderAlpha.coerceIn(0f, 1f) * navSurfaceAlpha)
    val pillBase = com.xvox.music.core.ui.chrome.parseHexColor(chrome.pillColor)
        // The bar itself follows the app background; this is the one quietly elevated element
        // that keeps the active destination legible in either theme.
        ?: colors.cardElevated.copy(alpha = if (colors.isLight) .62f else .54f)
    val customImageSurface = chrome.navigationImageUri.isNotBlank()
    val pillFill = if (chrome.pillColor.isBlank()) {
        // At 0% transparency a custom navbar image is the surface itself—do not wash a default
        // theme pill over it. A deliberately chosen custom pill colour still remains respected.
        val defaultPillAlpha = if (customImageSurface) {
            // Preserve the previously requested completely clear custom-image endpoint.
            navSurfaceAlpha * (1f - navSurfaceAlpha)
        } else {
            // Even when the navbar surface is transparent, retain a quiet active-destination
            // pill; otherwise the three fixed destinations lose their only selected state.
            .30f + .28f * navSurfaceAlpha
        }
        pillBase.copy(alpha = pillBase.alpha * defaultPillAlpha)
    } else pillBase.copy(alpha = pillBase.alpha * chrome.pillAlpha.coerceIn(0f, 1f) * navSurfaceAlpha)

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
                        .graphicsLayer { alpha = navSurfaceAlpha }
                )
                // A fully opaque custom image is the surface itself.  Do not put a theme wash
                // over it at 0% transparency; the image should be seen exactly as chosen.
            } else {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(colors.background.copy(alpha = navSurfaceAlpha))
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .pointerInput(destination, onLongPressSettings) {
                            detectTapGestures(
                                onLongPress = {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                    onLongPressSettings()
                                },
                                onTap = {
                                    position = index.toFloat()
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                    onSelected(destination)
                                }
                            )
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
