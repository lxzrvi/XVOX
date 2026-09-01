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
import androidx.compose.foundation.layout.Spacer
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

private val FluidEasing =
    CubicBezierEasing(
        0.16f,
        1f,
        0.30f,
        1f
    )

private val ContentEasing =
    CubicBezierEasing(
        0.22f,
        1f,
        0.36f,
        1f
    )

private const val CapsuleDuration = 520
private const val ContentDuration = 380

private val StageWidth = 350.dp
private val ParentHeight = 65.dp
private val ItemHeight = 57.dp

private val ParentPadding = 4.dp

private val SlotWidth = 70.dp
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

    val parentWidth by
        animateDpAsState(
            targetValue =
                parentWidthFor(
                    selected
                ),
            animationSpec =
                tween(
                    durationMillis =
                        CapsuleDuration,
                    easing =
                        FluidEasing
                ),
            label =
                "navParentWidth"
        )

    val parentShift by
        animateDpAsState(
            targetValue =
                parentShiftFor(
                    selected
                ),
            animationSpec =
                tween(
                    durationMillis =
                        CapsuleDuration,
                    easing =
                        FluidEasing
                ),
            label =
                "navParentShift"
        )

    val activeWidth by
        animateDpAsState(
            targetValue =
                activeWidthFor(
                    selected
                ),
            animationSpec =
                tween(
                    durationMillis =
                        CapsuleDuration,
                    easing =
                        FluidEasing
                ),
            label =
                "navActiveWidth"
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
            label =
                "navActiveCenter"
        )

    Box(
        modifier = modifier
            .offset(y = 2.dp)
            .width(StageWidth)
            .height(ParentHeight),
        contentAlignment =
            Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x =
                        parentShift
                )
                .width(
                    parentWidth
                )
                .height(
                    ParentHeight
                )
                .clip(
                    CircleShape
                )
                .background(
                    colors.surface
                )
                .border(
                    width = 0.5.dp,
                    color =
                        colors.cardBorder,
                    shape =
                        CircleShape
                )
                .pointerInput(
                    selected
                ) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragDistance =
                                0f
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
                                abs(
                                    dragDistance
                                ) >=
                                35.dp.toPx()
                            ) {
                                val target =
                                    if (
                                        dragDistance >
                                        0f
                                    ) {
                                        selectedIndex +
                                            1
                                    } else {
                                        selectedIndex -
                                            1
                                    }

                                destinations
                                    .getOrNull(
                                        target
                                    )
                                    ?.let(
                                        onSelected
                                    )
                            }

                            dragDistance =
                                0f
                        },
                        onDragCancel = {
                            dragDistance =
                                0f
                        }
                    )
                }
        )

        Box(
            modifier = Modifier
                .offset(
                    x =
                        parentShift
                )
                .width(
                    parentWidth
                )
                .height(
                    ParentHeight
                )
                .padding(
                    ParentPadding
                )
                .clip(
                    CircleShape
                )
        ) {
            NavigationTrack(
                selected =
                    selected,
                activeCenter =
                    activeCenter,
                activeWidth =
                    activeWidth,
                onSelected =
                    onSelected
            )
        }
    }
}

@Composable
private fun NavigationTrack(
    selected: XvoxDestination,
    activeCenter: Dp,
    activeWidth: Dp,
    onSelected: (XvoxDestination) -> Unit
) {
    val colors =
        XvoxTheme.colors

    val destinations =
        XvoxDestination.entries

    Box(
        modifier = Modifier
            .width(
                trackWidth()
            )
            .height(
                ItemHeight
            )
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (
                            activeCenter -
                                activeWidth /
                                2
                            )
                            .roundToPx(),
                        y = 0
                    )
                }
                .width(
                    activeWidth
                )
                .height(
                    ItemHeight
                )
                .clip(
                    CircleShape
                )
                .background(
                    colors.cardElevated
                )
        )

        Row(
            modifier = Modifier
                .width(
                    trackWidth()
                )
                .height(
                    ItemHeight
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            destinations.forEachIndexed {
                index,
                destination ->

                if (index > 0) {
                    Spacer(
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
                        destination ==
                            selected,
                    onClick = {
                        if (
                            destination !=
                            selected
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
private fun NavigationItem(
    destination: XvoxDestination,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors =
        XvoxTheme.colors

    val interaction =
        remember {
            MutableInteractionSource()
        }

    val pressed by
        interaction
            .collectIsPressedAsState()

    val progress by
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
                        ContentEasing
                ),
            label =
                "navItemContent"
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
                        ContentEasing
                ),
            label =
                "navItemPress"
        )

    val iconColor =
        lerpColor(
            start =
                colors.mutedText,
            end =
                colors.primaryText,
            amount =
                progress
        )

    Box(
        modifier = Modifier
            .width(
                SlotWidth
            )
            .height(
                ItemHeight
            )
            .clickable(
                interactionSource =
                    interaction,
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

                    translationX =
                        -progress *
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
                progress,
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
    val alpha =
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
        modifier =
            Modifier.graphicsLayer {
                this.alpha =
                    alpha

                translationX =
                    labelOffset(
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

private fun parentWidthFor(
    destination: XvoxDestination
): Dp {
    return when (destination) {
        XvoxDestination.HOME ->
            270.dp

        XvoxDestination.SEARCH ->
            282.dp

        XvoxDestination.SETTINGS ->
            298.dp
    }
}

private fun parentShiftFor(
    destination: XvoxDestination
): Dp {
    return when (destination) {
        XvoxDestination.HOME ->
            (-10).dp

        XvoxDestination.SEARCH ->
            0.dp

        XvoxDestination.SETTINGS ->
            10.dp
    }
}

private fun activeWidthFor(
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

private fun trackWidth(): Dp {
    return SlotWidth * 3 +
        SlotGap * 2
}

private fun slotCenter(
    index: Int
): Dp {
    return SlotWidth / 2 +
        (
            SlotWidth +
                SlotGap
            ) *
        index
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

private fun labelOffset(
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
