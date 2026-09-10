package com.xvox.music.player.nowplaying

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxNowPlayingArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/** A three-slot, song-identity anchored pager. High quality artwork rendering. */
@Composable
fun XvoxNowPlayingArtworkPager(
    queue: List<Song>, currentIndex: Int, navigationRequest: Int, onArtworkTap: () -> Unit,
    onSwipePalette: (Song, Song?, Float) -> Unit, onSettledPage: (Int) -> Unit,
    modifier: Modifier = Modifier, repeatMode: RepeatMode = RepeatMode.OFF
) {
    if (queue.isEmpty()) return
    val index = currentIndex.coerceIn(queue.indices)
    val current = queue[index]
    val wrap = repeatMode == RepeatMode.ALL && queue.size > 1
    val previousIndex = if (index > 0) index - 1 else if (wrap) queue.lastIndex else null
    val nextIndex = if (index < queue.lastIndex) index + 1 else if (wrap) 0 else null
    var handledRequest by remember { mutableIntStateOf(navigationRequest) }
    val settled by rememberUpdatedState(onSettledPage)
    val palette by rememberUpdatedState(onSwipePalette)
    val tap by rememberUpdatedState(onArtworkTap)
    key(current.id) {
        val pager = rememberPagerState(initialPage = 1, pageCount = { 3 })
        var navigated by remember { mutableStateOf(false) }
        val previous = previousIndex?.let { queue[it] }
        val next = nextIndex?.let { queue[it] }
        LaunchedEffect(navigationRequest) {
            if (navigationRequest == handledRequest) return@LaunchedEffect
            handledRequest = navigationRequest
            val target = if (navigationRequest > 0) 2 else 0
            if ((target == 2 && next != null) || (target == 0 && previous != null)) {
                pager.animateScrollToPage(target)
            }
        }
        LaunchedEffect(pager, previous?.id, next?.id) {
            snapshotFlow { pager.currentPage to pager.currentPageOffsetFraction }.collect { (page, offset) ->
                val direction = (page - 1) + offset
                palette(current, if (direction >= 0) next else previous, abs(direction).coerceIn(0f, 1f))
            }
        }
        LaunchedEffect(pager, previousIndex, nextIndex) {
            snapshotFlow { if (pager.isScrollInProgress) null else pager.settledPage }.distinctUntilChanged().collect { page ->
                if (page == null || page == 1 || navigated) return@collect
                val target = if (page == 0) previousIndex else nextIndex
                if (target == null) pager.animateScrollToPage(1)
                else { navigated = true; settled(target) }
            }
        }
        HorizontalPager(state = pager, beyondViewportPageCount = 1, modifier = modifier.fillMaxSize(),
            key = { page -> "$page:${when (page) { 0 -> previous?.id; 2 -> next?.id; else -> current.id }}" }) { page ->
            val song = when (page) { 0 -> previous; 2 -> next; else -> current } ?: current
            Box(Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(20.dp))
                .pointerInput(song.id) { detectTapGestures { if (!pager.isScrollInProgress) tap() } }, contentAlignment = Alignment.Center) {
                XvoxSongArtwork(song.artworkUri, requestSize = XvoxNowPlayingArtworkSize, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
