package com.xvox.music.features.settings.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.xvox.music.features.settings.SettingsState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Reusable choice row used throughout Settings sections */
@Composable
fun SettingsChoiceRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val colors = XvoxTheme.colors
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            val active = selected == key
            Text(
                text = label,
                color = if (active) colors.background else colors.primaryText,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) colors.primaryAccent else colors.card)
                    .xvoxPressScale { onSelect(key) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

/** Reusable frame for individual settings previews */
@Composable
fun SettingsPreviewFrame(
    status: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardElevated)
            .border(0.7.dp, colors.cardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = status,
                color = colors.primaryAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Box(Modifier.size(6.dp).clip(CircleShape).background(colors.primaryAccent))
        }
        content()
    }
}

/**
 * Live Equalizer graph preview:
 * Directly reflects Band boosts, Headroom ceiling, Noise floor, Reverb ripples,
 * Room acoustic size, Volume energy scaling, and Left/Right stereo balance.
 */
@Composable
fun EqSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "eqWaves")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    SettingsPreviewFrame(if (state.equalizerEnabled) "${state.eqBandCount}-band EQ · live visualization" else "Equalizer · off") {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val w = size.width
            val h = size.height
            val middle = h * 0.55f

            // Headroom ceiling guide line (drops down with eqHeadroomDb)
            val ceilingY = (18f - state.eqHeadroomDb.coerceIn(0f, 18f)) / 18f * (h * 0.28f)
            drawLine(
                color = colors.primaryAccent.copy(alpha = 0.45f),
                start = Offset(0f, ceilingY),
                end = Offset(w, ceilingY),
                strokeWidth = 1.5.dp.toPx()
            )

            // Room size acoustic boundary
            val roomWidth = w * (0.6f + state.roomAmount * 0.4f)
            val roomLeft = (w - roomWidth) / 2f
            drawRoundRect(
                color = colors.primaryAccent.copy(alpha = 0.08f + state.roomAmount * 0.12f),
                topLeft = Offset(roomLeft, 4.dp.toPx()),
                size = Size(roomWidth, h - 8.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(1.dp.toPx())
            )

            // Noise floor (bottom shaded band that shrinks with noise reduction)
            val noiseHeight = 24.dp.toPx() * (1f - state.noiseReduction.coerceIn(0f, 1f))
            if (noiseHeight > 1f) {
                drawRect(
                    color = Color.Red.copy(alpha = 0.12f),
                    topLeft = Offset(0f, h - noiseHeight),
                    size = Size(w, noiseHeight)
                )
            }

            // Reverb / Room reflection echoes
            val reverbGlow = state.reverbAmount.coerceIn(0f, 1f)
            val roomScale = 1f + state.roomAmount * 0.6f

            if (reverbGlow > 0.05f) {
                val echoPath = Path()
                repeat(state.eqBandCount) { i ->
                    val f = i.toFloat() / (state.eqBandCount - 1)
                    val highCut = state.softenHighs * 9f * ((f - 0.60f) / 0.40f).coerceIn(0f, 1f)
                    val balanceBias = if (f < 0.5f) (1f - state.balance * 0.4f) else (1f + state.balance * 0.4f)
                    val rawDb = if (state.equalizerEnabled) state.eqBands.getOrElse(i) { 0 }.toFloat() else 0f
                    val db = (rawDb - state.eqHeadroomDb - highCut) * state.appVolume * state.volumeLimit * balanceBias
                    val ripple = sin(phase + f * 4f) * (6f * reverbGlow * roomScale)
                    val pt = Offset(w * f, (middle - (db / 36f) * h * 0.8f + ripple).coerceIn(ceilingY, h - 4f))
                    if (i == 0) echoPath.moveTo(pt.x, pt.y) else echoPath.lineTo(pt.x, pt.y)
                }
                drawPath(echoPath, colors.primaryAccent.copy(alpha = reverbGlow * 0.35f), style = Stroke(4.dp.toPx()))
            }

            // Main EQ curve with volume & balance scaling and frequency nodes
            val path = Path()
            val fillPath = Path()
            repeat(state.eqBandCount) { i ->
                val f = i.toFloat() / (state.eqBandCount - 1)
                val highCut = state.softenHighs * 9f * ((f - 0.60f) / 0.40f).coerceIn(0f, 1f)
                val balanceBias = if (f < 0.5f) (1f - state.balance * 0.4f) else (1f + state.balance * 0.4f)
                val rawDb = if (state.equalizerEnabled) state.eqBands.getOrElse(i) { 0 }.toFloat() else 0f
                val db = (rawDb - state.eqHeadroomDb - highCut) * state.appVolume * state.volumeLimit * balanceBias
                val point = Offset(w * f, (middle - (db / 36f) * h * 0.8f).coerceIn(ceilingY, h - 4f))
                if (i == 0) {
                    path.moveTo(point.x, point.y)
                    fillPath.moveTo(point.x, h)
                    fillPath.lineTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                    fillPath.lineTo(point.x, point.y)
                }
                drawCircle(colors.primaryAccent, 4.dp.toPx(), point)
            }
            fillPath.lineTo(w, h)
            fillPath.close()

            drawPath(fillPath, colors.primaryAccent.copy(alpha = 0.16f))
            drawPath(path, colors.primaryAccent, style = Stroke(2.2.dp.toPx()))
        }
    }
}

/**
 * 3D sound visualizer:
 * Reacts to Orbit Speed, Spatial Width, Elevation, Sound Position, and Center Core.
 */
