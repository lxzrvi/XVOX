package com.xvox.music.player.nowplaying

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
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

/**
 * Cover-palette extraction for Now Playing.
 *
 * Pipeline: sampled cover pixels → similar-pixel clusters → actual dominant cluster → HSL →
 * adaptive safety adjustment → final background. It intentionally avoids fixed colour presets:
 * cover hue is retained, dark/light variants of the same hue remain distinct, and only unusable
 * extremes (black, white, or very harsh neon) are softened.
 */
class XvoxArtworkPaletteLoader(
    context: Context
) {
    private val appContext = context.applicationContext

    companion object {
        private val cache = android.util.LruCache<String, Color>(1024)
        private const val HueBins = 24
        private const val SaturationBins = 5
        private const val LightnessBins = 10
    }

    private data class Hsl(val hue: Float, val saturation: Float, val lightness: Float)

    private class PixelCluster(
        val hueBin: Int,
        val saturationBin: Int,
        val lightnessBin: Int
    ) {
        var population = 0
        var redTotal = 0L
        var greenTotal = 0L
        var blueTotal = 0L

        fun add(red: Int, green: Int, blue: Int) {
            population++
            redTotal += red
            greenTotal += green
            blueTotal += blue
        }
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

    /**
     * Build small HSL neighbourhood clusters rather than trusting one raw pixel or a fixed palette
     * swatch. A tiny logo or line of text has too little population to win; broad, visually similar
     * cover regions merge into the dominant candidate.
     */
    private fun extract(bitmap: Bitmap): Color {
        if (bitmap.width <= 0 || bitmap.height <= 0) return fallback("")
        val step = max(1, max(bitmap.width, bitmap.height) / 84)
        val clusters = HashMap<Int, PixelCluster>()
        var sampleCount = 0

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (AndroidColor.alpha(pixel) >= 224) {
                    val red = AndroidColor.red(pixel)
                    val green = AndroidColor.green(pixel)
                    val blue = AndroidColor.blue(pixel)
                    val hsl = rgbToHsl(red, green, blue)
                    // Near greys do not get an arbitrary hue bucket; lightness alone separates
                    // black covers, dark grey covers, white covers, and light grey covers.
                    val hueBin = if (hsl.saturation < .055f) -1
                    else (hsl.hue / 360f * HueBins).toInt().coerceIn(0, HueBins - 1)
                    val saturationBin = (hsl.saturation * SaturationBins).toInt()
                        .coerceIn(0, SaturationBins - 1)
                    val lightnessBin = (hsl.lightness * LightnessBins).toInt()
                        .coerceIn(0, LightnessBins - 1)
                    val key = ((hueBin + 1) shl 8) or (saturationBin shl 4) or lightnessBin
                    val cluster = clusters.getOrPut(key) {
                        PixelCluster(hueBin, saturationBin, lightnessBin)
                    }
                    cluster.add(red, green, blue)
                    sampleCount++
                }
                x += step
            }
            y += step
        }

        if (clusters.isEmpty() || sampleCount == 0) return fallback("")

        // A logo/text colour generally occupies far below this share. If an artistic cover is very
        // detailed and no bin clears the threshold, retain all clusters rather than forcing a hue.
        val minimumUsefulPopulation = max(4, sampleCount / 150)
        val eligible = clusters.values.filter { it.population >= minimumUsefulPopulation }
            .ifEmpty { clusters.values.toList() }

        val dominantSeed = eligible.maxByOrNull { candidate ->
            eligible.sumOf { neighbour ->
                if (areSimilar(candidate, neighbour)) neighbour.population else 0
            }
        } ?: return fallback("")

        val merged = eligible.filter { areSimilar(dominantSeed, it) }
        val population = merged.sumOf { it.population }.coerceAtLeast(1)
        val red = (merged.sumOf { it.redTotal } / population.toLong()).toInt().coerceIn(0, 255)
        val green = (merged.sumOf { it.greenTotal } / population.toLong()).toInt().coerceIn(0, 255)
        val blue = (merged.sumOf { it.blueTotal } / population.toLong()).toInt().coerceIn(0, 255)

        return adaptForBackground(rgbColor(red, green, blue))
    }

    private fun areSimilar(first: PixelCluster, second: PixelCluster): Boolean {
        if (first.hueBin < 0 || second.hueBin < 0) {
            return first.hueBin == second.hueBin && abs(first.lightnessBin - second.lightnessBin) <= 1
        }
        return circularDistance(first.hueBin, second.hueBin, HueBins) <= 1 &&
            abs(first.saturationBin - second.saturationBin) <= 1 &&
            abs(first.lightnessBin - second.lightnessBin) <= 1
    }

    /**
     * Preserve hue and almost all of the cover's saturation/lightness. Only extremes move toward
     * readable neighbours; this keeps dark red and light red, for example, visibly different.
     */
    private fun adaptForBackground(color: Color): Color {
        val hsl = rgbToHsl(
            (color.red * 255f).toInt(),
            (color.green * 255f).toInt(),
            (color.blue * 255f).toInt()
        )

        if (hsl.saturation <= .045f && hsl.lightness <= .035f) {
            // Pure black needs a little surface detail, but remains a dark neutral grey.
            return hslToColor(0f, .02f, .16f)
        }
        if (hsl.saturation <= .045f && hsl.lightness >= .965f) {
            // Pure white similarly becomes a light neutral grey, not a fixed themed colour.
            return hslToColor(0f, .02f, .84f)
        }

        val harshness = ((hsl.saturation - .90f) / .10f).coerceIn(0f, 1f)
        val saturation = (hsl.saturation - .10f * harshness).coerceIn(0f, 1f)
        val lowerReadableLightness = .09f + (1f - saturation) * .035f
        val upperReadableLightness = .91f - (1f - saturation) * .035f
        val lightness = when {
            hsl.lightness < lowerReadableLightness ->
                hsl.lightness + (lowerReadableLightness - hsl.lightness) * .72f
            hsl.lightness > upperReadableLightness ->
                hsl.lightness + (upperReadableLightness - hsl.lightness) * .72f
            else -> hsl.lightness
        }
        return hslToColor(hsl.hue, saturation, lightness)
    }

    private fun fallback(seed: String): Color {
        if (seed.isBlank()) return hslToColor(235f, .10f, .20f)
        val hue = (abs(seed.hashCode()) % 360).toFloat()
        return hslToColor(hue, .40f, .52f)
    }

    private fun rgbToHsl(red: Int, green: Int, blue: Int): Hsl {
        val r = red / 255f
        val g = green / 255f
        val b = blue / 255f
        val maximum = max(r, max(g, b))
        val minimum = min(r, min(g, b))
        val delta = maximum - minimum
        val lightness = (maximum + minimum) / 2f
        if (delta <= .00001f) return Hsl(0f, 0f, lightness)

        val saturation = delta / (1f - abs(2f * lightness - 1f)).coerceAtLeast(.00001f)
        val hue = when (maximum) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }
        return Hsl(hue, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
    }

    private fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)
        val chroma = (1f - abs(2f * l - 1f)) * s
        val secondary = chroma * (1f - abs((h / 60f) % 2f - 1f))
        val (red, green, blue) = when (h.toInt()) {
            in 0 until 60 -> Triple(chroma, secondary, 0f)
            in 60 until 120 -> Triple(secondary, chroma, 0f)
            in 120 until 180 -> Triple(0f, chroma, secondary)
            in 180 until 240 -> Triple(0f, secondary, chroma)
            in 240 until 300 -> Triple(secondary, 0f, chroma)
            else -> Triple(chroma, 0f, secondary)
        }
        val match = l - chroma / 2f
        return Color(red + match, green + match, blue + match)
    }

    private fun rgbColor(red: Int, green: Int, blue: Int): Color = Color(
        red = red / 255f,
        green = green / 255f,
        blue = blue / 255f
    )

    private fun circularDistance(first: Int, second: Int, size: Int): Int {
        val difference = abs(first - second)
        return min(difference, size - difference)
    }
}
