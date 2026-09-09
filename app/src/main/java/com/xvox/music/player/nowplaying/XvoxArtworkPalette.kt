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
    companion object {
        private val cache = android.util.LruCache<String, Color>(256)
    }

    suspend fun load(uri: Uri?): Color {
        if (uri == null) return fallback()
        val key = uri.toString()
        cache[key]?.let { return it }

        val result = withContext(Dispatchers.IO) {
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

        cache.put(key, result)
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

        val sampleStep = max(1, min(width, height) / 48)
        val counts = IntArray(4096)
        val red = LongArray(4096)
        val green = LongArray(4096)
        val blue = LongArray(4096)
        for (y in 0 until height step sampleStep) for (x in 0 until width step sampleStep) {
            val pixel = bitmap.getPixel(x, y)
            if (android.graphics.Color.alpha(pixel) < 128) continue
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            // Ignore borders / white labels, not large muted areas of real artwork.
            val value = max(r, max(g, b))
            if (value < 18 || min(r, min(g, b)) > 242) continue
            val bin = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
            counts[bin]++; red[bin] += r.toLong(); green[bin] += g.toLong(); blue[bin] += b.toLong()
        }
        val best = counts.indices.maxByOrNull { counts[it] } ?: return fallback()
        val count = counts[best]
        if (count == 0) return fallback()
        return normalize(Color((red[best] / count).toInt(), (green[best] / count).toInt(), (blue[best] / count).toInt()))
    }

    /**
     * Keeps the cover's true dominant hue and saturation and only nudges lightness into a
     * readable band, so a bright cover stays recognisably itself instead of being washed
     * toward grey, and a near-black cover is not blown out.
     */
    private fun normalize(source: Color): Color {
        val r = source.red.coerceIn(0f, 1f)
        val g = source.green.coerceIn(0f, 1f)
        val b = source.blue.coerceIn(0f, 1f)
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val l = (maxC + minC) / 2f

        // Stay inside the "normal" band — never very dark, never near-white — while preserving the
        // cover's true chroma. The band sits at light-but-readable lightness so text and controls
        // stay readable on a midnight-black cover, and a glaring-white cover is pulled down a touch.
        if (l in 0.42f..0.62f) return source

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
        val target = l.coerceIn(0.42f, 0.60f)
        val c = (1f - abs(2f * target - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = target - c / 2f
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

    private fun fallback(): Color {
        return Color(0xFF383842)
    }
}
