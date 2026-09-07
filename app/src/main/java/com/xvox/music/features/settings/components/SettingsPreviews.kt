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
fun HomeSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    var sample by remember { mutableIntStateOf(17) }
    val rows = state.homeHorizontalRows.coerceIn(3, 8)
    val tiles = remember(state.homeLayoutStyle, rows, sample) {
        if (state.homeLayoutStyle == "uniform") regularSpecs(4, rows * 4)
        else generateMosaicSpecs(4, rows, rows * 2 + 1, Random(sample))
    }
    SettingsPreviewFrame("Home · live layout preview") {
        Canvas(Modifier.fillMaxWidth().height(148.dp)) {
            val gap = 4.dp.toPx()
            val recentHeight = if (state.hideRecentlyPlayed) 0f else 24.dp.toPx()
            val start = if (state.recentsPlacement == "top") recentHeight else 0f
            val unitW = (size.width - gap * 3) / 4
            val unitH = (size.height - recentHeight - gap * (rows - 1)) / rows
            tiles.forEachIndexed { i, tile ->
                drawRoundRect(colors.primaryAccent.copy(alpha = .16f + (i % 4) * .09f),
                    Offset((unitW + gap) * tile.x, start + (unitH + gap) * tile.y),
                    Size(unitW * tile.width + gap * (tile.width - 1), unitH * tile.height + gap * (tile.height - 1)),
                    CornerRadius(if (state.homeLayoutStyle == "uniform") 5.dp.toPx() else (4 + i % 5 * 2).dp.toPx()))
            }
            if (!state.hideRecentlyPlayed) drawRoundRect(colors.secondaryText.copy(alpha = .25f),
                Offset(0f, if (state.recentsPlacement == "top") 0f else size.height - recentHeight + gap),
                Size(size.width, recentHeight - gap), CornerRadius(6.dp.toPx()))
        }
        Text(if (state.hideRecentlyPlayed) "Recents hidden" else "Recents ${state.recentsPlacement} · ${state.sortOrder}", color = colors.secondaryText, fontSize = 11.sp)
        if (state.homeLayoutStyle == "mosaic") Text("Try another mosaic ↗", color = colors.primaryAccent, fontSize = 12.sp,
            modifier = Modifier.xvoxPressScale { sample++ }.padding(vertical = 4.dp))
    }
}

@Composable
fun EqSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    SettingsPreviewFrame("XvoxMix · band contour") {
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            drawLine(colors.cardBorder, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
            val path = Path()
            repeat(5) { i ->
                val db = if (state.equalizerEnabled) state.eqBands.getOrElse(i) { 0 }.toFloat() else 0f
                val point = Offset(size.width * i / 4, size.height * (.5f - db / 28f))
                if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                drawCircle(colors.primaryAccent, 3.dp.toPx(), point)
            }
            drawPath(path, colors.primaryAccent, style = Stroke(2.dp.toPx()))
        }
        Text("Smooth DSP · ${state.eqHeadroomDb.toInt()} dB extra headroom · Peak guard always on", color = colors.secondaryText, fontSize = 10.sp)
    }
}

@Composable
fun SurroundSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val phase = if (state.stereoWidening) {
        val transition = rememberInfiniteTransition(label = "orbitPreview")
        transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(state.surroundPanSpeed * 1000, easing = LinearEasing)), label = "orbit").value
    } else 0f
    SettingsPreviewFrame("Spatial preview · headphones recommended") {
        Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            val radius = size.height * .42f
            drawCircle(colors.cardBorder, radius, center, style = Stroke(1.dp.toPx()))
            drawCircle(colors.secondaryText.copy(alpha = .4f), 8.dp.toPx(), center)
            val amount = if (state.stereoWidening) state.surroundDepth else 0f
            drawCircle(colors.primaryAccent, 5.dp.toPx(), center + Offset(sin(phase) * radius * amount, -cos(phase) * radius * amount))
        }
        Text("Delay + head shadow + stereo crossfeed, not just left/right volume", color = colors.secondaryText, fontSize = 10.sp)
    }
}

@Composable
fun CrossfadeSettingsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    SettingsPreviewFrame("Transition preview · ${state.crossfadeDuration}s overlap") {
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val outgoing = Path(); val incoming = Path()
            for (i in 0..100) {
                val t = i / 100f
                val a = if (state.crossfade) cos(t * PI / 2).toFloat() else if (t < .5f) 1f else 0f
                val b = if (state.crossfade) sin(t * PI / 2).toFloat() else if (t < .5f) 0f else 1f
                val x = size.width * t
                val y1 = size.height * (1 - a * .85f)
                val y2 = size.height * (1 - b * .85f)
                if (i == 0) { outgoing.moveTo(x, y1); incoming.moveTo(x, y2) }
                else { outgoing.lineTo(x, y1); incoming.lineTo(x, y2) }
            }
            drawPath(outgoing, colors.secondaryText, style = Stroke(2.dp.toPx()))
            drawPath(incoming, colors.primaryAccent, style = Stroke(2.dp.toPx()))
        }
        Text(if (state.crossfade) "Current + next play together. Equal-power blend, no fade-to-silence." else "Normal gapless queue playback",
            color = colors.secondaryText, fontSize = 11.sp)
    }
}
