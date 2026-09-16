package com.xvox.music.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.xvox.music.player.playback.XvoxSavedQueue
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

@Composable
fun QueueHeaderDropdown(
    activeQueueName: String,
    savedQueues: List<XvoxSavedQueue>,
    onSwitchQueue: (String) -> Unit
) {
    val colors = XvoxTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = activeQueueName,
                color = colors.primaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                painter = painterResource(R.drawable.ic_xvox_chevron_down),
                contentDescription = "Switch Queue",
                tint = colors.primaryAccent,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = if (expanded) 180f else 0f }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(colors.cardElevated)
                .clip(RoundedCornerShape(12.dp))
                .widthIn(min = 180.dp)
        ) {
            if (savedQueues.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "No other queues",
                            color = colors.mutedText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                activeQueueName,
                                color = colors.primaryAccent,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_check),
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    },
                    onClick = { expanded = false }
                )

                savedQueues.forEach { saved ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        saved.name,
                                        color = colors.primaryText,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${saved.songs.size} songs",
                                        color = colors.secondaryText,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        },
                        onClick = {
                            expanded = false
                            onSwitchQueue(saved.id)
                        }
                    )
                }
            }
        }
    }
}

private val RowHeight = 58.dp
private val RowSpacing = 6.dp

@Composable
fun XvoxQueueBoxContent(
    queue: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean = true,
    activeQueueName: String = "Current Queue",
    savedQueues: List<XvoxSavedQueue> = emptyList(),
    onSwitchQueue: (String) -> Unit = {},
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

    fun checkAndSwapSlots() {
        if (draggingSong == null || currentDragIndex < 0 || listViewportHeight <= 0f || itemTotalHeightPx <= 0f) return

        val currentSlotScreenTop = (currentDragIndex - listState.firstVisibleItemIndex) * itemTotalHeightPx - listState.firstVisibleItemScrollOffset

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

    LaunchedEffect(Unit) {
        val targetIdx = local.indexOfFirst { it.id == currentSongId }
        if (targetIdx > 1) {
            listState.scrollToItem((targetIdx - 1).coerceAtLeast(0))
        }
    }

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

    // Horizontal swipe gesture for queue switching
    var totalDragX by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        if (draggingSong == null) {
            totalDragX += delta
        }
    }

    val allQueueEntries = remember(activeQueueName, savedQueues) {
        listOf("active" to activeQueueName) + savedQueues.map { it.id to it.name }
    }
    val currentQueueIndex = remember(activeQueueName, allQueueEntries) {
        allQueueEntries.indexOfFirst { it.second == activeQueueName }.coerceAtLeast(0)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                enabled = draggingSong == null && savedQueues.isNotEmpty(),
                onDragStopped = {
                    val threshold = 120f
                    if (totalDragX < -threshold) {
                        // Swipe left -> next queue
                        val nextIdx = (currentQueueIndex + 1).coerceAtMost(allQueueEntries.lastIndex)
                        if (nextIdx != currentQueueIndex) {
                            onSwitchQueue(allQueueEntries[nextIdx].first)
                        }
                    } else if (totalDragX > threshold) {
                        // Swipe right -> prev queue
                        val prevIdx = (currentQueueIndex - 1).coerceAtLeast(0)
                        if (prevIdx != currentQueueIndex) {
                            onSwitchQueue(allQueueEntries[prevIdx].first)
                        }
                    }
                    totalDragX = 0f
                }
            )
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
                    text = "Remove from Queue?",
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

            AnimatedContent(
                targetState = activeQueueName,
                transitionSpec = {
                    val targetIdx = allQueueEntries.indexOfFirst { it.second == targetState }.coerceAtLeast(0)
                    val initialIdx = allQueueEntries.indexOfFirst { it.second == initialState }.coerceAtLeast(0)
                    val isNext = targetIdx >= initialIdx
                    if (isNext) {
                        (slideInHorizontally(tween(260)) { it } + fadeIn(tween(180)))
                            .togetherWith(slideOutHorizontally(tween(240)) { -it } + fadeOut(tween(140)))
                    } else {
                        (slideInHorizontally(tween(260)) { -it } + fadeIn(tween(180)))
                            .togetherWith(slideOutHorizontally(tween(240)) { it } + fadeOut(tween(140)))
                    }
                },
                label = "queueSwitchTransition"
            ) { _ ->
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

                                    val hitItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                        downY >= item.offset && downY <= (item.offset + item.size)
                                    }

                                    if (hitItem != null && hitItem.index in local.indices) {
                                        val longPressTimeout = 340L
                                        val longPressed = withTimeoutOrNull(longPressTimeout) {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) break
                                                if ((change.position - down.position).getDistance() > 14f) break
                                            }
                                        } == null && currentDragIndex == -1

                                        if (longPressed && hitItem.index in local.indices) {
                                            val song = local[hitItem.index]
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggingSong = song
                                            initialDragIndex = hitItem.index
                                            currentDragIndex = hitItem.index
                                            touchOffsetInCard = downY - hitItem.offset
                                            dragCardOffsetY = hitItem.offset.toFloat()

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id }
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
                                    current = isPlaying && song.id == currentSongId,
                                    onClick = {
                                        if (draggingSong == null) {
                                            val target = local.indexOfFirst { it.id == song.id }
                                            play(if (target >= 0) target else idx)
                                        }
                                    },
                                    onRemove = {
                                        pendingRemove = idx to song
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(RowHeight)
                                        .alpha(if (isThisItemBeingDragged) 0f else 1f)
                                        .semantics {
                                            customActions = listOf(
                                                CustomAccessibilityAction("Move up") {
                                                    if (idx > 0) { move(idx, idx - 1); true } else false
                                                },
                                                CustomAccessibilityAction("Move down") {
                                                    if (idx < local.lastIndex) { move(idx, idx + 1); true } else false
                                                }
                                            )
                                        }
                                )
                            }
                        }

                        if (draggingSong != null) {
                            val activeSong = draggingSong!!
                            QueueRowView(
                                song = activeSong,
                                current = isPlaying && activeSong.id == currentSongId,
                                onClick = { },
                                onRemove = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(RowHeight)
                                    .offset { IntOffset(0, dragCardOffsetY.roundToInt()) }
                                    .zIndex(10f)
                                    .shadow(12.dp, RoundedCornerShape(12.dp))
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
            .padding(start = 6.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        XvoxSongArtwork(
            artwork = song.artworkUri,
            requestSize = 96,
            modifier = Modifier
                .size(46.dp)
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

        if (current) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.primaryAccent.copy(alpha = 0.20f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Playing",
                    color = colors.primaryAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.size(8.dp))
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.cardElevated.copy(alpha = 0.85f))
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
                tint = colors.secondaryText,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
