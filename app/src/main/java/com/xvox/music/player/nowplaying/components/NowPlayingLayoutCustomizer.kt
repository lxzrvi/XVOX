package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.player.nowplaying.XvoxNowPlayingHeader
import kotlin.math.roundToInt

/** A discrete offset from a real Now Playing control's native anchor. */
private data class XvoxGridSlot(val x: Float, val y: Float)
private data class XvoxCanvasPoint(val x: Float, val y: Float)

/** Every movable replica target has an independent persisted offset pair. */
private enum class XvoxLayoutToken {
    METADATA, PROGRESS, UTILITY, ACTIONS, OUTER_TRANSPORT, PREVIOUS, PLAY, NEXT, BRAND
}

/**
 * Slots are shape-aware locations across the whole bottom panel. The grid intentionally has broad
 * lanes rather than the former +/-8dp nudge: a user can drag a control through its complete legal
 * panel zone, while a wide title/rail or paired edge control never overlaps an incompatible shape.
 */
private data class XvoxGridSpec(
    val label: String,
    val shape: Shape,
    val base: XvoxCanvasPoint,
    val hitRadius: Float,
    /** Only shapes in the same family may exchange a location. */
    val swapFamily: String,
    val slots: List<XvoxGridSlot>
) {
    fun nearest(x: Float, y: Float): XvoxGridSlot = slots.minByOrNull { slot ->
        val dx = slot.x - x
        val dy = slot.y - y
        dx * dx + dy * dy
    } ?: slots.first()
}

private fun xvoxPanelSlots(
    base: XvoxCanvasPoint,
    targets: List<XvoxCanvasPoint>
): List<XvoxGridSlot> = targets.map { target ->
    XvoxGridSlot(target.x - base.x, target.y - base.y)
}.distinct()

private val xvoxTopClusterTargets = listOf(
    XvoxCanvasPoint(-126f, 34f), XvoxCanvasPoint(120f, 34f),
    XvoxCanvasPoint(-126f, 82f), XvoxCanvasPoint(120f, 82f),
    XvoxCanvasPoint(-126f, 130f), XvoxCanvasPoint(120f, 130f)
)
private val xvoxTransportTargets = listOf(160f, 205f, 240f).flatMap { y ->
    // These are the real five live-control centers (10%, 30%, 50%, 70%, 90% on a 360dp frame),
    // plus two safe vertical rows. Keeping native centers here means an untouched layout stays
    // exactly untouched when the editor first normalizes it.
    listOf(-144f, -72f, 0f, 72f, 144f).map { x -> XvoxCanvasPoint(x, y) }
}

