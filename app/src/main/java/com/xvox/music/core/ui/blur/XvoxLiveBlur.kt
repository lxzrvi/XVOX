package com.xvox.music.core.ui.blur

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * The app has two Haze capture layers:
 * - [LocalXvoxSurfaceHazeState] captures the wallpaper/photo and is used by ordinary cards.
 * - [LocalXvoxOverlayHazeState] captures the live app beneath root-hosted sheets.
 *
 * Keeping them separate is intentional. A card is never placed inside the source it samples, while
 * a sheet can still blur the actual currently visible page beneath it.
 */
val LocalXvoxSurfaceHazeState = staticCompositionLocalOf<HazeState?> { null }
val LocalXvoxOverlayHazeState = staticCompositionLocalOf<HazeState?> { null }

enum class XvoxBlurLevel(
    val radius: Dp,
    val tintAlpha: Float,
    val fallbackAlpha: Float
) {
    /** Quiet selected states, so selected pills remain visibly distinct instead of becoming foggy. */
    Selection(14.dp, .30f, .72f),
    /** Regular song, playlist, search, and Settings cards. */
    Card(22.dp, .46f, .82f),
    /** Header, mini-player, navigation, and compact action groups. */
    Chrome(26.dp, .50f, .84f),
    /** Root-hosted sheets intentionally carry a slightly denser glass treatment. */
    Sheet(32.dp, .57f, .88f),
    /** The free area around an open sheet gets the strongest, still-readable blur. */
    SheetBackdrop(36.dp, .20f, .52f)
}

/**
 * A stable live-glass surface. [HazeStyle] is remembered so pointer moves, playback ticks, and
 * scrolling do not allocate a new style or cause a flickering effect node.
 *
 * On a platform where hardware blur is unavailable Haze renders [fallbackTint], keeping the same
 * contrast and border language rather than presenting a broken/empty glass surface.
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
    val edge = borderColor.copy(alpha = borderColor.alpha * borderAlpha.coerceIn(0f, 1f))
    val clipped = clip(shape)
    return if (state == null) {
        clipped
            .background(baseColor.copy(alpha = level.fallbackAlpha))
            .border(borderWidth, edge, shape)
    } else {
        clipped
            .hazeEffect(state = state, style = style)
            .border(borderWidth, edge, shape)
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
