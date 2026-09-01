package com.xvox.music.core.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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

private val NavFluidEasing =
    CubicBezierEasing(
        0.16f,
        1f,
        0.30f,
        1f
    )

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

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                colors.surface
            )
            .padding(4.dp)
            .pointerInput(selected) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragDistance = 0f
                    },
                    onHorizontalDrag = {
                        change,
                        dragAmount ->

                        change.consume()

                        dragDistance +=
                            dragAmount
                    },
                    onDragEnd = {
                        if (
                            kotlin.math.abs(
                                dragDistance
                            ) >= 35.dp.toPx()
                        ) {
                            val currentIndex =
                                destinations.indexOf(
                                    selected
                                )

                            val targetIndex =
                                if (
                                    dragDistance >
                                    0f
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

    val activeWidth =
        when (destination) {
            XvoxDestination.HOME ->
                122.dp

            XvoxDestination.SEARCH ->
                130.dp

            XvoxDestination.SETTINGS ->
                145.dp
        }

    val width by
        animateDpAsState(
            targetValue =
                if (active) {
                    activeWidth
                } else {
                    70.dp
                },
            animationSpec =
                tween(
                    durationMillis = 500,
                    easing =
                        NavFluidEasing
                ),
            label = "navPillWidth"
        )

    /*
     * Active is intentionally the visually
     * heavier semantic surface.
     *
     * No hardcoded mode-specific palette.
     */
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
                    durationMillis = 250,
                    easing =
                        NavFluidEasing
                ),
            label = "navPillColor"
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
                    durationMillis = 220
                ),
            label = "navIconColor"
        )

    val pressAlpha by
        animateFloatAsState(
            targetValue =
                if (pressed) {
                    0.83f
                } else {
                    1f
                },
            animationSpec =
                tween(
                    durationMillis =
                        if (pressed) {
                            70
                        } else {
                            150
                        },
                    easing =
                        NavFluidEasing
                ),
            label = "navPressAlpha"
        )

    val pressScale by
        animateFloatAsState(
            targetValue =
                if (pressed) {
                    0.985f
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
                        NavFluidEasing
                ),
            label = "navPressScale"
        )

    Box(
        modifier = Modifier
            .width(width)
            .height(54.dp)
            .graphicsLayer {
                alpha =
                    pressAlpha

                scaleX =
                    pressScale

                scaleY =
                    pressScale
            }
            .clip(
                RoundedCornerShape(
                    27.dp
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
        modifier =
            Modifier.padding(
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
                Modifier.size(
                    22.dp
                )
        )

        XvoxAnimatedNavigationLabel(
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
private fun XvoxAnimatedNavigationLabel(
    destination: XvoxDestination,
    visible: Boolean,
    color: Color
) {
    val labelWidth =
        when (destination) {
            XvoxDestination.HOME ->
                42.dp

            XvoxDestination.SEARCH ->
                51.dp

            XvoxDestination.SETTINGS ->
                63.dp
        }

    val width by
        animateDpAsState(
            targetValue =
                if (visible) {
                    labelWidth
                } else {
                    0.dp
                },
            animationSpec =
                tween(
                    durationMillis = 420,
                    easing =
                        NavFluidEasing
                ),
            label = "navTextWidth"
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
                    durationMillis = 420,
                    easing =
                        NavFluidEasing
                ),
            label = "navTextSpacing"
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
                            220
                        } else {
                            150
                        },
                    delayMillis =
                        if (visible) {
                            45
                        } else {
                            0
                        }
                ),
            label = "navTextAlpha"
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
                    alpha =
                        alpha
                ),
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight =
                FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
