package com.xvox.music.player.playback

import org.junit.Assert.*
import org.junit.Test

class EnergyBlendPlannerTest {
    @Test fun curvesNeverFadeBothTracksToSilence() {
        repeat(1001) { step ->
            val g = EnergyBlendPlanner.gains(step / 1000f, .43f, true)
            assertEquals(1f, g.incoming * g.incoming + g.outgoing * g.outgoing, .00001f)
        }
    }
    @Test fun bassHandoffOnlyAttenuatesAndNeverClips() {
        repeat(1001) { i ->
            val p = i / 1000f
            val g = EnergyBlendPlanner.gains(p, .5f, true)
            val bass = EnergyBlendPlanner.bassGains(p, .5f, .9f, g)
            assertTrue(bass.incoming in 0f..1f && bass.outgoing in 0f..1f)
        }
    }
    @Test fun graphPlanPreservesTheIntroAndTrackEnd() {
        val out = TrackBlendProfile(null, EnergyEnvelope(7000, 10, List(301) { if (it in 120..170) .05f else .8f }))
        val incoming = TrackBlendProfile(null, EnergyEnvelope(0, 10, List(301) { if (it in 60..100) .05f else .7f }))
        val plan = EnergyBlendPlanner.plan(10000, 3000, out, incoming, false)
        assertNotNull(plan)
        assertEquals(10000L, plan!!.startPositionMs + plan.overlapMs)
        assertTrue(plan.startPositionMs >= 7000)
        assertTrue(plan.handoff in .3f.. .7f)
        assertFalse(plan.beatAligned)
    }
    @Test fun missingAnalysisFallsBackWithoutWaiting() {
        assertNull(EnergyBlendPlanner.plan(10000, 3000, null, null, true))
    }
}
