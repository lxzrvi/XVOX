package com.xvox.music.core.ui.navigation

import androidx.compose.ui.unit.dp

object XvoxNavigationGeometry {

    val barWidth = 246.dp
    val barHeight = 64.dp

    /*
     * 80dp selector needs room beyond the
     * 64dp resting parent.
     *
     * Extra 20dp gives 10dp above and below.
     */
    val hostHeight = 84.dp
    val hostOverflow = 10.dp

    val selectorRestWidth = 78.dp
    val selectorRestHeight = 56.dp

    /*
     * Hold:
     * 78x56 -> 110x80
     */
    val selectorGrowWidth = 32.dp
    val selectorGrowHeight = 24.dp

    val selectorBaseRadius = 29.dp
    val selectorGrowRadius = 13.dp

    val selectorStart = 4.dp
    val selectorTravel = 160.dp
    val selectorGrowShift = 16.dp

    val iconSize = 25.dp

    val barRadius = 33.dp

    val barBorderWidth = 0.65.dp
    val selectorBorderWidth = 0.65.dp

    const val DragResistance = 1.22f

    const val SettleThreshold = 0.40f

    const val FarDestinationThreshold = 1.62f
}
