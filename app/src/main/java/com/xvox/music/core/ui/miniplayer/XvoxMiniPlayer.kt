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
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun XvoxMiniPlayer(
    queue: List<Song>,
    currentSongId: Long?,
    currentIndex: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    togglePlay: () -> Unit,
    playQueueIndex: (Int) -> Unit,
    openPlayer: () -> Unit,
    closePlayer: () -> Unit,
    onLike: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (
        currentSongId == null ||
        queue.isEmpty()
    ) {
        return
    }

    val realIndex =
        currentIndex
            .takeIf {
                it in queue.indices
            }
            ?: queue.indexOfFirst {
                it.id ==
                    currentSongId
            }
                .coerceAtLeast(0)

    val scope =
        rememberCoroutineScope()

    val x =
        remember {
            Animatable(0f)
        }

    val y =
        remember {
            Animatable(0f)
        }

    var previewIndex by
        remember(
            currentSongId,
            queue
        ) {
            mutableIntStateOf(
                realIndex
            )
        }

    var direction by
        remember {
            mutableIntStateOf(0)
        }

    var previewRevision by
        remember {
            mutableIntStateOf(0)
        }

    var axis by
        remember {
            mutableStateOf(
                XvoxMiniAxis.NONE
            )
        }

    var rawX by
        remember {
            mutableFloatStateOf(0f)
        }

    var rawY by
        remember {
            mutableFloatStateOf(0f)
        }

    var moved by
        remember {
            mutableStateOf(false)
        }

    var actionsVisible by
        remember {
            mutableStateOf(false)
        }

    LaunchedEffect(
        previewRevision
    ) {
        if (
            previewRevision <= 0
        ) {
            return@LaunchedEffect
        }

        val revision =
            previewRevision

        delay(
            XvoxMiniPlayerMotion
                .PreviewDelay
        )

        if (
            revision ==
            previewRevision
        ) {
            val target =
                previewIndex

            if (
                target in queue.indices &&
                target != realIndex
            ) {
                playQueueIndex(
                    target
                )
            }

            previewRevision = 0
        }
    }

    val song =
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
            visible =
                actionsVisible,
            onLike = {
                actionsVisible = false
                onLike()
            },
            onAdd = {
                actionsVisible = false
                onAdd()
            },
            onClose = {
                actionsVisible = false
                closePlayer()
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
                    queue
                ) {
                    detectDragGestures(
                        onDragStart = {
                            actionsVisible =
                                false

                            axis =
                                XvoxMiniAxis.NONE

                            rawX = 0f
                            rawY = 0f
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

                            scope.launch {
                                when (axis) {
                                    XvoxMiniAxis.HORIZONTAL -> {
                                        y.snapTo(0f)

                                        x.snapTo(
                                            XvoxMiniPlayerMotion
                                                .horizontalResistance(
                                                    rawX
                                                )
                                        )
                                    }

                                    XvoxMiniAxis.VERTICAL -> {
                                        x.snapTo(0f)

                                        y.snapTo(
                                            XvoxMiniPlayerMotion
                                                .verticalResistance(
                                                    rawY
                                                )
                                        )
                                    }

                                    XvoxMiniAxis.NONE ->
                                        Unit
                                }
                            }
                        },
                        onDragEnd = {
                            val finalAxis =
                                axis

                            val finalX =
                                rawX

                            val finalY =
                                rawY

                            rawX = 0f
                            rawY = 0f

                            axis =
                                XvoxMiniAxis.NONE

                            scope.launch {
                                when (
                                    finalAxis
                                ) {
                                    XvoxMiniAxis.HORIZONTAL -> {
                                        if (
                                            finalX <
                                            -XvoxMiniPlayerMotion
                                                .HorizontalThreshold &&
                                            previewIndex <
                                            queue.lastIndex
                                        ) {
                                            direction = 1
                                            previewIndex++
                                            previewRevision++
                                        } else if (
                                            finalX >
                                            XvoxMiniPlayerMotion
                                                .HorizontalThreshold &&
                                            previewIndex > 0
                                        ) {
                                            direction = -1
                                            previewIndex--
                                            previewRevision++
                                        }

                                        x.animateTo(
                                            0f,
                                            XvoxMiniPlayerMotion
                                                .horizontalReturnSpec
                                        )
                                    }

                                    XvoxMiniAxis.VERTICAL -> {
                                        when {
                                            finalY <=
                                                XvoxMiniPlayerMotion
                                                    .OpenThreshold -> {

                                                y.animateTo(
                                                    -90f,
                                                    XvoxMiniPlayerMotion
                                                        .exitSpec
                                                )

                                                openPlayer()

                                                y.snapTo(0f)
                                            }

                                            finalY >=
                                                XvoxMiniPlayerMotion
                                                    .CloseThreshold -> {

                                                y.animateTo(
                                                    90f,
                                                    XvoxMiniPlayerMotion
                                                        .exitSpec
                                                )

                                                closePlayer()
                                            }

                                            else -> {
                                                y.animateTo(
                                                    0f,
                                                    XvoxMiniPlayerMotion
                                                        .verticalReturnSpec
                                                )
                                            }
                                        }
                                    }

                                    XvoxMiniAxis.NONE ->
                                        Unit
                                }

                                moved = false
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                x.animateTo(
                                    0f,
                                    XvoxMiniPlayerMotion
                                        .horizontalReturnSpec
                                )

                                y.animateTo(
                                    0f,
                                    XvoxMiniPlayerMotion
                                        .verticalReturnSpec
                                )
                            }

                            rawX = 0f
                            rawY = 0f
                            axis =
                                XvoxMiniAxis.NONE
                            moved = false
                        }
                    )
                }
                .pointerInput(
                    currentSongId
                ) {
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
                                    openPlayer()
                                }
                            }
                        }
                    )
                }
        ) {
            XvoxMiniPlayerCard(
                song = song,
                isPlaying =
                    isPlaying,
                position =
                    if (
                        song.id ==
                        currentSongId
                    ) {
                        position
                    } else {
                        0L
                    },
                duration =
                    if (
                        song.id ==
                        currentSongId
                    ) {
                        duration
                    } else {
                        0L
                    },
                transitionDirection =
                    direction,
                togglePlay =
                    togglePlay,
                modifier = Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .graphicsLayer {
                        translationX =
                            x.value

                        translationY =
                            y.value
                    }
            )
        }
    }
}
