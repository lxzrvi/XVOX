package com.xvox.music.shell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
    val move by rememberUpdatedState(onMoveItem)
    val play by rememberUpdatedState(onPlayIndex)
    val local = remember { mutableStateListOf<Song>().apply { addAll(queue) } }

    LaunchedEffect(queue) {
        if (local.toList() != queue) {
            local.clear()
            local.addAll(queue)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            "${local.size} songs · Drag handle or long press to reorder",
            color = colors.secondaryText,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        if (local.isEmpty()) {
            Text("Your queue is empty", color = colors.mutedText, modifier = Modifier.padding(20.dp))
        } else Box(Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(RowSpacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(local, key = { _, it -> it.id }, contentType = { _, _ -> "queue_row" }) { idx, song ->
                    QueueRow(
                        song = song,
                        index = idx,
                        totalSize = local.size,
                        current = song.id == currentSongId,
                        onClick = {
                            val target = local.indexOfFirst { it.id == song.id }
                            if (target >= 0) play(target)
                        },
                        onMoveItem = { from, to ->
                            if (from in 0..local.lastIndex && to in 0..local.lastIndex && from != to) {
                                val item = local.removeAt(from)
                                local.add(to, item)
                                move(from, to)
                            }
                        },
                        modifier = Modifier.semantics {
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
    index: Int,
    totalSize: Int,
    current: Boolean,
    onClick: () -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val cardBg = rememberSongCardColor(song, current)

    var dragging by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val step = with(density) { 40.dp.toPx() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .zIndex(if (dragging) 10f else 0f)
            .graphicsLayer {
                translationY = dragY
                scaleX = if (dragging) 1.02f else 1f
                scaleY = if (dragging) 1.02f else 1f
                shadowElevation = if (dragging) 12.dp.toPx() else 0f
            }
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main card body: Click for playback + Long press to drag
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(song.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            dragging = true
                            dragY = 0f
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragCancel = {
                            dragging = false
                            dragY = 0f
                        },
                        onDragEnd = {
                            dragging = false
                            dragY = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragY += amount.y

                            if (dragY > step && index < totalSize - 1) {
                                onMoveItem(index, index + 1)
                                dragY -= step
                            } else if (dragY < -step && index > 0) {
                                onMoveItem(index, index - 1)
                                dragY += step
                            }
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            XvoxSongArtwork(
                artwork = song.artworkUri,
                requestSize = 96,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 6.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    color = if (current) colors.primaryAccent else colors.primaryText,
                    fontSize = 13.sp,
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

            // Visual indicator: "Playing" tag before the six-dot handle
            if (current) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primaryAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.5.dp),
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

        // Six-dots drag handle: Immediate direct drag on touch (no long press delay)
        Box(
            modifier = Modifier
                .size(40.dp)
                .pointerInput(song.id) {
                    detectDragGestures(
                        onDragStart = {
                            dragging = true
                            dragY = 0f
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragCancel = {
                            dragging = false
                            dragY = 0f
                        },
                        onDragEnd = {
                            dragging = false
                            dragY = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragY += amount.y

                            if (dragY > step && index < totalSize - 1) {
                                onMoveItem(index, index + 1)
                                dragY -= step
                            } else if (dragY < -step && index > 0) {
                                onMoveItem(index, index - 1)
                                dragY += step
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            SixDotsHandle(tint = colors.secondaryText.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun SixDotsHandle(tint: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.size(width = 12.dp, height = 18.dp)) {
        val radius = 1.4.dp.toPx()
        val colGap = 4.5.dp.toPx()
        val rowGap = 4.5.dp.toPx()
        val startX = (size.width - colGap) / 2f
        val startY = (size.height - rowGap * 2) / 2f
        for (c in 0..1) for (r in 0..2) {
            drawCircle(tint, radius, Offset(startX + c * colGap, startY + r * rowGap))
        }
    }
}
