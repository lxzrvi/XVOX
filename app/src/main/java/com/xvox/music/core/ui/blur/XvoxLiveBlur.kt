// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/blur/XvoxLiveBlur.kt
// =============================================================================================
// XVOX LIVE BLUR (Miuix / KernelSU engine) - YEH POORI FILE KA EXACT CODE REPLACE KARO.
// KOI EXTRA KAAM NAHI. Path: app/src/main/java/com/xvox/music/core/ui/blur/XvoxLiveBlur.kt
// Is folder me nayi file XvoxLiquidKit.kt bhi chahiye (alag se di hai).
// =============================================================================================
package com.xvox.music.core.ui.blur

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

/*
 * LAYERS (KernelSU method):
 *  Layer A  = a "Sky" = LayerBackdrop: Modifier.layerBackdrop() records the source (wallpaper,
 *             page, header art, Now Playing backdrop) into one GraphicsLayer.
 *  Layer B  = every glass surface: Modifier.textureBlur() draws ITS window of that layer, blurred
 *             on the GPU (RenderEffect/RuntimeShader), with built-in vibrancy + anti-banding noise
 *             + a flat tint. Surfaces sitting on moving content (navbar, mini player, bars) sample
 *             the live page which already contains the cards = glass over glass.
 * No bitmaps, no CPU blur, no invalidate() needed: the library redraws with the source.
 */

/** Blur dp per "radius dp" of the old API. Raise for heavier frost, lower for lighter. */
private const val BlurPerRadius = 1.6f
/** Colour vibrancy of the frost (1 = off). Same idea as KernelSU's vibrancy(). */
private const val FrostSaturation = 1.15f

/**
 * Blur layers are deliberately source-local.  The root surface is only the real app backdrop;
 * chrome which sits above moving page content, Header controls, and Now Playing each receive a
 * nearer sibling source instead of sampling a decorative or unrelated wallpaper layer.
 * A blurred surface is never placed inside the Sky it samples.
 */
val LocalXvoxSurfaceSky = staticCompositionLocalOf<Sky?> { null }
val LocalXvoxOverlaySky = staticCompositionLocalOf<Sky?> { null }
/** True inside sheet/box content: controls there are flat theme material (no blur) in every mode. */
val LocalXvoxInSheet = staticCompositionLocalOf { false }

/** True while the app background is a custom PHOTO: cards then blur it live (a gradient/flat background looks identical without the pass). */
val LocalXvoxPhotoWallpaper = staticCompositionLocalOf { false }
/** Actual tab/body content used by the persistent Mini Player, navbar, and active tab pill. */
val LocalXvoxShellContentSky = staticCompositionLocalOf<Sky?> { null }
/** Header artwork/material used only by the avatar and Header action controls. */
val LocalXvoxHeaderSky = staticCompositionLocalOf<Sky?> { null }
/** Now Playing's live backdrop, used by its top controls. */
val LocalXvoxNowPlayingSky = staticCompositionLocalOf<Sky?> { null }
/** The separate painted bottom-box layer used by Now Playing's bottom controls. */
val LocalXvoxNowPlayingBottomSky = staticCompositionLocalOf<Sky?> { null }
/** Global page-card transparency drives opacity everywhere and blur strength while blur is on. */
val LocalXvoxGlassTransparency = staticCompositionLocalOf { 0f }

enum class XvoxBlurLevel(
    val radius: Dp,
    val tintAlpha: Float,
    val fallbackAlpha: Float
) {
    /** Selected states: light, so they stay distinct instead of foggy. */
    Selection(12.dp, .28f, .72f),
    /** Songs, playlists, search, Settings cards. */
    Card(18.dp, .44f, .82f),
    /** Header, mini player, navbar, compact action groups. */
    Chrome(18.dp, .38f, .80f),
    /** Root sheets and boxes: denser glass over the live page. */
    Sheet(30.dp, .57f, .88f),
    /** Free area around an open sheet: strongest, still readable. */
    SheetBackdrop(36.dp, .20f, .52f),
    /** Bars over scrolling content: dense enough to keep text readable. */
    Bar(16.dp, .50f, .90f)
}

