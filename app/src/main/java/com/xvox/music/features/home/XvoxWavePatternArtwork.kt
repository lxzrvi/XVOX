package com.xvox.music.features.home

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.xvox.music.R
import com.xvox.music.artwork.XvoxImageWork
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxPalette
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/** Visible lockup sizing for the same fallback artwork at library, Mini Player, and Now Playing scale. */
enum class XvoxFallbackBrand {
    STANDARD,
    MINI_PLAYER,
    NOW_PLAYING,
    NONE
}

/** The three colours shared by a song's wave artwork and its Default Now Playing canvas. */
data class XvoxWavePalette(
    val surface: Color,
    val stripe: Color,
    val dominant: Color
)

/**
 * Produces an accent-derived but song-specific colour family. A fresh red-accent install still
 * begins from XVOX red, but every stable song seed takes a different tonal route around that
 * family instead of making a library of identical red/black placeholders.
 */
internal fun xvoxWavePalette(seed: Int, colors: XvoxPalette): XvoxWavePalette {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(colors.primaryAccent.toArgb(), hsl)
    val variation = xvoxSeedUnit(seed, 41)
    val lane = xvoxSeedIndex(seed, 73, 8)
    val hueOffsets = floatArrayOf(-7f, 38f, 83f, 129f, 174f, 220f, 266f, 313f)
    val baseHue = if (hsl[1] < .08f) 0f else hsl[0]
    val hue = (baseHue + hueOffsets[lane] + (variation - .5f) * 18f + 360f) % 360f
    val saturation = (
        if (hsl[1] < .15f) .60f
        else .46f + hsl[1] * .40f + (variation - .5f) * .12f
    ).coerceIn(.42f, .88f)
    val lightness = if (colors.isLight) {
        (.43f + variation * .12f).coerceIn(.43f, .55f)
    } else {
        (.48f + variation * .15f).coerceIn(.48f, .63f)
    }
    val stripe = Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness)))
    val baseSurface = if (colors.isLight) colors.cardElevated else colors.background
    val surface = lerp(baseSurface, stripe, if (colors.isLight) .12f else .18f)
    return XvoxWavePalette(
        surface = surface,
        stripe = stripe,
        dominant = lerp(surface, stripe, if (colors.isLight) .50f else .56f)
    )
}

/**
 * Theme-aware fallback artwork for songs whose embedded cover/thumbnail is absent or unreadable.
 * A stable song-derived seed controls both the unique warped stripe geometry and its colour family.
 */
