// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/miniplayer/XvoxDock.kt
package com.xvox.music.core.ui.miniplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.blur.LocalXvoxShellContentSky
import com.xvox.music.core.ui.blur.LocalXvoxSurfaceSky
import com.xvox.music.core.ui.blur.XvoxBlurLevel
import com.xvox.music.core.ui.blur.xvoxLiveBlur
import com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle
import com.xvox.music.core.ui.navigation.XvoxDestination
import com.xvox.music.core.ui.navigation.navigationIconRes

/** 0 = normal stacked mini player + navbar, 1 = docked: [Home] [mini player] [Search|Settings]. */
val LocalXvoxDockProgress = staticCompositionLocalOf<() -> Float> { { 0f } }

/** Last painted bounds (root coordinates) of the Mini Player card; Now Playing "Grow" reads it. */
object XvoxMiniBounds {
    @Volatile var rect: Rect? = null
    /** Mini Player cover slot, and Now Playing's cover slot (root coordinates): the Grow cover travels between them. */
    @Volatile var artRect: Rect? = null
    @Volatile var coverRect: Rect? = null
    /** iOS Now Playing always uses the in-place "Grow" handoff (set by the shell from the layout setting). */
    var iosGrow by androidx.compose.runtime.mutableStateOf(false)
}

object XvoxDockGeometry {
    val lane = 60.dp
    val laneBottom = 12.dp
    val edge = 12.dp
    val gap = 8.dp
    val pillWidth = 124.dp
    /** Mini card start (from screen edge) and the room taken on the right, when docked. */
    val miniStart = edge + lane + gap
    val miniEndReserve = gap + pillWidth + edge
    /** Card is 56dp tall; its centre must sit on the lane centre (laneBottom + lane/2). */
    val miniBottomPadding = laneBottom + lane / 2 - 28.dp

    /** The docked lane IS the real navbar: same height, and its visible bottom sits 5.5dp above the inset (+ user Y offset). */
    fun laneBottomFor(navigationBarOffsetY: Float): androidx.compose.ui.unit.Dp =
        (5.5f - navigationBarOffsetY.coerceIn(-260f, 260f)).dp.coerceAtLeast(0.dp)
    fun miniBottomFor(laneBottom: androidx.compose.ui.unit.Dp, lane: androidx.compose.ui.unit.Dp) =
        laneBottom + lane / 2 - 28.dp
}

/** The two navbar pieces that appear while docked: a Home circle and a Search/Settings pill. */
@Composable
fun BoxScope.XvoxDockPieces(
    progress: () -> Float,
    selected: XvoxDestination,
    onSelected: (XvoxDestination) -> Unit,
    laneHeight: androidx.compose.ui.unit.Dp = XvoxDockGeometry.lane,
    laneBottom: androidx.compose.ui.unit.Dp = XvoxDockGeometry.laneBottom
) {
    val colors = XvoxTheme.colors
    val chrome = LocalXvoxChromeStyle.current
    val sky = LocalXvoxShellContentSky.current ?: LocalXvoxSurfaceSky.current
    val glass = chrome.glassBlurEnabled && chrome.glassBlurIntensity > .001f
    val base = colors.background.copy(alpha = if (glass) .55f else .96f)
    val accent = colors.primaryAccent
    val inactive = colors.mutedText
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = XvoxDockGeometry.edge, end = XvoxDockGeometry.edge, bottom = laneBottom)
            .graphicsLayer { alpha = progress().coerceIn(0f, 1f) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            Modifier
                .graphicsLayer { translationX = (1f - progress()) * 56.dp.toPx() }
                .size(laneHeight)
                .xvoxLiveBlur(CircleShape, XvoxBlurLevel.Bar, base, sky, glassEdge = true)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelected(XvoxDestination.HOME) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(navigationIconRes(XvoxDestination.HOME)), null,
                tint = if (selected == XvoxDestination.HOME) accent else inactive, modifier = Modifier.size(26.dp)
            )
        }
        Row(
            Modifier
                .graphicsLayer { translationX = -(1f - progress()) * 56.dp.toPx() }
                .width(XvoxDockGeometry.pillWidth)
                .height(laneHeight)
                .xvoxLiveBlur(RoundedCornerShape(laneHeight / 2), XvoxBlurLevel.Bar, base, sky, glassEdge = true),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (d in listOf(XvoxDestination.SEARCH, XvoxDestination.SETTINGS)) {
                Box(
                    Modifier.size(52.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelected(d) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(navigationIconRes(d)), null,
                        tint = if (selected == d) accent else inactive, modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
