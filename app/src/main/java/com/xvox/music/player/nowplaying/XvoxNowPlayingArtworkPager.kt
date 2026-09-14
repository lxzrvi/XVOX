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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * Full-queue smooth HorizontalPager.
 * Rests cleanly with 11dp side gaps and 11dp page spacing.
 * Enables ultra-fast continuous swiping, non-blocking rapid button navigation,
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
    var targetPage by remember { mutableIntStateOf(initialIdx) }

    val settled by rememberUpdatedState(onSettledPage)
    val palette by rememberUpdatedState(onSwipePalette)
    val tap by rememberUpdatedState(onArtworkTap)

    // Handle external Next/Prev navigation requests (Rapid button clicking without freeze)
    LaunchedEffect(navigationRequest) {
        if (navigationRequest == lastHandledRequest) return@LaunchedEffect
        val delta = navigationRequest - lastHandledRequest
        lastHandledRequest = navigationRequest

        var nextTarget = targetPage + delta
        if (repeatMode == RepeatMode.ALL) {
            nextTarget = (nextTarget % queue.size + queue.size) % queue.size
        } else {
            nextTarget = nextTarget.coerceIn(0, queue.lastIndex)
        }
        targetPage = nextTarget

        if (targetPage in queue.indices) {
            pager.animateScrollToPage(targetPage, animationSpec = tween(220, easing = FastOutSlowInEasing))
        }
    }

    // Auto-advance or external track changes (from queue/service)
    LaunchedEffect(currentIndex) {
        targetPage = currentIndex
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

    // Debounced playback commit during rapid continuous flipping:
    // Single swipe/button switches after short settle (260ms).
    // Rapid continuous swiping keeps playing current song until user stops, then plays target song!
    LaunchedEffect(pager, queue) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { page ->
            if (page in queue.indices && page != currentIndex) {
                delay(260)
                if (pager.settledPage == page) {
                    settled(page)
                }
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
