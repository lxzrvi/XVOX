package com.xvox.music.split

import org.junit.Assert.*
import org.junit.Test
import java.nio.FloatBuffer
import kotlin.math.*

class SplitFftTest {
    @Test fun radix2RoundTripIsStable() {
        val fft = SplitFft(4096)
        val original = FloatArray(4096) { (sin(2 * PI * 19 * it / 4096) * .3).toFloat() }
        val real = original.copyOf(); val imaginary = FloatArray(4096)
        fft.transform(real, imaginary); fft.transform(real, imaginary, inverse = true)
        assertTrue(real.indices.maxOf { abs(real[it] - original[it]) } < .00001f)
    }
    @Test fun modelStftContractReconstructsTheStereoMean() {
        val stft = MdxSpectrum()
        val left = FloatArray(MdxSpectrum.CHUNK) { (sin(2 * PI * 440 * it / 44100) * .15).toFloat() }
        val right = FloatArray(MdxSpectrum.CHUNK) { (sin(2 * PI * 660 * it / 44100) * .10).toFloat() }
        val tensor = FloatBuffer.allocate(MdxSpectrum.TENSOR_SIZE)
        stft.encode(left, right, tensor)
        val reconstructed = FloatArray(left.size)
        stft.decodeMono(tensor, reconstructed)
        var worst = 0f
        for (i in MdxSpectrum.TRIM until MdxSpectrum.CHUNK - MdxSpectrum.TRIM) worst = max(worst, abs(reconstructed[i] - (left[i] + right[i]) * .5f))
        assertTrue("STFT/inverse mismatch: $worst", worst < .0001f)
    }
    @Test fun modelContractHasThePinnedShapeAndHash() {
        assertEquals(4096, MdxSpectrum.FFT)
        assertEquals(4 * 2048 * 256, MdxSpectrum.TENSOR_SIZE)
        assertEquals(64, SplitModel.SHA256.length)
    }
}
