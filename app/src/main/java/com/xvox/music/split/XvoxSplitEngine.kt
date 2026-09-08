package com.xvox.music.split

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** Real MDX-Net inference. Vocals are inferred; accompaniment is the mixture residual, not mid/side EQ. */
class XvoxSplitEngine(model: File) : AutoCloseable {
    private val env = OrtEnvironment.getEnvironment()
    private val options = OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(1); setInterOpNumThreads(1)
        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
    }
    private val session = env.createSession(model.absolutePath, options)
    private val transform = MdxSpectrum()
    private val tensorBuffer = ByteBuffer.allocateDirect(MdxSpectrum.TENSOR_SIZE * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val left = FloatArray(MdxSpectrum.CHUNK); private val right = FloatArray(MdxSpectrum.CHUNK)
    private val vocals = FloatArray(MdxSpectrum.CHUNK)

    suspend fun separate(source: DecodedSplitAudio, target: File, valid: () -> Boolean, progress: (Float) -> Unit) {
        SplitPcmReader(source).use { reader ->
            StemPairWriter(target).use { writer ->
                var offset = 0L
                val overlap = 8192
                val previous = FloatArray(overlap)
                var hasPrevious = false
                while (offset < reader.frames) {
                    coroutineContext.ensureActive(); if (!valid()) throw CancellationException("Track cancelled")
                    if (target.parentFile!!.usableSpace < 96L * 1024 * 1024) error("Storage is low; separation paused")
                    reader.read(offset - MdxSpectrum.TRIM, left, right)
                    transform.encode(left, right, tensorBuffer)
                    OnnxTensor.createTensor(env, tensorBuffer, longArrayOf(1, 4, 2048, 256)).use { tensor ->
                        session.run(mapOf("input" to tensor)).use { result ->
                            val predicted = result[0] as OnnxTensor
                            transform.decodeMono(predicted.floatBuffer, vocals)
                        }
                    }
                    coroutineContext.ensureActive(); if (!valid()) throw CancellationException("Track cancelled")
                    val remaining = reader.frames - offset
                    val last = remaining <= MdxSpectrum.OUTPUT
                    val count = if (last) remaining.toInt() else MdxSpectrum.OUTPUT - overlap
                    for (i in 0 until count) {
                        val at = i + MdxSpectrum.TRIM
                        val mixFraction = if (hasPrevious && i < overlap) (.5 - .5 * kotlin.math.cos(Math.PI * i / overlap)).toFloat() else 1f
                        val voice = if (hasPrevious && i < overlap) previous[i] * (1 - mixFraction) + vocals[at] * mixFraction else vocals[at]
                        require(voice.isFinite()) { "Model produced invalid audio" }
                        val mix = (left[at] + right[at]) * .5f
                        writer.write(mix - voice, voice)
                    }
                    if (!last) { vocals.copyInto(previous, 0, MdxSpectrum.TRIM + count, MdxSpectrum.TRIM + count + overlap); hasPrevious = true }
                    offset += count
                    progress(offset.toFloat() / reader.frames)
                    yield()
                }
            }
        }
    }
    override fun close() { session.close(); options.close() }

    companion object {
        suspend fun ensureModel(context: Context, allowMetered: Boolean, valid: () -> Boolean): File = withContext(Dispatchers.IO) {
            val target = XvoxSplitRepository.modelFile()
            fun digest(file: File): String {
                val md = MessageDigest.getInstance("SHA-256")
                file.inputStream().buffered().use { input ->
                    val block = ByteArray(128 * 1024)
                    while (true) { val n = input.read(block); if (n < 0) break; if (!valid()) throw CancellationException(); md.update(block, 0, n) }
                }
                return md.digest().joinToString("") { "%02x".format(it) }
            }
            if (target.length() == SplitModel.BYTES && digest(target) == SplitModel.SHA256) return@withContext target
            XvoxSplitRepository.modelNeedsDownload()
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            check(allowMetered || !connectivity.isActiveNetworkMetered) { "Connect to Wi-Fi or allow mobile data for the 28.3 MB model download" }
            val temporary = File(target.parentFile, target.name + ".${System.nanoTime()}.part")
            val connection = URL(SplitModel.URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 20000; connection.readTimeout = 20000; connection.instanceFollowRedirects = true
            try {
                check(connection.responseCode in 200..299) { "Model download failed: HTTP ${connection.responseCode}" }
                var bytes = 0L
                connection.inputStream.use { input -> temporary.outputStream().buffered().use { output ->
                    val block = ByteArray(128 * 1024); var last = 0L
                    while (true) {
                        coroutineContext.ensureActive(); if (!valid()) throw CancellationException()
                        val n = input.read(block); if (n < 0) break
                        bytes += n; check(bytes <= SplitModel.BYTES) { "Unexpected model size" }
                        output.write(block, 0, n)
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - last > 150) { last = now; XvoxSplitRepository.modelProgress(bytes.toFloat() / SplitModel.BYTES, "Downloading separation model") }
                    }
                } }
                check(temporary.length() == SplitModel.BYTES && digest(temporary) == SplitModel.SHA256) { "Model checksum mismatch; download rejected" }
                check(temporary.renameTo(target)) { "Could not install model" }
                target
            } finally { connection.disconnect(); temporary.delete() }
        }
    }
}
