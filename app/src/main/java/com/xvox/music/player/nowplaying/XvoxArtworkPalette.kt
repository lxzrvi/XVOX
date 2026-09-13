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
                    ?: XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_256")
                    ?: XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_160")
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
                        .size(128, 128)
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
                if (maxC < 14 || minC > 250) continue

                val delta = (maxC - minC).toFloat()
                val saturation = if (maxC == 0) 0f else delta / maxC
                val bin = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)

                counts[bin]++
                red[bin] += r.toLong()
                green[bin] += g.toLong()
                blue[bin] += b.toLong()
                // Heavily weight vibrant, rich colors over dull gray/white/black
                scores[bin] += (1f + saturation * 4.5f)
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
        return normalize(Color((red[bestBin] / count).toInt(), (green[bestBin] / count).toInt(), (blue[bestBin] / count).toInt()))
    }

    private fun normalize(source: Color): Color {
        val r = source.red.coerceIn(0f, 1f)
        val g = source.green.coerceIn(0f, 1f)
        val b = source.blue.coerceIn(0f, 1f)
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val l = (maxC + minC) / 2f

        val delta = maxC - minC
        val s = if (delta <= 0.0001f) 0f else delta / (1f - abs(2f * l - 1f)).coerceAtLeast(0.0001f)
        var h = 0f
        if (delta > 0.0001f) {
            when (maxC) {
                r -> h = (((g - b) / delta) % 6f) * 60f
                g -> h = (((b - r) / delta) + 2f) * 60f
                else -> h = (((r - g) / delta) + 4f) * 60f
            }
            if (h < 0f) h += 360f
        }

        val targetLightness = l.coerceIn(0.40f, 0.58f)
        val targetSaturation = if (s < 0.12f) 0.35f else s.coerceIn(0.40f, 0.88f)
        val c = (1f - abs(2f * targetLightness - 1f)) * targetSaturation
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = targetLightness - c / 2f
        val (rr, gg, bb) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(rr + m, gg + m, bb + m)
    }

    private fun fallback(seed: String): Color {
        if (seed.isBlank()) return Color(0xFF38384A)
        val hash = seed.hashCode()
        val hue = (abs(hash) % 360).toFloat()
        val hsv = floatArrayOf(hue, 0.55f, 0.48f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
