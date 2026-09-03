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

    /*
     * ========================================================
     * VERTICAL ENTRANCE / EXIT
     * ========================================================
     */

    val y =
        remember(riseKey) {
            Animatable(
                exitDistance
            )
        }

    /*
     * Horizontal drag is intentionally simple Float state.
     *
     * No Animatable here.
     * No coroutine-per-horizontal-frame.
     *
     * This prevents competing horizontal jobs from leaving
     * the MiniPlayer stuck at one side.
     */
    var dragX by remember {
        mutableFloatStateOf(0f)
    }

    /*
     * ========================================================
     * VISUAL PREVIEW
     * ========================================================
     *
     * previewIndex can move independently from actual playback.
     *
     * Example:
     *
     * actual playing = 1
     *
     * swipe -> preview 2
     * swipe -> preview 3
     * swipe -> preview 4
     *
     * actual audio remains 1.
     *
     * Stop swiping:
     * final preview commits once.
     */

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

    /*
     * +1 = next song
     * -1 = previous song
     *
     * XvoxMiniPlayerCard uses this for metadata animation.
     */
    var transitionDirection by remember {
        mutableIntStateOf(0)
    }

    /*
     * ========================================================
     * GESTURE STATE
     * ========================================================
     */

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

    /*
     * Delayed commit allows multiple separate swipes while
     * keeping the currently-playing audio untouched.
     */
    var commitJob by remember {
        mutableStateOf<Job?>(null)
    }

    /*
     * ========================================================
     * ENTRANCE
     * ========================================================
     */

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

    /*
     * ========================================================
     * EXTERNAL PLAYBACK SYNCHRONIZATION
     * ========================================================
     *
     * Once actual playback changes, synchronize the visual
     * preview only if there isn't an unfinished preview
     * sequence.
     */

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

    /*
     * ========================================================
     * PREVIEW HELPERS
     * ========================================================
     */

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
            target ==
            currentIndex
        ) {
            return
        }

        commitJob =
            scope.launch {
                delay(
                    PreviewCommitDelay
                )

                /*
                 * Playback is touched exactly once here.
                 *
                 * Until this point the original song continues
                 * playing/paused independently of the preview.
                 */
                playQueueIndex(
                    target
                )

                commitJob =
                    null
            }
    }

    /*
     * ========================================================
     * EXIT
     * ========================================================
     */

    fun exit(
        stopPlayback: Boolean,
        after: () -> Unit
    ) {
        if (exiting) {
            return
        }

        exiting = true

        cancelPendingCommit()

        scope.launch {
            actionsVisible =
                false

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

    /*
     * ========================================================
     * CURRENT VISUAL SONG
     * ========================================================
     */

    val visualSong =
        queue.getOrNull(
            previewIndex
        ) ?: return

    /*
     * ========================================================
     * ROOT
     * ========================================================
     */

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp),
        contentAlignment =
            Alignment.BottomCenter
    ) {
        /*
         * ====================================================
         * LONG-HOLD ACTIONS
         * ====================================================
         */

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
                exit(
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

        /*
         * ====================================================
         * GESTURE HOST
         * ====================================================
         *
         * Existing 78dp touch host retained.
         * Visible card remains 60dp.
         */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .align(
                    Alignment.BottomCenter
                )

                /*
                 * =============================================
                 * DRAG
                 * =============================================
                 */
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
                            /*
                             * Another swipe began before final
                             * preview commit.
                             *
                             * Keep previewIndex exactly where it
                             * is and keep actual playback untouched.
                             */
                            cancelPendingCommit()

                            actionsVisible =
                                false

                            axis =
                                XvoxMiniAxis.NONE

                            rawX = 0f
                            rawY = 0f

                            dragX = 0f

                            moved =
                                false
                        },

                        onDrag = {
                            change,
                            dragAmount ->

                            change.consume()

                            rawX +=
                                dragAmount.x

                            rawY +=
                                dragAmount.y

                            /*
                             * Lock gesture axis only after
                             * meaningful movement.
                             */
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
                                moved =
                                    true

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
                                /*
                                 * =====================================
                                 * HORIZONTAL PREVIEW
                                 * =====================================
                                 *
                                 * IMPORTANT:
                                 *
                                 * previewIndex does NOT change here.
                                 *
                                 * Drag distance can be:
                                 * 50px
                                 * 500px
                                 * 5000px
                                 *
                                 * A single finger-down/finger-up gesture
                                 * can still change only ONE song.
                                 */
                                XvoxMiniAxis
                                    .HORIZONTAL -> {

                                    dragX =
                                        XvoxMiniPlayerMotion
                                            .horizontalResistance(
                                                rawX
                                            )
                                }

                                /*
                                 * =====================================
                                 * VERTICAL OPEN / CLOSE
                                 * =====================================
                                 */
                                XvoxMiniAxis
                                    .VERTICAL -> {

                                    dragX =
                                        0f

                                    /*
                                     * Vertical behavior still uses the
                                     * existing entrance/exit Animatable.
                                     */
                                    scope.launch {
                                        y.snapTo(
                                            XvoxMiniPlayerMotion
                                                .verticalResistance(
                                                    rawY
                                                )
                                        )
                                    }
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

                            /*
                             * Reset physical gesture state first.
                             */
                            rawX = 0f
                            rawY = 0f

                            axis =
                                XvoxMiniAxis.NONE

                            when (finalAxis) {
                                /*
                                 * =====================================
                                 * ONE RELEASE = ONE SONG
                                 * =====================================
                                 */
                                XvoxMiniAxis
                                    .HORIZONTAL -> {

                                    dragX =
                                        0f

                                    when {
                                        /*
                                         * LEFT -> NEXT
                                         */
                                        finalX <=
                                            -XvoxMiniPlayerMotion
                                                .HorizontalThreshold &&
                                            previewIndex <
                                            queue.lastIndex -> {

                                            transitionDirection =
                                                1

                                            previewIndex++
                                        }

                                        /*
                                         * RIGHT -> PREVIOUS
                                         */
                                        finalX >=
                                            XvoxMiniPlayerMotion
                                                .HorizontalThreshold &&
                                            previewIndex >
                                            0 -> {

                                            transitionDirection =
                                                -1

                                            previewIndex--
                                        }

                                        /*
                                         * Threshold not reached.
                                         */
                                        else -> {
                                            transitionDirection =
                                                0
                                        }
                                    }

                                    /*
                                     * Do not immediately change audio.
                                     *
                                     * Another swipe inside 320ms cancels
                                     * this commit and continues previewing.
                                     */
                                    schedulePreviewCommit()

                                    moved =
                                        false
                                }

                                /*
                                 * =====================================
                                 * VERTICAL RELEASE
                                 * =====================================
                                 */
                                XvoxMiniAxis
                                    .VERTICAL -> {

                                    dragX =
                                        0f

                                    when {
                                        /*
                                         * Swipe UP:
                                         * hide MiniPlayer UI,
                                         * playback continues.
                                         */
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

                                        /*
                                         * Swipe DOWN:
                                         * completely stop playback.
                                         */
                                        finalY >=
                                            XvoxMiniPlayerMotion
                                                .CloseThreshold -> {

                                            exit(
                                                stopPlayback =
                                                    true,
                                                after = {}
                                            )
                                        }

                                        /*
                                         * Didn't cross threshold:
                                         * return to original position.
                                         */
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

                                    moved =
                                        false
                                }

                                XvoxMiniAxis.NONE -> {
                                    dragX =
                                        0f

                                    moved =
                                        false
                                }
                            }
                        },

                        onDragCancel = {
                            rawX =
                                0f

                            rawY =
                                0f

                            dragX =
                                0f

                            axis =
                                XvoxMiniAxis.NONE

                            moved =
                                false

                            /*
                             * Gesture cancellation does NOT throw away
                             * an already accumulated preview sequence.
                             * It simply restores physical card position.
                             */

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
                    )
                }

                /*
                 * =============================================
                 * TAP / LONG HOLD
                 * =============================================
                 */
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
                                /*
                                 * Long-hold means the user is no longer
                                 * continuing a swipe preview sequence.
                                 */
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
                                    /*
                                     * Existing future Now Playing hook.
                                     */
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
            /*
             * =================================================
             * VISIBLE CARD
             * =================================================
             */

            XvoxMiniPlayerCard(
                song =
                    visualSong,

                isPlaying =
                    isPlaying,

                /*
                 * Only actual currently-playing song gets
                 * playback progress.
                 *
                 * Preview covers do not pretend to have the
                 * original song's progress.
                 */
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

                /*
                 * Drives:
                 *
                 * NEXT:
                 * new metadata bottom -> center
                 * old metadata center -> top
                 *
                 * PREVIOUS:
                 * exact reverse
                 */
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
                            y.value
                    }
            )
        }
    }
}
