package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    onLike: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density =
        LocalDensity.current

    val scope =
        rememberCoroutineScope()

    val exitDistance =
        with(density) {
            190.dp.toPx()
        }

    val previewStep =
        with(density) {
            72.dp.toPx()
        }

    val y =
        remember(riseKey) {
            Animatable(
                exitDistance
            )
        }

    var dragX by remember {
        mutableFloatStateOf(0f)
    }

    var previewIndex by
        remember(
            currentSongId,
            queue
        ) {
            mutableIntStateOf(
                currentIndex
                    .takeIf {
                        it in queue.indices
                    }
                    ?: 0
            )
        }

    var direction by remember {
        mutableIntStateOf(0)
    }

    var axis by remember {
        mutableStateOf(
            XvoxMiniAxis.NONE
        )
    }

    var rawX by remember {
        mutableFloatStateOf(0f)
    }

    var rawY by remember {
        mutableFloatStateOf(0f)
    }

    var previewAnchorX by remember {
        mutableFloatStateOf(0f)
    }

    var moved by remember {
        mutableStateOf(false)
    }

    var actionsVisible by remember {
        mutableStateOf(false)
    }

    var exiting by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(riseKey) {
        y.animateTo(
            targetValue = 0f,
            animationSpec =
                XvoxMiniPlayerMotion
                    .riseSpec
        )
    }

    fun exit(
        stopPlayback: Boolean,
        after: () -> Unit
    ) {
        if (exiting) {
            return
        }

        exiting = true

        scope.launch {
            actionsVisible = false
            dragX = 0f

            y.animateTo(
                targetValue =
                    exitDistance,
                animationSpec =
                    XvoxMiniPlayerMotion
                        .exitSpec
            )

            if (stopPlayback) {
                stopAndDismiss()
            }

            after()
        }
    }

    val visualSong =
        queue.getOrNull(
            previewIndex
        ) ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp),
        contentAlignment =
            Alignment.BottomCenter
    ) {
        XvoxMiniPlayerActions(
            visible = actionsVisible,
            onLike = {
                actionsVisible = false
                onLike()
            },
            onAdd = {
                actionsVisible = false
                onAdd()
            },
            onClose = {
                exit(
                    stopPlayback = true,
                    after = {}
                )
            },
            modifier =
                Modifier.align(
                    Alignment.TopCenter
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .align(
                    Alignment.BottomCenter
                )
                .pointerInput(
                    currentSongId,
                    queue,
                    exiting
                ) {
                    if (exiting) {
                        return@pointerInput
                    }

                    detectDragGestures(
                        onDragStart = {
                            actionsVisible = false

                            axis =
                                XvoxMiniAxis.NONE

                            rawX = 0f
                            rawY = 0f
                            previewAnchorX = 0f
                            dragX = 0f
                            moved = false
                        },

                        onDrag = {
                            change,
                            amount ->

                            change.consume()

                            rawX += amount.x
                            rawY += amount.y

                            if (
                                axis ==
                                XvoxMiniAxis.NONE &&
                                (
                                    abs(rawX) >
                                        XvoxMiniPlayerMotion
                                            .AxisThreshold ||
                                    abs(rawY) >
                                        XvoxMiniPlayerMotion
                                            .AxisThreshold
                                    )
                            ) {
                                moved = true

                                axis =
                                    if (
                                        abs(rawX) >
                                        abs(rawY)
                                    ) {
                                        XvoxMiniAxis
                                            .HORIZONTAL
                                    } else {
                                        XvoxMiniAxis
                                            .VERTICAL
                                    }
                            }

                            when (axis) {
                                XvoxMiniAxis.HORIZONTAL -> {
                                    val local =
                                        rawX -
                                            previewAnchorX

                                    when {
                                        local <=
                                            -previewStep &&
                                            previewIndex <
                                            queue.lastIndex -> {

                                            previewIndex++
                                            direction = 1

                                            previewAnchorX -=
                                                previewStep
                                        }

                                        local >=
                                            previewStep &&
                                            previewIndex > 0 -> {

                                            previewIndex--
                                            direction = -1

                                            previewAnchorX +=
                                                previewStep
                                        }
                                    }

                                    dragX =
                                        XvoxMiniPlayerMotion
                                            .horizontalResistance(
                                                rawX -
                                                    previewAnchorX
                                            )
                                }

                                XvoxMiniAxis.VERTICAL -> {
                                    dragX = 0f

                                    scope.launch {
                                        y.snapTo(
                                            XvoxMiniPlayerMotion
                                                .verticalResistance(
                                                    rawY
                                                )
                                        )
                                    }
                                }

                                XvoxMiniAxis.NONE -> Unit
                            }
                        },

                        onDragEnd = {
                            val finalAxis = axis
                            val finalY = rawY

                            rawX = 0f
                            rawY = 0f
                            previewAnchorX = 0f
                            axis =
                                XvoxMiniAxis.NONE

                            when (finalAxis) {
                                XvoxMiniAxis.HORIZONTAL -> {
                                    dragX = 0f

                                    if (
                                        previewIndex in
                                        queue.indices &&
                                        previewIndex !=
                                        currentIndex
                                    ) {
                                        playQueueIndex(
                                            previewIndex
                                        )
                                    }

                                    moved = false
                                }

                                XvoxMiniAxis.VERTICAL -> {
                                    when {
                                        finalY <=
                                            XvoxMiniPlayerMotion
                                                .OpenThreshold -> {
                                            exit(
                                                stopPlayback =
                                                    false
                                            ) {
                                                openPlayer()
                                            }
                                        }

                                        finalY >=
                                            XvoxMiniPlayerMotion
                                                .CloseThreshold -> {
                                            exit(
                                                stopPlayback =
                                                    true,
                                                after = {}
                                            )
                                        }

                                        else -> {
                                            scope.launch {
                                                y.animateTo(
                                                    targetValue =
                                                        0f,
                                                    animationSpec =
                                                        XvoxMiniPlayerMotion
                                                            .verticalReturnSpec
                                                )
                                            }
                                        }
                                    }

                                    moved = false
                                }

                                XvoxMiniAxis.NONE -> {
                                    dragX = 0f
                                    moved = false
                                }
                            }
                        },

                        onDragCancel = {
                            rawX = 0f
                            rawY = 0f
                            previewAnchorX = 0f
                            dragX = 0f

                            axis =
                                XvoxMiniAxis.NONE

                            moved = false

                            scope.launch {
                                y.animateTo(
                                    targetValue = 0f,
                                    animationSpec =
                                        XvoxMiniPlayerMotion
                                            .verticalReturnSpec
                                )
                            }
                        }
                    )
                }
                .pointerInput(
                    currentSongId,
                    exiting
                ) {
                    if (exiting) {
                        return@pointerInput
                    }

                    detectTapGestures(
                        onLongPress = {
                            if (!moved) {
                                actionsVisible =
                                    !actionsVisible
                            }
                        },

                        onTap = {
                            if (!moved) {
                                if (
                                    actionsVisible
                                ) {
                                    actionsVisible =
                                        false
                                } else {
                                    exit(
                                        stopPlayback =
                                            false
                                    ) {
                                        openPlayer()
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            XvoxMiniPlayerCard(
                song = visualSong,
                isPlaying =
                    isPlaying,
                position =
                    if (
                        visualSong.id ==
                        currentSongId
                    ) {
                        position
                    } else {
                        0L
                    },
                duration =
                    if (
                        visualSong.id ==
                        currentSongId
                    ) {
                        duration
                    } else {
                        0L
                    },
                direction =
                    direction,
                togglePlay =
                    togglePlay,
                modifier = Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .graphicsLayer {
                        translationX =
                            dragX

                        translationY =
                            y.value
                    }
            )
        }
    }
}
