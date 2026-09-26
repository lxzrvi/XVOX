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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Now Playing's faithful cover-background extractor.
 *
 * Every cover is first reduced to a stable 64 × 64 image, then similar RGB pixels are clustered.
 * Tiny logo, text, and accent clusters are ignored before the largest meaningful region is mapped
 * from RGB to HSL. Hue stays untouched; only extreme lightness and extremely harsh saturation are
 * balanced so both light and dark theme text remain usable over the resulting background.
 */
class XvoxArtworkPaletteLoader(
    context: Context
) {
    private val appContext = context.applicationContext

    companion object {
        private val cache = android.util.LruCache<String, Color>(1024)
        private const val TargetSide = 64
        private const val SimilarRgbDistance = 48
        private const val SimilarRgbDistanceSquared = SimilarRgbDistance * SimilarRgbDistance
        private const val TinyClusterShare = .015f
    }

    private data class Hsl(val hue: Float, val saturation: Float, val lightness: Float)

    /** An online RGB cluster whose centre continuously follows its member pixels. */
    private class RgbCluster(red: Int, green: Int, blue: Int) {
        var population = 1
            private set
        private var redTotal = red.toLong()
        private var greenTotal = green.toLong()
        private var blueTotal = blue.toLong()

        fun distanceSquared(red: Int, green: Int, blue: Int): Int {
            val redDifference = red - averageRed
            val greenDifference = green - averageGreen
            val blueDifference = blue - averageBlue
            return redDifference * redDifference +
                greenDifference * greenDifference +
                blueDifference * blueDifference
        }

        fun add(red: Int, green: Int, blue: Int) {
            population++
            redTotal += red
            greenTotal += green
            blueTotal += blue
        }

        val averageRed: Int get() = (redTotal / population).toInt()
        val averageGreen: Int get() = (greenTotal / population).toInt()
        val averageBlue: Int get() = (blueTotal / population).toInt()
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

    /**
     * Never decode or examine pixels on the UI thread. The surrounding palette state preloads the
     * current and neighbouring covers, while this immediate value keeps a first frame responsive.
     */
    fun fastEstimate(uri: Uri?, songKey: String = ""): Color {
        val key = uri?.toString()?.takeIf { it.isNotBlank() } ?: songKey
        if (key.isBlank()) return fallback(songKey)
        return cache[key] ?: fallback(songKey)
    }

    suspend fun load(uri: Uri?, songKey: String = ""): Color {
        val key = uri?.toString()?.takeIf { it.isNotBlank() } ?: songKey
        if (key.isBlank()) return fallback(songKey)
        cache[key]?.let { return it }

        val result = withContext(Dispatchers.IO) {
            val bitmap = uri?.let { coverUri ->
                getCachedBitmap(coverUri) ?: runCatching {
                    val loader = SingletonImageLoader.get(appContext)
                    val request = ImageRequest.Builder(appContext)
                        .data(coverUri)
                        .size(TargetSide, TargetSide)
                        .allowHardware(false)
                        .build()
                    val response = loader.execute(request)
                    (response.image as? coil3.BitmapImage)?.bitmap
                }.getOrNull()
            }
            bitmap?.let { source ->
                runCatching { extract(source) }.getOrElse { fallback(songKey) }
            } ?: fallback(songKey)
        }

        cache.put(key, result)
        return result
    }

    /**
     * 64 × 64 RGB clustering deliberately favours the largest meaningful cover region. At this
     * scale a 1.5% cluster is roughly sixty pixels: small title lettering or a badge falls away,
     * while a genuine colourful region remains eligible.
     */
    private fun extract(bitmap: Bitmap): Color {
        if (bitmap.width <= 0 || bitmap.height <= 0) return fallback("")
        val resized = if (bitmap.width == TargetSide && bitmap.height == TargetSide) bitmap
        else Bitmap.createScaledBitmap(bitmap, TargetSide, TargetSide, true)

        try {
            val clusters = ArrayList<RgbCluster>()
            var opaqueSamples = 0

            for (y in 0 until TargetSide) {
                for (x in 0 until TargetSide) {
                    val pixel = resized.getPixel(x, y)
                    if (AndroidColor.alpha(pixel) < 224) continue
                    val red = AndroidColor.red(pixel)
                    val green = AndroidColor.green(pixel)
                    val blue = AndroidColor.blue(pixel)

                    var nearest: RgbCluster? = null
                    var nearestDistance = Int.MAX_VALUE
                    for (cluster in clusters) {
                        val distance = cluster.distanceSquared(red, green, blue)
                        if (distance < nearestDistance) {
                            nearest = cluster
                            nearestDistance = distance
                        }
                    }

                    if (nearest != null && nearestDistance <= SimilarRgbDistanceSquared) {
                        nearest.add(red, green, blue)
                    } else {
                        clusters += RgbCluster(red, green, blue)
                    }
                    opaqueSamples++
                }
            }

            if (clusters.isEmpty() || opaqueSamples == 0) return fallback("")
            val minimumMeaningfulPopulation = max(
                4,
                ceil(opaqueSamples.toDouble() * TinyClusterShare.toDouble()).toInt()
            )
            val meaningful = clusters.filter { it.population >= minimumMeaningfulPopulation }
                .ifEmpty { clusters }
            val dominant = meaningful.maxByOrNull { it.population } ?: return fallback("")

            return adaptForBackground(
                rgbColor(dominant.averageRed, dominant.averageGreen, dominant.averageBlue)
            )
        } finally {
            if (resized !== bitmap && !resized.isRecycled) resized.recycle()
        }
    }

    /**
     * Preserve the cover's hue exactly and almost all of its saturation. Lightness is compressed
     * into a durable middle band: black becomes charcoal, white becomes light grey, while dark
     * and light variants of red, blue, or green remain distinct rather than converging to a preset.
     */
    private fun adaptForBackground(color: Color): Color {
        val hsl = rgbToHsl(
            (color.red * 255f).toInt(),
            (color.green * 255f).toInt(),
            (color.blue * 255f).toInt()
        )

        // Only take the hard edge off neon-level saturation. Ordinary artwork saturation stays as-is.
        val harshness = ((hsl.saturation - .92f) / .08f).coerceIn(0f, 1f)
        val saturation = (hsl.saturation - .08f * harshness).coerceIn(0f, 1f)

        // This ordered, centre-preserving mapping keeps light/dark cover identity while removing
        // unusable extremes: 0.00 -> 0.30 charcoal, 0.50 -> 0.50, 1.00 -> 0.62 mid/light grey.
        // Dark-red versus light-red (and the other hues) therefore remain visibly distinct.
        val lightness = if (hsl.lightness <= .50f) {
            .30f + .40f * hsl.lightness
        } else {
            .50f + .24f * (hsl.lightness - .50f)
        }
        return hslToColor(hsl.hue, saturation, lightness.coerceIn(.30f, .62f))
    }

    private fun fallback(seed: String): Color {
        if (seed.isBlank()) return hslToColor(235f, .18f, .50f)
        val hue = (abs(seed.hashCode()) % 360).toFloat()
        return hslToColor(hue, .40f, .50f)
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
}
