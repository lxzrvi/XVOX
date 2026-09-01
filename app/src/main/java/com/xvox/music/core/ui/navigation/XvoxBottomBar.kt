package com.xvox.music.core.ui.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlin.math.abs
import kotlin.math.roundToInt

private val FluidEasing =
    CubicBezierEasing(
        0.16f,
        1f,
        0.30f,
        1f
    )

private val SoftEasing =
    CubicBezierEasing(
        0.22f,
        1f,
        0.36f,
        1f
    )

private const val CapsuleDuration = 520
private const val ContentDuration = 380

private val ParentWidth = 330.dp
private val ParentHeight = 65.dp

private val OuterPadding = 4.dp
private val ItemHeight = 57.dp

private val IconSlotWidth = 70.dp
private val SlotGap = 8.dp

@Composable
fun XvoxBottomBar(
    selected: XvoxDestination,
    onSelected: (XvoxDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    val destinations =
        XvoxDestination.entries

    val selectedIndex =
        destinations.indexOf(
            selected
        )

    var dragDistance by remember {
        mutableFloatStateOf(0f)
    }

    val activeWidth by
        animateDpAsState(
            targetValue =
                selectedWidth(
                    selected
                ),
            animationSpec =
                tween(
                    durationMillis =
                        CapsuleDuration,
                    easing =
                        FluidEasing
                ),
            label = "activeWidth"
        )

    val activeCenter by
        animateDpAsState(
            targetValue =
                slotCenter(
                    selectedIndex
                ),
            animationSpec =
                tween(
                    durationMillis =
                        CapsuleDuration,
                    easing =
                        FluidEasing
                ),
            label = "activeCenter"
        )

    Box(
        modifier = modifier
            .offset(y = 2.dp)
            .width(ParentWidth)
            .height(ParentHeight)
            .clip(CircleShape)
            .background(
                colors.surface
            )
            .border(
                width = 0.5.dp,
                color =
                    colors.cardBorder,
                shape = CircleShape
            )
            .padding(
                OuterPadding
            )
            .pointerInput(selected) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragDistance = 0f
                    },
                    onHorizontalDrag = {
                        change,
                        amount ->

                        change.consume()

                        dragDistance +=
                            amount
                    },
                    onDragEnd = {
                        if (
                            abs(dragDistance) >=
                            35.dp.toPx()
                        ) {
                            val target =
                                if (
                                    dragDistance >
                                    0f
                                ) {
                                    selectedIndex + 1
                                } else {
                                    selectedIndex - 1
                                }

                            destinations
                                .getOrNull(
                                    target
                                )
                                ?.let(
                                    onSelected
                                )
                        }

                        dragDistance = 0f
                    },
                    onDragCancel = {
                        dragDistance = 0f
                    }
                )
            }
    ) {
        ActiveCapsule(
            center =
                activeCenter,
            width =
                activeWidth,
            color =
                colors.cardElevated
        )

        Row(
            modifier =
                Modifier.height(
                    ItemHeight
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            destinations.forEachIndexed {
                index,
                destination ->

                if (index > 0) {
                    androidx.compose.foundation.layout.Spacer(
                        modifier =
                            Modifier.width(
                                SlotGap
                            )
                    )
                }

                NavigationItem(
                    destination =
                        destination,
                    active =
                        selected ==
                            destination,
                    onClick = {
                        if (
                            selected !=
                            destination
                        ) {
                            onSelected(
                                destination
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ActiveCapsule(
    center: Dp,
    width: Dp,
    color: Color
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (
                        center -
                            width / 2
                        )
                        .roundToPx(),
                    y = 0
                )
            }
            .width(width)
            .height(ItemHeight)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun NavigationItem(
    destination: XvoxDestination,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors =
        XvoxTheme.colors

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val pressed by
        interactionSource
            .collectIsPressedAsState()

    val activeProgress by
        animateFloatAsState(
            targetValue =
                if (active) {
                    1f
                } else {
                    0f
                },
            animationSpec =
                tween(
                    durationMillis =
                        ContentDuration,
                    easing =
                        SoftEasing
                ),
            label =
                "navContent"
        )

    val pressScale by
        animateFloatAsState(
            targetValue =
                if (pressed) {
                    0.94f
                } else {
                    1f
                },
            animationSpec =
                tween(
                    durationMillis =
                        if (pressed) {
                            70
                        } else {
                            180
                        },
                    easing =
                        SoftEasing
                ),
            label =
                "navPress"
        )

    val iconColor =
        lerpColor(
            start =
                colors.mutedText,
            end =
                colors.primaryText,
            amount =
                activeProgress
        )

    /*
     * Every navigation item always occupies
     * exactly 70dp.
     *
     * Nothing expands in layout space.
     * Therefore selecting another destination
     * cannot push neighbouring items.
     */
    Box(
        modifier = Modifier
            .width(
                IconSlotWidth
            )
            .height(
                ItemHeight
            )
            .clickable(
                interactionSource =
                    interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment =
            Alignment.Center
    ) {
        XvoxNavigationIcon(
            destination =
                destination,
            color =
                iconColor,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX =
                        pressScale

                    scaleY =
                        pressScale

                    /*
                     * The icon shifts slightly
                     * left only when its label
                     * becomes visible.
                     */
                    translationX =
                        -activeProgress *
                            iconShift(
                                destination
                            )
                            .toPx()
                }
        )

        NavigationLabel(
            destination =
                destination,
            progress =
                activeProgress,
            color =
                iconColor
        )
    }
}

@Composable
private fun NavigationLabel(
    destination: XvoxDestination,
    progress: Float,
    color: Color
) {
    val fade =
        smoothStep(
            (
                (progress - 0.08f) /
                    0.72f
                )
                .coerceIn(
                    0f,
                    1f
                )
        )

    val motion =
        smoothStep(
            progress.coerceIn(
                0f,
                1f
            )
        )

    Text(
        text =
            destination.label,
        color =
            color,
        fontSize = 14.sp,
        lineHeight = 17.sp,
        fontWeight =
            FontWeight.SemiBold,
        maxLines = 1,
        overflow =
            TextOverflow.Visible,
        modifier = Modifier
            .graphicsLayer {
                alpha =
                    fade

                translationX =
                    labelX(
                        destination
                    )
                        .toPx() +
                        (
                            1f -
                                motion
                            ) *
                        7.dp.toPx()

                val scale =
                    0.96f +
                        0.04f *
                        motion

                scaleX =
                    scale

                scaleY =
                    scale
            }
    )
}

private fun selectedWidth(
    destination: XvoxDestination
): Dp {
    return when (destination) {
        XvoxDestination.HOME ->
            124.dp

        XvoxDestination.SEARCH ->
            134.dp

        XvoxDestination.SETTINGS ->
            150.dp
    }
}

private fun slotCenter(
    index: Int
): Dp {
    val slot =
        IconSlotWidth +
            SlotGap

    return IconSlotWidth / 2 +
        slot * index
}

private fun iconShift(
    destination: XvoxDestination
): Dp {
    return when (destination) {
        XvoxDestination.HOME ->
            27.dp

        XvoxDestination.SEARCH ->
            31.dp

        XvoxDestination.SETTINGS ->
            38.dp
    }
}

private fun labelX(
    destination: XvoxDestination
): Dp {
    return when (destination) {
        XvoxDestination.HOME ->
            25.dp

        XvoxDestination.SEARCH ->
            28.dp

        XvoxDestination.SETTINGS ->
            33.dp
    }
}

private fun lerpColor(
    start: Color,
    end: Color,
    amount: Float
): Color {
    val value =
        amount.coerceIn(
            0f,
            1f
        )

    return Color(
        red =
            start.red +
                (
                    end.red -
                        start.red
                    ) *
                value,
        green =
            start.green +
                (
                    end.green -
                        start.green
                    ) *
                value,
        blue =
            start.blue +
                (
                    end.blue -
                        start.blue
                    ) *
                value,
        alpha =
            start.alpha +
                (
                    end.alpha -
                        start.alpha
                    ) *
                value
    )
}

private fun smoothStep(
    value: Float
): Float {
    return value *
        value *
        (
            3f -
                2f *
                value
            )
}
