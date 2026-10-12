package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
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
data class NowPlayingAlignmentGuide(
    val vertical: Boolean = false,
    val horizontal: Boolean = false
)

/**
 * The guide is held by the real Now Playing canvas, not a separate preview. It is therefore
 * drawn across the actual bottom panel while a draft item hits centre or a sibling alignment.
 */
data class NowPlayingEditSession(
    val draft: XvoxChromeStyle,
    val onDraftChange: (XvoxChromeStyle) -> Unit,
    val alignmentGuide: NowPlayingAlignmentGuide? = null,
    val onAlignmentGuideChange: (NowPlayingAlignmentGuide?) -> Unit = {}
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

private val NowPlayingEditableItemIds = listOf(
    NowPlayingEditItem.Utility,
    NowPlayingEditItem.Actions,
    NowPlayingEditItem.Metadata,
    NowPlayingEditItem.Progress,
    NowPlayingEditItem.Shuffle,
    NowPlayingEditItem.Repeat,
    NowPlayingEditItem.Previous,
    NowPlayingEditItem.Play,
    NowPlayingEditItem.Next,
    NowPlayingEditItem.Brand
)

private fun XvoxChromeStyle.alignmentGuideForNowPlayingItem(
    itemId: String,
    x: Float,
    y: Float
): NowPlayingAlignmentGuide? {
    // Snap is 16dp. Half a cell is a generous magnetic threshold for the visible guide while
    // avoiding an always-on centre crosshair as the user simply explores the layout.
    val threshold = 8f
    val siblings = NowPlayingEditableItemIds
        .asSequence()
        .filter { it != itemId && !isNowPlayingItemHidden(it) }
        .map { offsetForNowPlayingItem(it) }
        .toList()
    val vertical = abs(x) <= threshold || siblings.any { abs(it.first - x) <= threshold }
    val horizontal = abs(y) <= threshold || siblings.any { abs(it.second - y) <= threshold }
    return NowPlayingAlignmentGuide(vertical = vertical, horizontal = horizontal)
        .takeIf { it.vertical || it.horizontal }
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

/** A non-intercepting centre/sibling alignment guide over the genuine player canvas. */
@Composable
fun NowPlayingAlignmentGuideOverlay(
    guide: NowPlayingAlignmentGuide?,
    modifier: Modifier = Modifier
) {
    if (guide == null) return
    val colors = XvoxTheme.colors
    Canvas(modifier = modifier.fillMaxSize()) {
        val guideColor = colors.primaryText.copy(alpha = .66f)
        val stroke = Stroke(
            width = 1.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))
        )
        if (guide.vertical) {
            drawLine(
                color = guideColor,
                start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                strokeWidth = stroke.width,
                cap = stroke.cap,
                pathEffect = stroke.pathEffect
            )
        }
        if (guide.horizontal) {
            drawLine(
                color = guideColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                strokeWidth = stroke.width,
                cap = stroke.cap,
                pathEffect = stroke.pathEffect
            )
        }
    }
}

