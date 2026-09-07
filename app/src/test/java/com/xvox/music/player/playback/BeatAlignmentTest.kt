package com.xvox.music.player.playback

import org.junit.Assert.*
import org.junit.Test

class BeatAlignmentTest {
    @Test fun compatibleOnsetsAlignWithoutSkippingTheIntro() {
        val outgoing = BeatGrid(listOf(7800, 8300, 8800, 9300, 9800), 500.0, .9)
        val incoming = BeatGrid(listOf(100, 600, 1100, 1600), 500.0, .9)
        val plan = BeatAlignment.plan(10000, 2000, outgoing, incoming)
        assertNotNull(plan)
        assertEquals(8200L, plan!!.startPositionMs)
        assertEquals(1800L, plan.overlapMs)
        assertEquals(8300L, plan.startPositionMs + incoming.beatsMs.first())
    }
    @Test fun incompatibleOrUncertainRhythmsFallBack() {
        val outgoing = BeatGrid(listOf(8300, 8800, 9300, 9800), 500.0, .9)
        assertNull(BeatAlignment.plan(10000, 2000, outgoing, BeatGrid(listOf(100, 720), 620.0, .9)))
        assertNull(BeatAlignment.plan(10000, 2000, outgoing, BeatGrid(listOf(100, 600), 500.0, .2)))
        assertNull(BeatAlignment.plan(10000, 2000, null, outgoing))
    }
    @Test fun aLongQuietIntroIsNotCutOffForBeatMatching() {
        val grid = BeatGrid(listOf(3000, 3500, 4000), 500.0, .9)
        assertNull(BeatAlignment.plan(10000, 2000, grid, grid))
    }
    @Test fun detectsAnEvenSyntheticPulseTrain() {
        val energy = DoubleArray(1600) { if (it % 50 == 0) 1.0 else .0001 }
        val grid = BeatAlignment.detect(energy, 4000)
        assertNotNull(grid)
        assertEquals(500.0, grid!!.periodMs, 25.0)
        assertTrue(grid.confidence >= .42)
        assertTrue(grid.beatsMs.all { it >= 4000 })
    }
    @Test fun silenceNeverReportsBeatSync() {
        assertNull(BeatAlignment.detect(DoubleArray(1600), 0))
        assertNull(BeatAlignment.detect(DoubleArray(100), 0))
    }
}
