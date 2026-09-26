package com.xvox.music.player.nowplaying.lyrics

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.design.theme.XvoxUiFont
import com.xvox.music.data.preferences.LyricsSettings
import kotlin.math.abs

/** Keep the background intact; only the lyric layer fades, with independently visible edge regions. */
fun Modifier.lyricsEdgeFade(top: Float, bottom: Float): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    val t = top.coerceIn(0f, .45f); val b = bottom.coerceIn(0f, .45f)
    val stops = mutableListOf<Pair<Float, Color>>()
    stops += 0f to if (t == 0f) Color.White else Color.Transparent
    if (t > 0f) { stops += t * .45f to Color.White.copy(alpha = .08f); stops += t * .8f to Color.White.copy(alpha = .45f); stops += t to Color.White }
    stops += (1 - b) to Color.White
    if (b > 0f) { stops += (1 - b * .8f) to Color.White.copy(alpha = .45f); stops += (1 - b * .45f) to Color.White.copy(alpha = .08f) }
    stops += 1f to if (b == 0f) Color.White else Color.Transparent
    drawRect(Brush.verticalGradient(colorStops = stops.toTypedArray()), blendMode = BlendMode.DstIn)
}

@Composable
fun LyricPresentationLine(
    text: String,
    active: Boolean,
    distance: Int,
    settings: LyricsSettings,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    synchronized: Boolean = true
) {
    val colors = XvoxTheme.colors
    val themedColor = color.takeIf { it != Color.Unspecified && it != Color.Transparent }
        ?: colors.primaryText
    // With individual sizing off, the master size is genuinely used by every line.
    // Per-line values remain saved, but cannot affect presentation until the toggle is enabled.
    val maximumSize = if (settings.individualLineSizes) {
        maxOf(settings.currentSize, maxOf(settings.topSize, settings.bottomSize))
    } else {
        settings.currentSize
    }
    val targetSize = if (settings.individualLineSizes) {
        when {
            distance < 0 -> settings.topSize
            distance == 0 -> settings.currentSize
            else -> settings.bottomSize
        }
    } else {
        settings.currentSize
    }
    val wantedScale = targetSize.toFloat() / maximumSize.toFloat()

    val textAlign = when (settings.alignment) {
        "left" -> TextAlign.Start
        "right" -> TextAlign.End
        else -> TextAlign.Center
    }

    val transformOrigin = when (settings.alignment) {
        "left" -> TransformOrigin(0.5f, 0.5f)
        "right" -> TransformOrigin(0.5f, 0.5f)
        else -> TransformOrigin(0.5f, 0.5f)
    }

    val animStyle = when (settings.animation) {
        "glide", "drift" -> "glide"
        "rise", "wave" -> "rise"
        "pop", "aurora" -> "pop"
        else -> "off"
    }

    // Dynamic Entrance / Exit motion specs
    val transitionSpec: AnimationSpec<Float> = when (animStyle) {
        "glide" -> tween(300, easing = FastOutSlowInEasing)
        "rise" -> spring(dampingRatio = 0.72f, stiffness = 400f)
        "pop" -> spring(dampingRatio = 0.55f, stiffness = 500f)
        else -> tween(180, easing = LinearEasing)
    }

    val animatedScale by animateFloatAsState(
        targetValue = when (animStyle) {
            "pop" -> if (active) wantedScale * 1.04f else wantedScale * 0.92f
            "rise" -> if (active) wantedScale else wantedScale * 0.96f
            else -> wantedScale
        },
        animationSpec = transitionSpec,
        label = "lineScale"
    )

    // Keep neighbouring lines readable enough to provide lyric context, especially when Cover
    // matching is selected and its hue is close to the adaptive player background.
    val baseAlpha = when (abs(distance)) { 0 -> 1f; 1 -> .78f; 2 -> .52f; else -> .34f }
    val dimTarget = if (settings.fadeEqual) {
        if (distance == 0) 1f else (1f - settings.fadeIntensity * .82f).coerceIn(.28f, 1f)
    } else baseAlpha

    val alpha by animateFloatAsState(
        if (!synchronized) .85f else dimTarget,
        tween(240, easing = FastOutSlowInEasing),
        label = "lineAlpha"
    )

    val shiftX by animateFloatAsState(
        targetValue = when (animStyle) {
            "glide" -> if (active) 0f else (if (distance < 0) -22f else 22f)
            else -> 0f
        },
        animationSpec = transitionSpec,
        label = "lineShiftX"
    )

    val shiftY by animateFloatAsState(
        targetValue = when (animStyle) {
            "rise" -> if (active) 0f else (if (distance > 0) 14f else -14f)
            "pop" -> if (active) 0f else (if (distance > 0) 5f else -5f)
            else -> 0f
        },
        animationSpec = transitionSpec,
        label = "lineShiftY"
    )

    // The caller supplies the lyric-only Cover / Black / White color; player chrome remains themed elsewhere.
    val resolvedColor = if (active) themedColor else themedColor.copy(alpha = .88f)
    // Colour separation is resolved before this composable; lyrics intentionally have no glow
    // or shadow layer, so cover matching remains clean and typographic.
    val linePaddingVertical = (settings.lineGap / 2f).coerceAtLeast(4f).dp

    Text(
        text = text.ifBlank { "♪" },
        color = resolvedColor,
        style = TextStyle(
            fontFamily = XvoxUiFont,
            fontSize = maximumSize.sp,
            lineHeight = (maximumSize * 1.30f).sp,
            fontWeight = if (active) FontWeight(settings.fontWeight) else FontWeight((settings.fontWeight - 150).coerceAtLeast(300)),
            textAlign = textAlign,
            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(
                includeFontPadding = false
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = animatedScale
                this.scaleY = animatedScale
                this.translationX = shiftX.dp.toPx()
                this.translationY = shiftY.dp.toPx()
                this.transformOrigin = transformOrigin
            }
            .padding(
                horizontal = if (settings.alignment == "center") 16.dp else 10.dp,
                vertical = linePaddingVertical
            )
    )
}
