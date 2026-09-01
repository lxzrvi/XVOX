package com.xvox.music.core.ui.navigation

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlin.math.abs

private val NavFluidEasing =
    CubicBezierEasing(
        0.16f,
        1f,
        0.30f,
        1f
    )

private val NavLabelEasing =
    CubicBezierEasing(
        0.20f,
        0.90f,
        0.25f,
        1f
    )

private const val PillDuration = 580
private const val LabelDuration = 440

private val ItemHeight = 57.dp
private val InactiveWidth = 70.dp

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

    var dragDistance by remember {
        mutableFloatStateOf(0f)
    }

    val parentShape =
        CircleShape

    Row(
        modifier = modifier
            .clip(parentShape)
            .background(
                colors.surface
            )
            .border(
                width = 0.5.dp,
                color = colors.cardBorder,
                shape = parentShape
            )
            .padding(4.dp)
            .pointerInput(selected) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragDistance = 0f
                    },
                    onHorizontalDrag = {
                        change,
                        amount ->

                        change.consume()

                        dragDistance += amount
                    },
                    onDragEnd = {
                        if (
                            abs(dragDistance) >=
                            35.dp.toPx()
                        ) {
                            val currentIndex =
                                destinations.indexOf(
                                    selected
                                )

                            val targetIndex =
                                if (
                                    dragDistance > 0f
                                ) {
                                    currentIndex + 1
                                } else {
                                    currentIndex - 1
                                }

                            destinations
                                .getOrNull(
                                    targetIndex
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
            },
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        destinations.forEachIndexed {
            index,
            destination ->

            if (index > 0) {
                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )
            }

            XvoxNavigationItem(
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

@Composable
private fun XvoxNavigationItem(
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

    val expandedWidth =
        when (destination) {
            XvoxDestination.HOME ->
                124.dp

            XvoxDestination.SEARCH ->
                134.dp

            XvoxDestination.SETTINGS ->
                150.dp
        }

    val width by
        animateDpAsState(
            targetValue =
                if (active) {
                    expandedWidth
                } else {
                    InactiveWidth
                },
            animationSpec =
                tween(
                    durationMillis =
                        PillDuration,
                    easing =
                        NavFluidEasing
                ),
            label = "navWidth"
        )

    val background by
        animateColorAsState(
            targetValue =
                if (active) {
                    colors.cardElevated
                } else {
                    colors.accentSoft
                },
            animationSpec =
                tween(
                    durationMillis = 300,
                    easing =
                        NavFluidEasing
                ),
            label = "navBackground"
        )

    val foreground by
        animateColorAsState(
            targetValue =
                if (active) {
                    colors.primaryText
                } else {
                    colors.secondaryText
                },
            animationSpec =
                tween(
                    durationMillis = 260,
                    easing =
                        NavFluidEasing
                ),
            label = "navForeground"
        )

    val pressedAlpha by
        animateFloatAsState(
            targetValue =
                if (pressed) {
                    0.84f
                } else {
                    1f
                },
            animationSpec =
                tween(
                    durationMillis =
                        if (pressed) {
                            70
                        } else {
                            160
                        },
                    easing =
                        NavFluidEasing
                ),
            label = "navPressAlpha"
        )

    val pressedScale by
        animateFloatAsState(
            targetValue =
                if (pressed) {
                    0.982f
                } else {
                    1f
                },
            animationSpec =
                tween(
                    durationMillis =
                        if (pressed) {
                            75
                        } else {
                            190
                        },
                    easing =
                        NavFluidEasing
                ),
            label = "navPressScale"
        )

    Box(
        modifier = Modifier
            .width(width)
            .height(ItemHeight)
            .graphicsLayer {
                alpha =
                    pressedAlpha

                scaleX =
                    pressedScale

                scaleY =
                    pressedScale
            }
            .clip(
                RoundedCornerShape(
                    28.5.dp
                )
            )
            .background(
                background
            )
            .clickable(
                interactionSource =
                    interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment =
            Alignment.CenterStart
    ) {
        NavigationItemContent(
            destination =
                destination,
            active =
                active,
            color =
                foreground
        )
    }
}

@Composable
private fun NavigationItemContent(
    destination: XvoxDestination,
    active: Boolean,
    color: Color
) {
    Row(
        modifier = Modifier
            .padding(
                start = 24.dp,
                end = 20.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        XvoxNavigationIcon(
            destination =
                destination,
            color =
                color,
            modifier =
                Modifier.size(22.dp)
        )

        XvoxNavigationLabel(
            destination =
                destination,
            visible =
                active,
            color =
                color
        )
    }
}

@Composable
private fun XvoxNavigationLabel(
    destination: XvoxDestination,
    visible: Boolean,
    color: Color
) {
    val expandedWidth =
        when (destination) {
            XvoxDestination.HOME ->
                44.dp

            XvoxDestination.SEARCH ->
                53.dp

            XvoxDestination.SETTINGS ->
                66.dp
        }

    val width by
        animateDpAsState(
            targetValue =
                if (visible) {
                    expandedWidth
                } else {
                    0.dp
                },
            animationSpec =
                tween(
                    durationMillis =
                        LabelDuration,
                    delayMillis =
                        if (visible) {
                            35
                        } else {
                            0
                        },
                    easing =
                        NavLabelEasing
                ),
            label = "navLabelWidth"
        )

    val spacing by
        animateDpAsState(
            targetValue =
                if (visible) {
                    12.dp
                } else {
                    0.dp
                },
            animationSpec =
                tween(
                    durationMillis =
                        LabelDuration,
                    delayMillis =
                        if (visible) {
                            25
                        } else {
                            0
                        },
                    easing =
                        NavLabelEasing
                ),
            label = "navLabelGap"
        )

    val alpha by
        animateFloatAsState(
            targetValue =
                if (visible) {
                    1f
                } else {
                    0f
                },
            animationSpec =
                tween(
                    durationMillis =
                        if (visible) {
                            270
                        } else {
                            150
                        },
                    delayMillis =
                        if (visible) {
                            80
                        } else {
                            0
                        },
                    easing =
                        NavFluidEasing
                ),
            label = "navLabelAlpha"
        )

    Spacer(
        modifier =
            Modifier.width(
                spacing
            )
    )

    Box(
        modifier =
            Modifier.width(
                width
            ),
        contentAlignment =
            Alignment.CenterStart
    ) {
        Text(
            text =
                destination.label,
            color =
                color.copy(
                    alpha = alpha
                ),
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight =
                FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
