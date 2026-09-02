package com.xvox.music.core.ui.miniplayer

import androidx.compose.ui.unit.dp

object XvoxMiniPlayerPlacement {

    val horizontalEdge = 6.dp

    val navigationBottomGap = 18.dp

    val navigationVisualHeight = 64.dp

    /*
     * Same gap above and below navigation:
     *
     * screen safe bottom
     *     18
     * navbar 64
     *     18
     * mini player
     */
    val miniPlayerBottom =
        navigationBottomGap +
            navigationVisualHeight +
            navigationBottomGap
}
