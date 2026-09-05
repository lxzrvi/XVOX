package com.xvox.music.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork
import kotlinx.coroutines.delay

@Composable
fun XvoxQueueSheetContent(
    queue: List<Song>,
    currentSongId: Long?,
    onPlayIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit
) {
    val colors = XvoxTheme.colors
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val slotHeightPx = with(density) { 58.dp.toPx() }

    val localQueue = remember(queue) {
        mutableStateListOf<Song>().apply { addAll(queue) }
    }

    var draggingSongId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var autoScrollDirection by remember { mutableIntStateOf(0) }

    LaunchedEffect(draggingSongId, autoScrollDirection) {
        if (draggingSongId == null || autoScrollDirection == 0) return@LaunchedEffect

        while (draggingSongId != null && autoScrollDirection != 0) {
            val amount = 12f * autoScrollDirection
            val consumed = listState.scrollBy(amount)
            dragOffsetY -= consumed

            val songId = draggingSongId ?: break
            var from = localQueue.indexOfFirst { it.id == songId }
            if (from < 0) break

            while (dragOffsetY > slotHeightPx / 2f && from < localQueue.lastIndex) {
                val moving = localQueue.removeAt(from)
                localQueue.add(from + 1, moving)
                onMoveItem(from, from + 1)
                from++
                dragOffsetY -= slotHeightPx
            }

            while (dragOffsetY < -slotHeightPx / 2f && from > 0) {
                val moving = localQueue.removeAt(from)
                localQueue.add(from - 1, moving)
                onMoveItem(from, from - 1)
                from--
                dragOffsetY += slotHeightPx
            }

            delay(16L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playing Queue (${localQueue.size})",
                color = colors.primaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Long press & drag",
                color = colors.mutedText,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(6.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(items = localQueue, key = { _, song -> song.id }) { _, song ->
                val isCurrent = song.id == currentSongId
                val isDragging = song.id == draggingSongId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .zIndex(if (isDragging) 100f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffsetY else 0f
                            scaleX = if (isDragging) 1.025f else 1f
                            scaleY = if (isDragging) 1.025f else 1f
                            shadowElevation = if (isDragging) 16.dp.toPx() else 0f
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isDragging -> colors.cardElevated
                                isCurrent -> colors.card.copy(alpha = 0.95f)
                                else -> colors.cardElevated.copy(alpha = 0.40f)
                            }
                        )
                        .pointerInput(song.id, localQueue.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingSongId = song.id
                                    dragOffsetY = 0f
                                    autoScrollDirection = 0
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffsetY += amount.y

                                    var from = localQueue.indexOfFirst { it.id == song.id }
                                    if (from < 0) return@detectDragGesturesAfterLongPress

                                    while (dragOffsetY > slotHeightPx / 2f && from < localQueue.lastIndex) {
                                        val moving = localQueue.removeAt(from)
                                        localQueue.add(from + 1, moving)
                                        onMoveItem(from, from + 1)
                                        from++
                                        dragOffsetY -= slotHeightPx
                                    }

                                    while (dragOffsetY < -slotHeightPx / 2f && from > 0) {
                                        val moving = localQueue.removeAt(from)
                                        localQueue.add(from - 1, moving)
                                        onMoveItem(from, from - 1)
                                        from--
                                        dragOffsetY += slotHeightPx
                                    }

                                    val layout = listState.layoutInfo
                                    val draggedInfo = layout.visibleItemsInfo.firstOrNull { it.index == from }
                                    if (draggedInfo == null) {
                                        autoScrollDirection = 0
                                    } else {
                                        val top = draggedInfo.offset + dragOffsetY
                                        val bottom = top + draggedInfo.size
                                        val edgeZone = slotHeightPx * 1.35f
                                        autoScrollDirection = when {
                                            top < layout.viewportStartOffset + edgeZone -> -1
                                            bottom > layout.viewportEndOffset - edgeZone -> 1
                                            else -> 0
                                        }
                                    }
                                },
                                onDragEnd = {
                                    autoScrollDirection = 0
                                    dragOffsetY = 0f
                                    draggingSongId = null
                                },
                                onDragCancel = {
                                    autoScrollDirection = 0
                                    dragOffsetY = 0f
                                    draggingSongId = null
                                }
                            )
                        }
                        .clickable(
                            enabled = draggingSongId == null,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val index = localQueue.indexOfFirst { it.id == song.id }
                            if (index >= 0) {
                                onPlayIndex(index)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
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
                            .padding(start = 10.dp, end = 6.dp)
                    ) {
                        Text(
                            text = song.title,
                            color = if (isCurrent) colors.primaryAccent else colors.primaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
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

                    if (isCurrent) {
                        Text(
                            text = "Playing",
                            color = colors.primaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }

                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_more),
                        contentDescription = "Long press to reorder",
                        tint = colors.secondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
