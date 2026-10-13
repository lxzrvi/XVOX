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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Theme-aware fallback artwork for songs whose embedded cover/thumbnail is absent or unreadable.
 * Each caller supplies a stable song-derived seed, so a song keeps its own soft wave identity.
 */
@Composable
fun WavePatternSquare(
    modifier: Modifier = Modifier,
    darkColor: Color? = null,
    brightColor: Color? = null,
    seed: Int = 11,
    bitmapSize: Int = 360
) {
    val colors = XvoxTheme.colors
    // Never hardcode the former red/black placeholder. A custom accent and either app theme
    // generate their own cover surface while retaining enough base contrast for the wave texture.
    val resolvedDark = darkColor ?: if (colors.isLight) colors.cardElevated else colors.background
    val resolvedBright = brightColor ?: colors.primaryAccent
    val safeBitmapSize = bitmapSize.coerceIn(180, 540)
    val patternKey = xvoxWavePatternKey(resolvedDark, resolvedBright, seed, safeBitmapSize)

    var displayedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var outgoingBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var displayedKey by remember { mutableStateOf<String?>(null) }
    val reveal = remember { Animatable(1f) }

    LaunchedEffect(patternKey) {
        // Pixel generation remains off the main thread. A small process cache avoids regenerating
        // the same song's pattern during list scrolls, Now Playing, and a Mini Player handoff.
        val next = xvoxCachedWaveBitmap(patternKey)?.asImageBitmap()
            ?: withContext(Dispatchers.Default) {
                xvoxBuildWaveBitmap(safeBitmapSize, seed, resolvedDark, resolvedBright).also {
                    xvoxCacheWaveBitmap(patternKey, it)
                }.asImageBitmap()
            }
        val previous = displayedBitmap
        val previousKey = displayedKey
        displayedKey = patternKey
        if (previous != null && previousKey != patternKey) {
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

    // The original SVG is preserved in res/raw, with an Android VectorDrawable generated directly
    // from it for resource-safe use here and anywhere else that needs @drawable/xvox.
    val brandTint = if (resolvedDark.luminance() < .42f) {
        Color.White.copy(alpha = .88f)
    } else {
        Color.Black.copy(alpha = .72f)
    }
    Box(
        modifier = modifier.background(resolvedDark),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            outgoingBitmap?.let { drawWaveBitmap(it, 1f - reveal.value) }
            displayedBitmap?.let { drawWaveBitmap(it, reveal.value) }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 9.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.xvox),
                contentDescription = "XVOX",
                tint = brandTint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "XVOX",
                color = brandTint,
                fontFamily = XvoxLogoFont,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                letterSpacing = .35.sp,
                modifier = Modifier.padding(start = 3.dp)
            )
        }
    }
}

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

/** Bounded bitmap cache; patterns are tiny compared with decoded album art and safe to reuse. */
private object XvoxWavePatternCache {
    private val cache = object : LruCache<String, Bitmap>(12 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        if (cache.get(key) == null) cache.put(key, bitmap)
    }
}

/** Shared by the background bitmap path so null/error covers can morph without a blank frame. */
internal fun xvoxWavePatternKey(dark: Color, bright: Color, seed: Int, size: Int): String =
    "${dark.toArgb()}:${bright.toArgb()}:$seed:$size"

internal fun xvoxCachedWaveBitmap(key: String): Bitmap? = XvoxWavePatternCache.get(key)

internal fun xvoxCacheWaveBitmap(key: String, bitmap: Bitmap) {
    XvoxWavePatternCache.put(key, bitmap)
}

/** Red/dark reference waves, recoloured exclusively from the active app theme/accent. */
internal fun xvoxBuildWaveBitmap(size: Int, seed: Int, dark: Color, bright: Color): Bitmap {
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
