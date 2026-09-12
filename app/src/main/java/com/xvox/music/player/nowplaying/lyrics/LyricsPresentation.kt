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
import com.xvox.music.core.ui.chrome.parseHexColor
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

    val animation = settings.animation

    val scaleSpec: AnimationSpec<Float> = when (animation) {
        "drift" -> tween(300, easing = FastOutSlowInEasing)
        "aurora" -> spring(dampingRatio = 0.58f, stiffness = 420f)
        "wave" -> tween(380, easing = CubicBezierEasing(0.34f, 1.35f, 0.64f, 1f))
        else -> tween(200, easing = LinearEasing)
    }

    val scale by animateFloatAsState(wantedScale, scaleSpec, label = "lyricScale")

    val baseAlpha = when (abs(distance)) { 0 -> 1f; 1 -> .62f; 2 -> .34f; else -> .2f }
    val dimTarget = if (settings.fadeEqual) {
        if (distance == 0) 1f else (1f - settings.fadeIntensity).coerceIn(0f, 1f)
    } else baseAlpha

    val alpha by animateFloatAsState(
        if (!synchronized) .82f else dimTarget,
        tween(240, easing = FastOutSlowInEasing),
        label = "lyricAlpha"
    )

    val shiftX by animateFloatAsState(
        when (animation) {
            "drift" -> if (active) 0f else (if (distance < 0) -18f else 18f)
            else -> 0f
        },
        scaleSpec,
        label = "lyricShiftX"
    )

    val shiftY by animateFloatAsState(
        when (animation) {
            "wave" -> if (active) -6f else distance.coerceIn(-1, 1) * 14f
            "aurora" -> if (active) -2f else distance.coerceIn(-1, 1) * 8f
            "drift" -> if (active) 0f else distance.coerceIn(-1, 1) * 10f
            else -> 0f
        },
        scaleSpec,
        label = "lyricShiftY"
    )

    val resolvedColor = if (active) {
        if (settings.matchCoverColor) color else Color.White
    } else {
        if (settings.matchCoverColor) color.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.75f)
    }

    val linePaddingVertical = (settings.lineGap / 2f).dp

    Text(
        text = text.ifBlank { "♪" },
        color = resolvedColor,
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
