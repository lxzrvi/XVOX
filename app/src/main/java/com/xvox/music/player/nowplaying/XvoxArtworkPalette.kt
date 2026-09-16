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

    private fun getCachedBitmap(uri: Uri): Bitmap? {
        val base = XvoxArtworkCache.keyFor(uri)
        return XvoxArtworkCache.get("${base}_1024")
            ?: XvoxArtworkCache.get("${base}_640")
            ?: XvoxArtworkCache.get("${base}_512")
            ?: XvoxArtworkCache.get("${base}_320")
            ?: XvoxArtworkCache.get("${base}_256")
            ?: XvoxArtworkCache.get("${base}_160")
            ?: XvoxArtworkCache.get(base)
    }

    fun fastEstimate(uri: Uri?, songKey: String = ""): Color {
        val key = uri?.toString()?.takeIf { it.isNotBlank() } ?: songKey
        if (key.isBlank()) return fallback(songKey)
        cache[key]?.let { return it }

        if (uri != null) {
            val cached = getCachedBitmap(uri)
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
                val cached = getCachedBitmap(uri)
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

        val dominant = palette.dominantSwatch
        val vibrant = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.darkVibrantSwatch
        val muted = palette.mutedSwatch ?: palette.lightMutedSwatch ?: palette.darkMutedSwatch

        val chosenSwatch = when {
            dominant != null -> {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(dominant.rgb, hsv)
                // If dominant is pitch black or near white/grayscale, prefer vibrant or muted if available
                if ((hsv[1] < 0.10f || hsv[2] < 0.15f) && (vibrant != null || muted != null)) {
                    vibrant ?: muted ?: dominant
                } else {
                    dominant
                }
            }
            vibrant != null -> vibrant
            muted != null -> muted
            else -> palette.swatches.maxByOrNull { it.population }
        }

        if (chosenSwatch != null) {
            val rgb = chosenSwatch.rgb
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(rgb, hsv)
            // Retain accurate cover dominant color, slightly lightened for optimal black and white text visibility
            hsv[1] = hsv[1].coerceIn(0.22f, 0.88f)
            hsv[2] = hsv[2].coerceIn(0.44f, 0.64f)
            return Color(android.graphics.Color.HSVToColor(hsv))
        }

        return Color(0xFF38384A)
    }

    private fun fallback(seed: String): Color {
        if (seed.isBlank()) return Color(0xFF38384A)
        val hash = seed.hashCode()
        val hue = (abs(hash) % 360).toFloat()
        val hsv = floatArrayOf(hue, 0.40f, 0.52f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
