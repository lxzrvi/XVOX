package com.xvox.music.widget

import org.junit.Assert.*
import org.junit.Test

class WidgetCustomizationTest {
    @Test fun individualButtonsCanHideAndMoveWithoutChangingTheirNeighbours() {
        val initial = WidgetCustomization()
        val changed = initial.copy(buttons = initial.buttons + ("like" to initial.button("like").copy(position = "hidden"))).sanitized()
        assertEquals("hidden", changed.button("like").position)
        assertEquals("auto", changed.button("play").position)
    }
    @Test fun allElementBoundsAreSanitized() {
        val value = WidgetCustomization(marginX = 99, marginY = -5, coverSize = 1000, fullCoverShade = 1f,
            buttons = mapOf("play" to WidgetButtonStyle(size = 100, labelSize = 100, padding = 100)), buttonOrder = listOf("next", "next", "bad")).sanitized()
        assertEquals(32, value.marginX)
        assertEquals(0, value.marginY)
        assertEquals(160, value.coverSize)
        assertEquals(48, value.button("play").size)
        assertEquals(14, value.button("play").labelSize)
        assertEquals(4, value.buttonOrder.size)
    }
}