// ---------------------------------------------------------------------------------------------
// Sky: thin wrapper around Miuix LayerBackdrop (keeps every existing call-site unchanged)
// ---------------------------------------------------------------------------------------------

@Stable
class Sky internal constructor(
    internal val layer: LayerBackdrop,
    base: Sky? = null
) {
    /**
     * What a glass surface actually blurs. A source that only holds transparent page content
     * (list, header art) is composed OVER its base (the wallpaper), exactly like KernelSU draws
     * its surface colour under the content; otherwise the blur has nothing opaque to frost and
     * the glass degrades to "just transparency".
     */
    val backdrop: Backdrop = if (base != null) CombinedBackdrop(base.backdrop, layer) else layer

    /** Kept for source compatibility. The Miuix backdrop follows its source live. */
    fun invalidate() = Unit
}

/** [base] = the opaque layer under this source. Default: the wallpaper (root surface sky). */
@Composable
fun rememberSky(base: Sky? = LocalXvoxSurfaceSky.current): Sky {
    val layer = rememberLayerBackdrop()
    return remember(layer, base) { Sky(layer, base) }
}

/** Put on the source (page / wallpaper / list): its drawn output becomes the blurred backdrop. */
fun Modifier.sky(sky: Sky): Modifier = this.layerBackdrop(sky.layer)

// ---------------------------------------------------------------------------------------------
// Quality governor: same engine, sized to the device so heavy blur cannot OOM / close the app
// ---------------------------------------------------------------------------------------------

private enum class BlurTier(val maxBlurDp: Float, val liveCards: Boolean) {
    /** Low-RAM / small heap: only chrome, bars and sheets blur live; list cards stay flat glass. */
    // Radii are capped on purpose: very large radii force the GPU to blur a tiny down-sampled copy,
    // which is what made text/icons/covers look pixelated. ~28dp stays crisp and is much cheaper.
    Low(14f, false),
    Mid(24f, true),
    High(28f, true)
}

private var cachedBlurTier: BlurTier? = null

/** Detected once per process (it used to be a binder call for every card that composed). */
private fun detectBlurTier(context: Context): BlurTier =
    cachedBlurTier ?: detectBlurTierUncached(context).also { cachedBlurTier = it }

private fun detectBlurTierUncached(context: Context): BlurTier {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: return BlurTier.Mid
    return when {
        am.isLowRamDevice || am.memoryClass < 192 -> BlurTier.Low
        am.memoryClass < 256 -> BlurTier.Mid
        else -> BlurTier.High
    }
}

// ---------------------------------------------------------------------------------------------
// Public modifiers (unchanged API)
// ---------------------------------------------------------------------------------------------

/**
 * Clean blur surface: clip + live GPU backdrop blur (Miuix textureBlur) + flat tint.
 * Default mode: flat theme material + original border. Blur mode: live blur + shared liquid-edge rim.
 */
