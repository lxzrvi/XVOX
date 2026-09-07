package com.xvox.music.widget

import org.junit.Assert.*
import org.junit.Test

class WidgetLayoutSpecTest {
    @Test fun paddingAndCornersStayInsideEveryShape() {
        for (w in listOf(40, 80, 160, 240, 320, 600)) for (h in listOf(40, 70, 150, 230, 310, 600)) {
            val s = WidgetLayoutSpec.create(w, h, 32, 28, 48, true)
            assertTrue(s.paddingX >= 0 && s.paddingY >= 0)
            assertTrue(s.coverSide <= w - 2 * s.paddingX)
            assertTrue(s.coverSide <= h - 2 * s.paddingY)
            assertTrue(s.radius <= minOf(w, h) / 2f)
            assertTrue(s.coverRadius in 0f..s.coverSide / 2f)
        }
    }
    @Test fun tallAndWideWidgetsUseDifferentControlLayouts() {
        assertEquals(WidgetLayoutType.VERTICAL, WidgetLayoutSpec.create(80, 230, 10, 8, 24, true).type)
        assertEquals(WidgetLayoutType.HORIZONTAL, WidgetLayoutSpec.create(320, 70, 10, 8, 24, true).type)
        assertEquals(WidgetLayoutType.SQUARE, WidgetLayoutSpec.create(160, 160, 10, 8, 24, true).type)
    }
    @Test fun paddingAxesCanBeAdjustedIndependently() {
        val base = WidgetLayoutSpec.create(320, 100, 10, 8, 24, true)
        val xOnly = WidgetLayoutSpec.create(320, 100, 20, 8, 24, true)
        assertEquals(base.paddingY, xOnly.paddingY)
        assertNotEquals(base.paddingX, xOnly.paddingX)
    }
    @Test fun smallWidgetsPrioritizeControlsInsteadOfOverlappingThem() {
        val small = WidgetLayoutSpec.create(140, 64, 10, 8, 24, true)
        assertFalse(small.showLike)
        assertFalse(small.showPrevious)
        val wide = WidgetLayoutSpec.create(320, 70, 10, 8, 24, true)
        assertTrue(wide.showLike && wide.showPrevious)
    }
}
