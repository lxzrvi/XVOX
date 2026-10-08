package com.xvox.music.core.design.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xvox.music.core.ui.chrome.XvoxChromeStyle

/**
 * App-wide visual experiments. They deliberately sit above the ordinary light/dark choice: the
 * theme choice still supplies the accent and baseline palette, while this mode changes how every
 * shared surface is rendered.
 */
enum class XvoxExperimentalAppearance(val storageValue: String) {
    DEFAULT("default"),
    GLASS("glass"),
    DP_MINIMAL("dp_minimal");

    companion object {
        fun fromStorage(value: String?): XvoxExperimentalAppearance = when (value?.lowercase()) {
            GLASS.storageValue -> GLASS
            DP_MINIMAL.storageValue, "dp minimal", "minimal" -> DP_MINIMAL
            else -> DEFAULT
        }
    }
}

val LocalXvoxExperimentalAppearance = staticCompositionLocalOf { XvoxExperimentalAppearance.DEFAULT }

/**
 * Glass changes the material itself instead of merely dimming the whole app. The alpha lets the
 * blurred atmosphere and user-selected backdrop show through cards, and the pale outline gives
 * each surface a clean reflected edge. DP Minimal intentionally removes that decoration in favour
 * of near-black, text-led surfaces.
 */
fun XvoxPalette.withExperimentalAppearance(mode: XvoxExperimentalAppearance): XvoxPalette = when (mode) {
    XvoxExperimentalAppearance.DEFAULT -> this
    XvoxExperimentalAppearance.GLASS -> {
        val highlight = if (isLight) Color.White else Color(0xFFFFFFFF)
        val fill = if (isLight) Color(0xFFF8FBFF) else Color(0xFF151B28)
        copy(
            surface = fill.copy(alpha = .52f),
            card = fill.copy(alpha = .50f),
            cardElevated = fill.copy(alpha = .64f),
            cardBorder = highlight.copy(alpha = if (isLight) .70f else .38f),
            accentSoft = primaryAccent.copy(alpha = .20f),
            progressTrack = highlight.copy(alpha = if (isLight) .30f else .22f)
        )
    }
    XvoxExperimentalAppearance.DP_MINIMAL -> copy(
        background = Color(0xFF090909),
        surface = Color(0xFF0D0D0D),
        card = Color(0xFF101010),
        cardElevated = Color(0xFF151515),
        cardBorder = Color(0xFFFFFFFF).copy(alpha = .12f),
        primaryText = Color(0xFFF7F7F7),
        secondaryText = Color(0xFFC9C9C9),
        mutedText = Color(0xFF939393),
        accentSoft = primaryAccent.copy(alpha = .13f),
        progressTrack = Color(0xFFFFFFFF).copy(alpha = .17f),
        isLight = false
    )
}

/**
 * The chrome preferences stay durable and untouched while a visual experiment is active. This
 * derived style makes shared option boxes, header, Mini Player, navigation, and card outlines
 * participate in Glass without overwriting a person's Default-mode choices.
 */
fun XvoxChromeStyle.forExperimentalAppearance(mode: XvoxExperimentalAppearance): XvoxChromeStyle = when (mode) {
    XvoxExperimentalAppearance.DEFAULT -> this
    XvoxExperimentalAppearance.GLASS -> copy(
        optionBoxBgAlpha = optionBoxBgAlpha.coerceAtMost(.62f),
        optionBoxBorderAlpha = optionBoxBorderAlpha.coerceAtLeast(.56f),
        headerBgAlpha = headerBgAlpha.coerceAtMost(.54f),
        headerBorderAlpha = headerBorderAlpha.coerceAtLeast(.36f),
        miniBgAlpha = miniBgAlpha.coerceAtMost(.58f),
        miniBorderAlpha = miniBorderAlpha.coerceAtLeast(.62f),
        navBgAlpha = navBgAlpha.coerceAtMost(.58f),
        navBorderAlpha = navBorderAlpha.coerceAtLeast(.62f),
        cardBorderAlpha = cardBorderAlpha.coerceAtLeast(.42f)
    )
    XvoxExperimentalAppearance.DP_MINIMAL -> copy(
        optionBoxBgAlpha = 1f,
        headerBgAlpha = 1f,
        miniBgAlpha = 1f,
        navBgAlpha = 1f,
        headerBorderAlpha = headerBorderAlpha.coerceAtMost(.18f),
        miniBorderAlpha = miniBorderAlpha.coerceAtMost(.22f),
        navBorderAlpha = navBorderAlpha.coerceAtMost(.22f),
        cardBorderAlpha = cardBorderAlpha.coerceAtMost(.28f)
    )
}

/** A restrained diagonal reflection applied after a surface has clipped its own shape. */
fun Modifier.xvoxGlassReflection(): Modifier = composed {
    if (LocalXvoxExperimentalAppearance.current != XvoxExperimentalAppearance.GLASS) {
        this
    } else {
        this.drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = .16f),
                        Color.White.copy(alpha = .035f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * .82f, size.height)
                )
            )
        }
    }
}

/**
 * A deliberately soft, blurred colour field that remains behind all translucent Glass surfaces.
 * Android Compose cannot blur arbitrary content behind a composable on every supported API level;
 * placing this real blurred layer behind the translucent system gives cards, sheets, player, and
 * navigation a consistent backdrop blur/reflection treatment without a device-specific fallback.
 */
@Composable
fun XvoxGlassAtmosphere(modifier: Modifier = Modifier) {
    val colors = XvoxTheme.colors
    Canvas(
        modifier = modifier.blur(64.dp)
    ) {
        val largest = maxOf(size.width, size.height)
        drawCircle(
            color = colors.primaryAccent.copy(alpha = if (colors.isLight) .20f else .27f),
            radius = largest * .34f,
            center = androidx.compose.ui.geometry.Offset(size.width * .15f, size.height * .18f)
        )
        drawCircle(
            color = Color(0xFF5E8CFF).copy(alpha = if (colors.isLight) .13f else .19f),
            radius = largest * .28f,
            center = androidx.compose.ui.geometry.Offset(size.width * .88f, size.height * .38f)
        )
        drawCircle(
            color = Color(0xFFE4A7FF).copy(alpha = if (colors.isLight) .11f else .16f),
            radius = largest * .30f,
            center = androidx.compose.ui.geometry.Offset(size.width * .47f, size.height * .94f)
        )
    }
}
