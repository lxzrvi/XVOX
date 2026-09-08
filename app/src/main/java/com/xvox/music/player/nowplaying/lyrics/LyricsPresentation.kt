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
    val spec: AnimationSpec<Float> = if (settings.animation == "spring") spring(dampingRatio = .72f, stiffness = 340f) else tween(280, easing = FastOutSlowInEasing)
    val scale by animateFloatAsState(wanted * if (settings.animation == "focus" && !active) .97f else 1f, spec, label = "lyricScale")
    val alpha by animateFloatAsState(if (!synchronized) .82f else when (abs(distance)) { 0 -> 1f; 1 -> .65f; 2 -> .34f; else -> .18f }, tween(240), label = "lyricAlpha")
    val shiftX by animateFloatAsState(if (settings.animation == "glide" && !active) distance.coerceIn(-1, 1) * 12f else 0f, spec, label = "lyricGlide")
    val shiftY by animateFloatAsState(if (settings.animation == "slide" && !active) distance.coerceIn(-1, 1) * 6f else 0f, spec, label = "lyricSlide")
    // Always measure at the same maximum size and weight. Animating font metrics was moving the
    // list's row heights during centring and caused the old jitter / corrective jumps.
    Text(text.ifBlank { "♪" }, color = color,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = maximumSize.sp, lineHeight = (maximumSize * 1.3f).sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
        modifier = modifier.fillMaxWidth().graphicsLayer {
            this.alpha = alpha; scaleX = scale; scaleY = scale
            translationX = shiftX.dp.toPx(); translationY = shiftY.dp.toPx()
        }.padding(horizontal = 18.dp, vertical = 8.dp))
}
