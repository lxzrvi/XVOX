package com.xvox.music.player.playback

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.exp

/** Optional, bounded, on-device analysis. One background decoder; never awaited by playback. */
class XvoxBeatAnalyzer(context: Context) {
    private val app = context.applicationContext
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)
    private val cache = object : LinkedHashMap<String, TrackBlendProfile?>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, TrackBlendProfile?>): Boolean = size > 96
    }

    suspend fun analyze(uri: Uri, startMs: Long, windowMs: Long = 16000): TrackBlendProfile? = withContext(dispatcher) {
        val key = "$uri:$startMs:$windowMs"
        if (cache.containsKey(key)) return@withContext cache[key]
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val tid = Process.myTid()
        val oldPriority = Process.getThreadPriority(tid)
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            extractor.setDataSource(app, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return@withContext null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext null
            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            val energies = DoubleArray((windowMs / 10).toInt() + 1)
            val counts = IntArray(energies.size)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var low = 0.0
            var alpha = 1 - exp(-2 * PI * 220 / rate)
            val deadline = System.nanoTime() + 4_000_000_000L
            while (!outputDone && System.nanoTime() < deadline) {
                coroutineContext.ensureActive()
                if (!inputDone) {
                    val index = decoder.dequeueInputBuffer(1000)
                    if (index >= 0) {
                        val buffer = decoder.getInputBuffer(index) ?: break
                        buffer.clear()
                        val sampleUs = extractor.sampleTime
                        val size = if (sampleUs < 0 || sampleUs >= (startMs + windowMs) * 1000L) -1 else extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(index, 0, size, sampleUs, 0)
                            extractor.advance()
                        }
                    }
                }
                val index = decoder.dequeueOutputBuffer(info, 1000)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val output = decoder.outputFormat
                    rate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    encoding = if (output.containsKey(MediaFormat.KEY_PCM_ENCODING)) output.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        else AudioFormat.ENCODING_PCM_16BIT
                    alpha = 1 - exp(-2 * PI * 220 / rate)
                } else if (index >= 0) {
                    try {
                        val output = decoder.getOutputBuffer(index)
                        if (output != null && info.size > 0 && channels > 0) {
                            output.position(info.offset); output.limit(info.offset + info.size); output.order(ByteOrder.LITTLE_ENDIAN)
                            val bytes = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                            if (encoding != AudioFormat.ENCODING_PCM_FLOAT && encoding != AudioFormat.ENCODING_PCM_16BIT) break
                            var frame = 0L
                            while (output.remaining() >= bytes * channels) {
                                var mono = 0.0
                                repeat(channels) { mono += if (bytes == 4) output.float.toDouble() else output.short / 32768.0 }
                                mono /= channels
                                val positionUs = info.presentationTimeUs + frame * 1_000_000L / rate
                                val bin = ((positionUs - startMs * 1000L) / 10000L).toInt()
                                if (!mono.isFinite()) low = 0.0 else low += (mono - low) * alpha
                                if (positionUs >= startMs * 1000L && bin in energies.indices && mono.isFinite()) {
                                    energies[bin] += .75 * low * low + .25 * mono * mono
                                    counts[bin]++
                                }
                                frame++
                            }
                        }
                        outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    } finally { decoder.releaseOutputBuffer(index, false) }
                }
            }
            coroutineContext.ensureActive()
            val measured = DoubleArray(energies.size) { if (counts[it] > 0) energies[it] / counts[it] else 0.0 }
            TrackBlendProfile(BeatAlignment.detect(measured, startMs), EnergyEnvelope.fromEnergy(measured, startMs)).also { cache[key] = it }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            cache[key] = null
            null // Missing codecs / permissions / weak rhythm must not interrupt the normal crossfade.
        } finally {
            runCatching { codec?.stop() }; runCatching { codec?.release() }
            extractor.release()
            runCatching { Process.setThreadPriority(oldPriority) }
        }
    }
}
