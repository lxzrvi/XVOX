package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxNowPlayingArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Full-queue smooth HorizontalPager for Now Playing.
 * - Instant cover swiping with zero delay.
 * - Fast Next/Previous button taps immediately animate the cover without getting stuck.
 * - Currently playing audio remains playing while swiping/tapping fast; only when the
 *   user settles / releases on a target song does it begin playback.
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
    val scope = rememberCoroutineScope()

    val settled by rememberUpdatedState(onSettledPage)
    val palette by rememberUpdatedState(onSwipePalette)
    val tap by rememberUpdatedState(onArtworkTap)

    var lastHandledNavRequest by remember { mutableIntStateOf(navigationRequest) }
    var targetPage by remember(currentIndex) { mutableIntStateOf(initialIdx) }

    // Fast Next/Previous button animated swiping without lag or getting stuck
    LaunchedEffect(navigationRequest) {
        if (navigationRequest == lastHandledNavRequest) return@LaunchedEffect
        val delta = navigationRequest - lastHandledNavRequest
        lastHandledNavRequest = navigationRequest

        var nextTarget = targetPage + delta
        if (repeatMode == RepeatMode.ALL) {
            nextTarget = (nextTarget % queue.size + queue.size) % queue.size
        } else {
            nextTarget = nextTarget.coerceIn(0, queue.lastIndex)
        }
        targetPage = nextTarget

        if (nextTarget in queue.indices) {
            pager.animateScrollToPage(
                page = nextTarget,
                animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
            )
        }
    }

    // Synchronize pager when currentIndex changes externally (auto-advance / tap in queue)
    LaunchedEffect(currentIndex, queue.size) {
        if (currentIndex in queue.indices && currentIndex != pager.currentPage && !pager.isScrollInProgress) {
            targetPage = currentIndex
            val dist = abs(currentIndex - pager.currentPage)
            if (dist > 1) {
                pager.scrollToPage(currentIndex)
            } else {
                pager.animateScrollToPage(
                    page = currentIndex,
                    animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                )
            }
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

    // Playback change triggered with smooth debounce (380ms) when pager settles
    LaunchedEffect(pager, queue) {
        snapshotFlow {
            Pair(pager.settledPage, pager.isScrollInProgress)
        }.distinctUntilChanged().collect { (settledIndex, inProgress) ->
            if (!inProgress && settledIndex in queue.indices && settledIndex != currentIndex) {
                delay(380)
                if (!pager.isScrollInProgress && pager.settledPage == settledIndex && settledIndex in queue.indices && settledIndex != currentIndex) {
                    targetPage = settledIndex
                    settled(settledIndex)
                }
            }
        }
    }

    HorizontalPager(
        state = pager,
        beyondViewportPageCount = 3,
        snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Center,
        flingBehavior = PagerDefaults.flingBehavior(
            state = pager,
            snapAnimationSpec = tween(120, easing = FastOutSlowInEasing),
            snapPositionalThreshold = 0.35f
        ),
        contentPadding = PaddingValues(0.dp),
        pageSpacing = 12.dp,
        modifier = modifier.fillMaxSize(),
        key = { page -> queue.getOrNull(page)?.id ?: page }
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
