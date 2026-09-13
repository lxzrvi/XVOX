package com.xvox.music.shell

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor
import kotlinx.coroutines.isActive

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

    var draggingSongId by remember { mutableStateOf<Long?>(null) }
    var initialDragIndex by remember { mutableIntStateOf(-1) }
    var currentDragIndex by remember { mutableIntStateOf(-1) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }
    var pointerViewportY by remember { mutableFloatStateOf(0f) }
    var listViewportHeight by remember { mutableFloatStateOf(0f) }

    val rowTotalHeightPx = with(density) { (RowHeight + RowSpacing).toPx() }

    LaunchedEffect(queue) {
        if (draggingSongId == null && local.toList() != queue) {
            local.clear()
            local.addAll(queue)
        }
    }

    fun startDrag(song: Song, initialY: Float = 0f) {
        val idx = local.indexOfFirst { it.id == song.id }
        if (idx >= 0) {
            draggingSongId = song.id
            initialDragIndex = idx
            currentDragIndex = idx
            dragAccumulatedY = 0f
            pointerViewportY = initialY
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun onDrag(deltaY: Float) {
        if (draggingSongId == null) return
        dragAccumulatedY += deltaY
        pointerViewportY += deltaY

        val threshold = rowTotalHeightPx * 0.50f
        while (dragAccumulatedY > threshold && currentDragIndex < local.lastIndex) {
            val from = currentDragIndex
            val to = currentDragIndex + 1
            val item = local.removeAt(from)
            local.add(to, item)
            dragAccumulatedY -= rowTotalHeightPx
            currentDragIndex = to
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        while (dragAccumulatedY < -threshold && currentDragIndex > 0) {
            val from = currentDragIndex
            val to = currentDragIndex - 1
            val item = local.removeAt(from)
            local.add(to, item)
            dragAccumulatedY += rowTotalHeightPx
            currentDragIndex = to
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun finishDrag() {
        val initial = initialDragIndex
        val final = currentDragIndex
        draggingSongId = null
        initialDragIndex = -1
        currentDragIndex = -1
        dragAccumulatedY = 0f

        if (initial >= 0 && final >= 0 && initial != final) {
            move(initial, final)
        }
    }

    // Auto-scroll loop when item is dragged near top or bottom edges
    LaunchedEffect(draggingSongId) {
        if (draggingSongId == null) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        while (isActive && draggingSongId != null) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
            previousFrame = frame

            val edgeZone = rowTotalHeightPx * 1.6f
            val strength = when {
                edgeZone <= 0f -> 0f
                pointerViewportY < edgeZone -> -((edgeZone - pointerViewportY) / edgeZone).coerceIn(0f, 1.6f)
                pointerViewportY > listViewportHeight - edgeZone -> ((pointerViewportY - (listViewportHeight - edgeZone)) / edgeZone).coerceIn(0f, 1.6f)
                else -> 0f
            }

            if (strength != 0f) {
                val scrollSpeed = strength * rowTotalHeightPx * 14f
                val delta = scrollSpeed * seconds
                listState.scrollBy(delta)
                dragAccumulatedY += delta

                val threshold = rowTotalHeightPx * 0.50f
                while (dragAccumulatedY > threshold && currentDragIndex < local.lastIndex) {
                    val from = currentDragIndex
                    val to = currentDragIndex + 1
                    val item = local.removeAt(from)
                    local.add(to, item)
                    dragAccumulatedY -= rowTotalHeightPx
                    currentDragIndex = to
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                while (dragAccumulatedY < -threshold && currentDragIndex > 0) {
                    val from = currentDragIndex
                    val to = currentDragIndex - 1
                    val item = local.removeAt(from)
                    local.add(to, item)
                    dragAccumulatedY += rowTotalHeightPx
                    currentDragIndex = to
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        } else Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .onGloballyPositioned { listViewportHeight = it.size.height.toFloat() }
        ) {
            LazyColumn(
                state = listState,
                userScrollEnabled = draggingSongId == null,
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(RowSpacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(local, key = { _, it -> it.id }, contentType = { _, _ -> "queue_row" }) { idx, song ->
                    val isDragging = draggingSongId == song.id

                    val animatedElevation by animateDpAsState(
                        targetValue = if (isDragging) 14.dp else 0.dp,
                        animationSpec = tween(150),
                        label = "cardElevation"
                    )

                    QueueRow(
                        song = song,
                        isDragging = isDragging,
                        elevationDp = animatedElevation,
                        translationY = if (isDragging) dragAccumulatedY else 0f,
                        current = song.id == currentSongId,
                        onClick = {
                            val target = local.indexOfFirst { it.id == song.id }
                            if (target >= 0 && draggingSongId == null) play(target)
                        },
                        onStartDrag = { startY -> startDrag(song, startY) },
                        onDragDelta = ::onDrag,
                        onEndDrag = ::finishDrag,
                        modifier = Modifier
                            .zIndex(if (isDragging) 100f else 1f)
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
        }
    }
}

@Composable
private fun QueueRow(
    song: Song,
    isDragging: Boolean,
    elevationDp: androidx.compose.ui.unit.Dp,
    translationY: Float,
    current: Boolean,
    onClick: () -> Unit,
    onStartDrag: (Float) -> Unit,
    onDragDelta: (Float) -> Unit,
    onEndDrag: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val cardBg = rememberSongCardColor(song, current)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .graphicsLayer {
                this.translationY = translationY
                this.scaleX = if (isDragging) 1.03f else 1f
                this.scaleY = if (isDragging) 1.03f else 1f
            }
            .shadow(elevationDp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .pointerInput(song.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> onStartDrag(offset.y) },
                    onDragCancel = { onEndDrag() },
                    onDragEnd = { onEndDrag() },
                    onDrag = { change, amount ->
                        change.consume()
                        onDragDelta(amount.y)
                    }
                )
            }
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
