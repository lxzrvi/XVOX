package com.xvox.music.core.ui.navigation

import androidx.compose.ui.unit.dp

object XvoxNavigationGeometry {

    val barWidth = 246.dp
    val barHeight = 64.dp

    val selectorRestWidth = 78.dp
    val selectorRestHeight = 56.dp

    val selectorGrowWidth = 24.dp
    val selectorGrowHeight = 16.dp

    val selectorBaseRadius = 29.dp
    val selectorGrowRadius = 9.dp

    val selectorStart = 4.dp
    val selectorTravel = 160.dp
    val selectorGrowShift = 12.dp

    val iconSize = 25.dp

    val barRadius = 33.dp

    val barBorderWidth = 0.65.dp
    val selectorBorderWidth = 0.65.dp

    /*
     * Finger must travel farther than one
     * visual slot to move the selector by
     * one complete destination.
     */
    const val DragResistance = 1.34f

    /*
     * Movement below this is treated as
     * a tap/hold instead of drag.
     */
    const val TouchSlopMultiplier = 1.15f

    /*
     * Adjacent destination threshold.
     */
    const val SettleThreshold = 0.42f

    /*
     * Reaching two tabs in one gesture is
     * deliberately harder.
     */
    const val FarDestinationThreshold = 1.58f
}
