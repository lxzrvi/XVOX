package com.xvox.music.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StereoBalanceAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var balance: Float = 0f // -1.0f (Left only) .. 0.0f (Center) .. 1.0f (Right only)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return if (inputAudioFormat.channelCount == 2) {
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val b = balance
        val leftGain: Float
        val rightGain: Float

        if (b < 0f) {
            leftGain = 1.0f
            rightGain = (1.0f + b).coerceIn(0f, 1f)
        } else if (b > 0f) {
            leftGain = (1.0f - b).coerceIn(0f, 1f)
            rightGain = 1.0f
        } else {
            leftGain = 1.0f
            rightGain = 1.0f
        }

        val buffer = replaceOutputBuffer(remaining)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        while (inputBuffer.remaining() >= 4) {
            val leftSample = inputBuffer.short
            val rightSample = inputBuffer.short

            val processedLeft = (leftSample * leftGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            val processedRight = (rightSample * rightGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            buffer.putShort(processedLeft)
            buffer.putShort(processedRight)
        }

        buffer.flip()
    }
}
