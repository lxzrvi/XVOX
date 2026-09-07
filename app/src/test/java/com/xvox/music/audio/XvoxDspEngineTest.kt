package com.xvox.music.audio

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class XvoxDspEngineTest {
    @Test fun flatPipelinePreservesLowLevelStereo() {
        val dsp = XvoxDspEngine().apply { settings = AudioDspSettings(headroomDb = 0f); configure(48000) }
        repeat(48000) { i ->
            val l = (sin(2 * PI * 440 * i / 48000) * .3).toFloat()
            val r = (sin(2 * PI * 990 * i / 48000) * .2).toFloat()
            dsp.process(l, r)
            assertEquals(l.toDouble(), dsp.left.toDouble(), 0.00001)
            assertEquals(r.toDouble(), dsp.right.toDouble(), 0.00001)
        }
    }
    @Test fun fastBandChangesRemainFiniteAndBelowThePeakCeiling() {
        val dsp = XvoxDspEngine().apply { configure(44100) }
        repeat(180000) { i ->
            if (i % 512 == 0) dsp.settings = AudioDspSettings(equalizerEnabled = true,
                bands = List(5) { band -> if ((i / 512 + band) % 2 == 0) 12f else -12f },
                headroomDb = 0f, surroundEnabled = true, orbitSeconds = 2f, surroundDepth = 1f)
            val sample = (sin(2 * PI * 60 * i / 44100) + sin(2 * PI * 910 * i / 44100)).toFloat()
            dsp.process(sample, sample)
            assertTrue(dsp.left.isFinite() && dsp.right.isFinite())
            assertTrue(abs(dsp.left) <= .70001f && abs(dsp.right) <= .70001f)
        }
    }
    @Test fun bandDragDoesNotIntroduceSingleSampleImpulse() {
        val dsp = XvoxDspEngine().apply { settings = AudioDspSettings(headroomDb = 0f); configure(48000) }
        var previous = 0f
        var maxJump = 0f
        repeat(96000) { i ->
            if (i == 24000) dsp.settings = dsp.settings.copy(equalizerEnabled = true, bands = List(5) { 12f })
            if (i == 60000) dsp.settings = dsp.settings.copy(bands = List(5) { -12f })
            val sample = (sin(2 * PI * 230 * i / 48000) * .3).toFloat()
            dsp.process(sample, sample)
            maxJump = max(maxJump, abs(dsp.left - previous)); previous = dsp.left
        }
        assertTrue("Sudden impulse: $maxJump", maxJump < .08f)
    }
    @Test fun zeroVolumeReallyMutesInsteadOfClampingToTenPercent() {
        val dsp = XvoxDspEngine().apply { configure(44100); settings = settings.copy(masterVolume = 0f) }
        repeat(44100) { dsp.process(.2f, -.2f) }
        assertEquals(0.0, dsp.left.toDouble(), 0.00001)
        assertEquals(0.0, dsp.right.toDouble(), 0.00001)
    }
    @Test fun balanceStillWorksWhenSurroundIsEnabled() {
        val dsp = XvoxDspEngine().apply {
            settings = AudioDspSettings(balance = 1f, surroundEnabled = true, headroomDb = 0f)
            configure(44100)
        }
        repeat(44100) { dsp.process(.2f, .2f) }
        assertTrue(abs(dsp.left) < .0001f)
        assertTrue(abs(dsp.right) > .05f)
    }
    @Test fun spatialProcessingGivesMonoDistinctEarSignals() {
        val dsp = XvoxDspEngine().apply {
            settings = AudioDspSettings(surroundEnabled = true, surroundDepth = 1f, orbitSeconds = 2f)
            configure(48000)
        }
        var difference = 0.0
        repeat(96000) { i ->
            val sample = (sin(2 * PI * 600 * i / 48000) * .3).toFloat()
            dsp.process(sample, sample)
            difference += abs(dsp.left - dsp.right)
        }
        assertTrue(difference / 96000 > .01)
    }
    @Test fun extraHeadroomReducesOutputBeforeEQ() {
        fun output(db: Float): Float {
            val dsp = XvoxDspEngine().apply { settings = AudioDspSettings(headroomDb = db); configure(48000) }
            repeat(4800) { dsp.process(.25f, .25f) }
            return dsp.left
        }
        assertTrue(output(12f) < output(0f) * .3f)
    }
    @Test fun sampleRateChangesResetDelayMemory() {
        val dsp = XvoxDspEngine().apply { settings = AudioDspSettings(surroundEnabled = true); configure(48000) }
        repeat(5000) { dsp.process(.3f, -.3f) }
        dsp.configure(44100)
        repeat(5000) { dsp.process(0f, 0f); assertEquals(0f, dsp.left, .000001f); assertEquals(0f, dsp.right, .000001f) }
    }
}
