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

private data class PagerSlot(val pageType: Int, val targetIndex: Int?, val song: Song)

/** A song-identity anchored pager. No duplicated artwork at boundaries. */
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

    val previous = previousIndex?.let { queue[it] }
    val next = nextIndex?.let { queue[it] }

    val slots = remember(current.id, previous?.id, next?.id) {
        buildList {
            if (previous != null) add(PagerSlot(pageType = 0, targetIndex = previousIndex, song = previous))
            add(PagerSlot(pageType = 1, targetIndex = index, song = current))
            if (next != null) add(PagerSlot(pageType = 2, targetIndex = nextIndex, song = next))
        }
    }
    val currentSlotIndex = remember(slots) { slots.indexOfFirst { it.pageType == 1 }.coerceAtLeast(0) }

    var handledRequest by remember { mutableIntStateOf(navigationRequest) }
    val settled by rememberUpdatedState(onSettledPage)
    val palette by rememberUpdatedState(onSwipePalette)
    val tap by rememberUpdatedState(onArtworkTap)

    key(current.id) {
        val pager = rememberPagerState(initialPage = currentSlotIndex, pageCount = { slots.size })
        var navigated by remember { mutableStateOf(false) }

        LaunchedEffect(navigationRequest) {
            if (navigationRequest == handledRequest) return@LaunchedEffect
            handledRequest = navigationRequest
            val targetSlot = if (navigationRequest > 0) {
                slots.indexOfFirst { it.pageType == 2 }
            } else {
                slots.indexOfFirst { it.pageType == 0 }
            }
            if (targetSlot in slots.indices) {
                pager.animateScrollToPage(targetSlot)
            }
        }

        LaunchedEffect(pager, previous?.id, next?.id) {
            snapshotFlow { pager.currentPage to pager.currentPageOffsetFraction }.collect { (page, offset) ->
                val direction = (page - currentSlotIndex) + offset
                palette(current, if (direction >= 0) next else previous, abs(direction).coerceIn(0f, 1f))
            }
        }

        LaunchedEffect(pager, slots) {
            snapshotFlow { if (pager.isScrollInProgress) null else pager.settledPage }.distinctUntilChanged().collect { page ->
                if (page == null || page == currentSlotIndex || navigated) return@collect
                val slot = slots.getOrNull(page)
                if (slot?.targetIndex != null && slot.pageType != 1) {
                    navigated = true
                    settled(slot.targetIndex)
                }
            }
        }

        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 1,
            modifier = modifier.fillMaxSize(),
            key = { page -> "${slots.getOrNull(page)?.pageType}:${slots.getOrNull(page)?.song?.id ?: page}" }
        ) { page ->
            val slot = slots.getOrNull(page) ?: return@HorizontalPager
            val song = slot.song
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .pointerInput(song.id) { detectTapGestures { if (!pager.isScrollInProgress) tap() } },
                contentAlignment = Alignment.Center
            ) {
                XvoxSongArtwork(song.artworkUri, requestSize = XvoxNowPlayingArtworkSize, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
