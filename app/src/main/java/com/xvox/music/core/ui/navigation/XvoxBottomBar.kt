package com.xvox.music.core.ui.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlin.math.abs

private val ParentEasing =
    CubicBezierEasing(
        0.16f,
        1f,
        0.30f,
        1f
    )

private const val ParentDuration = 520

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

    val targetParentWidth =
        XvoxNavigationGeometry
            .parentWidth(
                selected
            )

    val parentWidth by
        animateDpAsState(
            targetValue =
                targetParentWidth,
            animationSpec =
                tween(
                    durationMillis =
                        ParentDuration,
                    easing =
                        ParentEasing
                ),
            label =
                "navParentWidth"
        )

    /*
     * Track width is the parent's inner width.
     * Both animate together from the same source.
     *
     * There is no second coordinate system.
     */
    val trackWidth =
        parentWidth -
            XvoxNavigationGeometry
                .parentPadding *
            2

    Box(
        modifier = modifier
            .offset(y = 2.dp)
            .width(
                parentWidth
            )
            .height(
                XvoxNavigationGeometry
                    .parentHeight
            )
            .background(
                color =
                    colors.surface,
                shape =
                    CircleShape
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
            .padding(
                XvoxNavigationGeometry
                    .parentPadding
            ),
        contentAlignment =
            Alignment.CenterStart
    ) {
        NavigationTrack(
            selected =
                selected,
            animatedTrackWidth =
                trackWidth,
            onSelected =
                onSelected
        )
    }
}

@Composable
private fun NavigationTrack(
    selected: XvoxDestination,
    animatedTrackWidth:
        androidx.compose.ui.unit.Dp,
    onSelected: (XvoxDestination) -> Unit
) {
    val colors =
        XvoxTheme.colors

    val destinations =
        XvoxDestination.entries

    val targetActiveWidth =
        XvoxNavigationGeometry
            .activePillWidth(
                selected
            )

    val activeWidth by
        animateDpAsState(
            targetValue =
                targetActiveWidth,
            animationSpec =
                tween(
                    durationMillis =
                        ParentDuration,
                    easing =
                        ParentEasing
                ),
            label =
                "navActiveWidth"
        )

    /*
     * Target active center is calculated from
     * actual selected/inactive item widths.
     */
    val targetCenter =
        XvoxNavigationGeometry
            .activeCenter(
                selected
            )

    val activeCenter by
        animateDpAsState(
            targetValue =
                targetCenter,
            animationSpec =
                tween(
                    durationMillis =
                        ParentDuration,
                    easing =
                        ParentEasing
                ),
            label =
                "navActiveCenter"
        )

    Box(
        modifier = Modifier
            .width(
                animatedTrackWidth
            )
            .height(
                XvoxNavigationGeometry
                    .itemHeight
            )
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x =
                        activeCenter -
                            activeWidth / 2
                )
                .width(
                    activeWidth
                )
                .height(
                    XvoxNavigationGeometry
                        .itemHeight
                )
                .background(
                    color =
                        colors.cardElevated,
                    shape =
                        CircleShape
                )
        )

        Row(
            modifier = Modifier
                .width(
                    animatedTrackWidth
                )
                .height(
                    XvoxNavigationGeometry
                        .itemHeight
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            destinations
                .forEachIndexed {
                    index,
                    destination ->

                    if (index > 0) {
                        Spacer(
                            modifier =
                                Modifier.width(
                                    XvoxNavigationGeometry
                                        .itemGap
                                )
                        )
                    }

                    NavigationSlot(
                        destination =
                            destination,
                        selected =
                            selected,
                        onSelected =
                            onSelected
                    )
                }
        }
    }
}

@Composable
private fun NavigationSlot(
    destination: XvoxDestination,
    selected: XvoxDestination,
    onSelected: (XvoxDestination) -> Unit
) {
    val active =
        destination ==
            selected

    val targetWidth =
        if (active) {
            XvoxNavigationGeometry
                .activePillWidth(
                    destination
                )
        } else {
            XvoxNavigationGeometry
                .inactiveWidth
        }

    val width by
        animateDpAsState(
            targetValue =
                targetWidth,
            animationSpec =
                tween(
                    durationMillis =
                        ParentDuration,
                    easing =
                        ParentEasing
                ),
            label =
                "navSlotWidth"
        )

    Box(
        modifier = Modifier
            .width(width)
            .height(
                XvoxNavigationGeometry
                    .itemHeight
            ),
        contentAlignment =
            Alignment.Center
    ) {
        /*
         * XvoxNavigationItem itself keeps the
         * icon/text animation responsibility.
         *
         * It receives the current active state,
         * while this wrapper owns only its slot.
         */
        XvoxNavigationItem(
            destination =
                destination,
            active =
                active,
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
