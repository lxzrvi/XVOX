package com.xvox.music.core.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.launch

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

    var dragDistance =
        remember {
            0f
        }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                colors.surface
            )
            .border(
                width = 1.dp,
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

                        dragDistance +=
                            amount
                    },
                    onDragEnd = {
                        if (
                            kotlin.math.abs(
                                dragDistance
                            ) >= 35.dp.toPx()
                        ) {
                            val current =
                                destinations.indexOf(
                                    selected
                                )

                            val target =
                                if (
                                    dragDistance > 0f
                                ) {
                                    current + 1
                                } else {
                                    current - 1
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
                    onSelected(
                        destination
                    )
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

    val interaction =
        remember {
            MutableInteractionSource()
        }

    val pressed by
        interaction.collectIsPressedAsState()

    val width by
        animateDpAsState(
            targetValue =
                when {
                    active &&
                        destination ==
                        XvoxDestination.SETTINGS ->
                        132.dp

                    active ->
                        112.dp

                    else ->
                        54.dp
                },
            animationSpec =
                spring(
                    dampingRatio = 0.80f,
                    stiffness = 380f
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
                tween(220),
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
                tween(200),
            label = "navForeground"
        )

    val pressAlpha by
        animateFloatAsState(
            targetValue =
                if (pressed) {
                    0.82f
                } else {
                    1f
                },
            animationSpec =
                tween(90),
            label = "navPress"
        )

    Box(
        modifier = Modifier
            .width(width)
            .height(54.dp)
            .graphicsLayer {
                alpha =
                    pressAlpha
            }
            .clip(
                RoundedCornerShape(
                    27.dp
                )
            )
            .background(
                background
            )
            .xvoxNavClick(
                interactionSource =
                    interaction,
                onClick = onClick
            ),
        contentAlignment =
            Alignment.CenterStart
    ) {
        Row(
            modifier =
                Modifier.padding(
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            XvoxNavigationIcon(
                destination =
                    destination,
                color =
                    foreground,
                modifier =
                    Modifier.size(
                        22.dp
                    )
            )

            AnimatedNavLabel(
                visible = active,
                label =
                    destination.label,
                color =
                    foreground
            )
        }
    }
}

@Composable
private fun AnimatedNavLabel(
    visible: Boolean,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    val width by
        animateDpAsState(
            targetValue =
                if (visible) {
                    when (label) {
                        "Settings" ->
                            66.dp

                        "Search" ->
                            50.dp

                        else ->
                            43.dp
                    }
                } else {
                    0.dp
                },
            animationSpec =
                spring(
                    dampingRatio = 0.82f,
                    stiffness = 380f
                ),
            label = "navLabelWidth"
        )

    val spacing by
        animateDpAsState(
            targetValue =
                if (visible) {
                    10.dp
                } else {
                    0.dp
                },
            animationSpec =
                spring(
                    dampingRatio = 0.82f,
                    stiffness = 380f
                ),
            label = "navLabelSpacing"
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
                            130
                        }
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
            )
    ) {
        Text(
            text = label,
            color =
                color.copy(
                    alpha = alpha
                ),
            fontSize = 14.sp,
            fontWeight =
                FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun Modifier.xvoxNavClick(
    interactionSource:
        MutableInteractionSource,
    onClick: () -> Unit
): Modifier =
    androidx.compose.foundation.clickable(
        interactionSource =
            interactionSource,
        indication = null,
        onClick = onClick
    )
