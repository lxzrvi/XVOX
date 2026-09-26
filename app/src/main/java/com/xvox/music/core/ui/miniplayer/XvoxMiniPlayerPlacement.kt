package com.xvox.music.core.ui.miniplayer

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.core.ui.navigation.XvoxNavigationGeometry

object XvoxMiniPlayerPlacement {
    val horizontalEdge = 6.dp
    /** Shared 10dp breathing room above either the Navbar or an open keyboard. */
    val controlGap = 10.dp
    val navigationHostBottom = 6.dp

    /** The floating nav retains its 20dp host allowance at every user-selected visual height. */
    fun navigationHostHeight(navigationBarHeight: Dp): Dp = navigationBarHeight + 20.dp

    /** Measure to the visible bar top, not the host's overflow / touch bounds. */
    fun miniPlayerBottom(navigationBarHeight: Dp): Dp =
        navigationHostBottom + navigationHostHeight(navigationBarHeight) -
            XvoxNavigationGeometry.hostOverflow + controlGap
}
