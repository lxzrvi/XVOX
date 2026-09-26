package com.xvox.music.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor
import com.xvox.music.player.playback.XvoxSavedQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

@Composable
fun QueueHeaderDropdown(
    activeQueueName: String,
    savedQueues: List<XvoxSavedQueue>,
    currentQueueSize: Int = 0,
    onSwitchQueue: (String) -> Unit
) {
    val colors = XvoxTheme.colors

    Box {
        Row(
            modifier = Modifier
                .height(40.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Queue",
                    color = colors.primaryText,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$currentQueueSize songs",
                    color = colors.primaryAccent,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private val RowHeight = 60.dp
private val RowSpacing = 4.dp

/** Stable visual identity survives drag reorders, including repeated copies of the same Song. */
private data class QueueEntry(val stableKey: String, val song: Song)

@Composable
fun XvoxQueueBoxContent(
    queue: List<Song>,
    currentSongId: Long?,
    /** An occurrence index distinguishes repeated copies of the same library song. */
    currentIndex: Int = -1,
    isPlaying: Boolean,
    savedQueues: List<XvoxSavedQueue> = emptyList(),
    activeQueueName: String = "Queue 1",
    isPlaybackActiveInThisQueue: Boolean = true,
    onSwitchQueue: (String) -> Unit = {},
    onPlayIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveIndex: (Int) -> Unit,
    onReorderQueue: ((List<Song>) -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    val move by rememberUpdatedState(onMoveItem)
    val remove by rememberUpdatedState(onRemoveIndex)
    val play by rememberUpdatedState(onPlayIndex)

    val sourceEntries = remember(queue) {
        queue.mapIndexed { index, song -> QueueEntry(stableKey = "${song.id}:$index", song = song) }
    }
    var draggingEntry by remember { mutableStateOf<QueueEntry?>(null) }
    var dragCardOffsetY by remember { mutableFloatStateOf(0f) }
    var initialDragIndex by remember { mutableIntStateOf(-1) }
    var currentDragIndex by remember { mutableIntStateOf(-1) }

    var dragList by remember { mutableStateOf<List<QueueEntry>?>(null) }
    val displayEntries = dragList ?: sourceEntries

    val rowHeightPx = with(density) { RowHeight.toPx() }
    val rowSpacingPx = with(density) { RowSpacing.toPx() }
    val itemSlotSpanPx = rowHeightPx + rowSpacingPx

    var listViewportHeight by remember { mutableFloatStateOf(0f) }

    fun checkAndSwapSlots() {
        if (draggingEntry == null) return
        val currentLocalList = dragList ?: return
        val fromSlot = currentDragIndex
        if (fromSlot !in currentLocalList.indices) return

        val visibleItems = listState.layoutInfo.visibleItemsInfo
        for (itemInfo in visibleItems) {
            val toSlot = itemInfo.index
            if (toSlot == fromSlot || toSlot !in currentLocalList.indices) continue

            val slotTop = itemInfo.offset.toFloat()
            val slotBottom = slotTop + itemInfo.size.toFloat()

            val dragMiddleY = dragCardOffsetY + (rowHeightPx / 2f)

            if (dragMiddleY in slotTop..slotBottom) {
                val mutable = currentLocalList.toMutableList()
                val removed = mutable.removeAt(fromSlot)
                mutable.add(toSlot, removed)
                dragList = mutable
                currentDragIndex = toSlot
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                return
            }
        }
    }

    // Auto-scroll when holding and dragging near top or bottom of list viewport
    LaunchedEffect(draggingEntry, dragCardOffsetY, listViewportHeight) {
        if (draggingEntry != null && listViewportHeight > 0f) {
            val scrollEdgeThreshold = with(density) { 56.dp.toPx() }
            while (isActive && draggingEntry != null) {
                if (dragCardOffsetY < scrollEdgeThreshold && listState.canScrollBackward) {
                    val speed = ((scrollEdgeThreshold - dragCardOffsetY) / scrollEdgeThreshold).coerceIn(0.2f, 1f) * with(density) { 14.dp.toPx() }
                    listState.scrollBy(-speed)
                    checkAndSwapSlots()
                } else if (dragCardOffsetY > (listViewportHeight - scrollEdgeThreshold - rowHeightPx) && listState.canScrollForward) {
                    val over = dragCardOffsetY - (listViewportHeight - scrollEdgeThreshold - rowHeightPx)
                    val speed = (over / scrollEdgeThreshold).coerceIn(0.2f, 1f) * with(density) { 14.dp.toPx() }
                    listState.scrollBy(speed)
                    checkAndSwapSlots()
                }
                delay(24)
            }
        }
    }

    // The viewport begins at the actual rows' combined height. Its parent constrains that request
    // on smaller displays, at which point LazyColumn alone becomes scrollable. This removes the
    // former fixed long-queue gap and keeps the final row fully reachable.
    val desiredQueueHeight = (queue.size * 64 + 16).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .animateContentSize(animationSpec = tween(180, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))
    ) {
        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty",
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(desiredQueueHeight)
                    .onGloballyPositioned { coordinates ->
                        listViewportHeight = coordinates.size.height.toFloat()
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downY = down.position.y

                            val hitItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                downY >= item.offset && downY <= (item.offset + item.size)
                            }

                            if (hitItem != null && hitItem.index in sourceEntries.indices) {
                                val hitIndex = hitItem.index
                                val initialItemTop = hitItem.offset.toFloat()
                                val touchOffsetYInCard = downY - initialItemTop

                                val longPressTriggered = withTimeoutOrNull(400) {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed) return@withTimeoutOrNull false
                                        val moveDist = Math.abs(change.position.y - down.position.y)
                                        if (moveDist > 18f) return@withTimeoutOrNull false
                                    }
                                    @Suppress("UNREACHABLE_CODE")
                                    true
                                } ?: true

                                if (longPressTriggered) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val entry = sourceEntries[hitIndex]
                                    draggingEntry = entry
                                    dragList = sourceEntries.toList()
                                    initialDragIndex = hitIndex
                                    currentDragIndex = hitIndex
                                    dragCardOffsetY = (downY - touchOffsetYInCard).coerceAtLeast(0f)

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed) break
                                        change.consume()

                                        val currentPointerY = change.position.y
                                        dragCardOffsetY = (currentPointerY - touchOffsetYInCard).coerceIn(
                                            0f,
                                            (listViewportHeight - rowHeightPx).coerceAtLeast(0f)
                                        )

                                        checkAndSwapSlots()
                                    }

                                    val finalList = dragList
                                    val from = initialDragIndex
                                    val to = currentDragIndex
                                    draggingEntry = null
                                    dragList = null
                                    initialDragIndex = -1
                                    currentDragIndex = -1
                                    if (finalList != null) {
                                        if (onReorderQueue != null) {
                                            onReorderQueue(finalList.map(QueueEntry::song))
                                        } else if (from in queue.indices && to in queue.indices && from != to) {
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
                    userScrollEnabled = draggingEntry == null,
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(RowSpacing),
                    modifier = Modifier
                        .fillMaxSize()
                        .xvoxBoxScroll(listState)
                ) {
                    itemsIndexed(
                        items = displayEntries,
                        key = { _, entry -> entry.stableKey },
                        contentType = { _, _ -> "queue_row" }
                    ) { idx, entry ->
                        val song = entry.song
                        val isThisItemBeingDragged = draggingEntry?.stableKey == entry.stableKey && idx == currentDragIndex

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(RowHeight)
                                .then(
                                    if (isThisItemBeingDragged) Modifier.alpha(0.12f)
                                    else Modifier
                                )
                        ) {
                            QueueItemRow(
                                song = song,
                                index = idx,
                                isPlayingThis = isPlaybackActiveInThisQueue && idx == currentIndex,
                                isPlayingAudio = isPlaying,
                                totalCount = displayEntries.size,
                                onPlay = { play(idx) },
                                onRemove = { remove(idx) },
                                onMoveUp = if (idx > 0) { { move(idx, idx - 1) } } else null,
                                onMoveDown = if (idx < displayEntries.lastIndex) { { move(idx, idx + 1) } } else null
                            )
                        }
                    }
                }

                if (draggingEntry != null) {
                    val draggedSong = draggingEntry!!.song
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(RowHeight)
                            .offset { IntOffset(0, dragCardOffsetY.roundToInt()) }
                            .zIndex(100f)
                            .shadow(16.dp, shape = RoundedCornerShape(14.dp))
                    ) {
                        QueueItemRow(
                            song = draggedSong,
                            index = currentDragIndex,
                            isPlayingThis = isPlaybackActiveInThisQueue && currentDragIndex == currentIndex,
                            isPlayingAudio = isPlaying,
                            totalCount = displayEntries.size,
                            onPlay = {},
                            onRemove = {},
                            onMoveUp = null,
                            onMoveDown = null,
                            isFloating = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItemRow(
    song: Song,
    index: Int,
    isPlayingThis: Boolean,
    isPlayingAudio: Boolean,
    totalCount: Int,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    isFloating: Boolean = false
) {
    val colors = XvoxTheme.colors
    val cardColor = rememberSongCardColor(song = song, current = isPlayingThis)

    val backgroundAlpha = when {
        isFloating -> 1f
        isPlayingThis -> 0.70f
        else -> 0.40f
    }

    val finalBackground = if (isPlayingThis) {
        colors.primaryAccent.copy(alpha = 0.20f)
    } else {
        cardColor.copy(alpha = backgroundAlpha)
    }

    val rowSemantics = Modifier.semantics {
        customActions = buildList {
            add(CustomAccessibilityAction("Remove from queue") { onRemove(); true })
            if (onMoveUp != null) {
                add(CustomAccessibilityAction("Move up") { onMoveUp(); true })
            }
            if (onMoveDown != null) {
                add(CustomAccessibilityAction("Move down") { onMoveDown(); true })
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(finalBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onPlay
            )
            .then(rowSemantics)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            XvoxSongArtwork(
                artwork = song.artworkUri,
                requestSize = 120,
                modifier = Modifier.fillMaxSize()
            )

            if (isPlayingThis) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlayingAudio) R.drawable.ic_xvox_waveform
                            else R.drawable.ic_xvox_play
                        ),
                        contentDescription = "Currently Playing",
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                color = if (isPlayingThis) colors.primaryAccent else colors.primaryText,
                fontSize = 13.5.sp,
                fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = song.artist,
                color = colors.secondaryText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRemove
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_close),
                contentDescription = "Remove from Queue",
                tint = colors.secondaryText.copy(alpha = 0.8f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
