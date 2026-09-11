package com.xvox.music.shell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val RowHeight = 54.dp
private val RowSpacing = 4.dp

@Composable
fun XvoxQueueBoxContent(
    queue: List<Song>,
    currentSongId: Long?,
    onPlayIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit
) {
    val colors = XvoxTheme.colors
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = LocalXvoxHaptics.current
    val move by rememberUpdatedState(onMoveItem)
    val play by rememberUpdatedState(onPlayIndex)
    val local = remember { mutableStateListOf<Song>().apply { addAll(queue) } }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    var grabOffset by remember { mutableFloatStateOf(0f) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    val rowHeightPx = with(density) { RowHeight.toPx() }
    val stridePx = with(density) { (RowHeight + RowSpacing).toPx() }
    val overlayY = (pointerY - grabOffset).coerceIn(0f, (viewportHeight - rowHeightPx).coerceAtLeast(0f))

    LaunchedEffect(queue, draggedId) {
        if (draggedId != null && queue.none { it.id == draggedId }) draggedId = null
        if (draggedId == null && local.toList() != queue) {
            local.clear()
            local.addAll(queue)
        }
    }

    fun targetIndex(): Int {
        if (local.isEmpty()) return -1
        val scrolledPx = listState.firstVisibleItemIndex * stridePx + listState.firstVisibleItemScrollOffset
        val centre = pointerY + scrolledPx
        return ((centre - rowHeightPx / 2f) / stridePx).roundToInt().coerceIn(0, local.lastIndex)
    }

    fun reorderAtPointer() {
        val id = draggedId ?: return
        val from = local.indexOfFirst { it.id == id }
        val to = targetIndex()
        if (from >= 0 && to >= 0 && to != from) {
            haptics.tap()
            local.add(to, local.removeAt(from))
        }
    }

    fun finishDrag(commit: Boolean) {
        val id = draggedId
        draggedId = null
        if (!commit || id == null) return
        val to = local.indexOfFirst { it.id == id }
        if (dragStartIndex >= 0 && to >= 0 && to != dragStartIndex) move(dragStartIndex, to)
        dragStartIndex = -1
    }

    // Auto-scroll loop when item is dragged near top or bottom edges
    LaunchedEffect(draggedId) {
        if (draggedId == null) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        while (isActive && draggedId != null) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
            previousFrame = frame

            val edgeZone = (rowHeightPx * 1.8f).coerceAtMost(viewportHeight * 0.35f).coerceAtLeast(40f)
            val strength = when {
                edgeZone <= 0f -> 0f
                pointerY < edgeZone -> -((edgeZone - pointerY) / edgeZone).coerceIn(0f, 1.2f)
                pointerY > viewportHeight - edgeZone -> ((pointerY - (viewportHeight - edgeZone)) / edgeZone).coerceIn(0f, 1.2f)
                else -> 0f
            }

            if (strength != 0f) {
                val scrollSpeed = strength * rowHeightPx * 18f
                val consumed = listState.scrollBy(scrollSpeed * seconds)
                if (abs(consumed) > 0.1f) {
                    reorderAtPointer()
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            "${local.size} songs · Hold the dots to reorder",
            color = colors.secondaryText,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        if (local.isEmpty()) {
            Text("Your queue is empty", color = colors.mutedText, modifier = Modifier.padding(20.dp))
        } else Box(Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            LazyColumn(
                state = listState,
                userScrollEnabled = draggedId == null,
                contentPadding = PaddingValues(vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(RowSpacing),
                modifier = Modifier.fillMaxWidth().onSizeChanged { viewportHeight = it.height }
            ) {
                itemsIndexed(local, key = { _, it -> it.id }, contentType = { _, _ -> "queue_row" }) { idx, song ->
                    QueueRow(
                        song = song,
                        current = song.id == currentSongId,
                        onClick = {
                            val index = local.indexOfFirst { it.id == song.id }
                            if (draggedId == null && index >= 0) play(index)
                        },
                        onStartDrag = {
                            draggedId = song.id
                            dragStartIndex = idx
                            val scrolledPx = listState.firstVisibleItemIndex * stridePx + listState.firstVisibleItemScrollOffset
                            val itemTop = idx * stridePx - scrolledPx
                            grabOffset = rowHeightPx / 2f
                            pointerY = itemTop + grabOffset
                            haptics.heavy()
                            scope.launch { listState.stopScroll() }
                        },
                        onDragDelta = { deltaY ->
                            pointerY += deltaY
                            reorderAtPointer()
                        },
                        onEndDrag = { finishDrag(commit = true) },
                        onCancelDrag = { finishDrag(commit = false) },
                        modifier = Modifier
                            .graphicsLayer { alpha = if (draggedId == song.id) 0f else 1f }
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction("Move up") {
                                        val index = local.indexOfFirst { it.id == song.id }
                                        if (index > 0) {
                                            move(index, index - 1)
                                            true
                                        } else false
                                    },
                                    CustomAccessibilityAction("Move down") {
                                        val index = local.indexOfFirst { it.id == song.id }
                                        if (index >= 0 && index < local.lastIndex) {
                                            move(index, index + 1)
                                            true
                                        } else false
                                    }
                                )
                            }
                    )
                }
            }
            local.firstOrNull { it.id == draggedId }?.let { song ->
                QueueRow(
                    song = song,
                    current = song.id == currentSongId,
                    onClick = null,
                    modifier = Modifier
                        .offset { IntOffset(0, overlayY.roundToInt()) }
                        .shadow(10.dp, RoundedCornerShape(14.dp))
                        .graphicsLayer {
                            scaleX = 1.015f
                            scaleY = 1.015f
                        }
                )
            }
        }
    }
}

@Composable
private fun QueueRow(
    song: Song,
    current: Boolean,
    onClick: (() -> Unit)?,
    onStartDrag: (() -> Unit)? = null,
    onDragDelta: ((Float) -> Unit)? = null,
    onEndDrag: (() -> Unit)? = null,
    onCancelDrag: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val color = rememberSongCardColor(song, current)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .then(if (onClick != null) Modifier.xvoxSongPress(onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        XvoxSongArtwork(
            song.artworkUri,
            requestSize = 96,
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp))
        )
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                song.title,
                color = if (current) colors.primaryAccent else colors.primaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                color = colors.secondaryText,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .then(
                    if (onStartDrag != null) {
                        Modifier.pointerInput(song.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onStartDrag() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragDelta?.invoke(dragAmount.y)
                                },
                                onDragEnd = { onEndDrag?.invoke() },
                                onDragCancel = { onCancelDrag?.invoke() }
                            )
                        }
                    } else Modifier
                )
        ) {
            XvoxDragDots(modifier = Modifier.semantics { contentDescription = "Reorder ${song.title}" })
        }
    }
}

@Composable
private fun XvoxDragDots(modifier: Modifier = Modifier) {
    val tint = XvoxTheme.colors.mutedText
    Canvas(modifier.size(width = 18.dp, height = 20.dp)) {
        val radius = 1.6.dp.toPx()
        val columnGap = 6.dp.toPx()
        val rowGap = 6.dp.toPx()
        val startX = (size.width - columnGap) / 2f
        val startY = (size.height - rowGap * 2) / 2f
        for (column in 0..1) for (row in 0..2) {
            drawCircle(tint, radius, Offset(startX + column * columnGap, startY + row * rowGap))
        }
    }
}
