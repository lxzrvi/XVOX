package com.xvox.music.player.nowplaying.lyrics

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    color: Color = Color.White,
    synchronized: Boolean = true
) {
    val maximumSize = maxOf(settings.currentSize, maxOf(settings.topSize, settings.bottomSize))
    val targetSize = when {
        distance < 0 -> settings.topSize
        distance == 0 -> settings.currentSize
        else -> settings.bottomSize
    }
    val wantedScale = targetSize.toFloat() / maximumSize.toFloat()

    val textAlign = when (settings.alignment) {
        "left" -> TextAlign.Start
        "right" -> TextAlign.End
        else -> TextAlign.Center
    }

    val transformOrigin = when (settings.alignment) {
        "left" -> TransformOrigin(0f, 0.5f)
        "right" -> TransformOrigin(1f, 0.5f)
        else -> TransformOrigin(0.5f, 0.5f)
    }

    val aligned = settings.alignment != "center"
    val animation = if (aligned) "fade" else settings.animation

    val spec: AnimationSpec<Float> = when (animation) {
        "spring" -> spring(dampingRatio = .55f, stiffness = 420f)
        "slide" -> tween(300, easing = FastOutSlowInEasing)
        "rise" -> tween(380, easing = LinearOutSlowInEasing)
        "wave" -> tween(420, easing = CubicBezierEasing(0.34f, 1.3f, 0.64f, 1f))
        else -> tween(240, easing = LinearEasing) // "fade"
    }

    val scale by animateFloatAsState(wantedScale, spec, label = "lyricScale")

    val baseAlpha = when (abs(distance)) { 0 -> 1f; 1 -> .62f; 2 -> .34f; else -> .2f }
    val dimTarget = if (settings.fadeEqual) {
        if (distance == 0) 1f else (1f - settings.fadeIntensity).coerceIn(0f, 1f)
    } else baseAlpha
    val alpha by animateFloatAsState(
        if (!synchronized) .82f else dimTarget,
        tween(240, easing = FastOutSlowInEasing),
        label = "lyricAlpha"
    )

    val slideY = animation == "slide" && !active
    val riseY = animation == "rise" && !active && distance > 0
    val springY = animation == "spring" && !active
    val shiftY by animateFloatAsState(
        when {
            slideY -> distance.coerceIn(-1, 1) * 20f
            riseY -> distance.coerceIn(1, 6) * 28f
            springY -> distance.coerceIn(-1, 1) * 12f
            else -> 0f
        }, spec, label = "lyricShiftY"
    )

    Text(
        text = text.ifBlank { "♪" },
        color = color,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = maximumSize.sp,
            lineHeight = (maximumSize * 1.3f).sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = textAlign
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
                this.translationY = shiftY.dp.toPx()
                this.transformOrigin = transformOrigin
            }
            .padding(horizontal = if (aligned) 8.dp else 18.dp, vertical = 7.dp)
    )
}
