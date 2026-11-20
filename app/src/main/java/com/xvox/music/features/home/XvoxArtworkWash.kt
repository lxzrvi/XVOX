package com.xvox.music.features.home

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The colour a detail page's cover "blurs down to": the mean of the cover's bottom band, pulled
 * into a lightness range that keeps the page's text readable. Ported from BitChord's
 * `ArtworkPalette.wash` (edge colour, `adaptedArtworkSaturation`, dark 0.14-0.24 / light 0.78-0.90).
 *
 * Returns null until the cover has been read, so the caller keeps its normal page colour for that
 * moment; a cover that was already read once is tinted on its first frame.
 */
@Composable
fun rememberXvoxArtworkWash(cover: Any?): Color? {
    val dark = !XvoxTheme.colors.isLight
    val context = LocalContext.current
    val key = cover?.toString()
    var edge by remember(key) { mutableStateOf(key?.let { washEdgeCache[it] }) }
    val known = remember(key) { edge != null }

    LaunchedEffect(key) {
        if (cover == null || edge != null) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(cover)
            .size(PALETTE_PX, PALETTE_PX)
            .allowHardware(false)
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        val bitmap = ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap ?: return@LaunchedEffect
        val mean = withContext(Dispatchers.Default) { bottomEdgeColor(bitmap) } ?: return@LaunchedEffect
        washEdgeCache[key!!] = mean
        edge = mean
    }

    val target = edge?.let { washOf(it, dark) } ?: return null
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = if (known) snap() else tween(260),
        label = "heroWash"
    )
    return animated
}

private const val PALETTE_PX = 128
private const val EDGE_BAND = 0.18f
private const val CHROMATIC_SATURATION_THRESHOLD = 0.12f

private val washEdgeCache = object : LinkedHashMap<String, Color>(0, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Color>) = size > 128
}

private fun bottomEdgeColor(bitmap: Bitmap): Color? {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return null
    val band = (h * EDGE_BAND).toInt().coerceIn(1, h)
    val pixels = IntArray(w * band)
    bitmap.getPixels(pixels, 0, w, 0, h - band, w, band)
    var r = 0L; var g = 0L; var b = 0L
    pixels.forEach { p ->
        r += (p shr 16) and 0xFF
        g += (p shr 8) and 0xFF
        b += p and 0xFF
    }
    val n = pixels.size.coerceAtLeast(1)
    return Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
}

private fun adaptedSaturation(source: Float, minimum: Float, maximum: Float): Float {
    val s = source.coerceIn(0f, 1f)
    return if (s < CHROMATIC_SATURATION_THRESHOLD) s else s.coerceIn(minimum, maximum)
}

private fun washOf(edge: Color, dark: Boolean): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(edge.toArgb(), hsl)
    if (dark) {
        hsl[1] = adaptedSaturation(hsl[1], 0.18f, 0.58f)
        hsl[2] = hsl[2].coerceIn(0.14f, 0.24f)
    } else {
        hsl[1] = adaptedSaturation(hsl[1], 0.12f, 0.46f)
        hsl[2] = hsl[2].coerceIn(0.78f, 0.90f)
    }
    return Color(ColorUtils.HSLToColor(hsl))
}