private val XvoxMetadataGrid = XvoxGridSpec(
    label = "Song title and artist",
    shape = RoundedCornerShape(7.dp),
    base = XvoxCanvasPoint(0f, 70f),
    hitRadius = 42f,
    swapFamily = "metadata",
    slots = xvoxPanelSlots(
        XvoxCanvasPoint(0f, 70f),
        listOf(
            XvoxCanvasPoint(-48f, 54f), XvoxCanvasPoint(0f, 54f), XvoxCanvasPoint(48f, 54f),
            XvoxCanvasPoint(-48f, 70f), XvoxCanvasPoint(0f, 70f), XvoxCanvasPoint(48f, 70f),
            XvoxCanvasPoint(-48f, 86f), XvoxCanvasPoint(0f, 86f), XvoxCanvasPoint(48f, 86f)
        )
    )
)
private val XvoxProgressGrid = XvoxGridSpec(
    label = "Progress rail and time",
    shape = RoundedCornerShape(7.dp),
    base = XvoxCanvasPoint(0f, 134f),
    hitRadius = 46f,
    swapFamily = "progress",
    slots = xvoxPanelSlots(
        XvoxCanvasPoint(0f, 134f),
        listOf(XvoxCanvasPoint(-36f, 118f), XvoxCanvasPoint(0f, 118f), XvoxCanvasPoint(36f, 118f),
            XvoxCanvasPoint(-36f, 134f), XvoxCanvasPoint(0f, 134f), XvoxCanvasPoint(36f, 134f),
            XvoxCanvasPoint(-36f, 150f), XvoxCanvasPoint(0f, 150f), XvoxCanvasPoint(36f, 150f))
    )
)
private val XvoxUtilityGrid = XvoxGridSpec(
    label = "Timer, queue, and info pill",
    shape = RoundedCornerShape(21.dp),
    base = XvoxCanvasPoint(-126f, 34f),
    hitRadius = 46f,
    swapFamily = "top",
    slots = xvoxPanelSlots(XvoxCanvasPoint(-126f, 34f), xvoxTopClusterTargets)
)
private val XvoxActionsGrid = XvoxGridSpec(
    label = "Action buttons",
    shape = RoundedCornerShape(21.dp),
    base = XvoxCanvasPoint(120f, 34f),
    hitRadius = 46f,
    swapFamily = "top",
    slots = xvoxPanelSlots(XvoxCanvasPoint(120f, 34f), xvoxTopClusterTargets)
)
private val XvoxOuterTransportGrid = XvoxGridSpec(
    label = "Shuffle and repeat",
    shape = RoundedCornerShape(21.dp),
    base = XvoxCanvasPoint(0f, 205f),
    hitRadius = 58f,
    swapFamily = "outer",
    slots = xvoxPanelSlots(
        XvoxCanvasPoint(0f, 205f),
        listOf(XvoxCanvasPoint(0f, 170f), XvoxCanvasPoint(0f, 205f), XvoxCanvasPoint(0f, 240f))
    )
)
private val XvoxPreviousGrid = XvoxGridSpec(
    label = "Previous track",
    shape = CircleShape,
    base = XvoxCanvasPoint(-72f, 205f),
    hitRadius = 27f,
    swapFamily = "transport",
    slots = xvoxPanelSlots(XvoxCanvasPoint(-72f, 205f), xvoxTransportTargets)
)
private val XvoxPlayGrid = XvoxGridSpec(
    label = "Play or pause",
    shape = CircleShape,
    base = XvoxCanvasPoint(0f, 205f),
    hitRadius = 32f,
    swapFamily = "transport",
    slots = xvoxPanelSlots(XvoxCanvasPoint(0f, 205f), xvoxTransportTargets)
)
private val XvoxNextGrid = XvoxGridSpec(
    label = "Next track",
    shape = CircleShape,
    base = XvoxCanvasPoint(72f, 205f),
    hitRadius = 27f,
    swapFamily = "transport",
    slots = xvoxPanelSlots(XvoxCanvasPoint(72f, 205f), xvoxTransportTargets)
)
private val XvoxBrandGrid = XvoxGridSpec(
    label = "XVOX label",
    shape = RoundedCornerShape(5.dp),
    base = XvoxCanvasPoint(0f, 260f),
    hitRadius = 22f,
    swapFamily = "brand",
    slots = xvoxPanelSlots(
        XvoxCanvasPoint(0f, 260f),
        listOf(XvoxCanvasPoint(-72f, 260f), XvoxCanvasPoint(0f, 260f), XvoxCanvasPoint(72f, 260f),
            XvoxCanvasPoint(-72f, 284f), XvoxCanvasPoint(0f, 284f), XvoxCanvasPoint(72f, 284f),
            XvoxCanvasPoint(-72f, 304f), XvoxCanvasPoint(0f, 304f), XvoxCanvasPoint(72f, 304f))
    )
)

private fun xvoxSpec(token: XvoxLayoutToken): XvoxGridSpec = when (token) {
    XvoxLayoutToken.METADATA -> XvoxMetadataGrid
    XvoxLayoutToken.PROGRESS -> XvoxProgressGrid
    XvoxLayoutToken.UTILITY -> XvoxUtilityGrid
    XvoxLayoutToken.ACTIONS -> XvoxActionsGrid
    XvoxLayoutToken.OUTER_TRANSPORT -> XvoxOuterTransportGrid
    XvoxLayoutToken.PREVIOUS -> XvoxPreviousGrid
    XvoxLayoutToken.PLAY -> XvoxPlayGrid
    XvoxLayoutToken.NEXT -> XvoxNextGrid
    XvoxLayoutToken.BRAND -> XvoxBrandGrid
}

