package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.chrome.isNowPlayingItemHidden
import com.xvox.music.core.ui.chrome.normalizedNowPlayingGrid
import com.xvox.music.core.ui.chrome.nowPlayingItemScale
import com.xvox.music.core.ui.chrome.withNowPlayingItemHidden
import com.xvox.music.core.ui.chrome.withNowPlayingItemScale
import kotlin.math.abs

/** Stable IDs for the actual bottom-box items—not a separate replica editor. */
object NowPlayingEditItem {
    const val Utility = "utility"
    const val Actions = "actions"
    const val Metadata = "metadata"
    const val Progress = "progress"
    const val Shuffle = "shuffle"
    const val Repeat = "repeat"
    const val Previous = "previous"
    const val Play = "play"
    const val Next = "next"
    const val Brand = "brand"
}

/**
 * Local-only customize transaction. XvoxNowPlaying renders its genuine live controls from this
 * draft, then persists it only when the header's Okay button is pressed.
 */
data class NowPlayingEditSession(
    val draft: XvoxChromeStyle,
    val onDraftChange: (XvoxChromeStyle) -> Unit
)

private fun XvoxChromeStyle.offsetForNowPlayingItem(itemId: String): Pair<Float, Float> = when (itemId) {
    NowPlayingEditItem.Utility -> nowPlayingUtilityOffsetX to nowPlayingUtilityOffsetY
    NowPlayingEditItem.Actions -> nowPlayingActionsOffsetX to nowPlayingActionsOffsetY
    NowPlayingEditItem.Metadata -> nowPlayingMetadataOffsetX to nowPlayingMetadataOffsetY
    NowPlayingEditItem.Progress -> nowPlayingProgressOffsetX to nowPlayingProgressOffsetY
    NowPlayingEditItem.Shuffle -> nowPlayingShuffleOffsetX to nowPlayingShuffleOffsetY
    NowPlayingEditItem.Repeat -> nowPlayingRepeatOffsetX to nowPlayingRepeatOffsetY
    NowPlayingEditItem.Previous -> nowPlayingPreviousOffsetX to nowPlayingPreviousOffsetY
    NowPlayingEditItem.Play -> nowPlayingPlayOffsetX to nowPlayingPlayOffsetY
    NowPlayingEditItem.Next -> nowPlayingNextOffsetX to nowPlayingNextOffsetY
    NowPlayingEditItem.Brand -> nowPlayingBrandOffsetX to nowPlayingBrandOffsetY
    else -> 0f to 0f
}

private fun XvoxChromeStyle.withNowPlayingItemOffset(
    itemId: String,
    x: Float,
    y: Float
): XvoxChromeStyle = when (itemId) {
    NowPlayingEditItem.Utility -> copy(nowPlayingUtilityOffsetX = x, nowPlayingUtilityOffsetY = y)
    NowPlayingEditItem.Actions -> copy(nowPlayingActionsOffsetX = x, nowPlayingActionsOffsetY = y)
    NowPlayingEditItem.Metadata -> copy(nowPlayingMetadataOffsetX = x, nowPlayingMetadataOffsetY = y)
    NowPlayingEditItem.Progress -> copy(nowPlayingProgressOffsetX = x, nowPlayingProgressOffsetY = y)
    NowPlayingEditItem.Shuffle -> copy(nowPlayingShuffleOffsetX = x, nowPlayingShuffleOffsetY = y)
    NowPlayingEditItem.Repeat -> copy(nowPlayingRepeatOffsetX = x, nowPlayingRepeatOffsetY = y)
    NowPlayingEditItem.Previous -> copy(nowPlayingPreviousOffsetX = x, nowPlayingPreviousOffsetY = y)
    NowPlayingEditItem.Play -> copy(nowPlayingPlayOffsetX = x, nowPlayingPlayOffsetY = y)
    NowPlayingEditItem.Next -> copy(nowPlayingNextOffsetX = x, nowPlayingNextOffsetY = y)
    NowPlayingEditItem.Brand -> copy(nowPlayingBrandOffsetX = x, nowPlayingBrandOffsetY = y)
    else -> this
}

private fun snapPanel(value: Float): Float =
    (kotlin.math.round(value / 16f) * 16f).coerceIn(-320f, 320f)

