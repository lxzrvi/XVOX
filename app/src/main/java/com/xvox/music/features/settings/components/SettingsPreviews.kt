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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

/**
 * Equalizer live graph preview: Reverb, room size, noise reduction, soften highs, boost
 * protection, and volume are all rendered directly inside the graph itself (no separate bars).
 */
@Composable
fun EqSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "eqWaves")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "wavePhase"
    )

    SettingsPreviewFrame(if (state.equalizerEnabled) "${state.eqBandCount}-band EQ · live visualization" else "Equalizer · off") {
        Canvas(Modifier.fillMaxWidth().height(118.dp)) {
            val w = size.width
            val h = size.height
            val middle = h * 0.48f

            // Baseline
            drawLine(colors.cardBorder, Offset(0f, middle), Offset(w, middle), 1.dp.toPx())

            // Boost protection ceiling line
            val ceilingY = 8.dp.toPx() + (state.eqHeadroomDb / 18f) * 16.dp.toPx()
            drawLine(
                color = colors.primaryAccent.copy(alpha = 0.4f),
                start = Offset(0f, ceilingY),
                end = Offset(w, ceilingY),
                strokeWidth = 1.5.dp.toPx()
            )

            // Noise floor (bottom shaded band that shrinks with noise reduction)
            val noiseHeight = 24.dp.toPx() * (1f - state.noiseReduction.coerceIn(0f, 1f))
            if (noiseHeight > 1f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, colors.secondaryText.copy(alpha = 0.14f))
                    ),
                    topLeft = Offset(0f, h - noiseHeight),
                    size = Size(w, noiseHeight)
                )
            }

            // Reverb / Room reflection echoes (rippling aura around curve)
            val reverbGlow = state.reverbAmount.coerceIn(0f, 1f)
            val roomScale = 1f + state.roomAmount * 0.5f

            if (reverbGlow > 0.05f) {
                val echoPath = Path()
                repeat(state.eqBandCount) { i ->
                    val f = i.toFloat() / (state.eqBandCount - 1)
                    val highCut = state.softenHighs * 9f * ((f - 0.60f) / 0.40f).coerceIn(0f, 1f)
                    val rawDb = if (state.equalizerEnabled) state.eqBands.getOrElse(i) { 0 }.toFloat() else 0f
                    val db = (rawDb - state.eqHeadroomDb - highCut) * state.appVolume * state.volumeLimit
                    val ripple = sin(phase + f * 4f) * (6f * reverbGlow * roomScale)
                    val pt = Offset(w * f, (middle - (db / 36f) * h * 0.8f + ripple).coerceIn(ceilingY, h - 4f))
                    if (i == 0) echoPath.moveTo(pt.x, pt.y) else echoPath.lineTo(pt.x, pt.y)
                }
                drawPath(echoPath, colors.primaryAccent.copy(alpha = reverbGlow * 0.35f), style = Stroke(4.dp.toPx()))
            }

            // Main EQ curve with volume scaling and frequency nodes
            val path = Path()
            val fillPath = Path()
            repeat(state.eqBandCount) { i ->
                val f = i.toFloat() / (state.eqBandCount - 1)
                val highCut = state.softenHighs * 9f * ((f - 0.60f) / 0.40f).coerceIn(0f, 1f)
                val rawDb = if (state.equalizerEnabled) state.eqBands.getOrElse(i) { 0 }.toFloat() else 0f
                val db = (rawDb - state.eqHeadroomDb - highCut) * state.appVolume * state.volumeLimit
                val point = Offset(w * f, (middle - (db / 36f) * h * 0.8f).coerceIn(ceilingY, h - 4f))
                if (i == 0) {
                    path.moveTo(point.x, point.y)
                    fillPath.moveTo(point.x, middle)
                    fillPath.lineTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                    fillPath.lineTo(point.x, point.y)
                }
                if (i == state.eqBandCount - 1) {
                    fillPath.lineTo(point.x, middle)
                    fillPath.close()
                }
                drawCircle(colors.primaryAccent, 3.5.dp.toPx(), point)
            }
            drawPath(fillPath, Brush.verticalGradient(listOf(colors.primaryAccent.copy(alpha = 0.25f), Color.Transparent)))
            drawPath(path, colors.primaryAccent, style = Stroke(2.2.dp.toPx()))
        }
    }
}

@Composable
fun SurroundSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val on = state.stereoWidening
    val transition = rememberInfiniteTransition(label = "orbitPreview")
    val animated by transition.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween((state.surroundPanSpeed * 1000).coerceAtLeast(1000), easing = LinearEasing)),
        label = "orbit"
    )
    val phase = if (on) animated else 0f
    val level = (state.appVolume * state.volumeLimit).coerceIn(0f, 1f)
    SettingsPreviewFrame(if (on) "3D sound · ${state.surroundPanSpeed}s orbit" else "3D sound · off") {
        Canvas(Modifier.fillMaxWidth().height(94.dp)) {
            val radius = size.height * .38f * (0.6f + state.surroundWidth * 0.4f)
            // Spatial orbit rings
            drawCircle(colors.cardBorder, radius, center, style = Stroke(1.dp.toPx()))
            if (state.hrtf > 0.2f) {
                drawCircle(colors.primaryAccent.copy(alpha = 0.15f * state.hrtf), radius * 1.25f, center, style = Stroke(1.dp.toPx()))
            }
            // Listener at center
            drawCircle(colors.secondaryText.copy(alpha = .45f), 8.dp.toPx(), center)
            val shift = Offset(state.balance * radius * .3f, 0f)
            val orbitX = sin(phase) * radius * state.surroundDepth
            val orbitY = -cos(phase) * radius * state.surroundDepth
            drawCircle(colors.primaryAccent.copy(alpha = .2f + level * .8f), 6.dp.toPx(), center + shift + Offset(orbitX, orbitY))
            val pan = sin(phase) * state.surroundDepth * .6f + state.balance
            val left = (1 - pan.coerceAtLeast(0f)).coerceIn(0f, 1f) * level
            val right = (1 + pan.coerceAtMost(0f)).coerceIn(0f, 1f) * level
            drawRoundRect(colors.primaryAccent.copy(alpha = .6f), Offset(8.dp.toPx(), size.height * (1 - left) / 2), Size(5.dp.toPx(), size.height * left), CornerRadius(3.dp.toPx()))
            drawRoundRect(colors.primaryAccent.copy(alpha = .6f), Offset(size.width - 13.dp.toPx(), size.height * (1 - right) / 2), Size(5.dp.toPx(), size.height * right), CornerRadius(3.dp.toPx()))
        }
    }
}

@Composable
fun CrossfadeSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    SettingsPreviewFrame(if (!state.crossfade) "Crossfade · off" else "Crossfade · ${state.crossfadeDuration}s") {
        Canvas(Modifier.fillMaxWidth().height(88.dp)) {
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
    }
}
