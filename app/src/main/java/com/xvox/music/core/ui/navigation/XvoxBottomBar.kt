package com.xvox.music.core.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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

private val NavMotionEasing =
    CubicBezierEasing(
        0.16f,
        1f,
        0.30f,
        1f
    )

private val NavColorEasing =
    CubicBezierEasing(
        0.20f,
        0.90f,
        0.25f,
        1f
    )

private const val NavMorphDuration = 540

private val NavigationWidth = 330.dp
private val NavigationHeight = 65.dp
private val ItemHeight = 57.dp
private val OuterPadding = 4.dp
private val ItemGap = 8.dp

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
            .offset(y = 2.dp)
            .width(NavigationWidth)
            .height(NavigationHeight)
            .clip(CircleShape)
            .background(
                colors.surface
            )
            .border(
                width = 0.5.dp,
                color = colors.cardBorder,
                shape = CircleShape
            )
            .padding(OuterPadding)
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
                            abs(dragDistance) >=
                            35.dp.toPx()
                        ) {
                            val currentIndex =
                                destinations
                                    .indexOf(
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
                                ?.let {
                                    onSelected(it)
                                }
                        }

                        dragDistance = 0f
                    },
                    onDragCancel = {
                        dragDistance = 0f
                    }
                )
            },
        horizontalArrangement =
            Arrangement.spacedBy(
                ItemGap
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        destinations.forEach {
            destination ->

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

@Composable
private fun RowScope.NavigationItem(
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
                        NavMorphDuration,
                    easing =
                        NavMotionEasing
                ),
            label =
                "navMorph"
        )

    val weight =
        1f +
            progress * 0.72f

    val background by
        animateColorAsState(
            targetValue =
                if (active) {
                    colors.cardElevated
                } else {
                    colors.card
                },
            animationSpec =
                tween(
                    durationMillis = 360,
                    easing =
                        NavColorEasing
                ),
            label =
                "navBackground"
        )

    val foreground by
        animateColorAsState(
            targetValue =
                if (active) {
                    colors.primaryText
                } else {
                    colors.mutedText
                },
            animationSpec =
                tween(
                    durationMillis = 320,
                    easing =
                        NavColorEasing
                ),
            label =
                "navForeground"
        )

    val pressScale by
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
                            180
                        },
                    easing =
                        NavMotionEasing
                ),
            label =
                "navPressScale"
        )

    val pressAlpha by
        animateFloatAsState(
            targetValue =
                if (pressed) {
                    0.86f
                } else {
                    1f
                },
            animationSpec =
                tween(
                    durationMillis =
                        if (pressed) {
                            70
                        } else {
                            170
                        },
                    easing =
                        NavMotionEasing
                ),
            label =
                "navPressAlpha"
        )

    Box(
        modifier = Modifier
            .weight(weight)
            .height(ItemHeight)
            .graphicsLayer {
                scaleX =
                    pressScale

                scaleY =
                    pressScale

                alpha =
                    pressAlpha
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
            Alignment.Center
    ) {
        NavigationContent(
            destination =
                destination,
            progress =
                progress,
            color =
                foreground
        )
    }
}

@Composable
private fun NavigationContent(
    destination: XvoxDestination,
    progress: Float,
    color: Color
) {
    /*
     * Icon begins centered in the inactive pill
     * and glides left as the label materializes.
     *
     * The label itself always has enough drawing
     * space. We do not animate a clipping width,
     * which prevents the straight-cut text effect.
     */

    val iconTravel =
        when (destination) {
            XvoxDestination.HOME ->
                27.dp

            XvoxDestination.SEARCH ->
                31.dp

            XvoxDestination.SETTINGS ->
                38.dp
        }

    val labelOffset =
        when (destination) {
            XvoxDestination.HOME ->
                25.dp

            XvoxDestination.SEARCH ->
                28.dp

            XvoxDestination.SETTINGS ->
                33.dp
        }

    val labelAlpha =
        smoothStep(
            (
                (progress - 0.12f) /
                    0.62f
                )
                .coerceIn(
                    0f,
                    1f
                )
        )

    val labelMotion =
        smoothStep(
            (
                progress /
                    0.84f
                )
                .coerceIn(
                    0f,
                    1f
                )
        )

    XvoxNavigationIcon(
        destination =
            destination,
        color =
            color,
        modifier = Modifier
            .size(22.dp)
            .graphicsLayer {
                translationX =
                    -iconTravel
                        .toPx() *
                        progress
            }
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
        modifier = Modifier
            .graphicsLayer {
                alpha =
                    labelAlpha

                translationX =
                    labelOffset
                        .toPx() +
                        (
                            1f -
                                labelMotion
                            ) *
                        8.dp.toPx()

                val scale =
                    0.965f +
                        0.035f *
                        labelMotion

                scaleX =
                    scale

                scaleY =
                    scale
            }
    )
}

private fun smoothStep(
    value: Float
): Float {
    return value *
        value *
        (
            3f -
                2f * value
            )
}
