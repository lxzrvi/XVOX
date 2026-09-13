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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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

    // Dynamic continuous looping animation transitions for active line
    val infiniteTransition = rememberInfiniteTransition(label = "lyricMotionLoop")
    val continuousPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuousPhase"
    )

    val scaleSpec: AnimationSpec<Float> = when (animation) {
        "drift" -> tween(320, easing = FastOutSlowInEasing)
        "aurora" -> spring(dampingRatio = 0.65f, stiffness = 380f)
        "wave" -> spring(dampingRatio = 0.70f, stiffness = 350f)
        else -> tween(200, easing = LinearEasing)
    }

    val baseScale by animateFloatAsState(wantedScale, scaleSpec, label = "lyricBaseScale")

    val activeScaleMultiplier = if (active && synchronized) {
        when (animation) {
            "aurora" -> 1f + 0.035f * sin(continuousPhase * 1.5f)
            "wave" -> 1f + 0.02f * sin(continuousPhase)
            else -> 1f
        }
    } else 1f

    val finalScale = baseScale * activeScaleMultiplier

    val baseAlpha = when (abs(distance)) { 0 -> 1f; 1 -> .62f; 2 -> .34f; else -> .2f }
    val dimTarget = if (settings.fadeEqual) {
        if (distance == 0) 1f else (1f - settings.fadeIntensity).coerceIn(0f, 1f)
    } else baseAlpha

    val alpha by animateFloatAsState(
        if (!synchronized) .85f else dimTarget,
        tween(240, easing = FastOutSlowInEasing),
        label = "lyricAlpha"
    )

    val dynamicShiftX = if (active && synchronized && animation == "drift") {
        sin(continuousPhase) * 6f
    } else 0f

    val dynamicShiftY = if (active && synchronized && animation == "wave") {
        sin(continuousPhase) * 4.5f
    } else 0f

    val stepShiftX by animateFloatAsState(
        when (animation) {
            "drift" -> if (active) 0f else (if (distance < 0) -14f else 14f)
            else -> 0f
        },
        scaleSpec,
        label = "lyricStepShiftX"
    )

    val stepShiftY by animateFloatAsState(
        when (animation) {
            "wave" -> if (active) 0f else distance.coerceIn(-1, 1) * 8f
            "aurora" -> if (active) 0f else distance.coerceIn(-1, 1) * 6f
            else -> 0f
        },
        scaleSpec,
        label = "lyricStepShiftY"
    )

    val totalShiftX = stepShiftX + dynamicShiftX
    val totalShiftY = stepShiftY + dynamicShiftY

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
            lineHeight = (maximumSize * 1.32f).sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = textAlign
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = finalScale
                this.scaleY = finalScale
                this.translationX = totalShiftX.dp.toPx()
                this.translationY = totalShiftY.dp.toPx()
                this.transformOrigin = transformOrigin
            }
            .padding(
                horizontal = if (settings.alignment == "center") 16.dp else 10.dp,
                vertical = linePaddingVertical
            )
    )
}
