package com.xvox.music.core.ui.miniplayer

import androidx.compose.ui.unit.dp

object XvoxMiniPlayerPlacement {

    val horizontalEdge =
        6.dp

    /*
     * Navbar host remains in its corrected position.
     *
     * Visible Navbar:
     * bottom = 18dp
     * height = 64dp
     * top    = 82dp
     */
    val navigationHostBottom =
        8.dp

    val navigationVisualHeight =
        64.dp

    /*
     * Shared Home rhythm is 12dp.
     *
     * Navbar visible top:
     * 18 + 64 = 82dp
     *
     * MiniPlayer bottom:
     * 82 + 12 = 94dp
     */
    val miniPlayerBottom =
        94.dp
}