private fun XvoxChromeStyle.slotOf(token: XvoxLayoutToken): XvoxGridSlot = when (token) {
    XvoxLayoutToken.METADATA -> XvoxGridSlot(nowPlayingMetadataOffsetX, nowPlayingMetadataOffsetY)
    XvoxLayoutToken.PROGRESS -> XvoxGridSlot(nowPlayingProgressOffsetX, nowPlayingProgressOffsetY)
    XvoxLayoutToken.UTILITY -> XvoxGridSlot(nowPlayingUtilityOffsetX, nowPlayingUtilityOffsetY)
    XvoxLayoutToken.ACTIONS -> XvoxGridSlot(nowPlayingActionsOffsetX, nowPlayingActionsOffsetY)
    XvoxLayoutToken.OUTER_TRANSPORT -> XvoxGridSlot(nowPlayingShuffleRepeatOffsetX, nowPlayingShuffleRepeatOffsetY)
    XvoxLayoutToken.PREVIOUS -> XvoxGridSlot(nowPlayingPreviousOffsetX, nowPlayingPreviousOffsetY)
    XvoxLayoutToken.PLAY -> XvoxGridSlot(nowPlayingPlayOffsetX, nowPlayingPlayOffsetY)
    XvoxLayoutToken.NEXT -> XvoxGridSlot(nowPlayingNextOffsetX, nowPlayingNextOffsetY)
    XvoxLayoutToken.BRAND -> XvoxGridSlot(nowPlayingBrandOffsetX, nowPlayingBrandOffsetY)
}

private fun XvoxChromeStyle.withSlot(token: XvoxLayoutToken, slot: XvoxGridSlot): XvoxChromeStyle = when (token) {
    XvoxLayoutToken.METADATA -> copy(nowPlayingMetadataOffsetX = slot.x, nowPlayingMetadataOffsetY = slot.y)
    XvoxLayoutToken.PROGRESS -> copy(nowPlayingProgressOffsetX = slot.x, nowPlayingProgressOffsetY = slot.y)
    XvoxLayoutToken.UTILITY -> copy(nowPlayingUtilityOffsetX = slot.x, nowPlayingUtilityOffsetY = slot.y)
    XvoxLayoutToken.ACTIONS -> copy(nowPlayingActionsOffsetX = slot.x, nowPlayingActionsOffsetY = slot.y)
    XvoxLayoutToken.OUTER_TRANSPORT -> copy(nowPlayingShuffleRepeatOffsetX = slot.x, nowPlayingShuffleRepeatOffsetY = slot.y)
    XvoxLayoutToken.PREVIOUS -> copy(nowPlayingPreviousOffsetX = slot.x, nowPlayingPreviousOffsetY = slot.y)
    XvoxLayoutToken.PLAY -> copy(nowPlayingPlayOffsetX = slot.x, nowPlayingPlayOffsetY = slot.y)
    XvoxLayoutToken.NEXT -> copy(nowPlayingNextOffsetX = slot.x, nowPlayingNextOffsetY = slot.y)
    XvoxLayoutToken.BRAND -> copy(nowPlayingBrandOffsetX = slot.x, nowPlayingBrandOffsetY = slot.y)
}

private fun XvoxChromeStyle.canvasPoint(token: XvoxLayoutToken): XvoxCanvasPoint {
    val base = xvoxSpec(token).base
    val slot = xvoxSpec(token).nearest(slotOf(token).x, slotOf(token).y)
    return XvoxCanvasPoint(base.x + slot.x, base.y + slot.y)
}

