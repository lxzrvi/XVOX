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

/**
 * Full-queue smooth HorizontalPager.
 * Rests cleanly with 11dp side gaps and 11dp page spacing.
 * Enables ultra-fast continuous swiping, immediate Next/Prev button navigation,
 * and live real-time backdrop palette crossfading.
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
    val initialIdx = currentIndex.coerceIn(0, queue.lastIndex)

    val pager = rememberPagerState(initialPage = initialIdx, pageCount = { queue.size })

    var lastHandledRequest by remember { mutableIntStateOf(navigationRequest) }
    val settled by rememberUpdatedState(onSettledPage)
    val palette by rememberUpdatedState(onSwipePalette)
    val tap by rememberUpdatedState(onArtworkTap)

    // Handle external Next/Prev navigation requests (Buttons in UI)
    LaunchedEffect(navigationRequest) {
        if (navigationRequest == lastHandledRequest) return@LaunchedEffect
        val isForward = navigationRequest > lastHandledRequest
        lastHandledRequest = navigationRequest

        val targetPage = if (isForward) {
            if (pager.currentPage < queue.lastIndex) pager.currentPage + 1
            else if (repeatMode == RepeatMode.ALL) 0
            else pager.currentPage
        } else {
            if (pager.currentPage > 0) pager.currentPage - 1
            else if (repeatMode == RepeatMode.ALL) queue.lastIndex
            else pager.currentPage
        }

        if (targetPage != pager.currentPage && targetPage in queue.indices) {
            pager.animateScrollToPage(targetPage, animationSpec = tween(220, easing = FastOutSlowInEasing))
        }
    }

    // Auto-advance or external track changes (from queue/service): animate smooth cover swipe transition
    LaunchedEffect(currentIndex) {
        if (currentIndex in queue.indices && currentIndex != pager.currentPage && !pager.isScrollInProgress) {
            pager.animateScrollToPage(currentIndex, animationSpec = tween(240, easing = FastOutSlowInEasing))
        }
    }

    // Real-time backdrop color crossfading matching finger/pager position with zero latency
    LaunchedEffect(pager, queue) {
        snapshotFlow {
            Pair(pager.currentPage, pager.currentPageOffsetFraction)
        }.collect { (page, offset) ->
            val curr = queue.getOrNull(page)
            val adj = if (offset > 0.001f) queue.getOrNull(page + 1)
            else if (offset < -0.001f) queue.getOrNull(page - 1)
            else null

            if (curr != null) {
                palette(curr, adj, abs(offset))
            }
        }
    }

    // Commit playback when user finishes swiping to a new page
    LaunchedEffect(pager, queue) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { page ->
            if (page in queue.indices && page != currentIndex) {
                settled(page)
            }
        }
    }

    HorizontalPager(
        state = pager,
        beyondViewportPageCount = 1,
        contentPadding = PaddingValues(horizontal = 11.dp),
        pageSpacing = 11.dp,
        modifier = modifier.fillMaxSize(),
        key = { page -> queue.getOrNull(page)?.id ?: page }
    ) { page ->
        val song = queue.getOrNull(page) ?: return@HorizontalPager
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .pointerInput(song.id) {
                    detectTapGestures {
                        if (!pager.isScrollInProgress) tap()
                    }
                },
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
