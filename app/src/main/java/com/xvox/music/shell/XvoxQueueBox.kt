package com.xvox.music.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The gesture belongs to the list, NOT to a recycled row; an overlay follows the finger at the edges. */
@Composable
fun XvoxQueueBoxContent(queue: List<Song>, currentSongId: Long?, onPlayIndex: (Int) -> Unit, onMoveItem: (Int, Int) -> Unit) {
    val colors = XvoxTheme.colors
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val move by rememberUpdatedState(onMoveItem)
    val play by rememberUpdatedState(onPlayIndex)
    val local = remember { mutableStateListOf<Song>().apply { addAll(queue) } }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    var grabOffset by remember { mutableFloatStateOf(0f) }
    var draggedHeight by remember { mutableFloatStateOf(54f) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    val overlayY = (pointerY - grabOffset).coerceIn(0f, (viewportHeight - draggedHeight).coerceAtLeast(0f))

    LaunchedEffect(queue, draggedId) {
        if (draggedId != null && queue.none { it.id == draggedId }) draggedId = null
        if (draggedId == null && local.toList() != queue) { local.clear(); local.addAll(queue) }
    }
    fun reorderAtPointer() {
        val id = draggedId ?: return
        val from = local.indexOfFirst { it.id == id }
        if (from < 0) return
        val top = (pointerY - grabOffset).coerceIn(0f, (viewportHeight - draggedHeight).coerceAtLeast(0f))
        val center = top + draggedHeight / 2
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key != id && center >= it.offset && center < it.offset + it.size
        } ?: return
        val to = local.indexOfFirst { it.id == target.key }
        if (to >= 0 && to != from) {
            local.add(to, local.removeAt(from))
            move(from, to)
        }
    }
    LaunchedEffect(draggedId) {
        if (draggedId == null) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        while (isActive && draggedId != null) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceIn(0f, .05f)
            previousFrame = frame
            val edge = (draggedHeight * 1.5f).coerceAtMost(viewportHeight / 3f)
            val strength = when {
                edge <= 0 -> 0f
                pointerY < edge -> -((edge - pointerY) / edge).coerceIn(0f, 1f)
                pointerY > viewportHeight - edge -> ((pointerY - viewportHeight + edge) / edge).coerceIn(0f, 1f)
                else -> 0f
            }
            if (strength != 0f) {
                listState.scrollBy(strength * draggedHeight * 10f * seconds)
                reorderAtPointer()
            }
        }
    }
    Column(Modifier.fillMaxWidth()) {
        Text("${local.size} songs · Hold and drag to reorder", color = colors.secondaryText, fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 10.dp))
        if (local.isEmpty()) {
            Text("Your queue is empty", color = colors.mutedText, modifier = Modifier.padding(20.dp))
        } else Box(Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            LazyColumn(state = listState, userScrollEnabled = draggedId == null,
                contentPadding = PaddingValues(vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().onSizeChanged { viewportHeight = it.height }
                    .pointerInput(local, listState) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { point ->
                                val row = listState.layoutInfo.visibleItemsInfo.firstOrNull { point.y >= it.offset && point.y < it.offset + it.size }
                                draggedId = row?.key as? Long
                                if (row != null) {
                                    pointerY = point.y; grabOffset = point.y - row.offset; draggedHeight = row.size.toFloat()
                                    scope.launch { listState.stopScroll() }
                                }
                            },
                            onDrag = { change, _ ->
                                if (draggedId != null) {
                                    change.consume(); pointerY = change.position.y; reorderAtPointer()
                                }
                            },
                            onDragEnd = { draggedId = null },
                            onDragCancel = { draggedId = null }
                        )
                    }) {
                items(local, key = { it.id }, contentType = { "queue_row" }) { song ->
                    QueueRow(song, song.id == currentSongId, onClick = {
                        val index = local.indexOfFirst { it.id == song.id }
                        if (draggedId == null && index >= 0) play(index)
                    }, modifier = Modifier.graphicsLayer { alpha = if (draggedId == song.id) 0f else 1f }
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction("Move up") {
                                    val index = local.indexOfFirst { it.id == song.id }
                                    if (index > 0) { move(index, index - 1); true } else false
                                },
                                CustomAccessibilityAction("Move down") {
                                    val index = local.indexOfFirst { it.id == song.id }
                                    if (index >= 0 && index < local.lastIndex) { move(index, index + 1); true } else false
                                })
                        })
                }
            }
            local.firstOrNull { it.id == draggedId }?.let { song ->
                QueueRow(song, song.id == currentSongId, onClick = null,
                    modifier = Modifier.offset { IntOffset(0, overlayY.roundToInt()) }
                        .shadow(10.dp, RoundedCornerShape(14.dp)).graphicsLayer { scaleX = 1.015f; scaleY = 1.015f })
            }
        }
    }
}

@Composable
private fun QueueRow(song: Song, current: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    val color = rememberSongCardColor(song, current)
    Row(modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp)).background(color)
        .then(if (onClick != null) Modifier.xvoxSongPress(onClick) else Modifier)
        .padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        XvoxSongArtwork(song.artworkUri, requestSize = 96, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(song.title, color = if (current) colors.primaryAccent else colors.primaryText,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = colors.secondaryText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(painterResource(R.drawable.ic_xvox_queue), "Reorder ${song.title}", tint = colors.mutedText, modifier = Modifier.size(20.dp))
    }
}
