package com.xvox.music.core.ui.blur

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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle
import com.xvox.music.core.ui.chrome.parseHexColor
import kotlin.math.ceil
import kotlin.math.roundToInt

/*
 * ENGINE (GPU, zero bitmaps):
 *  - A Sky is ONE GraphicsLayer that records its source subtree (the "layer" being blurred).
 *  - The source is rasterised once into a cached GPU texture (CompositingStrategy.Offscreen), so
 *    ten blurred cards share one render of the wallpaper/page instead of re-drawing it ten times.
 *  - Every glass surface owns a small padded GraphicsLayer that draws ONLY its own window of the
 *    Sky and blurs it with a native RenderEffect (Gaussian, GPU). No CPU/native blur, no Bitmap.
 *  - A Sky records ONLY while at least one surface is listening; otherwise it costs nothing.
 *  - A surface placed inside the Sky it samples is detected and falls back to flat material, so
 *    a layer can never recurse into itself.
 */

/** Gaussian sigma per "radius px" of the old API. Raise for a heavier frost, lower for lighter. */
private const val SigmaPerRadius = 0.5f
/** Hard GPU safety cap (px). Keeps extreme slider values from allocating oversized layers. */
private const val MaxSigmaPx = 96f

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
// Sky: the shared, GPU-cached source layer
// ---------------------------------------------------------------------------------------------

@Stable
class Sky internal constructor(internal val layer: GraphicsLayer) {
    internal var coordinates: LayoutCoordinates? = null
    internal var source: SkySourceNode? = null
    internal var hasContent = false
    internal var recording = false
    private val consumers = ArrayList<BackdropBlurNode>()

    init {
        // Render the source once into a hardware texture; every surface then samples that texture.
        layer.compositingStrategy = CompositingStrategy.Offscreen
    }

    /** Re-capture the source and refresh every surface (same call-sites as before). */
    fun invalidate() {
        source?.requestRecord()
        notifyConsumers()
    }

    internal val isActive: Boolean get() = consumers.isNotEmpty()

    internal fun addConsumer(node: BackdropBlurNode) {
        if (!consumers.contains(node)) consumers.add(node)
        if (consumers.size == 1) source?.requestRecord()
    }

    internal fun removeConsumer(node: BackdropBlurNode) {
        consumers.remove(node)
        if (consumers.isEmpty()) {
            hasContent = false
            source?.requestRecord()
        }
    }

    internal fun notifyConsumers() {
        for (i in consumers.indices.reversed()) {
            val node = consumers.getOrNull(i) ?: continue
            if (!node.nested) node.requestRedraw()
        }
    }
}

@Composable
fun rememberSky(): Sky {
    val layer = rememberGraphicsLayer()
    return remember(layer) { Sky(layer) }
}

/** Put on the source (page / wallpaper / list). It records only while a surface listens. */
fun Modifier.sky(sky: Sky): Modifier = this.then(SkySourceElement(sky))

private data class SkySourceElement(val sky: Sky) : ModifierNodeElement<SkySourceNode>() {
    override fun create() = SkySourceNode(sky)
    override fun update(node: SkySourceNode) = node.swap(sky)
}

