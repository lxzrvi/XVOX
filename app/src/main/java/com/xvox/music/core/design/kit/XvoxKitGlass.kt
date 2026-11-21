package com.xvox.music.core.design.kit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.xvox.music.core.design.theme.LocalXvoxDesign
import com.xvox.music.core.design.theme.XvoxDesignLanguage
import com.xvox.music.core.design.theme.XvoxTheme
import io.github.neoruaa.glasmio.core.GlassAppearance
import io.github.neoruaa.glasmio.core.LocalGlassAppearance
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.core.defaultGlassColors
import io.github.neoruaa.glasmio.glass.GlasmioBackdropProvider
import io.github.neoruaa.glasmio.ui.GlasmioTextSelectionHost
import io.github.neoruaa.glasmio.glass.glassBackdropSource
import io.github.neoruaa.glasmio.glass.render.backdrops.LayerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.glass.trackBackdropPosition

/**
 * XVOX design engine.
 *
 * The application owns data, layout and navigation. Presentation is delegated to two upstream
 * engines, selected by the persisted design language:
 *
 *  - `ios`      -> Glasmio (Liquid Glass material, optical refraction, iOS motion vocabulary).
 *  - `material` -> Material 3 Expressive (JustForPixel ExpressiveLab components and springs).
 *  - `xvox`     -> the original XVOX surface language, kept intact.
 *
 * Glass components cannot render without a backdrop, so the engine publishes the application's
 * real backdrop through [LocalXvoxGlassBackdrop] on the root background layer. Everything that
 * draws glass samples the live wallpaper/gradient instead of a flat fill.
 */
val LocalXvoxGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** True while the Glasmio glass host is mounted, so kit surfaces may use the native material. */
val LocalXvoxGlassReady = staticCompositionLocalOf { false }

/** Material 3 Expressive is the second half of the engine; the iOS half is Glasmio. */
@Composable
fun xvoxUsesGlass(): Boolean = LocalXvoxDesign.current.language == XvoxDesignLanguage.IOS

@Composable
fun xvoxUsesExpressive(): Boolean = LocalXvoxDesign.current.language == XvoxDesignLanguage.MATERIAL

@Composable
fun xvoxUsesGlassBackdrop(): LayerBackdrop? = LocalXvoxGlassBackdrop.current

/**
 * Root wrapper. In iOS mode it publishes Glasmio's colour/appearance tokens (mapped onto the
 * active XVOX palette so accent, text and separators stay on brand) and mounts the backdrop
 * provider that every glass surface samples. Other languages pass straight through.
 */
@Composable
fun XvoxDesignEngine(content: @Composable () -> Unit) {
    val colors = XvoxTheme.colors
    // Every language keeps the glass host mounted: the stage and the kit always need a live
    // backdrop, and switching the UI style must never leave glass without a source.
    val backdrop = rememberLayerBackdrop()
    val glassColors = remember(colors) {
        defaultGlassColors(isDark = !colors.isLight, accent = colors.primaryAccent).copy(
            content = colors.primaryText,
            secondaryContent = colors.secondaryText,
            tertiaryContent = colors.mutedText,
            container = colors.card,
            prominentContainer = colors.primaryAccent.copy(alpha = .86f),
            subtleStroke = colors.cardBorder,
            groupedBackground = colors.background,
            elevatedBackground = colors.cardElevated,
            separator = colors.cardBorder,
            destructive = com.xvox.music.core.design.theme.XvoxRed
        )
    }
    CompositionLocalProvider(
        LocalGlassColors provides glassColors,
        LocalGlassAppearance provides GlassAppearance(
            liquidGlassEnabled = !com.xvox.music.core.XvoxGraphicsGuard.safe.value
        ),
        LocalXvoxGlassBackdrop provides backdrop,
        LocalXvoxGlassReady provides true
    ) {
        GlasmioBackdropProvider(backdrop = backdrop) {
            // One host for the whole window: every Glasmio text field below gets the glass caret
            // loupe, selection handles and the liquid Edit Menu. Popup windows (menus, sheets)
            // are separate windows and mount their own host.
            GlasmioTextSelectionHost {
                content()
            }
        }
    }
}

/**
 * Provides everything Glasmio's glass needs for one backdrop: the palette mapped onto the active
 * XVOX colours, the appearance tokens, and the backdrop itself. Used by the rebuild stage and by
 * [XvoxDesignEngine] so both stay in sync.
 */
@Composable
fun XvoxGlassEnvironment(
    backdrop: LayerBackdrop,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val glassColors = remember(colors) {
        defaultGlassColors(isDark = !colors.isLight, accent = colors.primaryAccent).copy(
            content = colors.primaryText,
            secondaryContent = colors.secondaryText,
            tertiaryContent = colors.mutedText,
            container = colors.card,
            prominentContainer = colors.primaryAccent.copy(alpha = .86f),
            subtleStroke = colors.cardBorder,
            groupedBackground = colors.background,
            elevatedBackground = colors.cardElevated,
            separator = colors.cardBorder,
            destructive = com.xvox.music.core.design.theme.XvoxRed
        )
    }
    CompositionLocalProvider(
        LocalGlassColors provides glassColors,
        LocalGlassAppearance provides GlassAppearance(
            liquidGlassEnabled = !com.xvox.music.core.XvoxGraphicsGuard.safe.value
        ),
        LocalXvoxGlassBackdrop provides backdrop,
        LocalXvoxGlassReady provides true
    ) {
        GlasmioBackdropProvider(backdrop = backdrop) {
            content()
        }
    }
}

/**
 * Marks the layer that glass samples. Applied to the root background (wallpaper, gradient or
 * theme fill) so refraction and frost stay faithful to whatever is actually behind the UI.
 */
@Composable
fun Modifier.xvoxGlassSource(): Modifier {
    val backdrop = LocalXvoxGlassBackdrop.current ?: return this
    return this
        .glassBackdropSource(backdrop)
        .trackBackdropPosition(backdrop)
}
