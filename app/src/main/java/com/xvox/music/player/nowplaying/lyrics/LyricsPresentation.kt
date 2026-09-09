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
fun LyricPresentationLine(text: String, active: Boolean, distance: Int, settings: LyricsSettings,
    modifier: Modifier = Modifier, color: Color = Color.White, synchronized: Boolean = true) {
    val maximumSize = maxOf(settings.currentSize, settings.otherSize)
    val wanted = (if (active) settings.currentSize else settings.otherSize).toFloat() / maximumSize
    val animation = settings.animation
    val spec: AnimationSpec<Float> = if (animation == "spring") spring(dampingRatio = .72f, stiffness = 340f) else tween(300, easing = FastOutSlowInEasing)

    // Zoom ("focus") shrinks the passing lines slightly; wave alternates a soft left/right sway.
    val zoomOut = (animation == "focus" && !active) || (animation == "wave" && !active && (distance % 2 != 0))
    val scale by animateFloatAsState(wanted * (if (zoomOut) .97f else 1f), spec, label = "lyricScale")

    // Base dim by distance from the active line. "Equal fade" re-applies it symmetrically to
    // every line (centre stays clear) with its own strength via fadeIntensity.
    val baseAlpha = when (abs(distance)) { 0 -> 1f; 1 -> .62f; 2 -> .34f; else -> .2f }
    val dimTarget = if (settings.fadeEqual) 1f - (1f - baseAlpha) * settings.fadeIntensity else baseAlpha
    val alpha by animateFloatAsState(if (!synchronized) .82f else dimTarget, tween(240), label = "lyricAlpha")

    val glideX = animation == "glide" && !active
    val waveX = animation == "wave" && !active
    val shiftX by animateFloatAsState(
        if (glideX) distance.coerceIn(-1, 1) * 12f
        else if (waveX) if (distance % 2 == 0) 0f else distance.coerceIn(-1, 1) * 5f
        else 0f, spec, label = "lyricShiftX")

    val slideY = animation == "slide" && !active
    // "rise": future lines enter from below the active line and climb up into the centre.
    val riseY = animation == "rise" && !active && distance > 0
    val shiftY by animateFloatAsState(
        if (slideY) distance.coerceIn(-1, 1) * 6f
        else if (riseY) distance.coerceIn(1, 4) * 7f
        else 0f, spec, label = "lyricShiftY")

    // "pulse": the active line breathes gently instead of sitting static.
    var pulseScale by remember { mutableFloatStateOf(1f) }
    if (animation == "pulse" && active) {
        val transition = rememberInfiniteTransition(label = "pulse")
        pulseScale by transition.animateFloat(
            initialValue = 1f, targetValue = 1.045f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulseScale"
        )
    } else {
        pulseScale = 1f
    }

    // Always measure at the same maximum size and weight. Animating font metrics was moving the
    // list's row heights during centring and caused the old jitter / corrective jumps.
    Text(text.ifBlank { "♪" }, color = color,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = maximumSize.sp, lineHeight = (maximumSize * 1.3f).sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
        modifier = modifier.fillMaxWidth().graphicsLayer {
            this.alpha = alpha; scaleX = scale * pulseScale; scaleY = scale * pulseScale
            translationX = shiftX.dp.toPx(); translationY = shiftY.dp.toPx()
        }.padding(horizontal = 18.dp, vertical = 8.dp))
}
