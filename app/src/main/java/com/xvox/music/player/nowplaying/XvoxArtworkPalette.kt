package com.xvox.music.player.nowplaying

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.xvox.music.artwork.XvoxArtworkCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class XvoxArtworkPaletteLoader(
    context: Context
) {
    private val appContext = context.applicationContext
    private val cache = ConcurrentHashMap<String, Color>()

    suspend fun load(uri: Uri?): Color {
        if (uri == null) return fallback()
        val key = uri.toString()
        cache[key]?.let { return it }

        val result = withContext(Dispatchers.IO) {
            // First check memory cache
            val cached = XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_160")
                ?: XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_512")
            if (cached != null) {
                return@withContext extract(cached)
            }

            runCatching {
                decode(uri)
            }.getOrNull()?.let { bitmap ->
                try {
                    extract(bitmap)
                } finally {
                    bitmap.recycle()
                }
            } ?: fallback()
        }

        cache[key] = result
        return result
    }

    private fun decode(uri: Uri): Bitmap? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(appContext.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val maxSide = max(info.size.width, info.size.height)
                    decoder.setTargetSampleSize(max(1, maxSide / 64))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                appContext.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeStream(stream, null, options)
                }
            }
        }.getOrNull()
    }

    private fun extract(bitmap: Bitmap): Color {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return fallback()

        val sampleStep = max(1, min(width, height) / 32)
        val hsv = FloatArray(3)
        var maxScore = -1f
        var bestColor = 0

        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = android.graphics.Color.alpha(pixel)
                if (alpha < 128) continue

                android.graphics.Color.colorToHSV(pixel, hsv)
                val hue = hsv[0]
                val saturation = hsv[1]
                val value = hsv[2]

                // Filter out washed out whites, deep pitch blacks, and low-saturation greys
                if (value in 0.15f..0.92f && saturation >= 0.20f) {
                    // Score biased towards rich vibrant musical album colors
                    val score = saturation * 1.6f + (1.0f - abs(value - 0.55f)) * 1.1f
                    if (score > maxScore) {
                        maxScore = score
                        bestColor = pixel
                    }
                }
            }
        }

        if (bestColor == 0) {
            // Secondary fallback: average non-black pixels
            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var count = 0
            for (y in 0 until height step sampleStep) {
                for (x in 0 until width step sampleStep) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    val lum = (r + g + b) / 3
                    if (lum in 20..235) {
                        totalR += r
                        totalG += g
                        totalB += b
                        count++
                    }
                }
            }
            if (count > 0) {
                val avgR = (totalR / count).toInt()
                val avgG = (totalG / count).toInt()
                val avgB = (totalB / count).toInt()
                return normalize(Color(avgR, avgG, avgB))
            }
            return fallback()
        }

        val r = android.graphics.Color.red(bestColor)
        val g = android.graphics.Color.green(bestColor)
        val b = android.graphics.Color.blue(bestColor)

        return normalize(Color(r, g, b))
    }

    private fun normalize(source: Color): Color {
        val lum = source.luminance()
        // Ensure perfect contrast for NowPlaying backdrop
        val targetLum = when {
            lum < 0.22f -> 0.32f
            lum > 0.65f -> 0.48f
            else -> lum
        }

        if (abs(targetLum - lum) < 0.02f) return source

        val factor = if (targetLum > lum) {
            (targetLum - lum) / (1f - lum).coerceAtLeast(0.01f)
        } else {
            targetLum / lum.coerceAtLeast(0.01f)
        }

        return if (targetLum > lum) {
            Color(
                red = source.red + (1f - source.red) * factor * 0.7f,
                green = source.green + (1f - source.green) * factor * 0.7f,
                blue = source.blue + (1f - source.blue) * factor * 0.7f,
                alpha = 1f
            )
        } else {
            Color(
                red = (source.red * factor).coerceIn(0f, 1f),
                green = (source.green * factor).coerceIn(0f, 1f),
                blue = (source.blue * factor).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
    }

    private fun fallback(): Color {
        return Color(0xFF383842)
    }
}
