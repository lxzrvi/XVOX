package com.xvox.music.player.nowplaying.lyrics

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.data.preferences.LyricsSettings
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun XvoxSyncedLyrics(lyrics: XvoxLyrics, position: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier,
    strongEdgeFade: Boolean = false, settingsOverride: LyricsSettings? = null, preview: Boolean = false,
    textColor: Color = Color.White) {
    if (lyrics.lines.isEmpty()) return
    val context = LocalContext.current
    val prefs = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val saved by prefs.lyricsSettings.collectAsState(initial = LyricsSettings())
    val settings = settingsOverride ?: saved
    val active = if (lyrics.synchronized) lyrics.lines.indexOfLast { (it.timeMs ?: Long.MAX_VALUE) <= settings.position(position) } else -1
    val list = rememberLazyListState()
    val dragged by list.interactionSource.collectIsDraggedAsState()
    var browsing by remember { mutableStateOf(false) }
    LaunchedEffect(dragged) {
        if (dragged) browsing = true
        else if (browsing) {
            snapshotFlow { list.isScrollInProgress }.first { !it }
            delay(2500); browsing = false
        }
    }
    val measure = rememberTextMeasurer()
    val density = LocalDensity.current
    val maximum = maxOf(settings.currentSize, settings.otherSize)
    val textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = maximum.sp, lineHeight = (maximum * 1.3f).sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = when (settings.alignment) {
            "left" -> TextAlign.Start
            "right" -> TextAlign.End
            else -> TextAlign.Center
        })
    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewport = with(density) { maxHeight.toPx() }
        val textWidth = with(density) { (maxWidth - 36.dp).roundToPx().coerceAtLeast(1) }
        val rowHeight = if (active >= 0) remember(active, lyrics, maximum, textWidth, textStyle) {
            measure.measure(lyrics.lines[active].text.ifBlank { "♪" }, textStyle, constraints = Constraints(maxWidth = textWidth)).size.height + with(density) { 16.dp.toPx() }
        } else 0f
        LaunchedEffect(active, browsing, maximum, viewport, settings.animation) {
            if (active < 0 || browsing || !lyrics.synchronized) return@LaunchedEffect
            withFrameNanos { }
            val index = active + 1
            val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            if (item == null) {
                // A seek may jump hundreds of rows. Centre directly instead of jumping then correcting.
                list.scrollToItem(index, ((rowHeight - viewport) / 2).roundToInt())
            } else {
                val delta = item.offset + item.size / 2f - viewport / 2f
                if (abs(delta) > .75f) {
                    val motion: AnimationSpec<Float> = if (settings.animation == "spring") spring(dampingRatio = .9f, stiffness = 320f)
                        else tween(if (settings.animation == "fade") 240 else 320, easing = FastOutSlowInEasing)
                    list.animateScrollBy(delta, motion)
                }
            }
        }
        LazyColumn(state = list, userScrollEnabled = !preview,
            modifier = Modifier.fillMaxSize().lyricsEdgeFade(settings.fadeTop, settings.fadeBottom)) {
            item(key = "lyrics-top") { Spacer(Modifier.height(maxHeight / 2)) }
            itemsIndexed(lyrics.lines, key = { index, _ -> index }) { index, line ->
                LyricPresentationLine(line.text, index == active, index - active, settings, color = textColor,
                    synchronized = lyrics.synchronized,
                    modifier = Modifier.clickable(enabled = !preview && line.timeMs != null,
                        interactionSource = remember { MutableInteractionSource() }, indication = null) { line.timeMs?.let { onSeek(settings.seekPosition(it)) } })
            }
            item(key = "lyrics-bottom") { Spacer(Modifier.height(maxHeight / 2)) }
        }
    }
}