@Composable
fun WavePatternSquare(
    modifier: Modifier = Modifier,
    darkColor: Color? = null,
    brightColor: Color? = null,
    seed: Int = 11,
    bitmapSize: Int = 256,
    brand: XvoxFallbackBrand = XvoxFallbackBrand.STANDARD
) {
    val colors = XvoxTheme.colors
    val palette = remember(seed, colors) { xvoxWavePalette(seed, colors) }
    val resolvedDark = darkColor ?: palette.surface
    val resolvedBright = brightColor ?: palette.stripe
    val safeBitmapSize = bitmapSize.coerceIn(96, 540)
    val patternKey = xvoxWavePatternKey(resolvedDark, resolvedBright, seed, safeBitmapSize)
    // List cells should replace their own stable artwork directly. Crossfading two Canvas layers
    // while Lazy layouts recycle rapidly is pure extra draw/recomposition work; reserve that
    // premium morph only for the large Now Playing artwork surface.
    val animatePatternSwap = brand == XvoxFallbackBrand.NOW_PLAYING

    var displayedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var outgoingBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var displayedKey by remember { mutableStateOf<String?>(null) }
    val reveal = remember { Animatable(1f) }

    LaunchedEffect(patternKey) {
        // Generation is cancellable and cached. A quick All Songs fling therefore abandons covers
        // that left the viewport instead of letting stale pixel jobs compete with scrolling.
        val next = xvoxCachedWaveBitmap(patternKey)?.asImageBitmap()
            ?: withContext(XvoxImageWork.waveContext) {
                xvoxBuildWaveBitmap(safeBitmapSize, seed, resolvedDark, resolvedBright).also {
                    xvoxCacheWaveBitmap(patternKey, it)
                }.asImageBitmap()
            }
        val previous = displayedBitmap
        val previousKey = displayedKey
        displayedKey = patternKey
        if (animatePatternSwap && previous != null && previousKey != patternKey) {
            outgoingBitmap = previous
            displayedBitmap = next
            reveal.snapTo(0f)
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(260, easing = CubicBezierEasing(.22f, 0f, 0f, 1f))
            )
            if (displayedKey == patternKey) outgoingBitmap = null
        } else {
            outgoingBitmap = null
            displayedBitmap = next
            reveal.snapTo(1f)
        }
    }

    val brandTint = if (resolvedDark.luminance() < .42f) {
        Color.White.copy(alpha = .90f)
    } else {
        Color.Black.copy(alpha = .74f)
    }
    val brandSpec = when (brand) {
        XvoxFallbackBrand.STANDARD -> XvoxFallbackBrandSpec(13f, 10f, 3f, 12f, 10f)
        XvoxFallbackBrand.MINI_PLAYER -> XvoxFallbackBrandSpec(7f, 5.5f, 1.5f, 5f, 4f)
        XvoxFallbackBrand.NOW_PLAYING -> XvoxFallbackBrandSpec(19f, 16f, 4f, 20f, 17f)
        XvoxFallbackBrand.NONE -> null
    }

    Box(
        modifier = modifier.background(resolvedDark),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            outgoingBitmap?.let { drawWaveBitmap(it, 1f - reveal.value) }
            // On a recycled list slot the old bitmap must not be painted for one extra frame
            // while the new cancellable generation is queued; show the base palette instead.
            displayedBitmap
                ?.takeIf { animatePatternSwap || displayedKey == patternKey }
                ?.let { drawWaveBitmap(it, reveal.value) }
        }
        brandSpec?.let { spec ->
            // Original SVG stays in res/raw; the directly generated VectorDrawable keeps this
            // exact XVOX mark usable in Compose and resource-only surfaces.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = spec.end.dp, bottom = spec.bottom.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.xvox),
                    contentDescription = "XVOX",
                    tint = brandTint,
                    modifier = Modifier.size(spec.mark.dp)
                )
                Text(
                    text = "XVOX",
                    color = brandTint,
                    fontFamily = XvoxLogoFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = spec.text.sp,
                    letterSpacing = if (brand == XvoxFallbackBrand.MINI_PLAYER) .08.sp else .35.sp,
                    modifier = Modifier.padding(start = spec.gap.dp)
                )
            }
        }
    }
}

private data class XvoxFallbackBrandSpec(
    val mark: Float,
    val text: Float,
    val gap: Float,
    val end: Float,
    val bottom: Float
)

private fun DrawScope.drawWaveBitmap(bitmap: ImageBitmap, alpha: Float) {
    if (alpha <= .001f) return
    // Preserve the generated square as a true cover: crop it to fill wide/tall containers rather
    // than distorting the waves when this fallback is used in a landscape player or Mini Player.
    val side = maxOf(size.width, size.height).coerceAtLeast(1f)
    drawImage(
        image = bitmap,
        dstOffset = IntOffset(
            ((size.width - side) / 2f).roundToInt(),
            ((size.height - side) / 2f).roundToInt()
        ),
        dstSize = IntSize(side.roundToInt(), side.roundToInt()),
        alpha = alpha.coerceIn(0f, 1f),
        filterQuality = FilterQuality.High
    )
}

/** Retain a larger nearby library window so reverse flings reuse existing wave rasters. */
private object XvoxWavePatternCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        if (cache.get(key) == null) cache.put(key, bitmap)
    }
}

/** Shared by the blurred background bitmap path so null/error covers morph without a blank frame. */
internal fun xvoxWavePatternKey(dark: Color, bright: Color, seed: Int, size: Int): String =
    "${dark.toArgb()}:${bright.toArgb()}:$seed:$size"

