package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun XvoxMiniPlayer(
    queue: List<Song>,
    currentSongId: Long,
    currentIndex: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    riseKey: Int,
    togglePlay: () -> Unit,
    playQueueIndex: (Int) -> Unit,
    stopAndDismiss: () -> Unit,
    openPlayer: () -> Unit,
    isLiked: Boolean = false,
    onLike: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onOpenMiniPlayerSettings: () -> Unit = {},
    quickActionsVisible: Boolean = false,
    onQuickActionsVisibleChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val chrome = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current
    val scope = rememberCoroutineScope()
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val defaultPlacementY = if (isLandscape) (-0.5f).dp else 13.dp
    val userPlacementY = chrome.miniPlayerOffsetY.coerceIn(-260f, 260f).dp
    val totalPlacementY = defaultPlacementY + userPlacementY
    // The card itself is 68dp tall. One extra dp clears the landscape -0.5dp baseline too. Custom
    // placement remains additive even for the handoff: a user-raised card travels far enough to
    // be fully below the viewport before Now Playing can start.
    val fullyHiddenDistanceDp = (69.dp - totalPlacementY).coerceAtLeast(0.dp)
    val exitDistance = with(density) { maxOf(230.dp, fullyHiddenDistanceDp + 24.dp).toPx() }
    val fullyHiddenDistance = with(density) { fullyHiddenDistanceDp.toPx() }
    val y = remember(riseKey) { Animatable(exitDistance) }

    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var rawX by remember { mutableFloatStateOf(0f) }
    var rawY by remember { mutableFloatStateOf(0f) }
    var axis by remember { mutableStateOf(XvoxMiniAxis.NONE) }
    var moved by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }
    var commitJob by remember { mutableStateOf<Job?>(null) }
    val setQuickActionsVisible by rememberUpdatedState(onQuickActionsVisibleChange)
    val addToPlaylist by rememberUpdatedState(onAddToPlaylist)
    val openMiniSettings by rememberUpdatedState(onOpenMiniPlayerSettings)

    var previewIndex by remember(currentSongId, queue) {
        mutableIntStateOf(currentIndex.takeIf { it in queue.indices } ?: 0)
    }
    var transitionDirection by remember { mutableIntStateOf(0) }

    LaunchedEffect(riseKey) {
        y.animateTo(0f, XvoxMiniPlayerMotion.riseSpec)
    }

    LaunchedEffect(currentSongId, currentIndex) {
        if (commitJob == null && axis == XvoxMiniAxis.NONE && currentIndex in queue.indices) {
            previewIndex = currentIndex
            transitionDirection = 0
        }
    }

    fun cancelCommit() {
        commitJob?.cancel()
        commitJob = null
    }

    fun scheduleCommit() {
        cancelCommit()
        val target = previewIndex
        if (target !in queue.indices || target == currentIndex) return

        commitJob = scope.launch {
            delay(XvoxMiniPlayerMotion.PreviewDelay)
            playQueueIndex(target)
            commitJob = null
        }
    }

    fun exit(currentY: Float, stop: Boolean) {
        if (exiting) return
        exiting = true
        setQuickActionsVisible(false)
        cancelCommit()

        scope.launch {
            // This is intentionally sequential: the card first clears below the viewport. Once
            // its top is no longer visible, wait exactly one short beat before mounting Now
            // Playing. The rest of the downward motion may finish offscreen without overlap.
            dragX = 0f
            y.snapTo(currentY.coerceAtLeast(0f))
            dragY = 0f
            var handoffRequested = false
            y.animateTo(exitDistance, XvoxMiniPlayerMotion.exitSpec) {
                if (!stop && !handoffRequested && value >= fullyHiddenDistance) {
                    handoffRequested = true
                    scope.launch {
                        delay(XvoxPlayerTransitionMotion.HandoffDelay)
                        if (exiting) openPlayer()
                    }
                }
            }

            if (stop) {
                stopAndDismiss()
            } else if (!handoffRequested) {
                // Covers an interrupted / already-offscreen card without ever overlapping the
                // full player.
                delay(XvoxPlayerTransitionMotion.HandoffDelay)
                openPlayer()
            }
        }
    }

    val visualSong = queue.getOrNull(previewIndex) ?: return

    Box(
        modifier = modifier
            // Placement is deliberately applied to the complete Mini Player interaction surface,
            // including the quick-action pill, rather than only moving its painted card.
            .offset(
                x = chrome.miniPlayerOffsetX.coerceIn(-220f, 220f).dp,
                // Requested baseline placement plus the user's persisted adjustment. The editor
                // remains additive instead of replacing portrait/landscape defaults.
                y = totalPlacementY
            )
            .fillMaxWidth()
            .height(122.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .align(Alignment.BottomCenter)
                .pointerInput(queue, currentSongId, exiting) {
                    if (exiting) return@pointerInput

                    detectDragGestures(
                        onDragStart = {
                            setQuickActionsVisible(false)
                            cancelCommit()
                            axis = XvoxMiniAxis.NONE
                            rawX = 0f
                            rawY = 0f
                            dragX = 0f
                            dragY = 0f
                            moved = false
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            rawX += amount.x
                            rawY += amount.y

                            if (axis == XvoxMiniAxis.NONE &&
                                (abs(rawX) > XvoxMiniPlayerMotion.AxisThreshold || abs(rawY) > XvoxMiniPlayerMotion.AxisThreshold)
                            ) {
                                moved = true
                                axis = if (abs(rawX) > abs(rawY)) XvoxMiniAxis.HORIZONTAL else XvoxMiniAxis.VERTICAL
                            }

                            when (axis) {
                                XvoxMiniAxis.HORIZONTAL -> {
                                    dragY = 0f
                                    dragX = XvoxMiniPlayerMotion.horizontalResistance(rawX)
                                }
                                XvoxMiniAxis.VERTICAL -> {
                                    dragX = 0f
                                    dragY = XvoxMiniPlayerMotion.verticalResistance(rawY)
                                }
                                XvoxMiniAxis.NONE -> Unit
                            }
                        },
                        onDragEnd = {
                            val finalAxis = axis
                            val finalX = rawX
                            val finalY = rawY
                            val finalDragY = dragY

                            rawX = 0f
                            rawY = 0f
                            axis = XvoxMiniAxis.NONE

                            when (finalAxis) {
                                XvoxMiniAxis.HORIZONTAL -> {
                                    dragX = 0f
                                    dragY = 0f
                                    when {
                                        finalX <= -XvoxMiniPlayerMotion.HorizontalThreshold && previewIndex < queue.lastIndex -> {
                                            transitionDirection = 1
                                            previewIndex++
                                        }
                                        finalX >= XvoxMiniPlayerMotion.HorizontalThreshold && previewIndex > 0 -> {
                                            transitionDirection = -1
                                            previewIndex--
                                        }
                                        else -> {
                                            transitionDirection = 0
                                        }
                                    }
                                    scheduleCommit()
                                }
                                XvoxMiniAxis.VERTICAL -> {
                                    dragX = 0f
                                    when {
                                        finalY <= XvoxMiniPlayerMotion.OpenThreshold -> {
                                            moved = false
                                            exit(y.value + finalDragY, false)
                                        }
                                        finalY >= XvoxMiniPlayerMotion.CloseThreshold -> {
                                            moved = false
                                            exit(y.value + finalDragY, true)
                                        }
                                        else -> {
                                            dragY = 0f
                                        }
                                    }
                                }
                                XvoxMiniAxis.NONE -> {
                                    dragX = 0f
                                    dragY = 0f
                                }
                            }
                            moved = false
                        },
                        onDragCancel = {
                            rawX = 0f
                            rawY = 0f
                            dragX = 0f
                            dragY = 0f
                            axis = XvoxMiniAxis.NONE
                            moved = false
                        }
                    )
                }
                .pointerInput(currentSongId, exiting) {
                    if (exiting) return@pointerInput

                    detectTapGestures(
                        onLongPress = {
                            if (!moved) {
                                cancelCommit()
                                setQuickActionsVisible(true)
                            }
                        },
                        onTap = {
                            if (!moved) {
                                cancelCommit()
                                if (quickActionsVisible) {
                                    setQuickActionsVisible(false)
                                } else {
                                    exit(y.value, false)
                                }
                            }
                        }
                    )
                }
        ) {
            XvoxMiniPlayerCard(
                song = visualSong,
                isPlaying = isPlaying,
                position = if (visualSong.id == currentSongId) position else 0L,
                duration = if (visualSong.id == currentSongId) duration else visualSong.duration,
                direction = transitionDirection,
                togglePlay = togglePlay,
                isLiked = isLiked,
                onLike = onLike,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationX = dragX
                        translationY = y.value + dragY
                    }
            )
        }

        AnimatedVisibility(
            visible = quickActionsVisible,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(180, easing = XvoxPlayerTransitionMotion.easing)
            ) + fadeIn(tween(150, easing = XvoxPlayerTransitionMotion.easing)),
            exit = slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = tween(160, easing = XvoxPlayerTransitionMotion.easing)
            ) + fadeOut(tween(130, easing = XvoxPlayerTransitionMotion.easing)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            XvoxMiniQuickActionPill(
                onAdd = {
                    setQuickActionsVisible(false)
                    addToPlaylist()
                },
                onSettings = {
                    setQuickActionsVisible(false)
                    openMiniSettings()
                },
                onDismiss = {
                    setQuickActionsVisible(false)
                    exit(y.value, true)
                }
            )
        }
    }
}

/** A compact long-press menu that rises from the Mini Player instead of opening a sheet. */
@Composable
private fun XvoxMiniQuickActionPill(
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .height(44.dp)
            .width(164.dp)
            .clip(shape)
            .background(colors.cardElevated.copy(alpha = .94f))
            .padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        XvoxMiniQuickAction(R.drawable.ic_xvox_add, "+ Add to playlist", onAdd)
        XvoxMiniQuickAction(R.drawable.ic_xvox_settings, "Mini Player settings", onSettings)
        XvoxMiniQuickAction(R.drawable.ic_xvox_close, "Dismiss Mini Player", onDismiss)
    }
}

@Composable
private fun XvoxMiniQuickAction(icon: Int, description: String, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = colors.primaryText,
            modifier = Modifier.size(18.dp)
        )
    }
}
