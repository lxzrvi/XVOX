package com.xvox.music.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val HoldDelayMillis = 260L
private const val HoldSlopPx = 10f

@Composable
fun XvoxBottomBar(
    selected: XvoxDestination,
    onSelected: (XvoxDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    val density =
        LocalDensity.current

    val scope =
        rememberCoroutineScope()

    val destinations =
        XvoxDestination.entries

    val selectedIndex =
        destinations.indexOf(
            selected
        )

    var position by remember {
        mutableFloatStateOf(
            selectedIndex.toFloat()
        )
    }

    var dragging by remember {
        mutableStateOf(false)
    }

    var holding by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(
        selectedIndex
    ) {
        if (!dragging) {
            position =
                selectedIndex.toFloat()
        }
    }

    val motion =
        rememberXvoxNavigationMotion(
            position = position,
            dragging = dragging,
            holding = holding
        )

    val parentShape =
        RoundedCornerShape(
            XvoxNavigationGeometry
                .barRadius
        )

    val parentBorder =
        colors.cardBorder.copy(
            alpha = 0.62f
        )

    val selectorBorder =
        colors.cardBorder.copy(
            alpha = 0.72f
        )

    val inactive =
        colors.mutedText.copy(
            alpha = 0.76f
        )

    val active =
        colors.primaryText

    Box(
        modifier = modifier
            .width(
                XvoxNavigationGeometry
                    .barWidth
            )
            .height(
                XvoxNavigationGeometry
                    .hostHeight
            )
            .pointerInput(
                selectedIndex
            ) {
                awaitEachGesture {
                    val first =
                        awaitFirstDown()

                    val startX =
                        first.position.x

                    val startIndex =
                        selectedIndex

                    val physicalSlot =
                        size.width /
                            3f

                    val dragSlot =
                        physicalSlot *
                            XvoxNavigationGeometry
                                .DragResistance

                    var total =
                        0f

                    var moved =
                        false

                    var change =
                        first

                    /*
                     * Do not call this "dragging"
                     * until the pointer actually moves.
                     */
                    dragging = false
                    holding = false

                    var holdJob: Job? =
                        scope.launch {
                            delay(
                                HoldDelayMillis
                            )

                            if (!moved) {
                                holding = true
                            }
                        }

                    while (
                        change.pressed
                    ) {
                        val event =
                            awaitPointerEvent()

                        change =
                            event
                                .changes
                                .first()

                        if (
                            change.pressed
                        ) {
                            total =
                                change
                                    .position
                                    .x -
                                    startX

                            if (
                                !moved &&
                                abs(total) >
                                HoldSlopPx
                            ) {
                                moved = true
                                dragging = true

                                /*
                                 * A normal drag must NOT
                                 * trigger hold grow.
                                 */
                                holdJob?.cancel()
                                holdJob = null
                                holding = false
                            }

                            if (moved) {
                                position =
                                    (
                                        startIndex +
                                            total /
                                                dragSlot
                                        )
                                        .coerceIn(
                                            0f,
                                            2f
                                        )

                                change.consume()
                            }
                        }
                    }

                    holdJob?.cancel()
                    holding = false

                    val target =
                        if (!moved) {
                            /*
                             * Tap or stationary hold release:
                             * use touched destination.
                             */
                            (
                                first
                                    .position
                                    .x /
                                    physicalSlot
                                )
                                .toInt()
                                .coerceIn(
                                    0,
                                    2
                                )
                        } else {
                            settleDestination(
                                start =
                                    startIndex,
                                position =
                                    position
                            )
                        }

                    /*
                     * Release always returns selector
                     * to normal dimensions.
                     */
                    dragging = false

                    position =
                        target.toFloat()

                    if (
                        target !=
                        selectedIndex
                    ) {
                        onSelected(
                            destinations[
                                target
                            ]
                        )
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .align(
                    Alignment.TopCenter
                )
                .offset(
                    y =
                        XvoxNavigationGeometry
                            .hostOverflow
                )
                .size(
                    XvoxNavigationGeometry
                        .barWidth,
                    XvoxNavigationGeometry
                        .barHeight
                )
                .graphicsLayer {
                    scaleX =
                        motion.barScale

                    scaleY =
                        motion.barScale

                    shape =
                        parentShape

                    clip = true
                }
                .background(
                    colors.surface.copy(
                        alpha = 0.88f
                    )
                )
                .border(
                    width =
                        XvoxNavigationGeometry
                            .barBorderWidth,
                    color =
                        parentBorder,
                    shape =
                        parentShape
                )
        )

        val selectorWidth =
            XvoxNavigationGeometry
                .selectorRestWidth +
                XvoxNavigationGeometry
                    .selectorGrowWidth *
                motion.grow

        val selectorHeight =
            XvoxNavigationGeometry
                .selectorRestHeight +
                XvoxNavigationGeometry
                    .selectorGrowHeight *
                motion.grow

        val selectorRadius =
            XvoxNavigationGeometry
                .selectorBaseRadius +
                XvoxNavigationGeometry
                    .selectorGrowRadius *
                motion.grow

        val selectorShape =
            RoundedCornerShape(
                selectorRadius
            )

        Box(
            modifier = Modifier
                .align(
                    Alignment.TopStart
                )
                .offset(
                    y =
                        XvoxNavigationGeometry
                            .hostOverflow +
                            XvoxNavigationGeometry
                                .barHeight /
                            2 -
                            selectorHeight /
                            2
                )
                .graphicsLayer {
                    translationX =
                        (
                            XvoxNavigationGeometry
                                .selectorStart +
                                XvoxNavigationGeometry
                                    .selectorTravel *
                                (
                                    motion.position /
                                        2f
                                    ) -
                                XvoxNavigationGeometry
                                    .selectorGrowShift *
                                motion.grow
                            )
                            .toPx()
                }
                .size(
                    selectorWidth,
                    selectorHeight
                )
                .graphicsLayer {
                    shape =
                        selectorShape

                    clip = true
                }
                .background(
                    colors.cardElevated
                        .copy(
                            alpha = 0.42f
                        )
                )
                .border(
                    width =
                        XvoxNavigationGeometry
                            .selectorBorderWidth,
                    color =
                        selectorBorder,
                    shape =
                        selectorShape
                )
        )

        Row(
            modifier = Modifier
                .align(
                    Alignment.TopCenter
                )
                .offset(
                    y =
                        XvoxNavigationGeometry
                            .hostOverflow
                )
                .size(
                    XvoxNavigationGeometry
                        .barWidth,
                    XvoxNavigationGeometry
                        .barHeight
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            destinations
                .forEachIndexed {
                    index,
                    destination ->

                    val proximity =
                        navigationProximity(
                            position =
                                motion.position,
                            index =
                                index
                        )

                    XvoxNavigationItem(
                        destination =
                            destination,
                        proximity =
                            proximity,
                        dragging =
                            dragging ||
                                holding,
                        inactiveColor =
                            inactive,
                        activeColor =
                            active,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }
        }
    }
}

private fun settleDestination(
    start: Int,
    position: Float
): Int {
    val delta =
        position -
            start.toFloat()

    val distance =
        abs(delta)

    if (
        distance <
        XvoxNavigationGeometry
            .SettleThreshold
    ) {
        return start
    }

    val direction =
        if (
            delta > 0f
        ) {
            1
        } else {
            -1
        }

    val steps =
        if (
            distance >=
            XvoxNavigationGeometry
                .FarDestinationThreshold
        ) {
            2
        } else {
            1
        }

    return (
        start +
            direction *
            steps
        )
        .coerceIn(
            0,
            2
        )
}
