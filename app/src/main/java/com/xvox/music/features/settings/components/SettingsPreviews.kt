package com.xvox.music.features.settings.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.home.allsongs.generateMosaicSpecs
import com.xvox.music.features.home.allsongs.regularSpecs
import com.xvox.music.features.settings.SettingsState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SettingsChoiceRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val colors = XvoxTheme.colors
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (key, label) ->
            val active = selected == key
            Text(label, color = if (active) colors.background else colors.primaryText,
                fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (active) colors.primaryAccent else colors.card)
                    .xvoxPressScale { onSelect(key) }.padding(horizontal = 14.dp, vertical = 12.dp))
        }
    }
}

@Composable
fun SettingsPreviewFrame(label: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = XvoxTheme.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface)
        .border(.7.dp, colors.primaryAccent.copy(alpha = .22f), RoundedCornerShape(18.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label.uppercase(), color = colors.secondaryText, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
fun EqSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    SettingsPreviewFrame("${state.eqBandCount}-band output contour") {
        Canvas(Modifier.fillMaxWidth().height(92.dp)) {
            val middle = size.height * .42f
            drawLine(colors.cardBorder, Offset(0f, middle), Offset(size.width, middle), 1.dp.toPx())
            val path = Path()
            repeat(state.eqBandCount) { i ->
                val f = i.toFloat() / (state.eqBandCount - 1)
                val highCut = state.softenHighs * 9 * ((f - .60f) / .40f).coerceIn(0f, 1f)
                val db = (if (state.equalizerEnabled) state.eqBands.getOrElse(i) { 0 }.toFloat() else 0f) - state.eqHeadroomDb - highCut
                val point = Offset(size.width * f, (middle - db / 42f * size.height).coerceIn(2f, size.height - 2))
                if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                drawCircle(colors.primaryAccent, 2.5.dp.toPx(), point)
            }
            drawPath(path, colors.primaryAccent, style = Stroke(2.dp.toPx()))
            val noiseHeight = 10.dp.toPx() * (1 - state.noiseReduction)
            drawRect(colors.secondaryText.copy(alpha = .15f), Offset(0f, size.height - noiseHeight), Size(size.width, noiseHeight))
        }
        Text("−${state.eqHeadroomDb.toInt()} dB · highs ${(state.softenHighs * 100).toInt()}% · noise ${(state.noiseReduction * 100).toInt()}%",
            color = colors.secondaryText, fontSize = 10.sp)
    }
}

@Composable
fun SurroundSettingsPreview(state: SettingsState) {
    if (!state.stereoWidening) return
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "orbitPreview")
    val phase by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(state.surroundPanSpeed * 1000, easing = LinearEasing)), label = "orbit")
    val level = (state.appVolume * state.volumeLimit).coerceIn(0f, 1f)
    SettingsPreviewFrame("3D · layout preview") {
        Canvas(Modifier.fillMaxWidth().height(94.dp)) {
            val radius = size.height * .38f
            drawCircle(colors.cardBorder, radius, center, style = Stroke(1.dp.toPx()))
            drawCircle(colors.secondaryText.copy(alpha = .45f), 8.dp.toPx(), center)
            val shift = Offset(state.balance * radius * .3f, 0f)
            drawCircle(colors.primaryAccent.copy(alpha = .2f + level * .8f), 5.dp.toPx(),
                center + shift + Offset(sin(phase) * radius * state.surroundDepth, -cos(phase) * radius * state.surroundDepth))
            val pan = sin(phase) * state.surroundDepth * .6f + state.balance
            val left = (1 - pan.coerceAtLeast(0f)).coerceIn(0f, 1f) * level
            val right = (1 + pan.coerceAtMost(0f)).coerceIn(0f, 1f) * level
            drawRoundRect(colors.primaryAccent.copy(alpha = .6f), Offset(8.dp.toPx(), size.height * (1 - left) / 2), Size(5.dp.toPx(), size.height * left), CornerRadius(3.dp.toPx()))
            drawRoundRect(colors.primaryAccent.copy(alpha = .6f), Offset(size.width - 13.dp.toPx(), size.height * (1 - right) / 2), Size(5.dp.toPx(), size.height * right), CornerRadius(3.dp.toPx()))
        }
        Text("Depth ${(state.surroundDepth * 100).toInt()}% · orbit ${state.surroundPanSpeed}s", color = colors.secondaryText, fontSize = 10.sp)
    }
}

@Composable
fun CrossfadeSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    SettingsPreviewFrame("Transition · schematic") {
        Canvas(Modifier.fillMaxWidth().height(92.dp)) {
            val out = Path(); val incoming = Path()
            val handoff = if (state.crossfadeSmart) .43f else .5f
            for (i in 0..100) {
                val t = i / 100f
                val gain = if (state.crossfade) com.xvox.music.player.playback.EnergyBlendPlanner.gains(t, handoff, state.crossfadeSmart)
                    else com.xvox.music.player.playback.CrossfadeGains(if (t < .5f) 1f else 0f, if (t < .5f) 0f else 1f)
                val cycles = state.crossfadeDuration * 1.6f
                val bass = if (state.crossfadeSmart) com.xvox.music.player.playback.EnergyBlendPlanner.bassGains(t, handoff, state.crossfadeClashControl, gain)
                    else com.xvox.music.player.playback.CrossfadeGains(1f, 1f)
                val wave1 = .62f + .30f * kotlin.math.abs(sin(t * cycles * PI)).toFloat()
                val phase = if (state.crossfadeBeatSync) 0f else .8f
                val wave2 = .62f + .30f * kotlin.math.abs(sin(t * cycles * PI + phase)).toFloat()
                val x = t * size.width
                val y1 = size.height * (1 - gain.outgoing * wave1 * (.65f + .35f * bass.outgoing))
                val y2 = size.height * (1 - gain.incoming * wave2 * (.65f + .35f * bass.incoming))
                if (i == 0) { out.moveTo(x, y1); incoming.moveTo(x, y2) } else { out.lineTo(x, y1); incoming.lineTo(x, y2) }
            }
            drawPath(out, Color(0xFFE6AB6C), style = Stroke(2.dp.toPx()))
            drawPath(incoming, Color(0xFF62CDBD), style = Stroke(2.dp.toPx()))
        }
        Text(if (!state.crossfade) "Off" else "${state.crossfadeDuration}s · ${if (state.crossfadeSmart) "Seamless" else "Equal power"}${if (state.crossfadeBeatSync) " · beat aligned" else ""}",
            color = colors.secondaryText, fontSize = 10.sp)
    }
}
