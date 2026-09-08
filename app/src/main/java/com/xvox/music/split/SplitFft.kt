package com.xvox.music.split

import kotlin.math.*

/** Fixed-size radix-2 FFT and MDX STFT contract. Pure Kotlin, no platform DSP/complex custom ops. */
class SplitFft(val size: Int = 4096) {
    private val bits = Integer.numberOfTrailingZeros(size)
    private val reversal = IntArray(size) { Integer.reverse(it) ushr (32 - bits) }
    private val cosine = FloatArray(size / 2) { cos(2 * PI * it / size).toFloat() }
    private val sine = FloatArray(size / 2) { sin(2 * PI * it / size).toFloat() }
    init { require(size > 0 && size and (size - 1) == 0) }
    fun transform(real: FloatArray, imaginary: FloatArray, inverse: Boolean = false) {
        for (i in 0 until size) {
            val j = reversal[i]
            if (j > i) {
                val a = real[i]; real[i] = real[j]; real[j] = a
                val b = imaginary[i]; imaginary[i] = imaginary[j]; imaginary[j] = b
            }
        }
        var length = 2
        while (length <= size) {
            val half = length / 2; val stride = size / length
            var start = 0
            while (start < size) {
                for (j in 0 until half) {
                    val k = j * stride; val c = cosine[k]; val s = if (inverse) sine[k] else -sine[k]
                    val b = start + j + half; val a = start + j
                    val re = real[b] * c - imaginary[b] * s; val im = real[b] * s + imaginary[b] * c
                    real[b] = real[a] - re; imaginary[b] = imaginary[a] - im
                    real[a] += re; imaginary[a] += im
                }
                start += length
            }
            length *= 2
        }
        if (inverse) for (i in 0 until size) { real[i] /= size; imaginary[i] /= size }
    }
}

class MdxSpectrum {
    companion object {
        const val FFT = 4096
        const val HOP = 1024
        const val FRAMES = 256
        const val BINS = 2048
        const val CHUNK = HOP * (FRAMES - 1)
        const val TRIM = FFT / 2
        const val OUTPUT = CHUNK - TRIM * 2
        const val TENSOR_SIZE = 4 * BINS * FRAMES
    }
    private val fft = SplitFft(FFT)
    private val window = FloatArray(FFT) { (.5 - .5 * cos(2 * PI * it / FFT)).toFloat() }
    private val real = FloatArray(FFT); private val imaginary = FloatArray(FFT)
    private val accumulation = FloatArray(CHUNK + FFT)
    private val weight = FloatArray(CHUNK + FFT).apply {
        repeat(FRAMES) { frame -> for (i in 0 until FFT) this[frame * HOP + i] += window[i] * window[i] }
    }
    fun encode(left: FloatArray, right: FloatArray, output: java.nio.FloatBuffer) {
        require(left.size == CHUNK && right.size == CHUNK)
        for (channel in 0..1) {
            val wave = if (channel == 0) left else right
            repeat(FRAMES) { frame ->
                for (i in 0 until FFT) {
                    var index = frame * HOP - TRIM + i
                    if (index < 0) index = -index
                    if (index >= CHUNK) index = 2 * CHUNK - 2 - index
                    real[i] = wave[index] * window[i]; imaginary[i] = 0f
                }
                fft.transform(real, imaginary)
                for (bin in 0 until BINS) {
                    output.put(((channel * 2) * BINS + bin) * FRAMES + frame, real[bin])
                    output.put(((channel * 2 + 1) * BINS + bin) * FRAMES + frame, imaginary[bin])
                }
            }
        }
        output.position(0)
    }
    /** The model predicts complex vocal spectra. Recover mono vocals; accompaniment is mixture minus vocals. */
    fun decodeMono(spectrum: java.nio.FloatBuffer, output: FloatArray) {
        require(output.size == CHUNK)
        accumulation.fill(0f)
        for (channel in 0..1) repeat(FRAMES) { frame ->
            real.fill(0f); imaginary.fill(0f)
            for (bin in 0 until BINS) {
                val re = spectrum.get(((channel * 2) * BINS + bin) * FRAMES + frame)
                val im = if (bin == 0) 0f else spectrum.get(((channel * 2 + 1) * BINS + bin) * FRAMES + frame)
                real[bin] = re; imaginary[bin] = im
                if (bin > 0) { real[FFT - bin] = re; imaginary[FFT - bin] = -im }
            }
            // Nyquist was omitted by the model; it remains zero in both directions.
            fft.transform(real, imaginary, inverse = true)
            for (i in 0 until FFT) accumulation[frame * HOP + i] += real[i] * window[i] * .5f
        }
        for (i in output.indices) {
            val at = i + TRIM
            output[i] = if (weight[at] > 1e-8) accumulation[at] / weight[at] else 0f
        }
    }
}
