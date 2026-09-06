package com.xvox.music.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

class StereoBalanceAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var balance: Float = 0f // -1.0f (Left only) .. 0.0f (Center) .. 1.0f (Right only)

    @Volatile
    var surround3dEnabled: Boolean = false

    private var surroundPhase: Double = 0.0

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

        val buffer = replaceOutputBuffer(remaining)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val sampleRate = if (inputAudioFormat.sampleRate > 0) inputAudioFormat.sampleRate else 44100
        val is3d = surround3dEnabled
        val phaseStep = (2.0 * Math.PI) / (sampleRate * 8.0) // complete 8-second slow spatial orbit

        var currentPhase = surroundPhase

        while (inputBuffer.remaining() >= 4) {
            val leftSample = inputBuffer.short
            val rightSample = inputBuffer.short

            val leftGain: Float
            val rightGain: Float

            if (is3d) {
                val pan = sin(currentPhase).toFloat() * 0.70f
                val angle = (pan + 1f) * (Math.PI / 4.0)
                leftGain = cos(angle).toFloat() * 1.15f
                rightGain = sin(angle).toFloat() * 1.15f
                currentPhase += phaseStep
                if (currentPhase > 2.0 * Math.PI) {
                    currentPhase -= 2.0 * Math.PI
                }
            } else {
                val b = balance
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
            }

            val processedLeft = (leftSample * leftGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            val processedRight = (rightSample * rightGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            buffer.putShort(processedLeft)
            buffer.putShort(processedRight)
        }

        surroundPhase = currentPhase
        buffer.flip()
    }
}
