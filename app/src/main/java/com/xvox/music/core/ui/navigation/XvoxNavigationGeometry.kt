package com.xvox.music.core.ui.navigation

import androidx.compose.ui.unit.dp

object XvoxNavigationGeometry {

    val barWidth = 246.dp
    val barHeight = 62.dp

    val hostHeight = 82.dp
    val hostOverflow = 10.dp

    val selectorRestWidth = 74.dp
    val selectorRestHeight = 54.dp

    val selectorGrowWidth = 32.dp
    val selectorGrowHeight = 24.dp

    val selectorBaseRadius = 27.dp
    val selectorGrowRadius = 13.dp

    // Three equal slots in a 246 dp bar with a 74 dp pill. The pill keeps a 4 dp inset on
    // every side — the same 4 dp the 54 dp pill leaves above and below in the 62 dp bar — so the
    // gaps read as equal, and start = slotCentre - halfPill keeps it centred under Home, Search
    // and Settings.
    val selectorStart = 4.dp
    val selectorTravel = 164.dp
    val selectorGrowShift = 16.dp

    val iconSize = 25.dp

    val barRadius = 31.dp

    val barBorderWidth = 0.65.dp
    val selectorBorderWidth = 0.65.dp

    const val DragResistance = 1.22f

    const val SettleThreshold = 0.40f

    const val FarDestinationThreshold = 1.62f
}
