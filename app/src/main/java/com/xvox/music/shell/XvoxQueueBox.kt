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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

private val RowHeight = 58.dp
private val RowSpacing = 6.dp

@Composable
fun XvoxQueueBoxContent(
    queue: List<Song>,
    currentSongId: Long?,
    onPlayIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveIndex: (Int) -> Unit = {}
) {
    val colors = XvoxTheme.colors
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val move by rememberUpdatedState(onMoveItem)
    val play by rememberUpdatedState(onPlayIndex)
    val remove by rememberUpdatedState(onRemoveIndex)
    val local = remember { mutableStateListOf<Song>().apply { addAll(queue) } }

    var pendingRemove by remember { mutableStateOf<Pair<Int, Song>?>(null) }

    var draggingSong by remember { mutableStateOf<Song?>(null) }
    var initialDragIndex by remember { mutableIntStateOf(-1) }
    var currentDragIndex by remember { mutableIntStateOf(-1) }
    var touchOffsetInCard by remember { mutableFloatStateOf(0f) }
    var dragCardOffsetY by remember { mutableFloatStateOf(0f) }
    var listViewportHeight by remember { mutableFloatStateOf(0f) }

    val rowHeightPx = with(density) { RowHeight.toPx() }
    val rowSpacingPx = with(density) { RowSpacing.toPx() }
    val itemTotalHeightPx = rowHeightPx + rowSpacingPx

    LaunchedEffect(queue) {
        if (draggingSong == null && local.toList() != queue) {
            local.clear()
            local.addAll(queue)
        }
    }

    // Conflict-free step-by-step slot swapping with hysteresis
    fun checkAndSwapSlots() {
        if (draggingSong == null || currentDragIndex < 0 || listViewportHeight <= 0f || itemTotalHeightPx <= 0f) return

        val currentSlotScreenTop = (currentDragIndex - listState.firstVisibleItemIndex) * itemTotalHeightPx - listState.firstVisibleItemScrollOffset

        // Check moving down
        if (currentDragIndex < local.lastIndex) {
            if (dragCardOffsetY > currentSlotScreenTop + itemTotalHeightPx * 0.55f) {
                val from = currentDragIndex
                val to = currentDragIndex + 1
                if (from in local.indices && to in local.indices) {
                    val item = local.removeAt(from)
                    local.add(to, item)
                    currentDragIndex = to
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    return
                }
            }
        }

        // Check moving up
        if (currentDragIndex > 0) {
            val isAtVeryTop = dragCardOffsetY <= with(density) { 10.dp.toPx() } && listState.firstVisibleItemIndex == 0
            if (dragCardOffsetY < currentSlotScreenTop - itemTotalHeightPx * 0.55f || isAtVeryTop) {
                val from = currentDragIndex
                val to = currentDragIndex - 1
                if (from in local.indices && to in local.indices) {
                    val item = local.removeAt(from)
                    local.add(to, item)
                    currentDragIndex = to
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    return
                }
            }
        }
    }

    // Auto-scroll loop when finger is held near top or bottom viewport edges
    LaunchedEffect(draggingSong) {
        if (draggingSong == null) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        val edgeThreshold = with(density) { 48.dp.toPx() }
        val maxScrollSpeedPxPerSec = with(density) { 420.dp.toPx() }

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
                    checkAndSwapSlots()
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        if (pendingRemove != null) {
            val target = pendingRemove!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Remove from queue?",
                    color = colors.primaryAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Remove \"${target.second.title}\" from the current playing queue?",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.cardElevated)
                            .clickable { pendingRemove = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = colors.secondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFF5252))
                            .clickable {
                                val idx = target.first
                                pendingRemove = null
                                remove(idx)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Remove", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
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
                        .wrapContentHeight()
                        .onGloballyPositioned { listViewportHeight = it.size.height.toFloat() }
                        .pointerInput(local.size) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downY = down.position.y

                                // Find exact item touched
                                val hitItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                    downY >= item.offset && downY <= (item.offset + item.size)
                                }

                                if (hitItem != null && hitItem.index in local.indices) {
                                    val longPressTimeout = 340L
                                    var passedSlop = false
                                    val longPressed = withTimeoutOrNull(longPressTimeout) {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) break
                                            val movedDistance = kotlin.math.abs(change.position.y - downY)
                                            if (movedDistance > 12.dp.toPx()) {
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
                                            touchOffsetInCard = downY - hitItem.offset.toFloat()
                                            dragCardOffsetY = hitItem.offset.toFloat()
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
                                                dragCardOffsetY = (currentY - touchOffsetInCard).coerceIn(
                                                    0f,
                                                    (listViewportHeight - rowHeightPx).coerceAtLeast(0f)
                                                )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        itemsIndexed(
                            items = local,
                            key = { idx, song -> "${song.id}_${idx}_${song.source}" },
                            contentType = { _, _ -> "queue_row" }
                        ) { idx, song ->
                            val isThisItemBeingDragged = draggingSong?.id == song.id && idx == currentDragIndex

                            QueueRowView(
                                song = song,
                                current = song.id == currentSongId,
                                onClick = {
                                    if (draggingSong == null) {
                                        val target = local.indexOfFirst { it.id == song.id }
                                        if (target >= 0) play(target)
                                    }
                                },
                                onRemove = {
                                    pendingRemove = idx to song
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

                    // Floating Dragged Card Overlay
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
                                onRemove = { },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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
    onRemove: () -> Unit = {},
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
                .padding(start = 12.dp, end = 6.dp),
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.cardElevated.copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRemove
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_close),
                    contentDescription = "Remove from queue",
                    tint = colors.mutedText,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
