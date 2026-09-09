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
    val spec: AnimationSpec<Float> = if (animation == "spring") spring(dampingRatio = .55f, stiffness = 460f)
        else tween(280, easing = FastOutSlowInEasing)

    // "focus" shrinks passing lines away (strong zoom-out); "wave" keeps them full size and only
    // sways them; "pulse" breathes on the active line only (handled below).
    val focusScale = animation == "focus" && !active
    val waveScale = animation == "wave" && !active && (distance % 2 != 0)
    val scaleBase = wanted * when {
        focusScale -> .9f
        waveScale -> 1.02f
        else -> 1f
    }
    val scale by animateFloatAsState(scaleBase, spec, label = "lyricScale")

    // Base dim by distance from the active line. "Equal fade" is stricter: every line above and
    // below is faded out fully by fadeIntensity — only the current line stays clear, exactly as
    // if the lyrics above and below were not there.
    val baseAlpha = when (abs(distance)) { 0 -> 1f; 1 -> .62f; 2 -> .34f; else -> .2f }
    val dimTarget = if (settings.fadeEqual) {
        if (distance == 0) 1f else (1f - settings.fadeIntensity).coerceIn(0.05f, 1f)
    } else baseAlpha
    val alpha by animateFloatAsState(if (!synchronized) .82f else dimTarget, tween(240), label = "lyricAlpha")

    // Each animation moves differently, so they never feel the same:
    //  glide -> the whole line drifts sideways; wave -> neighbours sway in/out of the centre;
    //  slide -> lines drop one place; rise -> upcoming lines climb from below; spring -> a soft
    //  vertical hop on passing lines.
    val glideX = animation == "glide" && !active
    val waveX = animation == "wave" && !active
    val shiftX by animateFloatAsState(
        when {
            glideX -> distance.coerceIn(-1, 1) * 26f
            waveX -> if (distance % 2 != 0) distance.coerceIn(-1, 1) * 10f
                else -distance.coerceIn(-1, 1) * 4f
            else -> 0f
        }, spec, label = "lyricShiftX")

    val slideY = animation == "slide" && !active
    val riseY = animation == "rise" && !active && distance > 0
    val springY = animation == "spring" && !active
    val shiftY by animateFloatAsState(
        when {
            slideY -> distance.coerceIn(-1, 1) * 10f
            riseY -> distance.coerceIn(1, 6) * 16f
            springY -> distance.coerceIn(-1, 1) * 4f
            else -> 0f
        }, spec, label = "lyricShiftY")

    // "pulse": the active line breathes gently instead of sitting static.
    var pulseScale = 1f
    if (animation == "pulse" && active) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val pulse by transition.animateFloat(
            initialValue = 1f, targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulseScale"
        )
        pulseScale = pulse
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
