package com.xvox.music.player.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.player.playback.CrossfadeMath
import com.xvox.music.player.playback.XvoxBlendMonitor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val XvoxBlendInColor = Color(0xFF62CDBD)
val XvoxBlendOutColor = Color(0xFFE6AB6C)

/** Persistent cue zones: teal intro, amber tail. Separate flow avoids redrawing grids on mix ticks. */
@Composable
fun XvoxBlendZones(duration: Long, modifier: Modifier = Modifier) {
    val configuration = remember { XvoxBlendMonitor.state.map { it.enabled to it.configuredSeconds }.distinctUntilChanged() }
    val config by configuration.collectAsState(initial = false to 3)
    if (!config.first || duration <= 0) return
    val fraction = CrossfadeMath.windowMs(config.second, duration).toFloat() / duration
    if (fraction <= 0f) return
    Canvas(modifier) {
        val width = size.width * fraction.coerceIn(0f, .5f)
        drawRect(XvoxBlendInColor.copy(alpha = .48f), Offset.Zero, Size(width, size.height))
        drawRect(XvoxBlendOutColor.copy(alpha = .48f), Offset(size.width - width, 0f), Size(width, size.height))
    }
}

@Composable
fun XvoxBlendStatus(modifier: Modifier = Modifier) {
    val blend by XvoxBlendMonitor.state.collectAsState()
    val colors = XvoxTheme.colors
    if (!blend.enabled) return
    Column(modifier.fillMaxWidth()) {
        if (!blend.active) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Intro blend", color = XvoxBlendInColor, fontSize = 9.sp)
                Text("${blend.configuredSeconds}s tail blend", color = XvoxBlendOutColor, fontSize = 9.sp)
            }
        } else {
            Text(if (blend.beatAligned) "Beat-aligned blend" else "Blending tracks", color = colors.secondaryText, fontSize = 10.sp)
            BlendTrackLine(blend.outgoingTitle, blend.outgoingPosition, blend.outgoingDuration, XvoxBlendOutColor)
            BlendTrackLine(blend.incomingTitle, blend.incomingPosition, blend.incomingDuration, XvoxBlendInColor)
        }
    }
}

@Composable
private fun BlendTrackLine(title: String, position: Long, duration: Long, color: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = color, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 10.dp))
        val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
        Canvas(Modifier.width(90.dp).height(3.dp)) {
            drawRect(color.copy(alpha = .18f))
            drawRect(color, size = Size(size.width * progress, size.height))
        }
    }
}
