package com.xvox.music.core.ui.miniplayer

import androidx.compose.ui.unit.dp
import com.xvox.music.core.ui.navigation.XvoxNavigationGeometry

object XvoxMiniPlayerPlacement {
    val horizontalEdge = 6.dp
    val navigationHostBottom = 10.dp
    val navigationVisualHeight = XvoxNavigationGeometry.barHeight
    val controlGap = 12.dp
    // Measure to the visible bar top, not the host's overflow / touch bounds.
    val miniPlayerBottom = navigationHostBottom + XvoxNavigationGeometry.hostHeight -
        XvoxNavigationGeometry.hostOverflow + controlGap
}
