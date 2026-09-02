package com.xvox.music.core.ui.navigation

import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.xvox.music.core.design.theme.XvoxTheme
import kotlin.math.abs
import kotlin.math.floor

@Composable
fun XvoxBottomBar(
    selected: XvoxDestination,
    onSelected: (XvoxDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    val view =
        LocalView.current

    val destinations =
        XvoxDestination.entries

    val selectedIndex =
        destinations
            .indexOf(selected)

    var position by remember {
        mutableFloatStateOf(
            selectedIndex.toFloat()
        )
    }

    var dragging by remember {
        mutableStateOf(false)
    }

    var velocity by remember {
        mutableFloatStateOf(0f)
    }

    var lastHapticSlot by remember {
        mutableIntStateOf(
            selectedIndex
        )
    }

    LaunchedEffect(
        selectedIndex
    ) {
        if (!dragging) {
            position =
                selectedIndex.toFloat()

            lastHapticSlot =
                selectedIndex
        }
    }

    val motion =
        rememberXvoxNavigationMotion(
            position = position,
            dragging = dragging
        )

    val parentBackground =
        colors.surface.copy(
            alpha = 0.97f
        )

    val parentBorder =
        colors.cardBorder.copy(
            alpha = 0.70f
        )

    val selectorBackground =
        colors.cardElevated.copy(
            alpha = 0.58f
        )

    val selectorBorder =
        colors.cardBorder.copy(
            alpha = 0.82f
        )

    val inactive =
        colors.mutedText.copy(
            alpha = 0.78f
        )

    val active =
        colors.primaryText

    Box(
        modifier = modifier
            .size(
                XvoxNavigationGeometry
                    .barWidth,
                XvoxNavigationGeometry
                    .barHeight
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
                        size.width / 3f

                    val resistedSlot =
                        physicalSlot *
                            XvoxNavigationGeometry
                                .DragResistance

                    val touchSlop =
                        ViewConfiguration
                            .get(view.context)
                            .scaledTouchSlop *
                            XvoxNavigationGeometry
                                .TouchSlopMultiplier

                    var lastX =
                        startX

                    var total =
                        0f

                    var gestureDragging =
                        false

                    var change =
                        first

                    velocity = 0f

                    while (
                        change.pressed
                    ) {
                        val event =
                            awaitPointerEvent()

                        change =
                            event.changes.first()

                        if (
                            change.pressed
                        ) {
                            val currentX =
                                change.position.x

                            val dx =
                                currentX -
                                    lastX

                            total =
                                currentX -
                                    startX

                            lastX =
                                currentX

                            if (
                                !gestureDragging &&
                                abs(total) >
                                touchSlop
                            ) {
                                gestureDragging =
                                    true

                                dragging = true

                                view.performHapticFeedback(
                                    HapticFeedbackConstants
                                        .TEXT_HANDLE_MOVE
                                )
                            }

                            if (
                                gestureDragging
                            ) {
                                velocity =
                                    velocity *
                                        0.72f +
                                        dx *
                                        0.28f

                                val rawPosition =
                                    startIndex +
                                        total /
                                        resistedSlot

                                position =
                                    rawPosition
                                        .coerceIn(
                                            0f,
                                            2f
                                        )

                                val nearestSlot =
                                    (
                                        position +
                                            0.5f
                                        )
                                        .toInt()
                                        .coerceIn(
                                            0,
                                            2
                                        )

                                if (
                                    nearestSlot !=
                                    lastHapticSlot
                                ) {
                                    lastHapticSlot =
                                        nearestSlot

                                    view.performHapticFeedback(
                                        HapticFeedbackConstants
                                            .CLOCK_TICK
                                    )
                                }

                                change.consume()
                            }
                        }
                    }

                    val target =
                        if (!gestureDragging) {
                            /*
                             * Tap chooses whichever
                             * third was actually hit.
                             */
                            floor(
                                first.position.x /
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

                    velocity = 0f
                    dragging = false
                    position =
                        target.toFloat()

                    if (
                        target !=
                        selectedIndex
                    ) {
                        view.performHapticFeedback(
                            HapticFeedbackConstants
                                .CONFIRM
                        )

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
                    Alignment.Center
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
                        RoundedCornerShape(
                            XvoxNavigationGeometry
                                .barRadius
                        )

                    clip = true
                }
                .background(
                    parentBackground
                )
                .border(
                    width =
                        XvoxNavigationGeometry
                            .barBorderWidth,
                    color =
                        parentBorder,
                    shape =
                        RoundedCornerShape(
                            XvoxNavigationGeometry
                                .barRadius
                        )
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

        val stretch =
            if (dragging) {
                (
                    abs(velocity) /
                        24f
                    )
                    .coerceIn(
                        0f,
                        1f
                    )
            } else {
                0f
            }

        Box(
            modifier = Modifier
                .align(
                    Alignment.CenterStart
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

                    val skew =
                        (
                            velocity *
                                0.24f
                            )
                            .coerceIn(
                                -5f,
                                5f
                            )

                    scaleX =
                        1f +
                            stretch *
                                0.14f

                    scaleY =
                        1f -
                            stretch *
                                0.06f

                    rotationZ =
                        (
                            velocity *
                                0.07f
                            )
                            .coerceIn(
                                -1.3f,
                                1.3f
                            )

                    cameraDistance =
                        16f *
                            density

                    rotationY =
                        skew *
                            0.16f
                }
                .size(
                    selectorWidth,
                    selectorHeight
                )
                .graphicsLayer {
                    shape =
                        RoundedCornerShape(
                            selectorRadius
                        )

                    clip = true
                }
                .background(
                    selectorBackground
                )
                .border(
                    width =
                        XvoxNavigationGeometry
                            .selectorBorderWidth,
                    color =
                        selectorBorder,
                    shape =
                        RoundedCornerShape(
                            selectorRadius
                        )
                )
        )

        Row(
            modifier = Modifier
                .align(
                    Alignment.Center
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
            destinations.forEachIndexed {
                index,
                destination ->

                XvoxNavigationItem(
                    destination =
                        destination,
                    proximity =
                        navigationProximity(
                            position =
                                motion.position,
                            index =
                                index
                        ),
                    dragging =
                        dragging,
                    inactiveColor =
                        inactive,
                    activeColor =
                        active,
                    modifier =
                        Modifier.weight(1f)
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

    if (
        abs(delta) <
        XvoxNavigationGeometry
            .SettleThreshold
    ) {
        return start
    }

    val direction =
        if (delta > 0f) {
            1
        } else {
            -1
        }

    val distance =
        abs(delta)

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
