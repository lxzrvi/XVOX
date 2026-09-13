package com.xvox.music.player.nowplaying

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.xvox.music.artwork.XvoxArtworkCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class XvoxArtworkPaletteLoader(
    context: Context
) {
    private val appContext = context.applicationContext
    companion object {
        private val cache = android.util.LruCache<String, Color>(512)
    }

    fun fastEstimate(uri: Uri?, songKey: String = ""): Color {
        val key = uri?.toString()?.takeIf { it.isNotBlank() } ?: songKey
        if (key.isBlank()) return fallback(songKey)
        cache[key]?.let { return it }

        if (uri != null) {
            val cached = XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_256")
                ?: XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_512")
                ?: XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_1024")
            if (cached != null) {
                val extracted = extract(cached)
                cache.put(key, extracted)
                return extracted
            }
        }
        val fb = fallback(songKey)
        cache.put(key, fb)
        return fb
    }

    suspend fun load(uri: Uri?, songKey: String = ""): Color {
        val key = uri?.toString()?.takeIf { it.isNotBlank() } ?: songKey
        if (key.isBlank()) return fallback(songKey)
        cache[key]?.let { return it }

        val result = withContext(Dispatchers.IO) {
            // First check memory/disk artwork cache
            if (uri != null) {
                val cached = XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_1024")
                    ?: XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_512")
                    ?: XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_256")
                if (cached != null) {
                    return@withContext extract(cached)
                }
            }

            // Decode bitmap via Coil for reliable image loading
            if (uri != null) {
                runCatching {
                    val loader = SingletonImageLoader.get(appContext)
                    val req = ImageRequest.Builder(appContext)
                        .data(uri)
                        .size(160, 160)
                        .allowHardware(false)
                        .build()
                    val res = loader.execute(req)
                    (res.image as? coil3.BitmapImage)?.bitmap
                }.getOrNull()?.let { bitmap ->
                    return@withContext extract(bitmap)
                }
            }

            fallback(songKey)
        }

        cache.put(key, result)
        return result
    }

    private fun extract(bitmap: Bitmap): Color {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return fallback("")

        val sampleStep = max(1, min(width, height) / 48)
        val hueBins = 36 // 10 degrees each
        val binCounts = IntArray(hueBins)
        val binRed = LongArray(hueBins)
        val binGreen = LongArray(hueBins)
        val binBlue = LongArray(hueBins)
        val binScores = FloatArray(hueBins)

        var totalColorfulPixels = 0
        var totalPixels = 0
        val hsv = FloatArray(3)

        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) < 128) continue
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                totalPixels++

                android.graphics.Color.colorToHSV(pixel, hsv)
                val h = hsv[0]
                val s = hsv[1]
                val v = hsv[2]

                // Filter extreme black/white for colorful hue search
                if (v < 0.06f || v > 0.98f) continue

                // Check colorfulness
                if (s >= 0.10f) {
                    totalColorfulPixels++
                    val bin = ((h / 360f) * hueBins).toInt().coerceIn(0, hueBins - 1)
                    binCounts[bin]++
                    binRed[bin] += r.toLong()
                    binGreen[bin] += g.toLong()
                    binBlue[bin] += b.toLong()
                    // Weight colorful pixels
                    binScores[bin] += (s * 4.0f + v * 1.5f)
                }
            }
        }

        // For Black & White / Grayscale / Dark covers: return sleek soft light gray so text and cover shine
        if (totalPixels > 0 && totalColorfulPixels.toFloat() / totalPixels < 0.08f) {
            return Color(0xFF42424E)
        }

        var bestBin = -1
        var bestScore = -1f
        for (i in 0 until hueBins) {
            if (binCounts[i] > 0 && binScores[i] > bestScore) {
                bestScore = binScores[i]
                bestBin = i
            }
        }

        if (bestBin < 0 || binCounts[bestBin] == 0) return Color(0xFF42424E)
        val count = binCounts[bestBin]
        val avgR = (binRed[bestBin] / count).toInt().coerceIn(0, 255)
        val avgG = (binGreen[bestBin] / count).toInt().coerceIn(0, 255)
        val avgB = (binBlue[bestBin] / count).toInt().coerceIn(0, 255)

        // Tune dominant color with light, vibrant luminance so text in both themes is 100% visible
        val finalHsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(avgR, avgG, avgB, finalHsv)
        finalHsv[1] = finalHsv[1].coerceIn(0.35f, 0.78f)
        finalHsv[2] = finalHsv[2].coerceIn(0.48f, 0.72f)
        return Color(android.graphics.Color.HSVToColor(finalHsv))
    }

    private fun fallback(seed: String): Color {
        if (seed.isBlank()) return Color(0xFF42424E)
        val hash = seed.hashCode()
        val hue = (abs(hash) % 360).toFloat()
        val hsv = floatArrayOf(hue, 0.40f, 0.52f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