/**
 * Decorates one real Now Playing item while Customize is active. The visible top-right grip moves
 * it immediately, while a long press anywhere also moves it on an invisible 16dp lattice spanning
 * the complete bottom panel; no grid is drawn. The bottom-right < > grip (or pinch) changes local
 * scale, and the top-left X hides it only in the transaction draft.
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

    fun applyDraftOffset(nextX: Float, nextY: Float) {
        val snappedX = snapPanel(nextX)
        val snappedY = snapPanel(nextY)
        val draft = latestSession.draft
        latestSession.onAlignmentGuideChange(
            draft.alignmentGuideForNowPlayingItem(itemId, snappedX, snappedY)
        )
        latestSession.onDraftChange(
            draft
                .withNowPlayingItemOffset(itemId, snappedX, snappedY)
                .normalizedNowPlayingGrid()
        )
    }

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
                            color = colors.primaryText.copy(alpha = .68f),
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
                            applyDraftOffset(nextX, nextY)
                        },
                        onDragEnd = {
                            val nextX = with(scopeDensity) { startX + totalX.toDp().value }
                            val nextY = with(scopeDensity) { startY + totalY.toDp().value }
                            applyDraftOffset(nextX, nextY)
                            latestSession.onAlignmentGuideChange(null)
                        },
                        onDragCancel = {
                            totalX = 0f
                            totalY = 0f
                            latestSession.onAlignmentGuideChange(null)
                        }
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
            // A real grab button makes movement discoverable as well as preserving the existing
            // long-press-anywhere gesture. Its direct drag edits only the local Customize draft.
            Box(
                modifier = Modifier
                    // Controls deliberately straddle the dotted-box edge instead of consuming
                    // its content area. The visible move grip belongs at the top-right corner.
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(28.dp)
                    .background(colors.cardElevated.copy(alpha = .92f), CircleShape)
                    .pointerInput("move_$itemId") {
                        val scopeDensity = this
                        var startX = 0f
                        var startY = 0f
                        var totalX = 0f
                        var totalY = 0f
                        detectDragGestures(
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
                                val nextX = with(scopeDensity) { startX + totalX.toDp().value }
                                val nextY = with(scopeDensity) { startY + totalY.toDp().value }
                                applyDraftOffset(nextX, nextY)
                            },
                            onDragEnd = {
                                latestSession.onAlignmentGuideChange(null)
                            },
                            onDragCancel = {
                                latestSession.onAlignmentGuideChange(null)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_drag_dots),
                    contentDescription = "Drag to move item",
                    tint = colors.primaryText,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    // Hide lives at the top-left edge; move owns the symmetric top-right edge.
                    .align(Alignment.TopStart)
                    .offset(x = (-10).dp, y = (-10).dp)
                    .size(28.dp)
                    .background(colors.cardElevated.copy(alpha = .92f), CircleShape)
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
                    tint = colors.primaryText,
                    modifier = Modifier.size(15.dp)
                )
            }

            // The lower-right < > affordance makes resizing unmistakable without replacing the
            // existing two-finger pinch. Drag it outward/down-right to enlarge, inward/up-left to
            // shrink; its result is written only to the local Customize draft.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
                    .size(28.dp)
                    .background(colors.cardElevated.copy(alpha = .92f), CircleShape)
                    .pointerInput("resize_$itemId") {
                        val scopeDensity = this
                        var startScale = 1f
                        var totalDistance = 0f
                        detectDragGestures(
                            onDragStart = {
                                startScale = latestSession.draft.nowPlayingItemScale(itemId)
                                totalDistance = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                totalDistance += amount.x + amount.y
                                val rangePx = with(scopeDensity) { 160.dp.toPx() }
                                latestSession.onDraftChange(
                                    latestSession.draft.withNowPlayingItemScale(
                                        itemId,
                                        startScale + totalDistance / rangePx
                                    )
                                )
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_resize),
                    contentDescription = "Resize item",
                    tint = colors.primaryText,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/** Cover remains anchored/no-hide, but its scaled size is a real persisted Customize draft value. */
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
    val colors = XvoxTheme.colors
    val scale = session.draft.nowPlayingCoverScale.coerceIn(.65f, 1.55f)
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f) }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin.Center
            }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRoundRect(
                        color = colors.primaryText.copy(alpha = .68f),
                        style = Stroke(width = 1.dp.toPx(), pathEffect = dash)
                    )
                }
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
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
                .size(28.dp)
                .background(colors.cardElevated.copy(alpha = .92f), CircleShape)
                .pointerInput("nowPlayingCoverResizeHandle") {
                    val scopeDensity = this
                    var startScale = 1f
                    var totalDistance = 0f
                    detectDragGestures(
                        onDragStart = {
                            startScale = latestSession.draft.nowPlayingCoverScale
                            totalDistance = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            totalDistance += amount.x + amount.y
                            val rangePx = with(scopeDensity) { 190.dp.toPx() }
                            latestSession.onDraftChange(
                                latestSession.draft.copy(
                                    nowPlayingCoverScale = (startScale + totalDistance / rangePx)
                                        .coerceIn(.65f, 1.55f)
                                )
                            )
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_resize),
                contentDescription = "Resize cover",
                tint = colors.primaryText,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
