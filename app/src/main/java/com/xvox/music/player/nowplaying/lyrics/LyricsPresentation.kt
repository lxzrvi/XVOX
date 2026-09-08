package com.xvox.music.player.nowplaying.lyrics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

fun Modifier.lyricsEdgeFade(top: Float, bottom: Float): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    val t = top.coerceIn(0f, .45f); val b = bottom.coerceIn(0f, .45f)
    drawRect(Brush.verticalGradient(colorStops = arrayOf(
        0f to if (t == 0f) Color.White else Color.Transparent,
        t to Color.White, (1 - b) to Color.White,
        1f to if (b == 0f) Color.White else Color.Transparent)), blendMode = BlendMode.DstIn)
}

@Composable
fun LyricPresentationLine(text: String, active: Boolean, distance: Int, settings: LyricsSettings,
    modifier: Modifier = Modifier, color: Color = Color.White, synchronized: Boolean = true) {
    val duration = when (settings.animation) { "slide" -> 240; "focus" -> 300; else -> 160 }
    val font by animateFloatAsState((if (active) settings.currentSize else settings.otherSize).toFloat(), tween(duration), label = "lyricSize")
    val alpha by animateFloatAsState(if (!synchronized) .8f else when (abs(distance)) { 0 -> 1f; 1 -> .68f; 2 -> .38f; else -> .2f }, tween(duration), label = "lyricAlpha")
    val scale by animateFloatAsState(if (settings.animation == "focus" && !active) .94f else 1f, tween(duration), label = "lyricFocus")
    val shift by animateFloatAsState(if (settings.animation == "slide" && !active) distance.coerceIn(-1, 1) * 8f else 0f, tween(duration), label = "lyricSlide")
    Text(text.ifBlank { "♪" }, color = color, fontSize = font.sp, lineHeight = (font * 1.30f).sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().graphicsLayer {
            this.alpha = alpha; scaleX = scale; scaleY = scale; translationY = shift.dp.toPx()
        }.padding(horizontal = 18.dp, vertical = 8.dp))
}
