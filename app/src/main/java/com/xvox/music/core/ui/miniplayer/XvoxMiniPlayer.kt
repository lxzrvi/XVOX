package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import kotlinx.coroutines.delay
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

    val x =
        remember {
            Animatable(0f)
        }

    val y =
        remember {
            Animatable(0f)
        }

    val exitDistance =
        with(density) {
            190.dp.toPx()
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

    var revision by remember {
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
        y.snapTo(
            exitDistance
        )

        y.animateTo(
            targetValue = 0f,
            animationSpec =
                XvoxMiniPlayerMotion
                    .riseSpec
        )
    }

    LaunchedEffect(revision) {
        if (revision <= 0) {
            return@LaunchedEffect
        }

        val expected =
            revision

        delay(
            XvoxMiniPlayerMotion
                .PreviewDelay
        )

        if (
            expected ==
            revision
        ) {
            playQueueIndex(
                previewIndex
            )

            revision = 0
        }
    }

    fun exit(
        stop: Boolean,
        after: () -> Unit
    ) {
        if (exiting) {
            return
        }

        exiting = true

        scope.launch {
            actionsVisible = false

            y.animateTo(
                targetValue =
                    exitDistance,
                animationSpec =
                    XvoxMiniPlayerMotion
                        .exitSpec
            )

            if (stop) {
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
                exit(
                    stop = true,
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

                            val finalX = rawX
                            val finalY = rawY

                            rawX = 0f
                            rawY = 0f
                            axis =
                                XvoxMiniAxis.NONE

                            scope.launch {
                                when (
                                    finalAxis
                                ) {
                                    XvoxMiniAxis.HORIZONTAL -> {
                                        when {
                                            finalX <
                                                -XvoxMiniPlayerMotion
                                                    .HorizontalThreshold &&
                                                previewIndex <
                                                queue.lastIndex -> {

                                                direction = 1
                                                previewIndex++
                                                revision++
                                            }

                                            finalX >
                                                XvoxMiniPlayerMotion
                                                    .HorizontalThreshold &&
                                                previewIndex >
                                                0 -> {

                                                direction = -1
                                                previewIndex--
                                                revision++
                                            }
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

                                                exit(
                                                    stop = false
                                                ) {
                                                    openPlayer()
                                                }
                                            }

                                            finalY >=
                                                XvoxMiniPlayerMotion
                                                    .CloseThreshold -> {

                                                exit(
                                                    stop = true,
                                                    after = {}
                                                )
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
                                        stop = false
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
                            x.value

                        translationY =
                            y.value
                    }
            )
        }
    }
}
