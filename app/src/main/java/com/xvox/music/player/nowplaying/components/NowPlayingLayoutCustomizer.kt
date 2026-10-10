package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.xvox.music.features.settings.components.XvoxContinuousSlider
import com.xvox.music.player.nowplaying.XvoxNowPlayingHeader
import kotlin.math.roundToInt

/** A discrete position relative to a real Now Playing control's native lane. */
private data class XvoxGridSlot(val x: Float, val y: Float)

/**
 * Each token has a deliberately small safe lane. Wide text/rails cannot cross into button lanes;
 * pills keep their side of the card; circular transport controls retain their neighbor spacing.
 * This makes every accepted point visually aligned and collision-free by construction.
 */
private data class XvoxGridSpec(
    val label: String,
    val shape: Shape,
    val slots: List<XvoxGridSlot>
) {
    fun nearest(x: Float, y: Float): XvoxGridSlot = slots.minByOrNull { slot ->
        val dx = slot.x - x
        val dy = slot.y - y
        dx * dx + dy * dy
    } ?: slots.first()
}

private fun xvoxCenteredSlots(x: List<Float>, y: List<Float>) =
    x.flatMap { horizontal -> y.map { vertical -> XvoxGridSlot(horizontal, vertical) } }

private val XvoxMetadataGrid = XvoxGridSpec(
    label = "Song title and artist",
    shape = RoundedCornerShape(7.dp),
    // Full-width text stays centered in its own vertical band; it cannot run into the rail.
    slots = xvoxCenteredSlots(listOf(0f), listOf(-10f, 0f, 10f))
)
private val XvoxProgressGrid = XvoxGridSpec(
    label = "Progress rail and time",
    shape = RoundedCornerShape(7.dp),
    slots = xvoxCenteredSlots(listOf(0f), listOf(-8f, 0f, 8f))
)
private val XvoxUtilityGrid = XvoxGridSpec(
    label = "Timer, queue, and info pill",
    shape = RoundedCornerShape(21.dp),
    slots = xvoxCenteredSlots(listOf(-16f, 0f, 16f), listOf(-8f, 0f, 8f))
)
private val XvoxActionsGrid = XvoxGridSpec(
    label = "Action buttons",
    shape = RoundedCornerShape(21.dp),
    slots = xvoxCenteredSlots(listOf(-14f, 0f, 14f), listOf(-8f, 0f, 8f))
)
private val XvoxOuterTransportGrid = XvoxGridSpec(
    label = "Shuffle and repeat",
    shape = RoundedCornerShape(21.dp),
    // This token owns both outside controls, so it may only move vertically; horizontal travel
    // would push one of the two real edge controls through the rounded panel boundary.
    slots = xvoxCenteredSlots(listOf(0f), listOf(-8f, 0f, 8f))
)
private val XvoxPreviousGrid = XvoxGridSpec(
    label = "Previous track",
    shape = CircleShape,
    slots = xvoxCenteredSlots(listOf(-10f, 0f, 10f), listOf(-8f, 0f, 8f))
)
private val XvoxPlayGrid = XvoxGridSpec(
    label = "Play or pause",
    shape = CircleShape,
    // The play button remains on the true center line; it only steps vertically on its own lane.
    slots = xvoxCenteredSlots(listOf(0f), listOf(-8f, 0f, 8f))
)
private val XvoxNextGrid = XvoxGridSpec(
    label = "Next track",
    shape = CircleShape,
    slots = xvoxCenteredSlots(listOf(-10f, 0f, 10f), listOf(-8f, 0f, 8f))
)
private val XvoxBrandGrid = XvoxGridSpec(
    label = "XVOX label",
    shape = RoundedCornerShape(5.dp),
    slots = xvoxCenteredSlots(listOf(-12f, 0f, 12f), listOf(-4f, 0f, 4f))
)

