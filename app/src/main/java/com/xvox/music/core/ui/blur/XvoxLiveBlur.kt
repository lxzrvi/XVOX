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
import com.xvox.music.core.ui.chrome.parseHexColor
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
    Chrome(24.dp, .50f, .84f),
    /** Root sheets and boxes: denser glass over the live page. */
    Sheet(30.dp, .57f, .88f),
    /** Free area around an open sheet: strongest, still readable. */
    SheetBackdrop(36.dp, .20f, .52f),
    /** Bars over scrolling content: dense enough to keep text readable. */
    Bar(28.dp, .72f, .92f)
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
    Low(16f, false),
    Mid(32f, true),
    High(64f, true)
}

private fun detectBlurTier(context: Context): BlurTier {
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
 * The border follows Settings > Appearance > Glass border (see xvoxGlassBorder).
 */
@Composable
fun Modifier.xvoxLiveBlur(
    shape: Shape,
    level: XvoxBlurLevel = XvoxBlurLevel.Card,
    baseColor: Color = XvoxTheme.colors.card,
    sky: Sky? = LocalXvoxSurfaceSky.current,
    borderColor: Color = XvoxTheme.colors.cardBorder,
    borderAlpha: Float = .62f,
    borderWidth: Dp = .8.dp
): Modifier {
    val chrome = LocalXvoxChromeStyle.current
    val context = LocalContext.current
    val tier = remember(context) { detectBlurTier(context) }
    // The transparency slider changes the real material alpha and, while blur is on, widens the
    // blur so a more transparent card stays visibly frosted instead of merely faint.
    val globalTransparency = LocalXvoxGlassTransparency.current.coerceIn(0f, .60f)
    val surfaceTransparency = (1f - baseColor.alpha).coerceIn(0f, 1f)
    val transparencyDrive = maxOf(globalTransparency, surfaceTransparency)
    val configuredBlur = chrome.glassBlurIntensity.coerceIn(0f, 1f)
    val radiusScale = configuredBlur * (.72f + transparencyDrive * .78f)
    val blurDp = remember(level, radiusScale, tier) {
        (level.radius.value * radiusScale * BlurPerRadius).coerceIn(1f, tier.maxBlurDp)
    }
    val tint = remember(baseColor, level) {
        baseColor.copy(alpha = (baseColor.alpha * level.tintAlpha).coerceIn(0f, 1f))
    }
    val fallback = remember(baseColor, level) {
        baseColor.copy(alpha = (baseColor.alpha * level.fallbackAlpha).coerceIn(0f, 1f))
    }
    val colors = remember(tint) {
        BlurColors(
            blendColors = listOf(BlendColorEntry(color = tint)),
            saturation = FrostSaturation
        )
    }
    val flatCard = !tier.liveCards &&
        (level == XvoxBlurLevel.Card || level == XvoxBlurLevel.Selection)
    val clipped = clip(shape)
    val blurOn = chrome.glassBlurEnabled && configuredBlur > .001f
    val surface = when {
        // Master off and zero intensity both keep the exact transparency selected by the user.
        !blurOn -> clipped.background(baseColor)
        // No valid source / weak device card: the readable material fallback.
        sky == null || flatCard || Build.VERSION.SDK_INT < Build.VERSION_CODES.S ->
            clipped.background(fallback)
        else -> clipped.textureBlur(
            backdrop = sky.backdrop,
            shape = shape,
            blurRadius = blurDp,
            colors = colors
        )
    }
    return surface.xvoxGlassBorder(shape, borderColor, borderAlpha, borderWidth)
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

/**
 * Global glass border (Settings > Appearance > Glass border):
 *  - off        : no border on any blurred surface
 *  - width      : 0 - 4 dp
 *  - colour     : custom hex, or the surface's own theme colour when empty
 *  - transparency: via glassBorderAlpha
 *  - softness   : a feathered edge made from a few fading strokes (works on every API level)
 * A caller that passes width 0 or alpha 0 (scrim, header) stays borderless.
 */
@Composable
private fun Modifier.xvoxGlassBorder(
    shape: Shape,
    callColor: Color,
    callAlpha: Float,
    callWidth: Dp
): Modifier {
    if (callWidth <= 0.dp || callAlpha <= 0f) return this
    val chrome = LocalXvoxChromeStyle.current
    if (!chrome.glassBorderEnabled) return this
    val widthDp = chrome.glassBorderWidth.coerceIn(0f, 4f)
    if (widthDp <= 0f) return this
    val soft = chrome.glassBorderBlur.coerceIn(0f, 12f)
    val base = parseHexColor(chrome.glassBorderColor) ?: callColor
    val color = base.copy(alpha = (base.alpha * chrome.glassBorderAlpha.coerceIn(0f, 1f)).coerceIn(0f, 1f))

    return drawWithContent {
        drawContent()
        val path = outlinePath(shape.createOutline(size, layoutDirection, this))
        val w = widthDp.dp.toPx()
        val feather = soft.dp.toPx()
        // The outer half of a stroke is clipped by the surface, so strokes are drawn 2x wide.
        if (feather < .5f) {
            drawPath(path = path, color = color, style = Stroke(width = w * 2f))
        } else {
            val steps = 5
            for (k in steps downTo 0) {
                val t = k / steps.toFloat()
                val fade = (1f - t) * (1f - t)
                val a = if (k == 0) color.alpha else color.alpha * fade * .5f
                if (a > .003f) {
                    drawPath(
                        path = path,
                        color = color.copy(alpha = a),
                        style = Stroke(width = (w + feather * t) * 2f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.outlinePath(outline: Outline): Path = when (outline) {
    is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
    is Outline.Generic -> outline.path
}
