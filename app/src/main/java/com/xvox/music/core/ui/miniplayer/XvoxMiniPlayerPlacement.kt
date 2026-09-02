package com.xvox.music.core.ui.miniplayer

import androidx.compose.ui.unit.dp

object XvoxMiniPlayerPlacement {

    val horizontalEdge = 6.dp

    /*
     * Visible geometry target:
     *
     * MiniPlayer bottom
     *      18dp
     * Navbar 64dp
     *      18dp
     * Safe bottom
     */
    val visibleGap = 18.dp

    val navigationVisualHeight = 64.dp

    /*
     * Navbar host is 84dp with 10dp invisible overflow
     * below the visible 64dp bar.
     *
     * Therefore host bottom must sit 8dp from the safe
     * bottom for the visible bar bottom to be 18dp away.
     */
    val navigationHostBottom =
        8.dp

    /*
     * Do not move MiniPlayer.
     *
     * 18 + 64 + 18 = 100dp.
     */
    val miniPlayerBottom =
        visibleGap +
            navigationVisualHeight +
            visibleGap
}
