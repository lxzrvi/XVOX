package com.xvox.music.core.ui.navigation

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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlin.math.abs

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

    val motion =
        rememberNavigationBarMotion(
            selected =
                selected,
            selectedIndex =
                selectedIndex
        )

    Box(
        modifier = modifier
            .offset(y = 2.dp)
            .width(
                XvoxNavigationGeometry
                    .stageWidth
            )
            .height(
                XvoxNavigationGeometry
                    .parentHeight
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x =
                        motion.parentShift
                )
                .width(
                    motion.parentWidth
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

        NavigationTrack(
            selected =
                selected,
            motion =
                motion,
            onSelected =
                onSelected,
            modifier = Modifier
                .offset(
                    x =
                        motion.parentShift
                )
        )
    }
}

@Composable
private fun NavigationTrack(
    selected: XvoxDestination,
    motion: XvoxNavigationBarMotion,
    onSelected: (XvoxDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors =
        XvoxTheme.colors

    val destinations =
        XvoxDestination.entries

    Box(
        modifier = modifier
            .width(
                motion.parentWidth
            )
            .height(
                XvoxNavigationGeometry
                    .parentHeight
            )
            .padding(
                XvoxNavigationGeometry
                    .parentPadding
            ),
        contentAlignment =
            Alignment.TopStart
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (
                            motion.activeCenter -
                                motion.activeWidth /
                                2
                            )
                            .roundToPx(),
                        y = 0
                    )
                }
                .width(
                    motion.activeWidth
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
                    XvoxNavigationGeometry
                        .trackWidth()
                )
                .height(
                    XvoxNavigationGeometry
                        .itemHeight
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
                                XvoxNavigationGeometry
                                    .slotGap
                            )
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
}
