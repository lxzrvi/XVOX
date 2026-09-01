package com.xvox.music.core.ui.navigation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object XvoxNavigationGeometry {

    val parentHeight = 65.dp
    val parentPadding = 4.dp

    val itemHeight = 57.dp

    val inactiveWidth = 70.dp
    val itemGap = 8.dp

    val iconSize = 22.dp
    val iconTextGap = 6.dp

    val activeHorizontalPadding = 16.dp

    fun labelWidth(
        destination: XvoxDestination
    ): Dp {
        return when (destination) {
            XvoxDestination.HOME ->
                40.dp

            XvoxDestination.SEARCH ->
                49.dp

            XvoxDestination.SETTINGS ->
                61.dp
        }
    }

    fun contentWidth(
        destination: XvoxDestination
    ): Dp {
        return iconSize +
            iconTextGap +
            labelWidth(destination)
    }

    fun activePillWidth(
        destination: XvoxDestination
    ): Dp {
        return activeHorizontalPadding * 2 +
            contentWidth(destination)
    }

    fun itemWidth(
        destination: XvoxDestination,
        selected: XvoxDestination
    ): Dp {
        return if (
            destination == selected
        ) {
            activePillWidth(
                destination
            )
        } else {
            inactiveWidth
        }
    }

    fun trackWidth(
        selected: XvoxDestination
    ): Dp {
        return XvoxDestination.entries
            .fold(0.dp) {
                total,
                destination ->

                total +
                    itemWidth(
                        destination,
                        selected
                    )
            } +
            itemGap *
            (
                XvoxDestination.entries.size -
                    1
                )
    }

    fun parentWidth(
        selected: XvoxDestination
    ): Dp {
        return trackWidth(
            selected
        ) +
            parentPadding * 2
    }

    fun activeCenter(
        selected: XvoxDestination
    ): Dp {
        var position = 0.dp

        XvoxDestination.entries
            .forEach {
                destination ->

                val width =
                    itemWidth(
                        destination,
                        selected
                    )

                if (
                    destination ==
                    selected
                ) {
                    return position +
                        width / 2
                }

                position +=
                    width +
                        itemGap
            }

        return 0.dp
    }

    fun activeIconCenter(
        destination: XvoxDestination
    ): Dp {
        val contentWidth =
            contentWidth(
                destination
            )

        return -contentWidth / 2 +
            iconSize / 2
    }

    fun activeLabelCenter(
        destination: XvoxDestination
    ): Dp {
        val contentWidth =
            contentWidth(
                destination
            )

        val textWidth =
            labelWidth(
                destination
            )

        return -contentWidth / 2 +
            iconSize +
            iconTextGap +
            textWidth / 2
    }
}