internal fun xvoxCachedWaveBitmap(key: String): Bitmap? = XvoxWavePatternCache.get(key)

internal fun xvoxCacheWaveBitmap(key: String, bitmap: Bitmap) {
    XvoxWavePatternCache.put(key, bitmap)
}

/** Cancellable red/dark-reference waves, recoloured from each song's theme-aware palette. */
internal suspend fun xvoxBuildWaveBitmap(size: Int, seed: Int, dark: Color, bright: Color): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(size * size)

    val darkRed = dark.red * 255f
    val darkGreen = dark.green * 255f
    val darkBlue = dark.blue * 255f
    val brightRed = bright.red * 255f
    val brightGreen = bright.green * 255f
    val brightBlue = bright.blue * 255f

    val reference = 1080f
    val scale = reference / size
    val period = 92f
    for (py in 0 until size) {
        if (py % 12 == 0) currentCoroutineContext().ensureActive()
        for (px in 0 until size) {
            val x = px * scale
            val y = py * scale
            val noiseOne = xvoxWaveNoise(x, y, 230f, seed)
            val noiseTwo = xvoxWaveNoise(x, y, 230f, seed + 1)
            val noiseThree = xvoxWaveNoise(x, y, 110f, seed + 2)
            val warpedX = x + 70f * noiseOne
            val warpedY = y + 85f * noiseTwo + 28f * noiseThree + 22f * sin(warpedX / 85f)

            var stripe = .5f + .5f * sin(warpedY / period * 2f * PI.toFloat())
            stripe = ((stripe - .5f) * 1.6f + .5f).coerceIn(0f, 1f)
            stripe = stripe * stripe * (3f - 2f * stripe)

            val red = (darkRed + (brightRed - darkRed) * stripe).toInt().coerceIn(0, 255)
            val green = (darkGreen + (brightGreen - darkGreen) * stripe).toInt().coerceIn(0, 255)
            val blue = (darkBlue + (brightBlue - darkBlue) * stripe).toInt().coerceIn(0, 255)
            pixels[py * size + px] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
        }
    }
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap
}

private fun xvoxSeedIndex(seed: Int, salt: Int, bound: Int): Int {
    var value = seed * 1103515245 + salt * 12345
    value = value xor (value ushr 16)
    return (value and Int.MAX_VALUE) % bound
}

private fun xvoxSeedUnit(seed: Int, salt: Int): Float {
    var value = seed * 1103515245 + salt * 12345
    value = value xor (value ushr 13)
    value *= 1274126177
    value = value xor (value ushr 16)
    return (value and Int.MAX_VALUE).toFloat() / Int.MAX_VALUE.toFloat()
}

private fun xvoxWaveHash(ix: Int, iy: Int, seed: Int): Float {
    var value = ix * 374761393 + iy * 668265263 + seed * 1274126177
    value = (value xor (value ushr 13)) * 1274126177
    value = value xor (value ushr 16)
    return (value and 0xFFFF) / 32767.5f - 1f
}

private fun xvoxWaveFade(value: Float): Float =
    value * value * value * (value * (value * 6f - 15f) + 10f)

private fun xvoxWaveNoise(x: Float, y: Float, cell: Float, seed: Int): Float {
    val gridX = x / cell
    val gridY = y / cell
    val indexX = floor(gridX).toInt()
    val indexY = floor(gridY).toInt()
    val fractionX = xvoxWaveFade(gridX - indexX)
    val fractionY = xvoxWaveFade(gridY - indexY)
    val topLeft = xvoxWaveHash(indexX, indexY, seed)
    val topRight = xvoxWaveHash(indexX + 1, indexY, seed)
    val bottomLeft = xvoxWaveHash(indexX, indexY + 1, seed)
    val bottomRight = xvoxWaveHash(indexX + 1, indexY + 1, seed)
    val top = topLeft + (topRight - topLeft) * fractionX
    val bottom = bottomLeft + (bottomRight - bottomLeft) * fractionX
    return (top + (bottom - top) * fractionY) * 2.2f
}
