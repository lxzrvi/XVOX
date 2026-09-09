package com.xvox.music.core.ui.navigation

import androidx.compose.ui.unit.dp

object XvoxNavigationGeometry {

    val barWidth = 246.dp
    val barHeight = 64.dp

    val hostHeight = 84.dp
    val hostOverflow = 10.dp

    val selectorRestWidth = 78.dp
    val selectorRestHeight = 56.dp

    val selectorGrowWidth = 32.dp
    val selectorGrowHeight = 24.dp

    val selectorBaseRadius = 29.dp
    val selectorGrowRadius = 13.dp

    // Three equal 82 dp slots in a 246 dp bar with a 78 dp pill: start = slotCentre - halfPill
    // (2 dp) and each full step advances one slot (82 dp), so the pill sits exactly under Home,
    // Search and Settings.
    val selectorStart = 2.dp
    val selectorTravel = 164.dp
    val selectorGrowShift = 16.dp

    val iconSize = 25.dp

    val barRadius = 33.dp

    val barBorderWidth = 0.65.dp
    val selectorBorderWidth = 0.65.dp

    const val DragResistance = 1.22f

    const val SettleThreshold = 0.40f

    const val FarDestinationThreshold = 1.62f
}