@Composable
fun Modifier.xvoxLiveBlur(
    shape: Shape,
    level: XvoxBlurLevel = XvoxBlurLevel.Card,
    baseColor: Color = XvoxTheme.colors.card,
    sky: Sky? = LocalXvoxSurfaceSky.current,
    borderColor: Color = XvoxTheme.colors.cardBorder,
    borderAlpha: Float = .62f,
    borderWidth: Dp = .8.dp,
    /** Reflective rim (blur mode only). Bottom bar / mini player pass true; Now Playing is automatic. */
    glassEdge: Boolean = false,
    /** Blur mode only: draw the thin edge even when the default-mode border is disabled (alpha 0). */
    edgeInBlur: Boolean = false
): Modifier {
    val inSheet = LocalXvoxInSheet.current
    val chrome = LocalXvoxChromeStyle.current
    val npSky = LocalXvoxNowPlayingSky.current
    val npBottomSky = LocalXvoxNowPlayingBottomSky.current
    val context = LocalContext.current
    val tier = remember(context) { detectBlurTier(context) }
    val surfaceSky = LocalXvoxSurfaceSky.current
    // Blur mode == background-blur slider above zero. Default (slider 0) = plain theme material
    // with the original borders, zero blur cost.
    val blurMode = !inSheet && chrome.glassBlurEnabled && chrome.glassBlurIntensity > .001f
    val canBlur = blurMode && sky != null && !XvoxGpuGuard.safeMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // --- Sheets: NEVER blurred. Normal theme material in every mode. ---------------------------
    if (level == XvoxBlurLevel.Sheet) {
        return clip(shape).background(baseColor).xvoxPlainBorder(shape, borderColor, borderAlpha, borderWidth)
    }
    // --- Free area above a sheet: default = nothing (caller draws the faded theme scrim);
    //     blur mode = a LIGHT live blur of the page, small radius so it stays cheap and safe. ---
    if (level == XvoxBlurLevel.SheetBackdrop) {
        if (!canBlur) return this
        val light = remember(baseColor) {
            BlurColors(
                blendColors = listOf(BlendColorEntry(color = baseColor.copy(alpha = (baseColor.alpha * .12f).coerceIn(0f, 1f)))),
                saturation = 1.05f
            )
        }
        return textureBlur(backdrop = sky!!.backdrop, shape = shape, blurRadius = 18f.coerceAtMost(tier.maxBlurDp), colors = light)
    }

    val inNowPlaying = sky != null && (sky === npSky || sky === npBottomSky)
    // Now Playing glass: HEAVY blur (x1.7), still capped by the device tier.
    val densityValue = androidx.compose.ui.platform.LocalDensity.current.density
    val blurDp = remember(level, tier, inNowPlaying, densityValue) {
        xvoxBandSafeDp(
            (level.radius.value * BlurPerRadius * (if (level == XvoxBlurLevel.Card || level == XvoxBlurLevel.Selection) .95f else .7f) * (if (inNowPlaying) 1.15f else 1f)).coerceIn(1f, tier.maxBlurDp),
            densityValue
        )
    }
    val tint = remember(baseColor, level) {
        baseColor.copy(alpha = (baseColor.alpha * level.tintAlpha).coerceIn(0f, 1f))
    }
    val fallback = remember(baseColor, level) {
        baseColor.copy(alpha = (baseColor.alpha * level.fallbackAlpha).coerceIn(0f, 1f))
    }
    val colors = remember(tint) {
        BlurColors(blendColors = listOf(BlendColorEntry(color = tint)), saturation = FrostSaturation)
    }
    // PERFORMANCE: a live blur pass per list card is what made scroll/tab changes lag. Cards and
    // selection pills sit on the (static) app background, and Now Playing controls sit on a cover
    // that is already blurred, so a flat frosted tint looks the same there without a GPU pass each.
    // Live blur stays on everything that floats over MOVING content (header, navbar, mini player,
    // search bar, lyrics pill, Now Playing bottom box, sheet scrim).
    val photoWallpaper = LocalXvoxPhotoWallpaper.current
    // Capable devices (tier.liveCards) show a REAL live blur of whatever is behind each card.
    val flatStatic = blurMode && !tier.liveCards && !photoWallpaper && surfaceSky != null && sky === surfaceSky &&
        (level == XvoxBlurLevel.Card || level == XvoxBlurLevel.Selection || level == XvoxBlurLevel.Chrome)
    val wallpaperGlass = LocalXvoxWallpaperGlass.current
    // Photo wallpaper: each surface paints its window of ONE pre-blurred copy (no per-card GPU pass).
    val useWallpaperGlass = blurMode && !tier.liveCards && photoWallpaper && wallpaperGlass != null &&
        surfaceSky != null && sky === surfaceSky && level != XvoxBlurLevel.Bar
    val flatNowPlaying = blurMode && inNowPlaying && level != XvoxBlurLevel.Bar
    val flatTint = remember(baseColor, level) {
        val a = when (level) {
            XvoxBlurLevel.Card -> .62f
            XvoxBlurLevel.Selection -> .55f
            XvoxBlurLevel.Chrome -> .74f
            else -> .50f
        }
        baseColor.copy(alpha = (baseColor.alpha * a).coerceIn(0f, 1f))
    }
    // Default (blur off) song/settings cards are 6% see-through; everything else is the plain theme colour.
    val plainColor = remember(baseColor, level) {
        if (level == XvoxBlurLevel.Card) baseColor.copy(alpha = (baseColor.alpha * .94f).coerceIn(0f, 1f)) else baseColor
    }
    val clipped = clip(shape)
    val surface = when {
        !blurMode -> clipped.background(plainColor)
        flatStatic || flatNowPlaying -> clipped.background(flatTint)
        useWallpaperGlass -> clipped.xvoxWallpaperGlass(wallpaperGlass!!, tint)
        !canBlur || (!tier.liveCards && (level == XvoxBlurLevel.Card || level == XvoxBlurLevel.Selection)) ->
            clipped.background(fallback)
        else -> clipped.textureBlur(backdrop = sky!!.backdrop, shape = shape, blurRadius = blurDp, noiseCoefficient = 0f, colors = colors)
    }
    // Now Playing glass is ALWAYS borderless.
    if (inNowPlaying) return surface
    // Blur mode: bottom bar / mini player = reflective rim; every other blurred element = a very
    // thin, white-ish (not grey) hairline. Default mode keeps the call-site's normal border.
    // Navbar-style rim: the Mini Player uses it in BOTH modes so it always matches the navbar.
    // Blur OFF: theme-grey hairline (dark grey in dark mode, light grey in light mode).
    if (glassEdge && !blurMode) return surface.xvoxGreyEdge(shape)
    if (glassEdge) return surface.xvoxThinEdge(shape)
    if (blurMode && (edgeInBlur || (borderAlpha > 0f && borderWidth > 0.dp))) return surface.xvoxThinEdge(shape)
    if (borderWidth <= 0.dp || borderAlpha <= 0f) return surface
    return surface.xvoxPlainBorder(shape, borderColor, borderAlpha, borderWidth)
}

