package com.xvox.music.core.ui.blur

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skydoves.cloudy.Sky
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle
import com.xvox.music.core.ui.chrome.parseHexColor
import kotlin.math.roundToInt

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

/**
 * Clean blur surface: clip + Cloudy backdrop blur + flat tint. No shadow, no gloss.
 * Radius is converted dp -> px for Cloudy, and everything is remembered so playback ticks and
 * scrolling never rebuild it (that is what causes flicker).
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
    // than merely looking faint. With blur off we deliberately skip Cloudy entirely: the slider
    // then controls only the ordinary material transparency.
    val globalTransparency = LocalXvoxGlassTransparency.current.coerceIn(0f, .60f)
    val surfaceTransparency = (1f - baseColor.alpha).coerceIn(0f, 1f)
    val transparencyDrive = maxOf(globalTransparency, surfaceTransparency)
    val configuredBlur = chrome.glassBlurIntensity.coerceIn(0f, 1f)
    val radiusScale = configuredBlur * (.72f + transparencyDrive * .78f)
    val radiusPx = remember(level, density.density, radiusScale) {
        (level.radius.value * density.density * radiusScale).roundToInt().coerceAtLeast(1)
    }
    // Preserve the material's alpha. Previously `.copy(alpha = level.tintAlpha)` accidentally
    // discarded the user-controlled transparency and made the slider appear ineffective.
    val tint = remember(baseColor, level) {
        baseColor.copy(alpha = (baseColor.alpha * level.tintAlpha).coerceIn(0f, 1f))
    }
    val clipped = clip(shape)
    val liveBlurEnabled = chrome.glassBlurEnabled && configuredBlur > .001f && sky != null
    val surface = when {
        // Master off and zero intensity both keep the exact transparency selected by the user.
        !chrome.glassBlurEnabled || configuredBlur <= .001f -> clipped.background(baseColor)
        // A surface with no valid source receives the established readable material fallback.
        !liveBlurEnabled -> clipped.background(baseColor.copy(alpha = baseColor.alpha * level.fallbackAlpha))
        else -> clipped.cloudy(sky = sky!!, radius = radiusPx, tint = tint, shape = shape)
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
@Composable
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
