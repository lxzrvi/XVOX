package com.xvox.music.core.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Shared crop source and exporter. Static images retain the established JPEG route; animated GIFs
 * are sampled, cropped frame-by-frame, locally quantized, and written back as a looping GIF.
 */
internal object XvoxGifCrop {
    private const val MAX_INPUT_BYTES = 36 * 1024 * 1024
    private const val MAX_SOURCE_PIXELS = 18_000_000
    private const val STATIC_LONG_EDGE = 1600
    private const val GIF_LONG_EDGE = 720
    private const val MAX_GIF_FRAMES = 90

    sealed class CropSource(open val width: Int, open val height: Int) {
        data class Static(val bitmap: Bitmap) : CropSource(bitmap.width, bitmap.height)
        data class Gif(
            val bytes: ByteArray,
            val movie: Movie,
            val firstFrame: Bitmap
        ) : CropSource(movie.width(), movie.height())
    }

    suspend fun loadCropSource(context: Context, uri: Uri): CropSource? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use(::readBounded)
                ?: return@runCatching null
            if (bytes.isEmpty()) return@runCatching null
            if (isGif(bytes)) {
                val movie = Movie.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
                val width = movie.width()
                val height = movie.height()
                if (!isSafeDimensions(width, height)) return@runCatching null
                val first = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                Canvas(first).apply {
                    drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                    movie.setTime(0)
                    movie.draw(this, 0f, 0f)
                }
                CropSource.Gif(bytes, movie, first)
            } else {
                decodeStatic(bytes)?.let(CropSource::Static)
            }
        }.getOrNull()
    }

    suspend fun exportCrop(context: Context, source: CropSource, crop: RectF, aspect: Float): Uri? =
        withContext(Dispatchers.IO) {
            runCatching {
                // Crop results may be assigned directly to a profile/header before another layer
                // persists them, so keep them in app files rather than an evictable cache folder.
                val destination = File(context.filesDir, "xvox-crops").also { it.mkdirs() }
                when (source) {
                    is CropSource.Static -> {
                        val file = File(destination, "xvox-crop-${System.nanoTime()}.jpg")
                        val output = cropStatic(source.bitmap, crop, aspect)
                        FileOutputStream(file).use { stream ->
                            check(output.compress(Bitmap.CompressFormat.JPEG, 95, stream))
                        }
                        output.recycle()
                        Uri.fromFile(file)
                    }
                    is CropSource.Gif -> {
                        val file = File(destination, "xvox-crop-${System.nanoTime()}.gif")
                        cropGif(source, crop, aspect, file)
                        Uri.fromFile(file)
                    }
                }
            }.getOrNull()
        }

    private fun readBounded(input: java.io.InputStream): ByteArray {
        val out = ByteArrayOutputStream(min(MAX_INPUT_BYTES, 256 * 1024))
        val buffer = ByteArray(32 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_INPUT_BYTES) throw IllegalArgumentException("Image is too large")
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun isGif(bytes: ByteArray): Boolean = bytes.size >= 6 &&
        bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() &&
            bytes[3] == '8'.code.toByte() && (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
            bytes[5] == 'a'.code.toByte()

    private fun decodeStatic(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (!isSafeDimensions(bounds.outWidth, bounds.outHeight)) return null
        var sample = 1
        while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 2400) sample *= 2
        val decode = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decode)
    }

    private fun isSafeDimensions(width: Int, height: Int): Boolean =
        width > 0 && height > 0 && width.toLong() * height.toLong() <= MAX_SOURCE_PIXELS

    private fun cropStatic(bitmap: Bitmap, crop: RectF, aspect: Float): Bitmap {
        val source = normalizedCrop(bitmap.width, bitmap.height, crop)
        val (width, height) = outputSize(aspect, STATIC_LONG_EDGE)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { output ->
            Canvas(output).drawBitmap(
                bitmap,
                source.toAndroidRect(),
                Rect(0, 0, width, height),
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
            )
        }
    }

    private fun cropGif(source: CropSource.Gif, crop: RectF, aspect: Float, file: File) {
        val bounds = normalizedCrop(source.width, source.height, crop)
        val (width, height) = outputSize(aspect, GIF_LONG_EDGE)
        val sourceDuration = source.movie.duration()
        val duration = sourceDuration.takeIf { it > 0 } ?: 100
        // Sampling rather than decoding unlimited source frames bounds both export time and output
        // size while keeping a perceptibly animated result for normal GIFs. A zero-duration GIF
        // is effectively still artwork, so emit one frame rather than manufacturing a loop.
        val frameCount = if (sourceDuration > 0) {
            ceil(duration / 66.0).toInt().coerceIn(1, MAX_GIF_FRAMES)
        } else 1
        val delayCs = ((duration / frameCount) / 10).coerceIn(4, 40)
        FileOutputStream(file).use { stream ->
            val writer = GifWriter(stream)
            writer.header(width, height)
            repeat(frameCount) { index ->
                val moment = ((index.toLong() * duration) / frameCount).toInt()
                val frame = renderGifFrame(source.movie, source.width, source.height, bounds, width, height, moment)
                val quantized = GifQuantizer.quantize(frame)
                writer.frame(quantized.indices, quantized.palette, delayCs, quantized.hasTransparency)
                frame.recycle()
            }
            writer.finish()
        }
    }

    private fun renderGifFrame(
        movie: Movie,
        sourceWidth: Int,
        sourceHeight: Int,
        crop: RectF,
        outputWidth: Int,
        outputHeight: Int,
        timeMs: Int
    ): Bitmap {
        val full = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888)
        Canvas(full).apply {
            drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            movie.setTime(timeMs)
            movie.draw(this, 0f, 0f)
        }
        val result = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(
            full,
            crop.toAndroidRect(),
            Rect(0, 0, outputWidth, outputHeight),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        )
        full.recycle()
        return result
    }

    private fun normalizedCrop(width: Int, height: Int, requested: RectF): RectF {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        var cropWidth = requested.width().coerceIn(1f, safeWidth.toFloat())
        var cropHeight = requested.height().coerceIn(1f, safeHeight.toFloat())
        // A malformed/singular gesture rectangle must never crash export or create a zero-pixel
        // Rect. The dialog normally preserves aspect, this only guards restored state.
        if (!cropWidth.isFinite()) cropWidth = safeWidth.toFloat()
        if (!cropHeight.isFinite()) cropHeight = safeHeight.toFloat()
        val left = requested.left.coerceIn(0f, safeWidth - cropWidth)
        val top = requested.top.coerceIn(0f, safeHeight - cropHeight)
        return RectF(left, top, left + cropWidth, top + cropHeight)
    }

    private fun RectF.toAndroidRect(): Rect {
        val left = left.toInt().coerceAtLeast(0)
        val top = top.toInt().coerceAtLeast(0)
        val right = ceil(right.toDouble()).toInt().coerceAtLeast(left + 1)
        val bottom = ceil(bottom.toDouble()).toInt().coerceAtLeast(top + 1)
        return Rect(left, top, right, bottom)
    }

    private fun outputSize(aspect: Float, longEdge: Int): Pair<Int, Int> {
        val safeAspect = aspect.takeIf { it.isFinite() && it > .01f } ?: 1f
        return if (safeAspect >= 1f) {
            longEdge to max(1, (longEdge / safeAspect).toInt())
        } else {
            max(1, (longEdge * safeAspect).toInt()) to longEdge
        }
    }

    private data class QuantizedFrame(val indices: ByteArray, val palette: IntArray, val hasTransparency: Boolean)

    /** 3-3-2 quantization is deterministic, bounded, and maps exactly to one local 256-colour table. */
    private object GifQuantizer {
        fun quantize(bitmap: Bitmap): QuantizedFrame {
            // Palette slot 0 is reserved for transparent source pixels. The remaining 255 slots
            // cover the 3-3-2 cube, so GIFs with alpha keep their cut-outs instead of flattening
            // them against an arbitrary black background.
            val palette = IntArray(256)
            palette[0] = Color.TRANSPARENT
            for (index in 1..255) {
                val bucket = ((index - 1) * 255 / 254).coerceIn(0, 255)
                val red = (bucket shr 5) and 0x07
                val green = (bucket shr 2) and 0x07
                val blue = bucket and 0x03
                palette[index] = Color.rgb(red * 255 / 7, green * 255 / 7, blue * 255 / 3)
            }
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val indices = ByteArray(pixels.size)
            var hasTransparency = false
            pixels.forEachIndexed { i, pixel ->
                if (Color.alpha(pixel) < 128) {
                    indices[i] = 0
                    hasTransparency = true
                } else {
                    val bucket = ((Color.red(pixel) shr 5) shl 5) or
                        ((Color.green(pixel) shr 5) shl 2) or
                        (Color.blue(pixel) shr 6)
                    indices[i] = (1 + bucket * 254 / 255).toByte()
                }
            }
            return QuantizedFrame(indices, palette, hasTransparency)
        }
    }

    /** Minimal GIF89a writer using a local palette per frame, so animated colours remain accurate. */
    private class GifWriter(private val output: FileOutputStream) {
        fun header(width: Int, height: Int) {
            frameWidth = width
            frameHeight = height
            output.write("GIF89a".encodeToByteArray())
            word(width)
            word(height)
            output.write(0x70) // 8-bit colour resolution; each frame supplies a local table.
            output.write(0)
            output.write(0)
            // Netscape application extension: repeat indefinitely like the original animation.
            output.write(byteArrayOf(0x21, 0xFF.toByte(), 0x0B))
            output.write("NETSCAPE2.0".encodeToByteArray())
            output.write(byteArrayOf(0x03, 0x01, 0x00, 0x00, 0x00))
        }

        fun frame(indices: ByteArray, palette: IntArray, delayCs: Int, hasTransparency: Boolean) {
            // Disposal=restore background; transparent index 0 is enabled only when a sampled
            // source frame genuinely has alpha so opaque GIFs retain their entire colour table.
            output.write(byteArrayOf(0x21, 0xF9.toByte(), 0x04, (if (hasTransparency) 0x05 else 0x04).toByte()))
            word(delayCs)
            output.write(byteArrayOf(0x00, 0x00))
            output.write(0x2C)
            word(0); word(0)
            // Width/height are recovered from the pixel count by callers only after header; GIF's
            // image descriptor must explicitly carry them, so header's logical size is retained.
            word(frameWidth)
            word(frameHeight)
            output.write(0x87) // local colour table, 256 entries
            palette.forEach { color ->
                output.write(Color.red(color)); output.write(Color.green(color)); output.write(Color.blue(color))
            }
            output.write(8) // minimum LZW code size
            val packed = lzw(indices)
            var offset = 0
            while (offset < packed.size) {
                val count = min(255, packed.size - offset)
                output.write(count)
                output.write(packed, offset, count)
                offset += count
            }
            output.write(0)
        }

        private var frameWidth = 0
        private var frameHeight = 0

        // Capture logical size when the header is written. This avoids repeating it through every
        // frame call and keeps the writer's surface deliberately small.
        private fun word(value: Int) {
            output.write(value and 0xFF)
            output.write((value shr 8) and 0xFF)
        }

        fun finish() { output.write(0x3B) }

        private fun lzw(indices: ByteArray): ByteArray {
            val clear = 256
            val end = 257
            var nextCode = 258
            var codeSize = 9
            val dictionary = HashMap<Int, Int>(4096)
            val bits = ByteArrayOutputStream()
            var bitBuffer = 0
            var bitCount = 0
            fun writeCode(code: Int) {
                bitBuffer = bitBuffer or (code shl bitCount)
                bitCount += codeSize
                while (bitCount >= 8) {
                    bits.write(bitBuffer and 0xFF)
                    bitBuffer = bitBuffer ushr 8
                    bitCount -= 8
                }
            }
            if (indices.isEmpty()) {
                writeCode(clear); writeCode(end)
            } else {
                writeCode(clear)
                var prefix = indices[0].toInt() and 0xFF
                for (position in 1 until indices.size) {
                    val suffix = indices[position].toInt() and 0xFF
                    val key = (prefix shl 8) or suffix
                    val known = dictionary[key]
                    if (known != null) {
                        prefix = known
                    } else {
                        writeCode(prefix)
                        if (nextCode < 4096) {
                            dictionary[key] = nextCode++
                            if (nextCode == (1 shl codeSize) && codeSize < 12) codeSize++
                        } else {
                            writeCode(clear)
                            dictionary.clear()
                            nextCode = 258
                            codeSize = 9
                        }
                        prefix = suffix
                    }
                }
                writeCode(prefix)
                writeCode(end)
            }
            if (bitCount > 0) bits.write(bitBuffer and 0xFF)
            return bits.toByteArray()
        }
    }
}