/** Root sheets sample the live app layer, not only the wallpaper. */
@Composable
fun Modifier.xvoxOverlayBlur(
    shape: Shape,
    level: XvoxBlurLevel = XvoxBlurLevel.Sheet,
    baseColor: Color = XvoxTheme.colors.card,
    borderColor: Color = XvoxTheme.colors.cardBorder,
    borderAlpha: Float = .68f,
    borderWidth: Dp = .8.dp
): Modifier = xvoxLiveBlur(
    shape = shape,
    level = level,
    baseColor = baseColor,
    sky = LocalXvoxOverlaySky.current,
    borderColor = borderColor,
    borderAlpha = borderAlpha,
    borderWidth = borderWidth
)

/** Drop-in for a flat `.background(colors.card)` on a BIG static surface. No border. */
@Composable
fun Modifier.xvoxGlassFill(
    level: XvoxBlurLevel = XvoxBlurLevel.Card,
    baseColor: Color = XvoxTheme.colors.card,
    shape: Shape = RectangleShape
): Modifier = xvoxLiveBlur(
    shape = shape,
    level = level,
    baseColor = baseColor,
    borderAlpha = 0f,
    borderWidth = 0.dp
)

/** For a bar above a scrolling list: the list is the source, the bar blurs it. */
@Composable
fun rememberXvoxContentSky(): Sky =
    rememberSky(base = LocalXvoxNowPlayingSky.current ?: LocalXvoxSurfaceSky.current)

/** Put on the scrolling list (the source). */
fun Modifier.xvoxBlurSource(sky: Sky): Modifier = this.sky(sky)

/** Put on a SIBLING bar drawn above the list; it blurs the list scrolling beneath. */
@Composable
fun Modifier.xvoxBarBlur(
    sky: Sky,
    shape: Shape = RectangleShape,
    baseColor: Color = XvoxTheme.colors.background
): Modifier = xvoxLiveBlur(
    shape = shape,
    level = XvoxBlurLevel.Bar,
    baseColor = baseColor,
    sky = sky,
    borderAlpha = 0f,
    borderWidth = 0.dp
)