/** Existing pre-grid free offsets are normalized the first time the editor opens. */
private fun XvoxChromeStyle.snapNowPlayingLayoutToGrid(): XvoxChromeStyle {
    fun snapped(spec: XvoxGridSpec, x: Float, y: Float) = spec.nearest(x, y)
    val metadata = snapped(XvoxMetadataGrid, nowPlayingMetadataOffsetX, nowPlayingMetadataOffsetY)
    val progress = snapped(XvoxProgressGrid, nowPlayingProgressOffsetX, nowPlayingProgressOffsetY)
    val utility = snapped(XvoxUtilityGrid, nowPlayingUtilityOffsetX, nowPlayingUtilityOffsetY)
    val actions = snapped(XvoxActionsGrid, nowPlayingActionsOffsetX, nowPlayingActionsOffsetY)
    val outer = snapped(XvoxOuterTransportGrid, nowPlayingShuffleRepeatOffsetX, nowPlayingShuffleRepeatOffsetY)
    val previous = snapped(XvoxPreviousGrid, nowPlayingPreviousOffsetX, nowPlayingPreviousOffsetY)
    val play = snapped(XvoxPlayGrid, nowPlayingPlayOffsetX, nowPlayingPlayOffsetY)
    val next = snapped(XvoxNextGrid, nowPlayingNextOffsetX, nowPlayingNextOffsetY)
    val brand = snapped(XvoxBrandGrid, nowPlayingBrandOffsetX, nowPlayingBrandOffsetY)
    return copy(
        nowPlayingMetadataOffsetX = metadata.x,
        nowPlayingMetadataOffsetY = metadata.y,
        nowPlayingProgressOffsetX = progress.x,
        nowPlayingProgressOffsetY = progress.y,
        nowPlayingUtilityOffsetX = utility.x,
        nowPlayingUtilityOffsetY = utility.y,
        nowPlayingActionsOffsetX = actions.x,
        nowPlayingActionsOffsetY = actions.y,
        nowPlayingShuffleRepeatOffsetX = outer.x,
        nowPlayingShuffleRepeatOffsetY = outer.y,
        nowPlayingPreviousOffsetX = previous.x,
        nowPlayingPreviousOffsetY = previous.y,
        nowPlayingPlayOffsetX = play.x,
        nowPlayingPlayOffsetY = play.y,
        nowPlayingNextOffsetX = next.x,
        nowPlayingNextOffsetY = next.y,
        nowPlayingBrandOffsetX = brand.x,
        nowPlayingBrandOffsetY = brand.y
    )
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
    // Keep the editor gesture stable while persistence flows catch up after each discrete snap.
    var draft by remember { mutableStateOf(chrome.snapNowPlayingLayoutToGrid()) }
    val latestDraft by rememberUpdatedState(draft)

    fun update(next: XvoxChromeStyle, persist: Boolean = false) {
        val snapped = next.snapNowPlayingLayoutToGrid()
        draft = snapped
        if (persist) onChromeChange(snapped)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(80f)
            .background(colors.background)
            .semantics { contentDescription = "Customize Now Playing layout" }
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 10.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Customize", color = colors.primaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Long-press an outlined target and drag to a dotted guide. Every item snaps to a safe shape-aware grid.",
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
                        .xvoxPressScale {
                            update(latestDraft.resetNowPlayingLayout(), persist = true)
                        }
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

            // Reuse the live header composable rather than drawing an editor-only approximation.
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

            // The artwork field mirrors the real adaptive cover area without decoding music art
            // in an editor. It gives the controls their exact top/bottom frame relationship.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 86.dp)
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
                // Persist each discrete snap. There are only a few guide points, and this avoids
                // losing a valid placement if the editor is closed immediately after a drag.
                onDraftChange = { next -> update(next, persist = true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(324.dp)
            )

            NowPlayingTransparencyEditor(
                chrome = draft,
                onChange = { update(it, persist = true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun XvoxBottomLayoutCanvas(
    chrome: XvoxChromeStyle,
    onDraftChange: (XvoxChromeStyle) -> Unit,
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
            spec = XvoxUtilityGrid,
            x = chrome.nowPlayingUtilityOffsetX,
            y = chrome.nowPlayingUtilityOffsetY,
            onSlotChange = { slot ->
                onDraftChange(chrome.copy(nowPlayingUtilityOffsetX = slot.x, nowPlayingUtilityOffsetY = slot.y))
            },
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
            spec = XvoxActionsGrid,
            x = chrome.nowPlayingActionsOffsetX,
            y = chrome.nowPlayingActionsOffsetY,
            onSlotChange = { slot ->
                onDraftChange(chrome.copy(nowPlayingActionsOffsetX = slot.x, nowPlayingActionsOffsetY = slot.y))
            },
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
            spec = XvoxMetadataGrid,
            x = chrome.nowPlayingMetadataOffsetX,
            y = chrome.nowPlayingMetadataOffsetY,
            onSlotChange = { slot ->
                onDraftChange(chrome.copy(nowPlayingMetadataOffsetX = slot.x, nowPlayingMetadataOffsetY = slot.y))
            },
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
            spec = XvoxProgressGrid,
            x = chrome.nowPlayingProgressOffsetX,
            y = chrome.nowPlayingProgressOffsetY,
            onSlotChange = { slot ->
                onDraftChange(chrome.copy(nowPlayingProgressOffsetX = slot.x, nowPlayingProgressOffsetY = slot.y))
            },
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
            spec = XvoxOuterTransportGrid,
            x = chrome.nowPlayingShuffleRepeatOffsetX,
            y = chrome.nowPlayingShuffleRepeatOffsetY,
            onSlotChange = { slot ->
                onDraftChange(chrome.copy(nowPlayingShuffleRepeatOffsetX = slot.x, nowPlayingShuffleRepeatOffsetY = slot.y))
            },
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
            spec = XvoxPreviousGrid,
            x = chrome.nowPlayingPreviousOffsetX,
            y = chrome.nowPlayingPreviousOffsetY,
            onSlotChange = { slot ->
                onDraftChange(chrome.copy(nowPlayingPreviousOffsetX = slot.x, nowPlayingPreviousOffsetY = slot.y))
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = previousStart, top = 174.dp)
                .graphicsLayer { alpha = controlsAlpha }
        ) {
            XvoxTransportIcon(R.drawable.ic_xvox_skip_previous, 25.dp)
        }

        XvoxGridToken(
            spec = XvoxPlayGrid,
            x = chrome.nowPlayingPlayOffsetX,
            y = chrome.nowPlayingPlayOffsetY,
            onSlotChange = { slot ->
                onDraftChange(chrome.copy(nowPlayingPlayOffsetX = slot.x, nowPlayingPlayOffsetY = slot.y))
            },
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
            spec = XvoxNextGrid,
            x = chrome.nowPlayingNextOffsetX,
            y = chrome.nowPlayingNextOffsetY,
            onSlotChange = { slot ->
                onDraftChange(chrome.copy(nowPlayingNextOffsetX = slot.x, nowPlayingNextOffsetY = slot.y))
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = nextStart, top = 174.dp)
                .graphicsLayer { alpha = controlsAlpha }
        ) {
            XvoxTransportIcon(R.drawable.ic_xvox_skip_next, 25.dp)
        }

        XvoxGridToken(
            spec = XvoxBrandGrid,
            x = chrome.nowPlayingBrandOffsetX,
            y = chrome.nowPlayingBrandOffsetY,
            onSlotChange = { slot ->
                onDraftChange(chrome.copy(nowPlayingBrandOffsetX = slot.x, nowPlayingBrandOffsetY = slot.y))
            },
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
 * Gesture layer for one real replica element. It never exposes raw coordinates: the visible offset
 * is always the nearest [XvoxGridSpec] slot, and the spec's shape-specific lanes prevent overlap.
 */
@Composable
private fun XvoxGridToken(
    spec: XvoxGridSpec,
    x: Float,
    y: Float,
    onSlotChange: (XvoxGridSlot) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val latestX by rememberUpdatedState(x)
    val latestY by rememberUpdatedState(y)
    val latestChange by rememberUpdatedState(onSlotChange)
    var selected by remember(spec.label) { mutableStateOf(false) }
    val current = spec.nearest(latestX, latestY)

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    with(density) { current.x.dp.toPx() }.roundToInt(),
                    with(density) { current.y.dp.toPx() }.roundToInt()
                )
            }
            .then(
                if (selected) Modifier
                    .clip(spec.shape)
                    .border(1.dp, XvoxTheme.colors.primaryAccent, spec.shape)
                    .padding(2.dp)
                else Modifier
            )
            // Keep this detector stable across snap recompositions: an in-progress long press
            // must not restart merely because it crossed into another legal guide cell.
            .pointerInput(spec.label) {
                var start = current
                var accumulated = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        start = spec.nearest(latestX, latestY)
                        accumulated = Offset.Zero
                        selected = true
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        accumulated += amount
                        val candidate = spec.nearest(
                            start.x + with(density) { accumulated.x.toDp().value },
                            start.y + with(density) { accumulated.y.toDp().value }
                        )
                        if (candidate != spec.nearest(latestX, latestY)) latestChange(candidate)
                    },
                    onDragEnd = { selected = false },
                    onDragCancel = { selected = false }
                )
            }
            .semantics {
                contentDescription = "${spec.label}. Long press and drag to snap on its safe alignment grid."
            }
    ) {
        content()
    }
}

@Composable
private fun NowPlayingTransparencyEditor(
    chrome: XvoxChromeStyle,
    onChange: (XvoxChromeStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Transparency", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        XvoxLayoutTransparencySlider(
            "Bottom box",
            1f - chrome.nowPlayingBottomBoxAlpha,
            { onChange(chrome.copy(nowPlayingBottomBoxAlpha = 1f - it)) }
        )
        XvoxLayoutTransparencySlider(
            "Header, right pills & playback controls",
            1f - chrome.nowPlayingControlsAlpha,
            { onChange(chrome.copy(nowPlayingControlsAlpha = 1f - it)) }
        )
        XvoxLayoutTransparencySlider(
            "Play button",
            1f - chrome.nowPlayingPlayAlpha,
            { onChange(chrome.copy(nowPlayingPlayAlpha = 1f - it)) }
        )
    }
}

@Composable
private fun XvoxLayoutTransparencySlider(label: String, value: Float, onChange: (Float) -> Unit) {
    val colors = XvoxTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = colors.secondaryText, fontSize = 10.sp)
            Text("${(value.coerceIn(0f, 1f) * 100f).roundToInt()}%", color = colors.primaryAccent, fontSize = 10.sp)
        }
        XvoxContinuousSlider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { onChange(it.coerceIn(0f, 1f)) },
            valueRange = 0f..1f,
            defaultValue = 0f,
            contentDescription = "$label transparency"
        )
    }
}
