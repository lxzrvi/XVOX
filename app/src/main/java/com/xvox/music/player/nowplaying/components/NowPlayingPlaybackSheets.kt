@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/player/nowplaying/components/NowPlayingPlaybackSheets.kt
package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.player.playback.RepeatMode

private val ShuffleMethods = listOf(
    Triple("RANDOM", "Random", "Any song can play next"),
    Triple("EVERY_OTHER", "Skip one", "1st, 3rd, 5th… then the skipped ones"),
    Triple("PAIRS", "Two by two", "Play 2, skip 2, play 2…"),
    Triple("GROWING_JUMP", "Growing jump", "a, b, d, g, k… gaps keep growing"),
    Triple("REVERSE", "Backwards", "Last song first, walking back")
)

@Composable
private fun PlaybackChoice(
    label: String,
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.accentSoft else colors.cardElevated.copy(alpha = .58f))
            .border(
                if (selected) 1.dp else .8.dp,
                if (selected) colors.primaryAccent else colors.cardBorder.copy(alpha = .84f),
                shape
            )
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                color = if (selected) colors.primaryAccent else colors.primaryText,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            if (description.isNotEmpty()) {
                Text(
                    description,
                    color = if (selected) colors.primaryAccent.copy(alpha = .74f) else colors.mutedText,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

/** Long-press on Shuffle: choose HOW the queue gets shuffled. Tapping a method applies it. */
@Composable
fun NowPlayingShuffleBox(
    shuffleEnabled: Boolean,
    method: String,
    onApplyMethod: (String) -> Unit,
    onTurnOff: () -> Unit,
    onDismiss: () -> Unit
) {
    XvoxBox(onDismiss = onDismiss, title = "Shuffle method") {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShuffleMethods.forEach { (id, label, desc) ->
                PlaybackChoice(
                    label = label,
                    description = desc,
                    selected = shuffleEnabled && method == id,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onApplyMethod(id); onDismiss() }
                )
            }
            if (shuffleEnabled) {
                PlaybackChoice(
                    label = "Shuffle off",
                    description = "Back to the original order",
                    selected = false,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onTurnOff(); onDismiss() }
                )
            }
        }
    }
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/** Long-press on Repeat: how many times to repeat, plus an A-B loop inside the current song. */
@Composable
fun NowPlayingRepeatBox(
    repeatMode: RepeatMode,
    repeatTimesLeft: Int,
    durationMs: Long,
    positionMs: Long,
    abStartMs: Long,
    abEndMs: Long,
    onRepeatOff: () -> Unit,
    onRepeatAll: () -> Unit,
    onRepeatSong: (Int) -> Unit,
    onAbLoop: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = XvoxTheme.colors
    val hasDuration = durationMs > 2000L
    var range by remember {
        mutableStateOf(
            if (abStartMs >= 0L && abEndMs > abStartMs) abStartMs.toFloat()..abEndMs.toFloat()
            else 0f..(durationMs.coerceAtLeast(1L)).toFloat()
        )
    }
    val loopOn = abStartMs >= 0L && abEndMs > abStartMs
    XvoxBox(onDismiss = onDismiss, title = "Repeat") {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaybackChoice("Off", "", repeatMode == RepeatMode.OFF, Modifier.weight(1f)) { onRepeatOff() }
                PlaybackChoice("All songs", "", repeatMode == RepeatMode.ALL, Modifier.weight(1f)) { onRepeatAll() }
            }
            Text("Repeat this song", color = colors.mutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, -1).forEach { n ->
                    val sel = repeatMode == RepeatMode.ONE &&
                        if (n < 0) repeatTimesLeft < 0 else repeatTimesLeft == n
                    PlaybackChoice(
                        label = if (n < 0) "∞" else "${n}×",
                        description = "",
                        selected = sel,
                        modifier = Modifier.weight(1f),
                        onClick = { onRepeatSong(n) }
                    )
                }
            }
            Text("Loop a part of this song (A – B)", color = colors.mutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
            if (hasDuration) {
                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    onValueChangeFinished = {
                        val a = range.start.toLong()
                        val b = range.endInclusive.toLong()
                        if (b - a >= 1000L) onAbLoop(a, b)
                    },
                    valueRange = 0f..durationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primaryAccent,
                        activeTrackColor = colors.primaryAccent,
                        inactiveTrackColor = colors.cardBorder
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("A  ${fmt(range.start.toLong())}", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("B  ${fmt(range.endInclusive.toLong())}", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaybackChoice("A = now", fmt(positionMs), false, Modifier.weight(1f)) {
                        val a = positionMs.toFloat().coerceIn(0f, range.endInclusive - 1000f)
                        range = a..range.endInclusive
                        onAbLoop(a.toLong(), range.endInclusive.toLong())
                    }
                    PlaybackChoice("B = now", fmt(positionMs), false, Modifier.weight(1f)) {
                        val b = positionMs.toFloat().coerceIn(range.start + 1000f, durationMs.toFloat())
                        range = range.start..b
                        onAbLoop(range.start.toLong(), b.toLong())
                    }
                }
                PlaybackChoice(
                    label = if (loopOn) "Loop on – tap to clear" else "Loop off",
                    description = if (loopOn) "${fmt(abStartMs)} → ${fmt(abEndMs)} repeats" else "Drag A / B to start a loop",
                    selected = loopOn,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (loopOn) {
                            onAbLoop(-1L, -1L)
                            range = 0f..durationMs.toFloat()
                        }
                    }
                )
            }
        }
    }
}
