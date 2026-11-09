// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/blur/XvoxWallpaperGlass.kt
package com.xvox.music.core.ui.blur

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pre-blurred copy of a PHOTO wallpaper (tiny bitmap, blurred once on the CPU).
 *
 * Why: a live blur pass per glass card over a static photo is the single most expensive thing the
 * app drew while scrolling. Every glass surface that sits on the wallpaper now just paints its own
 * WINDOW of this blurred copy (one cheap image draw, tracked to the surface's screen position), so
 * the frosted look and the live feel stay, but the GPU blur cost is ~0 and scrolling stays smooth.
 */
val LocalXvoxWallpaperGlass = staticCompositionLocalOf<ImageBitmap?> { null }

/** Loads [uri] as a small, softly blurred bitmap. Null while loading / on failure. */
@Composable
fun rememberBlurredWallpaper(uri: String, enabled: Boolean, dim: Float = 0f): ImageBitmap? {
    val context = LocalContext.current
    var result by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri, enabled, dim) {
        if (!enabled || uri.isBlank()) {
            result = null
            return@LaunchedEffect
        }
        result = withContext(Dispatchers.IO) { runCatching { loadBlurred(context, uri, dim) }.getOrNull() }
    }
    return result
}

private suspend fun loadBlurred(context: Context, uri: String, dim: Float): ImageBitmap? {
    val request = ImageRequest.Builder(context)
        .data(Uri.parse(uri))
        .size(160, 160)
        .allowHardware(false)
        .build()
    val image = SingletonImageLoader.get(context).execute(request).image ?: return null
    val source = image.toBitmap()
    val bmp = source.copy(Bitmap.Config.ARGB_8888, true) ?: return null
    boxBlur(bmp, radius = 3, passes = 3, keep = (1f - dim).coerceIn(.05f, 1f))
    return bmp.asImageBitmap()
}

/** Small separable box blur (3 passes approximate a Gaussian). Runs on a ~160px bitmap. */
private fun boxBlur(bmp: Bitmap, radius: Int, passes: Int, keep: Float) {
    val w = bmp.width
    val h = bmp.height
    if (w < 2 || h < 2) return
    var a = IntArray(w * h)
    var b = IntArray(w * h)
    bmp.getPixels(a, 0, w, 0, 0, w, h)
    repeat(passes) {
        blurLine(a, b, w, h, radius, horizontal = true)
        blurLine(b, a, w, h, radius, horizontal = false)
    }
    if (keep < .999f) {
        for (i in a.indices) {
            val p = a[i]
            a[i] = (p and (0xFF shl 24)) or
                ((((p shr 16) and 255) * keep).toInt() shl 16) or
                ((((p shr 8) and 255) * keep).toInt() shl 8) or
                ((p and 255) * keep).toInt()
        }
    }
    bmp.setPixels(a, 0, w, 0, 0, w, h)
}

private fun blurLine(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
    val lines = if (horizontal) h else w
    val len = if (horizontal) w else h
    val div = 2 * r + 1
    for (line in 0 until lines) {
        var sa = 0; var sr = 0; var sg = 0; var sb = 0
        fun idx(i: Int) = if (horizontal) line * w + i.coerceIn(0, len - 1) else i.coerceIn(0, len - 1) * w + line
        for (i in -r..r) {
            val p = src[idx(i)]
            sa += p ushr 24; sr += (p shr 16) and 255; sg += (p shr 8) and 255; sb += p and 255
        }
        for (i in 0 until len) {
            val o = idx(i)
            dst[o] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
            val add = src[idx(i + r + 1)]
            val rem = src[idx(i - r)]
            sa += (add ushr 24) - (rem ushr 24)
            sr += ((add shr 16) and 255) - ((rem shr 16) and 255)
            sg += ((add shr 8) and 255) - ((rem shr 8) and 255)
            sb += (add and 255) - (rem and 255)
        }
    }
}

/** Paints this surface's window of the pre-blurred wallpaper, then a flat tint. */
fun Modifier.xvoxWallpaperGlass(image: ImageBitmap, tint: Color): Modifier =
    this then WallpaperGlassElement(image, tint)

private data class WallpaperGlassElement(val image: ImageBitmap, val tint: Color) :
    ModifierNodeElement<WallpaperGlassNode>() {
    override fun create() = WallpaperGlassNode(image, tint)
    override fun update(node: WallpaperGlassNode) {
        node.image = image
        node.tint = tint
        node.invalidateDraw()
    }
    override fun InspectorInfo.inspectableProperties() { name = "xvoxWallpaperGlass" }
}

private class WallpaperGlassNode(var image: ImageBitmap, var tint: Color) :
    Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    private var coords: LayoutCoordinates? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        coords = coordinates
        // Fires whenever the surface moves on screen (scroll, slide): repaint its window.
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val c = coords
        if (c != null && c.isAttached) {
            val pos = c.positionInRoot()
            val root = c.findRootCoordinates().size
            val rw = root.width.toFloat()
            val rh = root.height.toFloat()
            if (rw > 0f && rh > 0f) {
                // ContentScale.Crop of the bitmap over the whole window, like the wallpaper itself.
                val scale = max(rw / image.width, rh / image.height)
                val cropW = rw / scale
                val cropH = rh / scale
                val srcX = ((image.width - cropW) / 2f).roundToInt().coerceAtLeast(0)
                val srcY = ((image.height - cropH) / 2f).roundToInt().coerceAtLeast(0)
                val srcW = cropW.roundToInt().coerceAtMost(image.width - srcX).coerceAtLeast(1)
                val srcH = cropH.roundToInt().coerceAtMost(image.height - srcY).coerceAtLeast(1)
                drawImage(
                    image = image,
                    srcOffset = IntOffset(srcX, srcY),
                    srcSize = IntSize(srcW, srcH),
                    dstOffset = IntOffset((-pos.x).roundToInt(), (-pos.y).roundToInt()),
                    dstSize = IntSize(root.width, root.height),
                    filterQuality = FilterQuality.Medium
                )
            }
        }
        drawRect(tint)
        drawContent()
    }
}
