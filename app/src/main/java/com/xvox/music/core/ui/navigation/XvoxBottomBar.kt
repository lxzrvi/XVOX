package com.xvox.music.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxBottomBar(
    selected: XvoxDestination,
    onSelected:
        (XvoxDestination) -> Unit,
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

    var position by
        remember {
            mutableFloatStateOf(
                selectedIndex
                    .toFloat()
            )
        }

    LaunchedEffect(
        selectedIndex
    ) {
        position =
            selectedIndex
                .toFloat()
    }

    val motion =
        rememberXvoxNavigationMotion(
            position = position
        )

    val parentShape =
        RoundedCornerShape(
            XvoxNavigationGeometry
                .barRadius
        )

    val selectorShape =
        RoundedCornerShape(
            XvoxNavigationGeometry
                .selectorBaseRadius
        )

    Box(
        modifier =
            modifier
                .width(
                    XvoxNavigationGeometry
                        .barWidth
                )
                .height(
                    XvoxNavigationGeometry
                        .hostHeight
                )
    ) {
        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter
                    )
                    .offset(
                        y =
                            XvoxNavigationGeometry
                                .hostOverflow
                    )
                    .size(
                        XvoxNavigationGeometry
                            .barWidth,
                        XvoxNavigationGeometry
                            .barHeight
                    )
                    .graphicsLayer {
                        shape =
                            parentShape
                        clip = true
                    }
                    .background(
                        colors.surface
                            .copy(
                                alpha =
                                    0.88f
                            )
                    )
                    .border(
                        width =
                            XvoxNavigationGeometry
                                .barBorderWidth,
                        color =
                            colors.cardBorder
                                .copy(
                                    alpha =
                                        0.62f
                                ),
                        shape =
                            parentShape
                    )
        )

        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.TopStart
                    )
                    .offset(
                        y =
                            XvoxNavigationGeometry
                                .hostOverflow +
                                XvoxNavigationGeometry
                                    .barHeight /
                                2 -
                                XvoxNavigationGeometry
                                    .selectorRestHeight /
                                2
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
                                        )
                                )
                                .toPx()

                        shape =
                            selectorShape

                        clip = true
                    }
                    .size(
                        XvoxNavigationGeometry
                            .selectorRestWidth,
                        XvoxNavigationGeometry
                            .selectorRestHeight
                    )
                    .background(
                        colors.cardElevated
                            .copy(
                                alpha =
                                    0.42f
                            )
                    )
                    .border(
                        width =
                            XvoxNavigationGeometry
                                .selectorBorderWidth,
                        color =
                            colors.cardBorder
                                .copy(
                                    alpha =
                                        0.72f
                                ),
                        shape =
                            selectorShape
                    )
        )

        Row(
            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter
                    )
                    .offset(
                        y =
                            XvoxNavigationGeometry
                                .hostOverflow
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
            destinations
                .forEachIndexed {
                    index,
                    destination ->

                    val interaction =
                        remember(
                            destination
                        ) {
                            MutableInteractionSource()
                        }

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable(
                                    interactionSource =
                                        interaction,
                                    indication =
                                        null
                                ) {
                                    position =
                                        index
                                            .toFloat()

                                    onSelected(
                                        destination
                                    )
                                },
                        contentAlignment =
                            Alignment.Center
                    ) {
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
                                false,
                            inactiveColor =
                                colors.mutedText
                                    .copy(
                                        alpha =
                                            0.76f
                                    ),
                            activeColor =
                                colors.primaryText,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                        )
                    }
                }
        }
    }
}
