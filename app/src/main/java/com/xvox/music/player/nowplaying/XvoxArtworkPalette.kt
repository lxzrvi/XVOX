package com.xvox.music.player.nowplaying

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.xvox.music.artwork.XvoxArtworkCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class XvoxArtworkPaletteLoader(
    context: Context
) {
    private val appContext = context.applicationContext
    companion object {
        private val cache = android.util.LruCache<String, Color>(1024)
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
        return fallback(songKey)
    }

    suspend fun load(uri: Uri?, songKey: String = ""): Color {
        val key = uri?.toString()?.takeIf { it.isNotBlank() } ?: songKey
        if (key.isBlank()) return fallback(songKey)
        cache[key]?.let { return it }

        val result = withContext(Dispatchers.IO) {
            if (uri != null) {
                val cached = XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_1024")
                    ?: XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_512")
                    ?: XvoxArtworkCache.get("${XvoxArtworkCache.keyFor(uri)}_256")
                if (cached != null) {
                    val extracted = extract(cached)
                    cache.put(key, extracted)
                    return@withContext extracted
                }
            }

            if (uri != null) {
                runCatching {
                    val loader = SingletonImageLoader.get(appContext)
                    val req = ImageRequest.Builder(appContext)
                        .data(uri)
                        .size(240, 240)
                        .allowHardware(false)
                        .build()
                    val res = loader.execute(req)
                    (res.image as? coil3.BitmapImage)?.bitmap
                }.getOrNull()?.let { bitmap ->
                    val extracted = extract(bitmap)
                    cache.put(key, extracted)
                    return@withContext extracted
                }
            }

            fallback(songKey)
        }

        cache.put(key, result)
        return result
    }

    private fun extract(bitmap: Bitmap): Color {
        val palette = Palette.from(bitmap)
            .maximumColorCount(32)
            .generate()

        val swatch = palette.dominantSwatch
            ?: palette.vibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.lightVibrantSwatch

        if (swatch != null) {
            val rgb = swatch.rgb
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(rgb, hsv)
            // Ensure pure cover color tone with balanced luminance so all text is clearly visible
            hsv[1] = hsv[1].coerceIn(0.30f, 0.90f)
            hsv[2] = hsv[2].coerceIn(0.38f, 0.65f)
            return Color(android.graphics.Color.HSVToColor(hsv))
        }

        return Color(0xFF38384A)
    }

    private fun fallback(seed: String): Color {
        if (seed.isBlank()) return Color(0xFF38384A)
        val hash = seed.hashCode()
        val hue = (abs(hash) % 360).toFloat()
        val hsv = floatArrayOf(hue, 0.45f, 0.52f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
