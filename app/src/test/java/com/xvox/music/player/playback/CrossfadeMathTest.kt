package com.xvox.music.player.playback

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class CrossfadeMathTest {
    @Test fun overlapMaintainsConstantPower() {
        repeat(1001) { i ->
            val g = CrossfadeMath.gains(i / 1000f)
            assertEquals(1f, g.incoming * g.incoming + g.outgoing * g.outgoing, .00001f)
            assertTrue(g.incoming >= 0 && g.outgoing >= 0)
        }
    }
    @Test fun endpointsAreNotBothSilent() {
        val start = CrossfadeMath.gains(-1f)
        val end = CrossfadeMath.gains(2f)
        assertEquals(1f, start.outgoing, .00001f); assertEquals(0f, start.incoming, .00001f)
        assertEquals(0f, end.outgoing, .00001f); assertEquals(1f, end.incoming, .00001f)
        val middle = CrossfadeMath.gains(.5f)
        assertTrue(middle.incoming > .7f && middle.outgoing > .7f)
    }
    @Test fun twoProtectedDecksCannotClipTheMixerEvenWithCorrelatedPeaks() {
        repeat(1001) { i ->
            val g = CrossfadeMath.gains(i / 1000f)
            assertTrue(.70f * (abs(g.incoming) + abs(g.outgoing)) < 1f)
        }
    }
    @Test fun durationRespectsShortTracksAndUnknownDurations() {
        assertEquals(3000L, CrossfadeMath.windowMs(3, 180000))
        assertEquals(1000L, CrossfadeMath.windowMs(12, 2000))
        assertEquals(500L, CrossfadeMath.windowMs(5, 180000, 1000))
        assertEquals(0L, CrossfadeMath.windowMs(3, -1))
        assertEquals(0L, CrossfadeMath.windowMs(3, 180000, 400))
        assertEquals(12000L, CrossfadeMath.windowMs(99, 180000))
    }
    @Test fun pauseFreezesProgressAndCompletionIsClamped() {
        assertEquals(.5f, CrossfadeMath.progress(4500, 3000, 3000), .0001f)
        assertEquals(.5f, CrossfadeMath.progress(4500, 3000, 3000), .0001f)
        assertEquals(0f, CrossfadeMath.progress(2000, 3000, 3000), .0001f)
        assertEquals(1f, CrossfadeMath.progress(9000, 3000, 3000), .0001f)
    }
}
