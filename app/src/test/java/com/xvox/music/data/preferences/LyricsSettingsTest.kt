package com.xvox.music.data.preferences

import org.junit.Assert.*
import org.junit.Test

class LyricsSettingsTest {
    @Test fun positiveOffsetDelaysAndNegativeAdvances() {
        assertEquals(1500L, LyricsSettings(offsetMs = 500).position(2000))
        assertEquals(2500L, LyricsSettings(offsetMs = -500).position(2000))
        assertEquals(2500L, LyricsSettings(offsetMs = 500).seekPosition(2000))
        assertEquals(0L, LyricsSettings(offsetMs = 500).position(0))
    }
    @Test fun sizesAndFadeRegionsAreBounded() {
        val value = LyricsSettings(offsetMs = 9000, currentSize = 99, otherSize = 1, fadeTop = .8f, fadeBottom = -.1f, animation = "unknown").sanitized()
        assertEquals(5000, value.offsetMs)
        assertEquals(42, value.currentSize)
        assertEquals(10, value.otherSize)
        assertEquals(.45f, value.fadeTop, .001f)
        assertEquals(0f, value.fadeBottom, .001f)
        assertEquals("focus", value.animation)
    }
}
