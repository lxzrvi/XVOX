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

    /** Cover offsets and text nudges may be negative, so artwork and text can leave the box. */
    @Test fun coverAndTextMayBePushedOutsideTheWidgetBox() {
        val value = WidgetCustomization(
            coverMarginX = -20, coverMarginY = -99, coverPaddingX = -8, coverPaddingY = 40,
            labels = WidgetCustomization.defaultLabels() + ("title" to WidgetLabelStyle(offsetX = -30, offsetY = 99))
        ).sanitized()
        assertEquals(-20, value.coverMarginX)
        assertEquals(-32, value.coverMarginY)
        assertEquals(-8, value.coverPaddingX)
        assertEquals(24, value.coverPaddingY)
        assertEquals(-30, value.label("title").offsetX)
        assertEquals(48, value.label("title").offsetY)
    }

    @Test fun negativeOffsetsSurviveEncodeAndDecode() {
        val value = WidgetCustomization(
            coverMarginX = -18,
            labels = WidgetCustomization.defaultLabels() + ("artist" to WidgetLabelStyle(offsetX = -12, offsetY = -7))
        ).sanitized()
        val restored = WidgetCustomization.decode(value.encode())
        assertEquals(-18, restored.coverMarginX)
        assertEquals(-12, restored.label("artist").offsetX)
        assertEquals(-7, restored.label("artist").offsetY)
    }
}
