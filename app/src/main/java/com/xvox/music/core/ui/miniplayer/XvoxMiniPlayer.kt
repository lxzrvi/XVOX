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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val PreviewCommitDelay =
    320L

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

    val y =
        remember(riseKey) {
            Animatable(
                exitDistance
            )
        }

    var dragX by remember {
        mutableFloatStateOf(0f)
    }

    var dragY by remember {
        mutableFloatStateOf(0f)
    }

    var previewIndex by remember(
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

    var transitionDirection by remember {
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

    var commitJob by remember {
        mutableStateOf<Job?>(null)
    }

    LaunchedEffect(
        riseKey
    ) {
        y.animateTo(
            targetValue = 0f,
            animationSpec =
                XvoxMiniPlayerMotion
                    .riseSpec
        )
    }

    LaunchedEffect(
        currentSongId,
        currentIndex
    ) {
        if (
            commitJob == null &&
            axis ==
            XvoxMiniAxis.NONE &&
            currentIndex in queue.indices
        ) {
            previewIndex =
                currentIndex

            transitionDirection =
                0
        }
    }

    fun cancelPendingCommit() {
        commitJob?.cancel()
        commitJob = null
    }

    fun schedulePreviewCommit() {
        cancelPendingCommit()

        val target =
            previewIndex

        if (
            target !in queue.indices ||
            target == currentIndex
        ) {
            return
        }

        commitJob =
            scope.launch {
                delay(
                    PreviewCommitDelay
                )

                playQueueIndex(
                    target
                )

                commitJob = null
            }
    }

    fun exitFromCurrentPosition(
        stopPlayback: Boolean,
        currentDragY: Float,
        after: () -> Unit
    ) {
        if (exiting) {
            return
        }

        exiting = true
        cancelPendingCommit()

        scope.launch {
            actionsVisible = false
            dragX = 0f

            /*
             * Transfer the current physical drag position
             * directly into the exit Animatable.
             *
             * No return to rest position.
             */
            y.snapTo(
                currentDragY
            )

            dragY = 0f

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

    fun normalExit(
        stopPlayback: Boolean,
        after: () -> Unit
    ) {
        exitFromCurrentPosition(
            stopPlayback =
                stopPlayback,
            currentDragY =
                y.value,
            after =
                after
        )
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
                actionsVisible =
                    false

                onLike()
            },
            onAdd = {
                actionsVisible =
                    false

                onAdd()
            },
            onClose = {
                normalExit(
                    stopPlayback =
                        true,
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
                    queue,
                    currentSongId,
                    exiting
                ) {
                    if (exiting) {
                        return@pointerInput
                    }

                    detectDragGestures(
                        onDragStart = {
                            cancelPendingCommit()

                            actionsVisible =
                                false

                            axis =
                                XvoxMiniAxis.NONE

                            rawX = 0f
                            rawY = 0f

                            dragX = 0f
                            dragY = 0f

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
                                    dragY = 0f

                                    dragX =
                                        XvoxMiniPlayerMotion
                                            .horizontalResistance(
                                                rawX
                                            )
                                }

                                XvoxMiniAxis.VERTICAL -> {
                                    dragX = 0f

                                    /*
                                     * Direct state update.
                                     * No coroutine races.
                                     */
                                    dragY =
                                        XvoxMiniPlayerMotion
                                            .verticalResistance(
                                                rawY
                                            )
                                }

                                XvoxMiniAxis.NONE ->
                                    Unit
                            }
                        },

                        onDragEnd = {
                            val finalAxis =
                                axis

                            val finalX =
                                rawX

                            val finalY =
                                rawY

                            val finalDragY =
                                dragY

                            rawX = 0f
                            rawY = 0f

                            axis =
                                XvoxMiniAxis.NONE

                            when (finalAxis) {
                                XvoxMiniAxis.HORIZONTAL -> {
                                    dragX = 0f
                                    dragY = 0f

                                    when {
                                        finalX <=
                                            -XvoxMiniPlayerMotion
                                                .HorizontalThreshold &&
                                            previewIndex <
                                            queue.lastIndex -> {

                                            transitionDirection = 1
                                            previewIndex++
                                        }

                                        finalX >=
                                            XvoxMiniPlayerMotion
                                                .HorizontalThreshold &&
                                            previewIndex > 0 -> {

                                            transitionDirection = -1
                                            previewIndex--
                                        }

                                        else -> {
                                            transitionDirection = 0
                                        }
                                    }

                                    schedulePreviewCommit()

                                    moved = false
                                }

                                XvoxMiniAxis.VERTICAL -> {
                                    dragX = 0f

                                    when {
                                        finalY <=
                                            XvoxMiniPlayerMotion
                                                .OpenThreshold -> {

                                            exitFromCurrentPosition(
                                                stopPlayback =
                                                    false,
                                                currentDragY =
                                                    finalDragY
                                            ) {
                                                openPlayer()
                                            }
                                        }

                                        finalY >=
                                            XvoxMiniPlayerMotion
                                                .CloseThreshold -> {

                                            exitFromCurrentPosition(
                                                stopPlayback =
                                                    true,
                                                currentDragY =
                                                    finalDragY,
                                                after = {}
                                            )
                                        }

                                        else -> {
                                            /*
                                             * Only a cancelled/short
                                             * vertical drag returns home.
                                             */
                                            dragY = 0f
                                        }
                                    }

                                    moved = false
                                }

                                XvoxMiniAxis.NONE -> {
                                    dragX = 0f
                                    dragY = 0f
                                    moved = false
                                }
                            }
                        },

                        onDragCancel = {
                            rawX = 0f
                            rawY = 0f

                            dragX = 0f
                            dragY = 0f

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
                                cancelPendingCommit()

                                actionsVisible =
                                    !actionsVisible
                            }
                        },

                        onTap = {
                            if (!moved) {
                                cancelPendingCommit()

                                if (
                                    actionsVisible
                                ) {
                                    actionsVisible =
                                        false
                                } else {
                                    normalExit(
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
                song =
                    visualSong,
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
                    transitionDirection,
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
                            y.value +
                                dragY
                    }
            )
        }
    }
}
