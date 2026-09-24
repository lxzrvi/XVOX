package com.xvox.music.core.ui.navigation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object XvoxNavigationGeometry {

    /** Original three-item width remains unchanged; extra library entries get compact equal slots. */
    fun barWidth(itemCount: Int): Dp = when (itemCount.coerceIn(3, 5)) {
        3 -> 246.dp
        4 -> 288.dp
        else -> 330.dp
    }

    val barHeight = 64.dp
    val hostHeight = 84.dp
    val hostOverflow = 10.dp

    /** Leaves a four-dp optical inset in every equal-width navigation slot. */
    fun selectorWidth(barWidth: Dp, itemCount: Int): Dp =
        ((barWidth / itemCount.coerceAtLeast(1).toFloat()) - 8.dp).coerceIn(48.dp, 74.dp)

    val selectorRestHeight = 56.dp
    val selectorGrowWidth = 32.dp
    val selectorGrowHeight = 24.dp
    val selectorBaseRadius = 29.dp
    val selectorGrowRadius = 13.dp

    val iconSize = 25.dp
    val barRadius = 33.dp
    val barBorderWidth = 0.65.dp
    val selectorBorderWidth = 0.65.dp

    const val DragResistance = 1.22f
    const val SettleThreshold = 0.40f
    const val FarDestinationThreshold = 1.62f
}