internal class SkySourceNode(private var sky: Sky) :
    Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    override fun onAttach() {
        sky.source = this
    }

    override fun onDetach() {
        release(sky)
    }

    private fun release(target: Sky) {
        if (target.source === this) {
            target.source = null
            target.coordinates = null
            target.hasContent = false
        }
    }

    fun swap(next: Sky) {
        if (next === sky) return
        release(sky)
        sky = next
        sky.source = this
        invalidateDraw()
    }

    fun requestRecord() {
        if (isAttached) invalidateDraw()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        sky.coordinates = coordinates
    }

    override fun ContentDrawScope.draw() {
        val w = size.width.roundToInt()
        val h = size.height.roundToInt()
        val target = sky
        // Nobody listening (or nothing drawable): behave exactly like a plain Box. Zero cost.
        if (!target.isActive || w <= 0 || h <= 0 || target.layer.isReleased) {
            target.hasContent = false
            drawContent()
            return
        }
        try {
            target.recording = true
            try {
                target.layer.record(this, layoutDirection, IntSize(w, h)) {
                    this@draw.drawContent()
                }
            } finally {
                target.recording = false
            }
            drawLayer(target.layer)
            target.hasContent = true
            target.notifyConsumers()
        } catch (_: RuntimeException) {
            target.hasContent = false
            drawContent()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Glass surface: native GPU backdrop blur of its own window of the Sky
// ---------------------------------------------------------------------------------------------

private data class BackdropBlurElement(
    val sky: Sky,
    val layer: GraphicsLayer,
    val sigmaPx: Float,
    val tint: Color,
    val fallback: Color
) : ModifierNodeElement<BackdropBlurNode>() {
    override fun create() = BackdropBlurNode(sky, layer, sigmaPx, tint, fallback)
    override fun update(node: BackdropBlurNode) = node.update(sky, layer, sigmaPx, tint, fallback)
}

internal class BackdropBlurNode(
    private var sky: Sky,
    private var layer: GraphicsLayer,
    private var sigmaPx: Float,
    private var tint: Color,
    private var fallback: Color
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var coords: LayoutCoordinates? = null
    private var lastRel: Offset = Offset.Unspecified
    private var appliedSigma = -1f
    /** True while this surface sits inside the Sky it samples; it then stays flat (no recursion). */
    internal var nested = false

    override fun onAttach() {
        sky.addConsumer(this)
    }

    override fun onDetach() {
        sky.removeConsumer(this)
        coords = null
        lastRel = Offset.Unspecified
    }

    fun update(
        newSky: Sky,
        newLayer: GraphicsLayer,
        newSigma: Float,
        newTint: Color,
        newFallback: Color
    ) {
        if (newSky !== sky) {
            sky.removeConsumer(this)
            sky = newSky
            sky.addConsumer(this)
        }
        if (newLayer !== layer) {
            layer = newLayer
            appliedSigma = -1f
        }
        sigmaPx = newSigma
        tint = newTint
        fallback = newFallback
        invalidateDraw()
    }

    fun requestRedraw() {
        if (isAttached) invalidateDraw()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        coords = coordinates
        val rel = relativeOrigin()
        if (rel != lastRel) {
            lastRel = rel
            invalidateDraw()
        }
    }

    private fun relativeOrigin(): Offset {
        val me = coords ?: return Offset.Unspecified
        val src = sky.coordinates ?: return Offset.Unspecified
        if (!me.isAttached || !src.isAttached) return Offset.Unspecified
        return try {
            src.localPositionOf(me, Offset.Zero)
        } catch (_: IllegalArgumentException) {
            Offset.Unspecified
        }
    }

    override fun ContentDrawScope.draw() {
        if (!drawBackdrop()) drawRect(fallback)
        drawContent()
    }

    private fun DrawScope.drawBackdrop(): Boolean {
        if (sky.recording) {
            nested = true
            return false
        }
        nested = false
        if (!sky.hasContent || layer.isReleased || sky.layer.isReleased) return false
        val w = ceil(size.width).toInt()
        val h = ceil(size.height).toInt()
        if (w <= 0 || h <= 0) return false
        val rel = relativeOrigin()
        if (rel == Offset.Unspecified) return false
        return try {
            val sigma = sigmaPx.coerceIn(1f, MaxSigmaPx)
            // Padding lets the blur read real pixels beyond the edges (no dark/clamped rim).
            val pad = ceil(sigma * 2.5f).toInt().coerceAtLeast(1)
            val source = sky.layer
            layer.record(this, layoutDirection, IntSize(w + 2 * pad, h + 2 * pad)) {
                translate(pad - rel.x, pad - rel.y) { drawLayer(source) }
            }
            layer.clip = true
            layer.topLeft = IntOffset(-pad, -pad)
            if (appliedSigma != sigma) {
                layer.renderEffect = BlurEffect(sigma, sigma, TileMode.Clamp)
                appliedSigma = sigma
            }
            drawLayer(layer)
            if (tint.alpha > 0f) drawRect(tint)
            true
        } catch (_: RuntimeException) {
            false
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Public modifiers (unchanged API)
// ---------------------------------------------------------------------------------------------

/**
 * Clean blur surface: clip + live GPU backdrop blur + flat tint. No shadow, no gloss.
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
    val density = LocalDensity.current
    val chrome = LocalXvoxChromeStyle.current
    // The existing transparency slider changes the real material alpha. While live blur is on,
    // it also increases the blur reach so a more transparent card remains visibly frosted rather
    // than merely looking faint. With blur off we skip the blur entirely: the slider then
    // controls only the ordinary material transparency.
    val globalTransparency = LocalXvoxGlassTransparency.current.coerceIn(0f, .60f)
    val surfaceTransparency = (1f - baseColor.alpha).coerceIn(0f, 1f)
    val transparencyDrive = maxOf(globalTransparency, surfaceTransparency)
    val configuredBlur = chrome.glassBlurIntensity.coerceIn(0f, 1f)
    val radiusScale = configuredBlur * (.72f + transparencyDrive * .78f)
    val sigmaPx = remember(level, density.density, radiusScale) {
        (level.radius.value * density.density * radiusScale * SigmaPerRadius)
            .coerceIn(1f, MaxSigmaPx)
    }
    // Preserve the material's alpha so the transparency slider stays effective.
    val tint = remember(baseColor, level) {
        baseColor.copy(alpha = (baseColor.alpha * level.tintAlpha).coerceIn(0f, 1f))
    }
    val fallback = remember(baseColor, level) {
        baseColor.copy(alpha = (baseColor.alpha * level.fallbackAlpha).coerceIn(0f, 1f))
    }
    val clipped = clip(shape)
    val blurOn = chrome.glassBlurEnabled && configuredBlur > .001f
    val surface = when {
        // Master off and zero intensity both keep the exact transparency selected by the user.
        !blurOn -> clipped.background(baseColor)
        // No valid source: the established readable material fallback.
        sky == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S ->
            clipped.background(fallback)
        else -> {
            val layer = rememberGraphicsLayer()
            clipped.then(BackdropBlurElement(sky, layer, sigmaPx, tint, fallback))
        }
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
fun rememberXvoxContentSky(): Sky = rememberSky()

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
