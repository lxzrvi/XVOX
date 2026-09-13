package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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

/**
 * A song-identity anchored pager.
 * Centered card rests cleanly with 11dp side gaps, sliding in smoothly from the true screen edges
 * with 11dp spacing between adjacent songs, and animates smooth swipe on auto-next tracks.
 */
@Composable
fun XvoxNowPlayingArtworkPager(
    queue: List<Song>,
    currentIndex: Int,
    navigationRequest: Int,
    onArtworkTap: () -> Unit,
    onSwipePalette: (Song, Song?, Float) -> Unit,
    onSettledPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    repeatMode: RepeatMode = RepeatMode.OFF
) {
    if (queue.isEmpty()) return
    val index = currentIndex.coerceIn(queue.indices)
    val current = queue[index]
    val wrap = repeatMode == RepeatMode.ALL && queue.size > 1

    var displayedIndex by remember { mutableIntStateOf(index) }

    val prevIdx = if (displayedIndex > 0) displayedIndex - 1 else if (wrap) queue.lastIndex else null
    val nextIdx = if (displayedIndex < queue.lastIndex) displayedIndex + 1 else if (wrap) 0 else null

    val prevSong = prevIdx?.let { queue.getOrNull(it) }
    val currSong = queue.getOrNull(displayedIndex) ?: current
    val nextSong = nextIdx?.let { queue.getOrNull(it) }

    val slots = remember(currSong.id, prevSong?.id, nextSong?.id) {
        buildList {
            if (prevSong != null) add(PagerSlot(pageType = 0, targetIndex = prevIdx, song = prevSong))
            add(PagerSlot(pageType = 1, targetIndex = displayedIndex, song = currSong))
            if (nextSong != null) add(PagerSlot(pageType = 2, targetIndex = nextIdx, song = nextSong))
        }
    }
    val centerSlotIndex = remember(slots) { slots.indexOfFirst { it.pageType == 1 }.coerceAtLeast(0) }

    var handledRequest by remember { mutableIntStateOf(navigationRequest) }
    val settled by rememberUpdatedState(onSettledPage)
    val palette by rememberUpdatedState(onSwipePalette)
    val tap by rememberUpdatedState(onArtworkTap)

    val pager = rememberPagerState(initialPage = centerSlotIndex, pageCount = { slots.size })
    var isNavigatingInternally by remember { mutableStateOf(false) }

    // External navigation button requests (Next/Prev buttons in UI)
    LaunchedEffect(navigationRequest) {
        if (navigationRequest == handledRequest) return@LaunchedEffect
        handledRequest = navigationRequest
        val targetSlot = if (navigationRequest > 0) {
            slots.indexOfFirst { it.pageType == 2 }
        } else {
            slots.indexOfFirst { it.pageType == 0 }
        }
        if (targetSlot in slots.indices) {
            pager.animateScrollToPage(targetSlot, animationSpec = tween(320, easing = FastOutSlowInEasing))
        }
    }

    // Auto-advance or external track changes: animate smooth cover swipe transition!
    LaunchedEffect(currentIndex, queue) {
        if (currentIndex == displayedIndex) return@LaunchedEffect
        val targetSlot = slots.indexOfFirst { it.targetIndex == currentIndex }
        if (targetSlot in slots.indices && targetSlot != pager.currentPage && !pager.isScrollInProgress) {
            isNavigatingInternally = true
            pager.animateScrollToPage(targetSlot, animationSpec = tween(340, easing = FastOutSlowInEasing))
            displayedIndex = currentIndex
            pager.scrollToPage(centerSlotIndex)
            isNavigatingInternally = false
        } else {
            displayedIndex = currentIndex
        }
    }

    // Live color blending only while user/pager scroll is actively in progress
    LaunchedEffect(pager, prevSong?.id, nextSong?.id) {
        snapshotFlow {
            Triple(pager.currentPage, pager.currentPageOffsetFraction, pager.isScrollInProgress)
        }.collect { (page, offset, inProgress) ->
            if (inProgress) {
                val direction = (page - centerSlotIndex) + offset
                palette(currSong, if (direction >= 0) nextSong else prevSong, abs(direction).coerceIn(0f, 1f))
            }
        }
    }

    // User swipe settle commit
    LaunchedEffect(pager, slots) {
        snapshotFlow { if (pager.isScrollInProgress) null else pager.settledPage }.distinctUntilChanged().collect { page ->
            if (page == null || page == centerSlotIndex || isNavigatingInternally) return@collect
            val slot = slots.getOrNull(page)
            if (slot?.targetIndex != null && slot.pageType != 1) {
                isNavigatingInternally = true
                displayedIndex = slot.targetIndex
                settled(slot.targetIndex)
                pager.scrollToPage(centerSlotIndex)
                isNavigatingInternally = false
            }
        }
    }

    HorizontalPager(
        state = pager,
        beyondViewportPageCount = 1,
        contentPadding = PaddingValues(horizontal = 11.dp),
        pageSpacing = 11.dp,
        modifier = modifier.fillMaxSize(),
        key = { page -> "${slots.getOrNull(page)?.pageType}:${slots.getOrNull(page)?.song?.id ?: page}" }
    ) { page ->
        val slot = slots.getOrNull(page) ?: return@HorizontalPager
        val song = slot.song
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .pointerInput(song.id) { detectTapGestures { if (!pager.isScrollInProgress) tap() } },
            contentAlignment = Alignment.Center
        ) {
            XvoxSongArtwork(
                artwork = song.artworkUri,
                requestSize = XvoxNowPlayingArtworkSize,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
