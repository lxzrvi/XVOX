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
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor
import com.xvox.music.player.playback.XvoxSavedQueue
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
                    .size(17.dp)
                    .graphicsLayer { rotationZ = if (expanded) 180f else 0f }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(colors.cardElevated)
                .clip(RoundedCornerShape(16.dp))
                .widthIn(min = 230.dp, max = 320.dp)
        ) {
            if (savedQueues.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "No other queues",
                            color = colors.mutedText,
                            fontSize = 13.sp,
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                activeQueueName,
                                color = colors.primaryAccent,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_check),
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    onClick = { expanded = false }
                )

                savedQueues.forEach { saved ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        saved.name,
                                        color = colors.primaryText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${saved.songs.size} songs",
                                        color = colors.secondaryText,
                                        fontSize = 11.5.sp
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

    val initialScrollIndex = remember {
        val idx = queue.indexOfFirst { it.id == currentSongId }
        if (idx > 1) (idx - 1).coerceAtLeast(0) else 0
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)

    val move by rememberUpdatedState(onMoveItem)
    val play by rememberUpdatedState(onPlayIndex)
    val remove by rememberUpdatedState(onRemoveIndex)

    var draggingSong by remember { mutableStateOf<Song?>(null) }
    var dragList by remember { mutableStateOf<List<Song>?>(null) }
    var initialDragIndex by remember { mutableIntStateOf(-1) }
    var currentDragIndex by remember { mutableIntStateOf(-1) }
    var touchOffsetInCard by remember { mutableFloatStateOf(0f) }
    var dragCardOffsetY by remember { mutableFloatStateOf(0f) }
    var listViewportHeight by remember { mutableFloatStateOf(0f) }

    val displayList = dragList ?: queue
    val currentListRef by rememberUpdatedState(displayList)

    val rowHeightPx = with(density) { RowHeight.toPx() }
    val rowSpacingPx = with(density) { RowSpacing.toPx() }
    val itemTotalHeightPx = rowHeightPx + rowSpacingPx

    fun checkAndSwapSlots() {
        val currentList = dragList ?: return
        if (draggingSong == null || currentDragIndex < 0 || listViewportHeight <= 0f || itemTotalHeightPx <= 0f) return

        val currentSlotScreenTop = (currentDragIndex - listState.firstVisibleItemIndex) * itemTotalHeightPx - listState.firstVisibleItemScrollOffset

        if (currentDragIndex < currentList.lastIndex) {
            if (dragCardOffsetY > currentSlotScreenTop + itemTotalHeightPx * 0.50f) {
                val from = currentDragIndex
                val to = currentDragIndex + 1
                if (from in currentList.indices && to in currentList.indices) {
                    val mutable = currentList.toMutableList()
                    val item = mutable.removeAt(from)
                    mutable.add(to, item)
                    dragList = mutable
                    currentDragIndex = to
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    return
                }
            }
        }

        if (currentDragIndex > 0) {
            val isAtVeryTop = dragCardOffsetY <= with(density) { 10.dp.toPx() } && listState.firstVisibleItemIndex == 0
            if (dragCardOffsetY < currentSlotScreenTop - itemTotalHeightPx * 0.50f || isAtVeryTop) {
                val from = currentDragIndex
                val to = currentDragIndex - 1
                if (from in currentList.indices && to in currentList.indices) {
                    val mutable = currentList.toMutableList()
                    val item = mutable.removeAt(from)
                    mutable.add(to, item)
                    dragList = mutable
                    currentDragIndex = to
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    return
                }
            }
        }
    }

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
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty",
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 460.dp)
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

                            val listSnapshot = currentListRef
                            if (hitItem != null && hitItem.index in listSnapshot.indices) {
                                val longPressed = withTimeoutOrNull(280L) {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        if ((change.position - down.position).getDistance() > 14f) break
                                    }
                                } == null && currentDragIndex == -1

                                if (longPressed && hitItem.index in listSnapshot.indices) {
                                    val song = listSnapshot[hitItem.index]
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dragList = listSnapshot.toList()
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
                                    dragList = null
                                    initialDragIndex = -1
                                    currentDragIndex = -1
                                    if (from in queue.indices && to in queue.indices && from != to) {
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
                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                ) {
                    itemsIndexed(
                        items = displayList,
                        key = { idx, song -> "${song.id}_$idx" },
                        contentType = { _, _ -> "queue_row" }
                    ) { idx, song ->
                        val isThisItemBeingDragged = draggingSong?.id == song.id && idx == currentDragIndex

                        QueueRowView(
                            song = song,
                            current = isPlaying && song.id == currentSongId,
                            onClick = {
                                if (draggingSong == null) {
                                    play(idx)
                                }
                            },
                            onRemove = {
                                remove(idx)
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
                                            if (idx < queue.lastIndex) { move(idx, idx + 1); true } else false
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
