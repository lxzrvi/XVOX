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
        private val cache = android.util.LruCache<String, Color>(256)
    }

    suspend fun load(uri: Uri?, songKey: String = ""): Color {
        val key = uri?.toString() ?: songKey
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

            // Next decode via Coil for 100% reliable image loading
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
        val counts = IntArray(4096)
        val red = LongArray(4096)
        val green = LongArray(4096)
        val blue = LongArray(4096)
        val scores = FloatArray(4096)

        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) < 128) continue
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)

                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                // Ignore extreme near-blacks and near-whites to find the true colorful dominant hue
                if (maxC < 12 || minC > 252) continue

                val delta = (maxC - minC).toFloat()
                val saturation = if (maxC == 0) 0f else delta / maxC
                val bin = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)

                counts[bin]++
                red[bin] += r.toLong()
                green[bin] += g.toLong()
                blue[bin] += b.toLong()
                // Weight vibrant dominant colors faithfully
                scores[bin] += (1f + saturation * 3.8f)
            }
        }

        var bestBin = -1
        var bestScore = -1f
        for (i in 0 until 4096) {
            if (counts[i] > 0 && scores[i] > bestScore) {
                bestScore = scores[i]
                bestBin = i
            }
        }

        if (bestBin < 0 || counts[bestBin] == 0) return fallback("")
        val count = counts[bestBin]
        val rawR = (red[bestBin] / count).toInt()
        val rawG = (green[bestBin] / count).toInt()
        val rawB = (blue[bestBin] / count).toInt()
        return tuneDominant(Color(rawR, rawG, rawB))
    }

    /**
     * Preserves the live dominant artwork color directly, gently ensuring readability.
     */
    private fun tuneDominant(source: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (source.red * 255).toInt(),
            (source.green * 255).toInt(),
            (source.blue * 255).toInt(),
            hsv
        )
        // Ensure color has sufficient presence and readable lightness
        hsv[1] = hsv[1].coerceIn(0.28f, 0.88f)
        hsv[2] = hsv[2].coerceIn(0.35f, 0.75f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun fallback(seed: String): Color {
        if (seed.isBlank()) return Color(0xFF323240)
        val hash = seed.hashCode()
        val hue = (abs(hash) % 360).toFloat()
        val hsv = floatArrayOf(hue, 0.52f, 0.45f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
