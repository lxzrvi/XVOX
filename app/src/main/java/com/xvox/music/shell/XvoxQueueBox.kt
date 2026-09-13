package com.xvox.music.shell

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
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
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val move by rememberUpdatedState(onMoveItem)
    val play by rememberUpdatedState(onPlayIndex)
    val local = remember { mutableStateListOf<Song>().apply { addAll(queue) } }

    var draggingSongId by remember { mutableStateOf<Long?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragCurrentIndex by remember { mutableIntStateOf(-1) }
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

    fun commitDrag() {
        val from = dragStartIndex
        val to = dragCurrentIndex
        draggingSongId = null
        dragStartIndex = -1
        dragCurrentIndex = -1
        dragAccumulatedY = 0f

        if (from in 0..local.lastIndex && to in 0..local.lastIndex && from != to) {
            val item = local.removeAt(from)
            local.add(to, item)
            move(from, to)
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

            val edgeZone = rowTotalHeightPx * 1.5f
            val strength = when {
                edgeZone <= 0f -> 0f
                pointerViewportY < edgeZone -> -((edgeZone - pointerViewportY) / edgeZone).coerceIn(0f, 1.8f)
                pointerViewportY > listViewportHeight - edgeZone -> ((pointerViewportY - (listViewportHeight - edgeZone)) / edgeZone).coerceIn(0f, 1.8f)
                else -> 0f
            }

            if (strength != 0f) {
                val scrollSpeed = strength * rowTotalHeightPx * 16f
                listState.scrollBy(scrollSpeed * seconds)
            }
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
        } else Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .onGloballyPositioned { listViewportHeight = it.size.height.toFloat() }
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(RowSpacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(local, key = { _, it -> it.id }, contentType = { _, _ -> "queue_row" }) { idx, song ->
                    val isDragging = draggingSongId == song.id

                    // Calculate space-making visual displacement for non-dragged rows
                    val targetVisualDisplacementY = when {
                        isDragging -> dragAccumulatedY
                        draggingSongId == null -> 0f
                        dragStartIndex >= 0 && dragCurrentIndex >= 0 -> {
                            when {
                                dragCurrentIndex > dragStartIndex && idx in (dragStartIndex + 1)..dragCurrentIndex -> -rowTotalHeightPx
                                dragCurrentIndex < dragStartIndex && idx in dragCurrentIndex until dragStartIndex -> rowTotalHeightPx
                                else -> 0f
                            }
                        }
                        else -> 0f
                    }

                    val animatedDisplacementY by animateFloatAsState(
                        targetValue = targetVisualDisplacementY,
                        animationSpec = if (isDragging) spring(dampingRatio = 1f, stiffness = 1000f) else spring(dampingRatio = 0.82f, stiffness = 420f),
                        label = "rowDisplacement"
                    )

                    val animatedElevation by animateDpAsState(
                        targetValue = if (isDragging) 16.dp else 0.dp,
                        animationSpec = tween(150),
                        label = "cardElevation"
                    )

                    QueueRow(
                        song = song,
                        isDragging = isDragging,
                        elevationDp = animatedElevation,
                        translationY = animatedDisplacementY,
                        current = song.id == currentSongId,
                        onClick = {
                            val target = local.indexOfFirst { it.id == song.id }
                            if (target >= 0 && draggingSongId == null) play(target)
                        },
                        onStartDrag = {
                            val initialIdx = local.indexOfFirst { it.id == song.id }
                            if (initialIdx >= 0) {
                                draggingSongId = song.id
                                dragStartIndex = initialIdx
                                dragCurrentIndex = initialIdx
                                dragAccumulatedY = 0f
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDragDelta = { deltaY ->
                            dragAccumulatedY += deltaY
                            pointerViewportY += deltaY

                            if (dragStartIndex >= 0) {
                                val deltaSlots = (dragAccumulatedY / rowTotalHeightPx).roundToInt()
                                dragCurrentIndex = (dragStartIndex + deltaSlots).coerceIn(0, local.lastIndex)
                            }
                        },
                        onEndDrag = ::commitDrag,
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
    onStartDrag: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onEndDrag: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val cardBg = rememberSongCardColor(song, current)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .graphicsLayer {
                this.translationY = translationY
                this.scaleX = if (isDragging) 1.04f else 1f
                this.scaleY = if (isDragging) 1.04f else 1f
            }
            .shadow(elevationDp, RoundedCornerShape(12.dp))
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
                        onDragStart = { onStartDrag() },
                        onDragCancel = { onEndDrag() },
                        onDragEnd = { onEndDrag() },
                        onDrag = { change, amount ->
                            change.consume()
                            onDragDelta(amount.y)
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
                requestSize = 160,
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

            // Visual indicator: "Playing" badge before the six-dot handle
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

        // Six-dots drag handle: Immediate direct drag on touch
        Box(
            modifier = Modifier
                .size(40.dp)
                .pointerInput(song.id) {
                    detectDragGestures(
                        onDragStart = { onStartDrag() },
                        onDragCancel = { onEndDrag() },
                        onDragEnd = { onEndDrag() },
                        onDrag = { change, amount ->
                            change.consume()
                            onDragDelta(amount.y)
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