/**
 * Decorates one real Now Playing item while Customize is active. A long press moves it on an
 * invisible 16dp lattice spanning the complete bottom panel; no grid is drawn. Pinch changes its
 * local scale. The close affordance hides it only in the transaction draft.
 */
@Composable
fun NowPlayingEditableTarget(
    session: NowPlayingEditSession?,
    itemId: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (session == null) {
        Box(modifier = modifier, content = content)
        return
    }

    val colors = XvoxTheme.colors
    val latestSession by rememberUpdatedState(session)
    val hidden = session.draft.isNowPlayingItemHidden(itemId)
    val scale = session.draft.nowPlayingItemScale(itemId)
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f) }

    Box(
        modifier = modifier
            // Hidden targets remain measured so the live layout does not unexpectedly collapse;
            // their transparent interception layer prevents an invisible player action firing.
            .graphicsLayer {
                alpha = if (hidden) 0f else 1f
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin.Center
            }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    if (!hidden) {
                        drawRoundRect(
                            color = colors.primaryAccent.copy(alpha = .92f),
                            style = Stroke(width = 1.dp.toPx(), pathEffect = dash)
                        )
                    }
                }
            }
    ) {
        content()

        // This layer sits above genuine buttons/rails while editing, so playback, likes and sheet
        // actions cannot leak through a draft manipulation.
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(itemId) {
                    val scopeDensity = this
                    var totalX = 0f
                    var totalY = 0f
                    var startX = 0f
                    var startY = 0f
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            val (x, y) = latestSession.draft.offsetForNowPlayingItem(itemId)
                            startX = x
                            startY = y
                            totalX = 0f
                            totalY = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            totalX += amount.x
                            totalY += amount.y
                            // Track the finger in the real layout rather than showing a proxy.
                            val nextX = with(scopeDensity) { startX + totalX.toDp().value }
                            val nextY = with(scopeDensity) { startY + totalY.toDp().value }
                            latestSession.onDraftChange(
                                latestSession.draft
                                    .withNowPlayingItemOffset(itemId, snapPanel(nextX), snapPanel(nextY))
                                    .normalizedNowPlayingGrid()
                            )
                        },
                        onDragEnd = {
                            val nextX = with(scopeDensity) { startX + totalX.toDp().value }
                            val nextY = with(scopeDensity) { startY + totalY.toDp().value }
                            latestSession.onDraftChange(
                                latestSession.draft
                                    .withNowPlayingItemOffset(itemId, snapPanel(nextX), snapPanel(nextY))
                                    .normalizedNowPlayingGrid()
                            )
                        },
                        onDragCancel = { totalX = 0f; totalY = 0f }
                    )
                }
                // A second detector deliberately reacts only to an actual pinch zoom. A single
                // finger's normal pan does nothing here, leaving long-press movement intact.
                .pointerInput(itemId) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (abs(zoom - 1f) > .01f) {
                            val current = latestSession.draft.nowPlayingItemScale(itemId)
                            latestSession.onDraftChange(
                                latestSession.draft.withNowPlayingItemScale(itemId, current * zoom)
                            )
                        }
                    }
                }
                .clickable(
                    interactionSource = remember(itemId) { MutableInteractionSource() },
                    indication = null,
                    onClick = { }
                )
        )

        if (!hidden) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .background(colors.background.copy(alpha = .94f), CircleShape)
                    .clickable(
                        interactionSource = remember("hide_$itemId") { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            latestSession.onDraftChange(
                                latestSession.draft.withNowPlayingItemHidden(itemId, true)
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_close),
                    contentDescription = "Hide item",
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/** Cover is resizable by pinch but deliberately has no move/hide wrapper or close affordance. */
@Composable
fun NowPlayingEditableCover(
    session: NowPlayingEditSession?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (session == null) {
        Box(modifier = modifier, content = content)
        return
    }
    val latestSession by rememberUpdatedState(session)
    val scale = session.draft.nowPlayingCoverScale.coerceIn(.65f, 1.55f)
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
        }
    ) {
        content()
        Box(
            Modifier
                .matchParentSize()
                .pointerInput("nowPlayingCoverResize") {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (abs(zoom - 1f) > .01f) {
                            latestSession.onDraftChange(
                                latestSession.draft.copy(
                                    nowPlayingCoverScale = (latestSession.draft.nowPlayingCoverScale * zoom)
                                        .coerceIn(.65f, 1.55f)
                                )
                            )
                        }
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { }
                )
        )
    }
}
