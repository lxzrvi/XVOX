package com.xvox.music.player.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.player.playback.XvoxBlendMonitor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val XvoxBlendInColor = Color(0xFF62CDBD)
val XvoxBlendOutColor = Color(0xFFE6AB6C)

/** Colours only in the subdued progress track. An intro zone exists only after a real fade-in. */
@Composable
fun XvoxBlendZones(songId: Long?, duration: Long, modifier: Modifier = Modifier) {
    val projection = remember(songId) { XvoxBlendMonitor.state.map {
        if (it.enabled && it.currentId == songId && songId != null) it.introZoneMs to it.tailZoneMs else 0L to 0L
    }.distinctUntilChanged() }
    val zones by projection.collectAsState(initial = 0L to 0L)
    val colors = XvoxTheme.colors
    if (duration <= 0) return
    Canvas(modifier) {
        val intro = (zones.first.toFloat() / duration).coerceIn(0f, .5f)
        val tail = (zones.second.toFloat() / duration).coerceIn(0f, .5f)
        // Same rounded track as the progress line and the accent's own colour, so the blend zones
        // read as part of that line rather than a second, mismatched strip.
        val radius = CornerRadius(size.height / 2f)
        if (intro > 0) {
            drawRoundRect(
                // Its own colour, clearly different from the played/unplayed track.
                color = XvoxBlendInColor.copy(alpha = .85f),
                topLeft = Offset.Zero,
                size = Size(size.width * intro, size.height),
                cornerRadius = radius
            )
        }
        if (tail > 0) {
            drawRoundRect(
                color = XvoxBlendOutColor.copy(alpha = .85f),
                topLeft = Offset(size.width * (1 - tail), 0f),
                size = Size(size.width * tail, size.height),
                cornerRadius = radius
            )
        }
    }
}

@Composable
fun XvoxBlendStatus(modifier: Modifier = Modifier) {
    val active by remember { XvoxBlendMonitor.state.map { it.enabled && it.active }.distinctUntilChanged() }.collectAsState(initial = false)
    if (!active) return
    Box(modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
        Text("Crossfading", color = XvoxTheme.colors.primaryAccent, fontSize = 10.sp,
            modifier = Modifier.background(XvoxTheme.colors.primaryAccent.copy(alpha = .10f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 4.dp))
    }
}
