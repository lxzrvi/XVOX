package com.xvox.music.split

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.*

data class DecodedSplitAudio(val file: File, val sampleRate: Int, val frames: Long)

object SplitAudioFiles {
    suspend fun decode(context: Context, item: SplitTrack, target: File, valid: () -> Boolean,
        progress: (Float) -> Unit): DecodedSplitAudio = withContext(Dispatchers.IO) {
        require(android.net.Uri.parse(item.source).scheme in setOf("content", "file")) { "Only local audio files are supported" }
        val extractor = MediaExtractor(); var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, android.net.Uri.parse(item.source), null)
            val track = (0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: error("No audio track found")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            require(channels in 1..2) { "XvoxSplit supports mono and stereo files" }
            require(rate in 8000..384000) { "Unsupported sample rate" }
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = decoder; decoder.configure(format, null, null, 0); decoder.start()
            val info = MediaCodec.BufferInfo(); var inputDone = false; var outputDone = false; var frames = 0L
            var encoding = AudioFormat.ENCODING_PCM_16BIT; var lastProgress = 0L; var emptyPolls = 0
            target.outputStream().buffered(256 * 1024).use { stream ->
                while (!outputDone) {
                    coroutineContext.ensureActive(); if (!valid()) throw CancellationException("Track cancelled")
                    if (!inputDone) {
                        val index = decoder.dequeueInputBuffer(2000)
                        if (index >= 0) {
                            val buffer = decoder.getInputBuffer(index)!!; buffer.clear()
                            val size = extractor.readSampleData(buffer, 0)
                            if (size < 0) { decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true }
                            else { decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0); extractor.advance() }
                        }
                    }
                    val index = decoder.dequeueOutputBuffer(info, 2000)
                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val output = decoder.outputFormat
                        rate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE); channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        require(channels in 1..2) { "Only mono/stereo source separation is supported" }
                        encoding = if (output.containsKey(MediaFormat.KEY_PCM_ENCODING)) output.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                    } else if (index >= 0) {
                        emptyPolls = 0
                        try {
                            val data = decoder.getOutputBuffer(index)
                            if (data != null && info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                data.position(info.offset); data.limit(info.offset + info.size); data.order(ByteOrder.LITTLE_ENDIAN)
                                val bytes = when (encoding) { AudioFormat.ENCODING_PCM_FLOAT, AudioFormat.ENCODING_PCM_32BIT -> 4; AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3; else -> 2 }
                                val count = data.remaining() / (bytes * channels)
                                val output = ByteBuffer.allocate(count * 8).order(ByteOrder.LITTLE_ENDIAN)
                                fun sample(): Float = when (encoding) {
                                    AudioFormat.ENCODING_PCM_FLOAT -> data.float.takeIf { it.isFinite() } ?: 0f
                                    AudioFormat.ENCODING_PCM_32BIT -> data.int / 2147483648f
                                    AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
                                        val value = (data.get().toInt() and 255) or ((data.get().toInt() and 255) shl 8) or (data.get().toInt() shl 16)
                                        value / 8388608f
                                    }
                                    else -> data.short / 32768f
                                }
                                repeat(count) { val left = sample(); val right = if (channels == 2) sample() else left; output.putFloat(left); output.putFloat(right) }
                                stream.write(output.array()); frames += count
                            }
                            outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        } finally { decoder.releaseOutputBuffer(index, false) }
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastProgress > 150) {
                            lastProgress = now
                            if (target.parentFile!!.usableSpace < 96L * 1024 * 1024) error("Not enough free storage to prepare this track")
                            progress(if (item.duration > 0) (info.presentationTimeUs / 1000f / item.duration).coerceIn(0f, 1f) else 0f)
                        }
                    } else if (++emptyPolls > 15000) error("Audio decoder stopped responding")
                }
            }
            require(frames > 0) { "Audio file is empty" }
            DecodedSplitAudio(target, rate, frames)
        } finally {
            runCatching { codec?.stop() }; runCatching { codec?.release() }; extractor.release()
        }
    }
}

/** Bounded file windows, with a windowed-sinc resampler to the model's 44.1 kHz input. */
class SplitPcmReader(private val source: DecodedSplitAudio) : AutoCloseable {
    private val file = RandomAccessFile(source.file, "r")
    val frames: Long = (source.frames.toDouble() * SplitModel.SAMPLE_RATE / source.sampleRate).roundToLong()
    fun read(start: Long, left: FloatArray, right: FloatArray) {
        left.fill(0f); right.fill(0f)
        val ratio = source.sampleRate.toDouble() / SplitModel.SAMPLE_RATE
        val first = max(0L, floor(start * ratio).toLong() - 18)
        val last = min(source.frames, ceil((start + left.size) * ratio).toLong() + 18)
        if (last <= first) return
        val bytes = ByteArray(((last - first) * 8).toInt()); file.seek(first * 8); file.readFully(bytes)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val cutoff = min(1.0, SplitModel.SAMPLE_RATE.toDouble() / source.sampleRate) * .96
        for (i in left.indices) {
            val outputFrame = start + i
            if (outputFrame !in 0 until frames) continue
            val position = outputFrame * ratio
            val base = floor(position).toLong()
            if (source.sampleRate == SplitModel.SAMPLE_RATE) {
                val offset = ((base - first) * 2).toInt()
                left[i] = buffer.get(offset); right[i] = buffer.get(offset + 1)
                continue
            }
            var l = 0.0; var r = 0.0; var sum = 0.0
            for (tap in -15..16) {
                val at = base + tap
                if (at < first || at >= last) continue
                val distance = at - position
                val x = PI * distance * cutoff
                val sinc = if (abs(x) < 1e-8) 1.0 else sin(x) / x
                val window = .5 + .5 * cos(PI * distance / 17)
                val weight = sinc * window * cutoff
                val offset = ((at - first) * 2).toInt()
                l += buffer.get(offset) * weight; r += buffer.get(offset + 1) * weight; sum += weight
            }
            if (abs(sum) > 1e-8) { left[i] = (l / sum).toFloat(); right[i] = (r / sum).toFloat() }
        }
    }
    override fun close() { file.close() }
}

class StemPairWriter(target: File) : AutoCloseable {
    private val file = RandomAccessFile(target, "rw").apply { setLength(0); write(ByteArray(44)) }
    private var frames = 0L
    fun write(music: Float, voice: Float) {
        // Store each real stem at half gain; the renderer restores this before its peak guard.
        val l = (music * .5f * 32767).roundToInt().coerceIn(-32768, 32767)
        val r = (voice * .5f * 32767).roundToInt().coerceIn(-32768, 32767)
        pending.putShort(l.toShort()); pending.putShort(r.toShort()); frames++
        if (pending.remaining() < 4) flush()
    }
    private val pending = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN)
    private fun flush() { file.write(pending.array(), 0, pending.position()); pending.clear() }
    override fun close() {
        flush()
        val bytes = frames * 4
        require(bytes < 0xFFFFFFFFL - 36) { "Track is too large for a WAV cache file" }
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt((36 + bytes).toInt()).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(2).putInt(SplitModel.SAMPLE_RATE).putInt(SplitModel.SAMPLE_RATE * 4)
            .putShort(4).putShort(16).put("data".toByteArray()).putInt(bytes.toInt())
        file.seek(0); file.write(header.array()); file.close()
    }
}