/** Existing free offsets become one of the full-panel safe slots as soon as the editor opens. */
private fun XvoxChromeStyle.snapNowPlayingLayoutToGrid(): XvoxChromeStyle =
    XvoxLayoutToken.values().fold(this) { style, token ->
        val spec = xvoxSpec(token)
        val raw = style.slotOf(token)
        style.withSlot(token, spec.nearest(raw.x, raw.y))
    }

/**
 * A true visual replica of the portrait Now Playing frame. It intentionally uses the same panel,
 * header-pill, metadata, rail, action and transport geometry as the live player, but its controls
 * are non-playing editor targets. Long-pressing a target only selects a legal grid slot.
 */
@Composable
fun NowPlayingLayoutCustomizer(
    chrome: XvoxChromeStyle,
    onChromeChange: (XvoxChromeStyle) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    // This is deliberately a transaction. Nothing reaches Settings/DataStore or live Now Playing
    // until Done; Reset and all drag changes affect this local preview only.
    var draft by remember { mutableStateOf(chrome.snapNowPlayingLayoutToGrid()) }
    val latestDraft by rememberUpdatedState(draft)
    val swallowInteraction = remember { MutableInteractionSource() }

    fun update(next: XvoxChromeStyle) {
        draft = next.snapNowPlayingLayoutToGrid()
    }

    fun commitDrop(token: XvoxLayoutToken, from: XvoxGridSlot, to: XvoxGridSlot) {
        val sourceSpec = xvoxSpec(token)
        val sourcePoint = XvoxCanvasPoint(sourceSpec.base.x + from.x, sourceSpec.base.y + from.y)
        val destinationPoint = XvoxCanvasPoint(sourceSpec.base.x + to.x, sourceSpec.base.y + to.y)
        // A drop onto another compatible target is a true swap: the target goes to the exact
        // legal slot the dragged item occupied. Different footprints never trade places, so a
        // title/rail cannot become an invalid circular-control overlap.
        val target = XvoxLayoutToken.values()
            .asSequence()
            .filter { it != token }
            .filter { xvoxSpec(it).swapFamily == sourceSpec.swapFamily }
            .map { candidate ->
                val point = latestDraft.canvasPoint(candidate)
                val dx = point.x - destinationPoint.x
                val dy = point.y - destinationPoint.y
                candidate to (dx * dx + dy * dy)
            }
            .filter { (candidate, distance) ->
                val radius = sourceSpec.hitRadius + xvoxSpec(candidate).hitRadius
                distance <= radius * radius
            }
            .minByOrNull { it.second }
            ?.first

        var next = latestDraft.withSlot(token, to)
        if (target != null) {
            val targetSpec = xvoxSpec(target)
            val returnSlot = targetSpec.nearest(
                sourcePoint.x - targetSpec.base.x,
                sourcePoint.y - targetSpec.base.y
            )
            next = next.withSlot(target, returnSlot)
        }
        update(next)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(80f)
            .background(colors.background)
            .semantics { contentDescription = "Customize Now Playing layout" }
    ) {
        // This full-window hit target owns otherwise-empty editor space. It prevents a tap or
        // drag from falling through to Now Playing/Home while leaving the targets above usable.
        Box(
            Modifier
                .fillMaxSize()
                .clickable(swallowInteraction, indication = null) { }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                // The editor title begins beneath the status bar rather than under system icons.
                .statusBarsPadding()
                .zIndex(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 10.dp, end = 10.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Customize", color = colors.primaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Long-press and drag anywhere across a safe bottom-box grid. Drop on a compatible control to swap places. Changes apply only after Done.",
                        color = colors.secondaryText,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
                Text(
                    "Reset",
                    color = colors.primaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(10.dp)
                        .xvoxPressScale { update(latestDraft.resetNowPlayingLayout()) }
                )
                Text(
                    "Done",
                    color = colors.primaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(10.dp)
                        .xvoxPressScale {
                            onChromeChange(latestDraft.snapNowPlayingLayoutToGrid())
                            onClose()
                        }
                )
            }

            // Reuse the real header geometry rather than drawing an editor-only imitation.
            XvoxNowPlayingHeader(
                onClose = {},
                onShare = {},
                onMore = {},
                playingSource = "All Songs",
                useSystemInsets = false,
                optionsGroupSide = draft.nowPlayingOptionsGroupSide,
                controlsAlpha = draft.nowPlayingControlsAlpha,
                surfaceAlpha = .27f,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            // This reserved cover field matches the live player's gap immediately above the real
            // bottom card. It is intentionally non-interactive: every movable target belongs to
            // the exact panel below, not an approximate free-floating canvas.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 76.dp)
                    .padding(horizontal = 22.dp, vertical = 7.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(colors.cardElevated.copy(alpha = .46f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.primaryAccent.copy(alpha = .16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "XVOX",
                        color = colors.primaryAccent.copy(alpha = .88f),
                        fontFamily = XvoxLogoFont,
                        fontSize = 22.sp,
                        letterSpacing = 3.sp
                    )
                }
            }

            XvoxBottomLayoutCanvas(
                chrome = draft,
                onDrop = ::commitDrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(332.dp)
            )
        }
    }
}

@Composable
private fun XvoxBottomLayoutCanvas(
    chrome: XvoxChromeStyle,
    onDrop: (XvoxLayoutToken, XvoxGridSlot, XvoxGridSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val panelShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
    val materialAlpha = .27f
    val bottomAlpha = materialAlpha * chrome.nowPlayingBottomBoxAlpha.coerceIn(0f, 1f)
    val controlsAlpha = chrome.nowPlayingControlsAlpha.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .clip(panelShape)
            .background(colors.cardElevated.copy(alpha = bottomAlpha))
    ) {
        XvoxAlignmentGuides(Modifier.matchParentSize())

        XvoxGridToken(
            token = XvoxLayoutToken.UTILITY,
            spec = XvoxUtilityGrid,
            x = chrome.nowPlayingUtilityOffsetX,
            y = chrome.nowPlayingUtilityOffsetY,
            onDrop = { from, slot -> onDrop(XvoxLayoutToken.UTILITY, from, slot) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 13.dp)
        ) {
            XvoxReplicaPill(
                icons = listOf(R.drawable.ic_xvox_timer, R.drawable.ic_xvox_queue, R.drawable.ic_xvox_info),
                alpha = controlsAlpha
            )
        }

        XvoxGridToken(
            token = XvoxLayoutToken.ACTIONS,
            spec = XvoxActionsGrid,
            x = chrome.nowPlayingActionsOffsetX,
            y = chrome.nowPlayingActionsOffsetY,
            onDrop = { from, slot -> onDrop(XvoxLayoutToken.ACTIONS, from, slot) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 14.dp, top = 13.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer { alpha = controlsAlpha }
            ) {
                XvoxReplicaCircle(R.drawable.ic_xvox_star)
                XvoxReplicaCircle(R.drawable.ic_xvox_heart_outline)
            }
        }

        XvoxGridToken(
            token = XvoxLayoutToken.METADATA,
            spec = XvoxMetadataGrid,
            x = chrome.nowPlayingMetadataOffsetX,
            y = chrome.nowPlayingMetadataOffsetY,
            onDrop = { from, slot -> onDrop(XvoxLayoutToken.METADATA, from, slot) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 68.dp)
        ) {
            Column(Modifier.widthIn(max = maxWidth - 28.dp)) {
                Text("Song title", color = colors.primaryText, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("Artist", color = colors.secondaryText, fontSize = 11.5.sp, lineHeight = 14.sp, maxLines = 1)
            }
        }

        val railWidth = (maxWidth - 28.dp).coerceAtLeast(120.dp).coerceAtMost(280.dp)
        XvoxGridToken(
            token = XvoxLayoutToken.PROGRESS,
            spec = XvoxProgressGrid,
            x = chrome.nowPlayingProgressOffsetX,
            y = chrome.nowPlayingProgressOffsetY,
            onDrop = { from, slot -> onDrop(XvoxLayoutToken.PROGRESS, from, slot) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 119.dp)
        ) {
            Column(Modifier.width(railWidth)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(colors.secondaryText.copy(alpha = .28f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(.48f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(colors.primaryAccent)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1:24", color = colors.secondaryText, fontSize = 9.sp)
                    Text("3:48", color = colors.secondaryText, fontSize = 9.sp)
                }
            }
        }

        // The live transport layout uses five evenly spaced centers: 10%, 30%, 50%, 70%, 90%.
        // Reproduce those physical slots here; the outer pair shares one safe vertical grid lane.
        val outerBandWidth = (maxWidth - 24.dp).coerceAtLeast(120.dp)
        val previousStart = (maxWidth * .30f - 21.dp).coerceAtLeast(0.dp)
        val playStart = (maxWidth * .50f - 28.dp).coerceAtLeast(0.dp)
        val nextStart = (maxWidth * .70f - 21.dp).coerceAtMost((maxWidth - 42.dp).coerceAtLeast(0.dp))
        XvoxGridToken(
            token = XvoxLayoutToken.OUTER_TRANSPORT,
            spec = XvoxOuterTransportGrid,
            x = chrome.nowPlayingShuffleRepeatOffsetX,
            y = chrome.nowPlayingShuffleRepeatOffsetY,
            onDrop = { from, slot -> onDrop(XvoxLayoutToken.OUTER_TRANSPORT, from, slot) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 174.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(outerBandWidth)
                    .height(42.dp)
                    .graphicsLayer { alpha = controlsAlpha }
            ) {
                XvoxTransportIcon(R.drawable.ic_xvox_shuffle, modifier = Modifier.align(Alignment.CenterStart))
                XvoxTransportIcon(R.drawable.ic_xvox_repeat, modifier = Modifier.align(Alignment.CenterEnd))
            }
        }

        XvoxGridToken(
            token = XvoxLayoutToken.PREVIOUS,
            spec = XvoxPreviousGrid,
            x = chrome.nowPlayingPreviousOffsetX,
            y = chrome.nowPlayingPreviousOffsetY,
            onDrop = { from, slot -> onDrop(XvoxLayoutToken.PREVIOUS, from, slot) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = previousStart, top = 174.dp)
                .graphicsLayer { alpha = controlsAlpha }
        ) {
            XvoxTransportIcon(R.drawable.ic_xvox_skip_previous, 25.dp)
        }

        XvoxGridToken(
            token = XvoxLayoutToken.PLAY,
            spec = XvoxPlayGrid,
            x = chrome.nowPlayingPlayOffsetX,
            y = chrome.nowPlayingPlayOffsetY,
            onDrop = { from, slot -> onDrop(XvoxLayoutToken.PLAY, from, slot) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = playStart, top = 167.dp)
                .graphicsLayer { alpha = chrome.nowPlayingPlayAlpha.coerceIn(0f, 1f) }
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.card.copy(alpha = materialAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_play),
                    contentDescription = null,
                    tint = colors.primaryText,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        XvoxGridToken(
            token = XvoxLayoutToken.NEXT,
            spec = XvoxNextGrid,
            x = chrome.nowPlayingNextOffsetX,
            y = chrome.nowPlayingNextOffsetY,
            onDrop = { from, slot -> onDrop(XvoxLayoutToken.NEXT, from, slot) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = nextStart, top = 174.dp)
                .graphicsLayer { alpha = controlsAlpha }
        ) {
            XvoxTransportIcon(R.drawable.ic_xvox_skip_next, 25.dp)
        }

        XvoxGridToken(
            token = XvoxLayoutToken.BRAND,
            spec = XvoxBrandGrid,
            x = chrome.nowPlayingBrandOffsetX,
            y = chrome.nowPlayingBrandOffsetY,
            onDrop = { from, slot -> onDrop(XvoxLayoutToken.BRAND, from, slot) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 252.dp)
        ) {
            Text(
                "XVOX",
                color = colors.secondaryText.copy(alpha = .58f),
                fontFamily = XvoxLogoFont,
                fontSize = 11.5.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

/** Subtle non-interactive guides make the snap system legible without changing Now Playing itself. */
@Composable
private fun XvoxAlignmentGuides(modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    val density = LocalDensity.current
    Canvas(modifier) {
        val line = colors.primaryAccent.copy(alpha = .11f)
        val dot = colors.primaryAccent.copy(alpha = .24f)
        val xStep = with(density) { 24.dp.toPx() }
        val yStep = with(density) { 16.dp.toPx() }
        var x = xStep
        while (x < size.width) {
            drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += xStep
        }
        var y = yStep
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += yStep
        }
        // Canonical center/side anchors are slightly stronger: wide rails, pills, and circles
        // each snap against these same visual axes but never enter another shape's lane.
        drawCircle(dot, radius = 2f, center = Offset(size.width * .5f, 136.dp.toPx()))
        drawCircle(dot, radius = 2f, center = Offset(size.width * .5f, 202.dp.toPx()))
        drawCircle(dot, radius = 2f, center = Offset(size.width * .5f, 280.dp.toPx()))
    }
}

@Composable
private fun XvoxReplicaPill(icons: List<Int>, alpha: Float) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .height(42.dp)
            .graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f) }
            .clip(RoundedCornerShape(21.dp))
            .background(colors.card.copy(alpha = .27f))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icons.forEach { icon ->
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.primaryText,
                modifier = Modifier.size(18.dp).padding(0.dp)
            )
        }
    }
}

@Composable
private fun XvoxReplicaCircle(icon: Int) {
    val colors = XvoxTheme.colors
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(colors.card.copy(alpha = .27f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun XvoxTransportIcon(
    icon: Int,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    Box(modifier.size(42.dp), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Gesture layer for one real replica target. The preview follows only legal grid slots while a
 * long-press is held; the draft changes once on release, so a drop on a compatible target can
 * atomically swap both locations without an intermediate overlapping persisted layout.
 */
@Composable
private fun XvoxGridToken(
    token: XvoxLayoutToken,
    spec: XvoxGridSpec,
    x: Float,
    y: Float,
    onDrop: (XvoxGridSlot, XvoxGridSlot) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val latestX by rememberUpdatedState(x)
    val latestY by rememberUpdatedState(y)
    val latestDrop by rememberUpdatedState(onDrop)
    var selected by remember(token) { mutableStateOf(false) }
    var previewSlot by remember(token) { mutableStateOf<XvoxGridSlot?>(null) }
    val current = spec.nearest(latestX, latestY)
    val visibleSlot = previewSlot ?: current

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    with(density) { visibleSlot.x.dp.toPx() }.roundToInt(),
                    with(density) { visibleSlot.y.dp.toPx() }.roundToInt()
                )
            }
            .then(
                if (selected) Modifier
                    .clip(spec.shape)
                    .border(1.dp, XvoxTheme.colors.primaryAccent, spec.shape)
                    .padding(2.dp)
                else Modifier
            )
            // Keep this detector stable across preview snaps: moving through the complete panel
            // must not restart the long-press just because the displayed guide changed.
            .pointerInput(token) {
                var start = current
                var candidate = current
                var accumulated = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        start = spec.nearest(latestX, latestY)
                        candidate = start
                        accumulated = Offset.Zero
                        previewSlot = start
                        selected = true
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        accumulated += amount
                        candidate = spec.nearest(
                            start.x + with(density) { accumulated.x.toDp().value },
                            start.y + with(density) { accumulated.y.toDp().value }
                        )
                        previewSlot = candidate
                    },
                    onDragEnd = {
                        latestDrop(start, candidate)
                        previewSlot = null
                        selected = false
                    },
                    onDragCancel = {
                        previewSlot = null
                        selected = false
                    }
                )
            }
            .semantics {
                contentDescription = "${spec.label}. Long press and drag across the safe bottom-box grid. Compatible targets swap when dropped together."
            }
    ) {
        content()
    }
}
