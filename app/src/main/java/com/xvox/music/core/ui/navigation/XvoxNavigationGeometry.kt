package com.xvox.music.core.ui.navigation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object XvoxNavigationGeometry {

    val stageWidth = 350.dp

    val parentHeight = 65.dp
    val parentPadding = 4.dp

    val itemHeight = 57.dp

    val slotWidth = 70.dp
    val slotGap = 8.dp

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

    fun parentWidth(
        destination: XvoxDestination
    ): Dp {
        return when (destination) {
            XvoxDestination.HOME ->
                266.dp

            XvoxDestination.SEARCH ->
                275.dp

            XvoxDestination.SETTINGS ->
                287.dp
        }
    }

    fun parentShift(
        destination: XvoxDestination
    ): Dp {
        return when (destination) {
            XvoxDestination.HOME ->
                (-9).dp

            XvoxDestination.SEARCH ->
                0.dp

            XvoxDestination.SETTINGS ->
                9.dp
        }
    }

    fun trackWidth(): Dp {
        return slotWidth * 3 +
            slotGap * 2
    }

    fun slotCenter(
        index: Int
    ): Dp {
        return slotWidth / 2 +
            (slotWidth + slotGap) *
            index
    }

    fun activeIconCenter(
        destination: XvoxDestination
    ): Dp {
        return -contentWidth(destination) / 2 +
            iconSize / 2
    }

    fun activeLabelCenter(
        destination: XvoxDestination
    ): Dp {
        val textWidth =
            labelWidth(destination)

        return -contentWidth(destination) / 2 +
            iconSize +
            iconTextGap +
            textWidth / 2
    }
}