/** Default-mode border: the exact original look (theme border colour, caller's alpha/width). */
@Composable
private fun Modifier.xvoxPlainBorder(
    shape: Shape,
    color: Color,
    alpha: Float,
    width: Dp
): Modifier {
    if (width <= 0.dp || alpha <= 0f) return this
    val c = color.copy(alpha = (color.alpha * alpha).coerceIn(0f, 1f))
    return drawWithContent {
        drawContent()
        drawPath(outlinePath(shape.createOutline(size, layoutDirection, this)), c, style = Stroke(width = width.toPx() * 2f))
    }
}

/** Blur-off rim for navbar / mini player: dark grey on dark themes, light grey on light themes. */
@Composable
fun Modifier.xvoxGreyEdge(shape: Shape): Modifier {
    val grey = if (XvoxTheme.colors.isLight) Color(0xFFCFCFD4) else Color(0xFF3A3A3F)
    return drawWithContent {
        drawContent()
        drawPath(outlinePath(shape.createOutline(size, layoutDirection, this)), grey, style = Stroke(width = .5.dp.toPx() * 2f))
    }
}

/**
 * The blur engine renders TWO full blur passes and cross-fades them when the radius sits within
 * +-12% of a downscale boundary (sigma 3.5 / 9.5 / 20 / 44). Nudging the radius just outside that
 * band (<=12%, visually identical) halves the cost of that surface.
 */
fun xvoxBandSafeDp(dp: Float, density: Float): Float {
    if (dp <= 0f || density <= 0f) return dp
    val sigma = dp * density * 0.45f
    for (b in floatArrayOf(3.5496478f, 9.5f, 20f, 44.10215f)) {
        val lo = b * .88f
        val hi = b * 1.12f
        if (sigma > lo && sigma < hi) {
            val target = if (sigma < b) lo * .99f else hi * 1.01f
            return target / .45f / density
        }
    }
    return dp
}

/** Hairline used by every normal blurred element in blur mode: 0.5dp, light, never grey. */
@Composable
fun Modifier.xvoxThinEdge(shape: Shape): Modifier {
    val colors = XvoxTheme.colors
    val hi = Color.White.copy(alpha = if (colors.isLight) .85f else .34f)
    val lo = Color.White.copy(alpha = if (colors.isLight) .35f else .10f)
    return drawWithContent {
        drawContent()
        val brush = Brush.linearGradient(listOf(hi, lo, hi.copy(alpha = hi.alpha * .7f)), start = Offset.Zero, end = Offset(size.width, size.height))
        drawPath(outlinePath(shape.createOutline(size, layoutDirection, this)), brush, style = Stroke(width = .5.dp.toPx() * 2f))
    }
}

/**
 * Blur-mode border, shared by EVERY blurred element and styled like the liquid navbar edge:
 * a thin specular rim, bright on the light-facing corner and fading across the surface.
 */
@Composable
fun Modifier.xvoxGlassEdge(shape: Shape): Modifier {
    val colors = XvoxTheme.colors
    val rim = if (colors.isLight) colors.primaryText else Color.White
    val strong = rim.copy(alpha = if (colors.isLight) .42f else .78f)
    val faint = rim.copy(alpha = if (colors.isLight) .10f else .14f)
    return drawWithContent {
        drawContent()
        val brush = Brush.linearGradient(
            colorStops = arrayOf(0f to strong, .28f to faint, .62f to faint, .85f to strong.copy(alpha = strong.alpha * .55f), 1f to faint),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
        drawPath(outlinePath(shape.createOutline(size, layoutDirection, this)), brush, style = Stroke(width = 1.dp.toPx() * 2f))
    }
}

private fun DrawScope.outlinePath(outline: Outline): Path = when (outline) {
    is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
    is Outline.Generic -> outline.path
}
