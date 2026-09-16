package com.xvox.music.audio

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class EqBandsAndGuardTest {
    @Test fun bandModesPreserveFlatAndConstantCurves() {
        assertEquals(List(10) { 0 }, EqBands.convert(List(5) { 0 }, 10))
        assertEquals(List(5) { 6 }, EqBands.convert(List(10) { 6 }, 5))
        assertEquals(10, EqBands.frequencies(10).size)
    }
    @Test fun boostsIncreaseAQuietSignalInsteadOfAutoAttenuatingIt() {
        fun rms(boost: Int): Double {
            val dsp = XvoxDspEngine().apply {
                settings = AudioDspSettings(equalizerEnabled = true, bands = listOf(0f, boost.toFloat(), 0f, 0f, 0f), headroomDb = 0f)
                configure(48000)
            }
            var total = 0.0
            repeat(96000) { i ->
                val value = (sin(2 * PI * 230 * i / 48000) * .04).toFloat()
                dsp.process(value, value)
                if (i >= 48000) total += dsp.left * dsp.left
            }
            return sqrt(total / 48000)
        }
        assertTrue(rms(6) > rms(0) * 1.5)
    }
    @Test fun tenBandSwitchAndToneControlsStayFinite() {
        val dsp = XvoxDspEngine().apply { configure(44100) }
        repeat(90000) { i ->
            if (i % 4096 == 0) dsp.settings = AudioDspSettings(equalizerEnabled = true,
                bandCount = if (i % 8192 == 0) 10 else 5, bands = List(if (i % 8192 == 0) 10 else 5) { 12f },
                softenHighs = .8f, noiseReduction = .5f)
            val sample = sin(2 * PI * 440 * i / 44100).toFloat()
            dsp.process(sample, sample)
            assertTrue(dsp.left.isFinite() && dsp.right.isFinite())
            assertTrue(abs(dsp.left) <= .70001f && abs(dsp.right) <= .70001f)
        }
    }
    @Test fun limiterDequeHandlesLongDescendingSignalsWithoutLosingPeakHistory() {
        val guard = StereoPeakGuard().apply { configure(48000) }
        repeat(30000) { i ->
            val value = 4.0 * exp(-i / 5000.0)
            guard.process(value, value)
            assertTrue(abs(guard.left) <= .700001 && abs(guard.right) <= .700001)
        }
    }
}
