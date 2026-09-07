package com.xvox.music.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Per-deck effects and smoothed mix gain; stereo output also enables spatial processing of mono songs. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class StereoBalanceAudioProcessor : BaseAudioProcessor() {
    val engine = XvoxDspEngine()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount !in 1..2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        engine.configure(inputAudioFormat.sampleRate)
        return AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, 2, C.ENCODING_PCM_16BIT)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val channels = inputAudioFormat.channelCount
        val frames = inputBuffer.remaining() / (2 * channels)
        if (frames == 0) return
        val output = replaceOutputBuffer(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames) {
            val l = inputBuffer.short / 32768f
            val r = if (channels == 2) inputBuffer.short / 32768f else l
            engine.process(l, r)
            output.putShort((engine.left * 32767).toInt().toShort())
            output.putShort((engine.right * 32767).toInt().toShort())
        }
        output.flip()
    }

    override fun onFlush() { engine.reset() }
    override fun onReset() { engine.reset() }
}
