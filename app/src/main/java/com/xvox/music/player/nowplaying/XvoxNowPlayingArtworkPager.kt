package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Enables ultra-fast, snappy continuous swiping, non-blocking rapid button navigation,
 * and live real-time backdrop palette crossfading with zero lag.
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
            pager.animateScrollToPage(targetPage, animationSpec = tween(160, easing = FastOutSlowInEasing))
        }
    }

    // Auto-advance or external track changes (from queue/service)
    LaunchedEffect(currentIndex) {
        targetPage = currentIndex
        if (currentIndex in queue.indices && currentIndex != pager.currentPage && !pager.isScrollInProgress) {
            pager.animateScrollToPage(currentIndex, animationSpec = tween(160, easing = FastOutSlowInEasing))
        }
    }

    // Real-time backdrop color crossfading matching finger/pager position with zero latency
    LaunchedEffect(pager, queue) {
        snapshotFlow {
            Pair(pager.currentPage, pager.currentPageOffsetFraction)
        }.collect { (page, offset) ->
            val curr = queue.getOrNull(page)
            val adj = if (offset > 0.0001f) queue.getOrNull(page + 1)
            else if (offset < -0.0001f) queue.getOrNull(page - 1)
            else null

            if (curr != null) {
                palette(curr, adj, abs(offset))
            }
        }
    }

    // Snappy playback commit when settled on track
    LaunchedEffect(pager, queue) {
        snapshotFlow {
            Pair(pager.settledPage, pager.isScrollInProgress)
        }.distinctUntilChanged().collect { (settledIndex, inProgress) ->
            if (!inProgress && settledIndex in queue.indices && settledIndex != currentIndex) {
                delay(90)
                if (!pager.isScrollInProgress && pager.settledPage == settledIndex) {
                    settled(settledIndex)
                }
            }
        }
    }

    HorizontalPager(
        state = pager,
        beyondViewportPageCount = 2,
        snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Center,
        flingBehavior = PagerDefaults.flingBehavior(
            state = pager,
            snapAnimationSpec = tween(160, easing = FastOutSlowInEasing),
            snapPositionalThreshold = 0.35f
        ),
        contentPadding = PaddingValues(horizontal = 11.dp),
        pageSpacing = 11.dp,
        modifier = modifier.fillMaxSize(),
        key = { page -> "${queue.getOrNull(page)?.id}_${page}" }
    ) { page ->
        val song = queue.getOrNull(page) ?: return@HorizontalPager
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!pager.isScrollInProgress) tap()
                    }
                ),
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
