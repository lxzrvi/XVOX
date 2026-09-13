package com.xvox.music.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

private val RowHeight = 56.dp
private val RowSpacing = 6.dp

@Composable
fun XvoxQueueBoxContent(
    queue: List<Song>,
    currentSongId: Long?,
    onPlayIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit
) {
    val colors = XvoxTheme.colors
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val move by rememberUpdatedState(onMoveItem)
    val play by rememberUpdatedState(onPlayIndex)
    val local = remember { mutableStateListOf<Song>().apply { addAll(queue) } }

    var draggingSong by remember { mutableStateOf<Song?>(null) }
    var initialDragIndex by remember { mutableIntStateOf(-1) }
    var currentDragIndex by remember { mutableIntStateOf(-1) }
    var initialTouchY by remember { mutableFloatStateOf(0f) }
    var currentTouchY by remember { mutableFloatStateOf(0f) }
    var accumulatedScrollDistance by remember { mutableFloatStateOf(0f) }
    var dragCardOffsetY by remember { mutableFloatStateOf(0f) }
    var listViewportHeight by remember { mutableFloatStateOf(0f) }

    val rowHeightPx = with(density) { RowHeight.toPx() }
    val itemTotalHeightPx = with(density) { (RowHeight + RowSpacing).toPx() }

    LaunchedEffect(queue) {
        if (draggingSong == null && local.toList() != queue) {
            local.clear()
            local.addAll(queue)
        }
    }

    // Mathematically stable slot swapping with hysteresis (zero glitching in middle area)
    fun checkAndSwapSlots() {
        if (draggingSong == null || initialDragIndex < 0 || itemTotalHeightPx <= 0f) return
        val totalDeltaY = (currentTouchY - initialTouchY) + accumulatedScrollDistance
        val currentSlotDelta = (currentDragIndex - initialDragIndex) * itemTotalHeightPx
        val relativeDelta = totalDeltaY - currentSlotDelta
        val threshold = itemTotalHeightPx * 0.48f

        if (relativeDelta > threshold && currentDragIndex < local.lastIndex) {
            val from = currentDragIndex
            val to = currentDragIndex + 1
            val item = local.removeAt(from)
            local.add(to, item)
            currentDragIndex = to
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else if (relativeDelta < -threshold && currentDragIndex > 0) {
            val from = currentDragIndex
            val to = currentDragIndex - 1
            val item = local.removeAt(from)
            local.add(to, item)
            currentDragIndex = to
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Auto-scroll loop when finger is held near top or bottom viewport edges
    LaunchedEffect(draggingSong) {
        if (draggingSong == null) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        val edgeThreshold = with(density) { 56.dp.toPx() }
        val maxScrollSpeedPxPerSec = with(density) { 450.dp.toPx() }

        while (isActive && draggingSong != null) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
            previousFrame = frame

            if (listViewportHeight > 0f) {
                val cardTop = dragCardOffsetY
                val cardBottom = dragCardOffsetY + rowHeightPx

                val strength = when {
                    cardTop < edgeThreshold -> -((edgeThreshold - cardTop) / edgeThreshold).coerceIn(0f, 1f)
                    cardBottom > listViewportHeight - edgeThreshold -> ((cardBottom - (listViewportHeight - edgeThreshold)) / edgeThreshold).coerceIn(0f, 1f)
                    else -> 0f
                }

                if (strength != 0f) {
                    val delta = strength * maxScrollSpeedPxPerSec * seconds
                    listState.scrollBy(delta)
                    accumulatedScrollDistance += delta
                    checkAndSwapSlots()
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            "${local.size} songs · Tap to play · Hold & drag to reorder",
            color = colors.secondaryText,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (local.isEmpty()) {
            Text("Your queue is empty", color = colors.mutedText, modifier = Modifier.padding(20.dp))
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .onGloballyPositioned { listViewportHeight = it.size.height.toFloat() }
                    .pointerInput(local.size) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downY = down.position.y

                            // Check if down was over a visible item
                            val hitItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                downY >= item.offset && downY <= (item.offset + item.size)
                            }

                            if (hitItem != null && hitItem.index in local.indices) {
                                // Long press detection
                                val longPressTimeout = 380L
                                var passedSlop = false
                                val longPressed = withTimeoutOrNull(longPressTimeout) {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        val movedDistance = kotlin.math.abs(change.position.y - downY)
                                        if (movedDistance > 14.dp.toPx()) {
                                            passedSlop = true
                                            break
                                        }
                                    }
                                    false
                                } == null && !passedSlop

                                if (longPressed) {
                                    down.consume()
                                    val song = local.getOrNull(hitItem.index)
                                    if (song != null) {
                                        draggingSong = song
                                        initialDragIndex = hitItem.index
                                        currentDragIndex = hitItem.index
                                        initialTouchY = downY
                                        currentTouchY = downY
                                        accumulatedScrollDistance = 0f
                                        dragCardOffsetY = (downY - rowHeightPx / 2f).coerceIn(0f, listViewportHeight - rowHeightPx)
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                                        // Drag loop: persists unconditionally across the whole queue
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change: PointerInputChange? = event.changes.firstOrNull { it.id == down.id }
                                            if (change == null || !change.pressed) {
                                                change?.consume()
                                                break
                                            }
                                            change.consume()
                                            val currentY = change.position.y
                                            currentTouchY = currentY
                                            dragCardOffsetY = (currentY - rowHeightPx / 2f).coerceIn(0f, listViewportHeight - rowHeightPx)
                                            checkAndSwapSlots()
                                        }

                                        // Commit final drag reorder
                                        val from = initialDragIndex
                                        val to = currentDragIndex
                                        draggingSong = null
                                        initialDragIndex = -1
                                        currentDragIndex = -1
                                        if (from in local.indices && to in local.indices && from != to) {
                                            move(from, to)
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                LazyColumn(
                    state = listState,
                    userScrollEnabled = draggingSong == null,
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(RowSpacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(
                        items = local,
                        key = { _, it -> it.id },
                        contentType = { _, _ -> "queue_row" }
                    ) { idx, song ->
                        val isThisItemBeingDragged = draggingSong?.id == song.id

                        QueueRowView(
                            song = song,
                            current = song.id == currentSongId,
                            onClick = {
                                if (draggingSong == null) {
                                    val target = local.indexOfFirst { it.id == song.id }
                                    if (target >= 0) play(target)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(RowHeight)
                                .alpha(if (isThisItemBeingDragged) 0.20f else 1f)
                                .semantics {
                                    customActions = listOf(
                                        CustomAccessibilityAction("Move up") {
                                            val index = local.indexOfFirst { it.id == song.id }
                                            if (index > 0) {
                                                val item = local.removeAt(index)
                                                local.add(index - 1, item)
                                                move(index, index - 1)
                                                true
                                            } else false
                                        },
                                        CustomAccessibilityAction("Move down") {
                                            val index = local.indexOfFirst { it.id == song.id }
                                            if (index >= 0 && index < local.lastIndex) {
                                                val item = local.removeAt(index)
                                                local.add(index + 1, item)
                                                move(index, index + 1)
                                                true
                                            } else false
                                        }
                                    )
                                }
                        )
                    }
                }

                // Floating Dragged Card Overlay (Never disappears, clamped cleanly within queue box boundaries)
                if (draggingSong != null) {
                    val activeSong = draggingSong!!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(RowHeight)
                            .offset { IntOffset(0, dragCardOffsetY.roundToInt()) }
                            .zIndex(999f)
                            .shadow(16.dp, RoundedCornerShape(12.dp))
                            .graphicsLayer {
                                scaleX = 1.03f
                                scaleY = 1.03f
                            }
                    ) {
                        QueueRowView(
                            song = activeSong,
                            current = activeSong.id == currentSongId,
                            onClick = { },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRowView(
    song: Song,
    current: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val cardBg = rememberSongCardColor(song, current)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        XvoxSongArtwork(
            artwork = song.artworkUri,
            requestSize = 160,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                color = if (current) colors.primaryAccent else colors.primaryText,
                fontSize = 13.5.sp,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = colors.secondaryText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (current) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.primaryAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Playing",
                    color = colors.primaryAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
