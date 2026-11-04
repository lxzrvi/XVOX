package com.xvox.music.core.ui.blur

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Capture layers:
 * - Surface: wallpaper/photo behind the app. Ordinary cards, header, navbar blur this.
 * - Overlay: the live app beneath root-hosted sheets.
 * - Content (see rememberXvoxContentHaze): a scrolling list that a bar sits above.
 * A surface must never sit inside the source it samples.
 */
val LocalXvoxSurfaceHazeState = staticCompositionLocalOf<HazeState?> { null }
val LocalXvoxOverlayHazeState = staticCompositionLocalOf<HazeState?> { null }

enum class XvoxBlurLevel(
    val radius: Dp,
    val tintAlpha: Float,
    val fallbackAlpha: Float
) {
    Selection(14.dp, .30f, .72f),
    Card(22.dp, .46f, .82f),
    Chrome(26.dp, .50f, .84f),
    Sheet(32.dp, .57f, .88f),
    SheetBackdrop(36.dp, .20f, .52f),
    /** Bars over scrolling content: dense enough to keep text readable. */
    Bar(30.dp, .72f, .92f)
}

/**
 * Stable live-glass surface. Style and rim brush are remembered, so playback ticks and
 * scrolling never rebuild them (that is what causes flicker).
 * The border is a soft top-to-bottom fading rim, not a hard solid line.
 * borderWidth = 0.dp or borderAlpha = 0f removes it completely.
 */
@Composable
fun Modifier.xvoxLiveBlur(
    shape: Shape,
    level: XvoxBlurLevel = XvoxBlurLevel.Card,
    baseColor: Color = XvoxTheme.colors.card,
    state: HazeState? = LocalXvoxSurfaceHazeState.current,
    borderColor: Color = XvoxTheme.colors.cardBorder,
    borderAlpha: Float = .62f,
    borderWidth: Dp = .8.dp
): Modifier {
    val style = remember(baseColor, level) {
        HazeStyle(
            backgroundColor = baseColor,
            tint = HazeTint(baseColor.copy(alpha = level.tintAlpha)),
            blurRadius = level.radius,
            noiseFactor = .03f,
            fallbackTint = HazeTint(baseColor.copy(alpha = level.fallbackAlpha))
        )
    }
    val edgeAlpha = (borderColor.alpha * borderAlpha.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    val rim = remember(borderColor, edgeAlpha) {
        Brush.verticalGradient(
            listOf(
                borderColor.copy(alpha = edgeAlpha),
                borderColor.copy(alpha = edgeAlpha * .45f)
            )
        )
    }
    val clipped = clip(shape)
    val surface = if (state == null) {
        clipped.background(baseColor.copy(alpha = level.fallbackAlpha))
    } else {
        clipped.hazeEffect(state = state, style = style)
    }
    return if (borderWidth > 0.dp && edgeAlpha > .001f) {
        surface.border(borderWidth, rim, shape)
    } else {
        surface
    }
}

/** Root sheets sample the live app layer, not only the static wallpaper/photo source. */
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
    state = LocalXvoxOverlayHazeState.current,
    borderColor = borderColor,
    borderAlpha = borderAlpha,
    borderWidth = borderWidth
)

/**
 * Drop-in for a flat `.background(colors.card)` on a big static surface:
 * live blur of the app background, no border.
 */
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

/** For a bar over a scrolling list: one state, shared by the list (source) and the bar (effect). */
@Composable
fun rememberXvoxContentHaze(): HazeState = rememberHazeState()

/** Put on the scrolling list. Use it before any background modifiers. */
fun Modifier.xvoxBlurSource(state: HazeState): Modifier = hazeSource(state = state)

/** Put on a SIBLING bar drawn above the list; it blurs the list scrolling beneath it. */
@Composable
fun Modifier.xvoxBarBlur(
    state: HazeState,
    shape: Shape = RectangleShape,
    baseColor: Color = XvoxTheme.colors.background
): Modifier = xvoxLiveBlur(
    shape = shape,
    level = XvoxBlurLevel.Bar,
    baseColor = baseColor,
    state = state,
    borderAlpha = 0f,
    borderWidth = 0.dp
)
