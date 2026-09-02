package com.xvox.music.core.ui.miniplayer

import androidx.compose.ui.unit.dp

object XvoxMiniPlayerPlacement {

    val horizontalEdge = 6.dp

    /*
     * Navbar host placement remains unchanged.
     *
     * With 10dp host overflow this produces:
     *
     * visible navbar bottom = 18dp
     * visible navbar top    = 82dp
     */
    val navigationHostBottom =
        8.dp

    val navigationVisualHeight =
        64.dp

    /*
     * Visible MiniPlayer now sits only 6dp above
     * the visible navbar.
     *
     * Navbar top:
     * 18 + 64 = 82dp
     *
     * MiniPlayer bottom:
     * 82 + 6 = 88dp
     */
    val miniPlayerBottom =
        88.dp
}
