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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

private val NavFluidEasing =
    CubicBezierEasing(
        0.16f,
        1f,
        0.30f,
        1f
    )

private const val MorphDuration = 520

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

    Row(
        modifier = modifier
            .offset(y = 2.dp)
            .clip(CircleShape)
            .background(
                colors.surface
            )
            .border(
                width = 0.5.dp,
                color = colors.cardBorder,
                shape = CircleShape
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
                        MorphDuration,
                    easing =
                        NavFluidEasing
                ),
            label =
                "navMorph"
        )

    val inactiveWidth =
        InactiveWidth.value

    val activeWidth =
        when (destination) {
            XvoxDestination.HOME ->
                124.dp.value

            XvoxDestination.SEARCH ->
                134.dp.value

            XvoxDestination.SETTINGS ->
                150.dp.value
        }

    val currentWidth =
        (
            inactiveWidth +
                (
                    activeWidth -
                        inactiveWidth
                    ) *
                progress
            ).dp

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
                    durationMillis = 300,
                    easing =
                        NavFluidEasing
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
                    durationMillis = 260,
                    easing =
                        NavFluidEasing
                ),
            label =
                "navForeground"
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
                            170
                        },
                    easing =
                        NavFluidEasing
                ),
            label =
                "navPressScale"
        )

    val pressAlpha by
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
                            65
                        } else {
                            160
                        },
                    easing =
                        NavFluidEasing
                ),
            label =
                "navPressAlpha"
        )

    Box(
        modifier = Modifier
            .width(currentWidth)
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
            Alignment.CenterStart
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
    val labelWidth =
        when (destination) {
            XvoxDestination.HOME ->
                44.dp

            XvoxDestination.SEARCH ->
                53.dp

            XvoxDestination.SETTINGS ->
                66.dp
        }

    /*
     * Same progress drives pill width,
     * spacing and label reveal.
     * This prevents two animations from
     * fighting during fast tab changes.
     */
    val reveal =
        smoothStep(
            (
                (progress - 0.05f) /
                    0.95f
                )
                .coerceIn(
                    0f,
                    1f
                )
        )

    val textAlpha =
        smoothStep(
            (
                (progress - 0.12f) /
                    0.70f
                )
                .coerceIn(
                    0f,
                    1f
                )
        )

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

        Spacer(
            modifier =
                Modifier.width(
                    12.dp *
                        reveal
                )
        )

        /*
         * HTML-style horizontal reveal is
         * retained, but text itself also fades
         * and subtly translates so the edge
         * doesn't look harsh.
         */
        Box(
            modifier =
                Modifier.width(
                    labelWidth *
                        reveal
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
                            textAlpha
                    ),
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight =
                    FontWeight.SemiBold,
                maxLines = 1,
                modifier =
                    Modifier.graphicsLayer {
                        translationX =
                            (
                                1f -
                                    reveal
                                ) *
                                5.dp.toPx()

                        val scale =
                            0.98f +
                                0.02f *
                                reveal

                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
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