@Composable
fun SurroundSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val on = state.stereoWidening
    val transition = rememberInfiniteTransition(label = "orbitPreview")
    val orbitDuration = ((12 - state.surroundPanSpeed).coerceIn(2, 10) * 800)
    val animated by transition.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(orbitDuration, easing = LinearEasing)),
        label = "orbit"
    )
    val phase = if (on) animated else 0f
    val level = (state.appVolume * state.volumeLimit).coerceIn(0f, 1f)

    SettingsPreviewFrame(if (on) "3D sound · ${state.surroundPanSpeed} speed orbit" else "3D sound · off") {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val centerOffset = Offset(center.x + state.surroundPosition * (size.width * 0.22f), center.y)
            val radius = size.height * .36f * (0.6f + state.surroundWidth * 0.4f)

            // Spatial orbit ring (moves with surroundPosition)
            drawCircle(colors.cardBorder, radius, centerOffset, style = Stroke(1.dp.toPx()))
            if (state.hrtf > 0.2f) {
                drawCircle(colors.primaryAccent.copy(alpha = 0.15f * state.hrtf), radius * 1.25f, centerOffset, style = Stroke(1.dp.toPx()))
            }

            // Listener Center Core (scales with centerPreservation)
            val centerRadius = 5.dp.toPx() + state.centerPreservation * 8.dp.toPx()
            drawCircle(colors.secondaryText.copy(alpha = .45f), centerRadius, centerOffset)

            // Orbiting sound orb
            val shift = Offset(state.balance * radius * .3f, 0f)
            val orbitX = sin(phase) * radius * state.surroundDepth
            val orbitY = -cos(phase) * radius * state.surroundDepth
            drawCircle(colors.primaryAccent.copy(alpha = .2f + level * .8f), 6.dp.toPx(), centerOffset + shift + Offset(orbitX, orbitY))

            // Left / Right channel meters
            val pan = sin(phase) * state.surroundDepth * .6f + state.balance
            val left = (1 - pan.coerceAtLeast(0f)).coerceIn(0f, 1f) * level
            val right = (1 + pan.coerceAtMost(0f)).coerceIn(0f, 1f) * level
            drawRect(colors.primaryAccent.copy(alpha = .65f), Offset(12.dp.toPx(), size.height - (left * 36.dp.toPx())), Size(5.dp.toPx(), left * 36.dp.toPx()))
            drawRect(colors.primaryAccent.copy(alpha = .65f), Offset(size.width - 17.dp.toPx(), size.height - (right * 36.dp.toPx())), Size(5.dp.toPx(), right * 36.dp.toPx()))
        }
    }
}

/**
 * Crossfade & Playback preview:
 * Wave shape reacts to Clash Control, Beat Sync, Pitch (stroke thickness & frequency),
 * and animated moving speed dots along the line.
 */
@Composable
fun CrossfadeSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val transition = rememberInfiniteTransition(label = "crossfadeSpeedDots")
    val isDefaultSpeed = kotlin.math.abs(state.playbackSpeed - 1.0f) < 0.05f
    val dotSpeedDuration = if (isDefaultSpeed) 100000 else (3000f / state.playbackSpeed).toInt().coerceIn(600, 8000)

    val dotProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(dotSpeedDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotProgress"
    )

    SettingsPreviewFrame(if (!state.crossfade) "Playback & Crossfade · off" else "Crossfade · ${state.crossfadeDuration}s (${String.format("%.2f", state.playbackSpeed)}×)") {
        Canvas(Modifier.fillMaxWidth().height(92.dp)) {
            val out = Path()
            val incoming = Path()
            val handoff = if (state.crossfadeSmart) .43f else .5f

            // Pitch effect: lower pitch -> thicker line, higher pitch -> thinner line
            val strokeThickness = (3.2f / state.playbackPitch.coerceIn(0.5f, 2.0f)).dp.toPx()

            for (i in 0..100) {
                val t = i / 100f
                val gain = if (state.crossfade) com.xvox.music.player.playback.EnergyBlendPlanner.gains(t, handoff, state.crossfadeSmart)
                    else com.xvox.music.player.playback.CrossfadeGains(if (t < .5f) 1f else 0f, if (t < .5f) 0f else 1f)
                val cycles = state.crossfadeDuration * 1.6f * state.playbackPitch.coerceIn(0.5f, 2.0f)
                val bass = if (state.crossfadeSmart) com.xvox.music.player.playback.EnergyBlendPlanner.bassGains(t, handoff, state.crossfadeClashControl, gain)
                    else com.xvox.music.player.playback.CrossfadeGains(1f, 1f)
                val wave1 = .62f + .30f * kotlin.math.abs(sin(t * cycles * PI)).toFloat()
                val wave2 = .62f + .30f * kotlin.math.abs(cos(t * cycles * PI)).toFloat()
                val x = size.width * t
                val y1 = size.height * (1 - gain.outgoing * wave1 * (.65f + .35f * bass.outgoing))
                val y2 = size.height * (1 - gain.incoming * wave2 * (.65f + .35f * bass.incoming))
                if (i == 0) { out.moveTo(x, y1); incoming.moveTo(x, y2) } else { out.lineTo(x, y1); incoming.lineTo(x, y2) }
            }

            drawPath(out, Color(0xFFE6AB6C), style = Stroke(strokeThickness))
            drawPath(incoming, Color(0xFF62CDBD), style = Stroke(strokeThickness))

            // Animated moving dots along playback line
            if (!isDefaultSpeed) {
                val dotT = dotProgress
                val dotX = dotT * size.width
                val dotY = size.height * 0.5f
                drawCircle(Color.White, 3.5.dp.toPx(), Offset(dotX, dotY))
            }
        }
    }
}
