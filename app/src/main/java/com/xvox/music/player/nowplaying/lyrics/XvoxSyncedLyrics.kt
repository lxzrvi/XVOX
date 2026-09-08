package com.xvox.music.player.nowplaying.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.LyricsSettings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun XvoxSyncedLyrics(
    lyrics: XvoxLyrics,
    position: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    strongEdgeFade: Boolean = false
) {
    if (lyrics.lines.isEmpty()) return

    val context = LocalContext.current
    val prefs = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val settings by prefs.lyricsSettings.collectAsState(initial = LyricsSettings())
    val effectivePosition = settings.position(position)
    val activeIndex = if (lyrics.synchronized) lyrics.lines.indexOfLast { (it.timeMs ?: Long.MAX_VALUE) <= effectivePosition } else -1

    val listState = rememberLazyListState()
    var userBrowsing by remember { mutableStateOf(false) }
    var autoFollowing by remember { mutableStateOf(false) }
    var interactionToken by remember { mutableLongStateOf(0L) }

    LaunchedEffect(listState.isScrollInProgress, autoFollowing) {
        if (listState.isScrollInProgress && !autoFollowing) {
            userBrowsing = true
            interactionToken++
        } else if (!listState.isScrollInProgress && userBrowsing && !autoFollowing) {
            val token = ++interactionToken
            delay(3000L)
            if (token == interactionToken && !listState.isScrollInProgress) {
                userBrowsing = false
            }
        }
    }

    LaunchedEffect(activeIndex, userBrowsing, lyrics, settings.currentSize, settings.otherSize, settings.animation) {
        if (!lyrics.synchronized || activeIndex < 0 || userBrowsing) return@LaunchedEffect
        autoFollowing = true
        try {
            withFrameNanos { }
            centerLyricExactly(listState, activeIndex + 1, settings.animation)
        } finally {
            autoFollowing = false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val boundarySpace = maxHeight / 2
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .lyricsEdgeFade(settings.fadeTop, settings.fadeBottom)
        ) {
            item(key = "lyrics-top") { Spacer(Modifier.height(boundarySpace)) }
            itemsIndexed(items = lyrics.lines, key = { index, line -> "$index-${line.timeMs}-${line.text}" }) { index, line ->
                val isActive = lyrics.synchronized && index == activeIndex
                LyricPresentationLine(line.text, isActive, index - activeIndex, settings,
                    synchronized = lyrics.synchronized,
                    modifier = Modifier.clickable(enabled = line.timeMs != null,
                        interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        line.timeMs?.let { onSeek(settings.seekPosition(it)) }
                    })
            }
            item(key = "lyrics-bottom") { Spacer(Modifier.height(boundarySpace)) }
        }
    }
}

private suspend fun centerLyricExactly(state: LazyListState, lazyIndex: Int, animation: String) {
    var target = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == lazyIndex }
    if (target == null) {
        state.scrollToItem(lazyIndex)
        withFrameNanos { }
        target = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == lazyIndex } ?: return
    }
    fun correction(): Float? {
        val layout = state.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.index == lazyIndex } ?: return null
        val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
        val itemCenter = item.offset + item.size / 2f
        return itemCenter - viewportCenter
    }
    val first = correction() ?: return
    if (abs(first) > 0.5f) {
        state.animateScrollBy(value = first, animationSpec = tween(durationMillis = when (animation) { "fade" -> 180; "focus" -> 300; else -> 260 }, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    withFrameNanos { }
    val final = correction() ?: return
    if (abs(final) > 0.75f) {
        state.scrollBy(final)
    }
}
