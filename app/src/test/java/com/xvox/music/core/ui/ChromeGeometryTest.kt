package com.xvox.music.core.ui

import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayerPlacement
import com.xvox.music.core.ui.navigation.XvoxNavigationGeometry
import org.junit.Assert.assertEquals
import org.junit.Test

class ChromeGeometryTest {
    @Test fun miniPlayerUsesTheSameTwelveDpGapAsTheKeyboard() {
        val visibleNavTop = XvoxMiniPlayerPlacement.navigationHostBottom + XvoxNavigationGeometry.hostHeight - XvoxNavigationGeometry.hostOverflow
        assertEquals(12f, (XvoxMiniPlayerPlacement.miniPlayerBottom - visibleNavTop).value, .0001f)
        assertEquals(12f, XvoxMiniPlayerPlacement.controlGap.value, .0001f)
    }
}
