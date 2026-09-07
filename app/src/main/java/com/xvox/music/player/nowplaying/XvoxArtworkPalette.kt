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
    companion object { private val cache = android.util.LruCache<String, Color>(256) }

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

    private fun normalize(source: Color): Color {
        val lum = source.luminance()
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
