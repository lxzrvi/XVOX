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
import io.github.neoruaa.glasmio.glass.glassBackdropSource
import io.github.neoruaa.glasmio.glass.render.backdrops.LayerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop

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
    val glass = LocalXvoxDesign.current.language == XvoxDesignLanguage.IOS
    if (!glass) {
        content()
        return
    }
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
        LocalGlassAppearance provides GlassAppearance(),
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
    return this.glassBackdropSource(backdrop)
}
