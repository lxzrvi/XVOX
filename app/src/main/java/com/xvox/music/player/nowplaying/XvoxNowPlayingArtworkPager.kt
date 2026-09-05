package com.xvox.music.player.nowplaying

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxRecentArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XvoxNowPlayingArtworkPager(
    queue: List<Song>,
    currentIndex: Int,
    navigationRequest: Int,
    onArtworkTap: () -> Unit,
    onSwipePalette: (
        Song,
        Song?,
        Float
    ) -> Unit,
    onSettledPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    repeatMode: RepeatMode = RepeatMode.OFF
) {
    if (queue.isEmpty()) return

    val screenWidth =
        LocalConfiguration.current
            .screenWidthDp.dp

    val latestIndex =
        rememberUpdatedState(currentIndex)

    val latestCommit =
        rememberUpdatedState(onSettledPage)

    val isWrapEnabled = repeatMode == RepeatMode.ALL && queue.size > 1
    val infinitePageCount = if (isWrapEnabled) Int.MAX_VALUE else queue.size
    val startPage = if (isWrapEnabled) {
        // Start in middle so we can swipe both directions infinitely
        val middle = Int.MAX_VALUE / 2
        middle - (middle % queue.size) + currentIndex.coerceIn(0, queue.lastIndex)
    } else currentIndex.coerceIn(0, queue.lastIndex)

    val pagerState =
        rememberPagerState(
            initialPage = startPage,
            pageCount = { infinitePageCount }
        )

    // Map infinite page -> queue index
    fun toQueueIndex(page: Int): Int = ((page % queue.size) + queue.size) % queue.size
    fun targetInfinitePage(targetQueueIndex: Int, currentPagerPage: Int): Int {
        if (!isWrapEnabled) return targetQueueIndex
        val currentMapped = toQueueIndex(currentPagerPage)
        var delta = targetQueueIndex - currentMapped
        // Choose shortest delta but preserve wrap direction: from last->0 is +1 not -(n-1)
        if (abs(delta) > queue.size / 2) {
            delta = if (delta > 0) delta - queue.size else delta + queue.size
        }
        return currentPagerPage + delta
    }

    LaunchedEffect(
        currentIndex,
        queue.size,
        isWrapEnabled
    ) {
        if (queue.isEmpty() || currentIndex !in queue.indices) return@LaunchedEffect
        if (isWrapEnabled) {
            val target = targetInfinitePage(currentIndex, pagerState.currentPage)
            if (!pagerState.isScrollInProgress && pagerState.currentPage != target) {
                // Use animate for wrap to keep forward/backward slide, not teleport
                // If distance is 1 and it is wrap, animate; otherwise scroll for non-wrap external change
                val distance = abs(target - pagerState.currentPage)
                if (distance == 1) {
                    // Will animate as normal pager motion (forward for last->first, backward for first->last)
                    pagerState.animateScrollToPage(target)
                } else if (pagerState.settledPage != target) {
                    // For non-adjacent jumps (e.g., initial), just snap
                    pagerState.scrollToPage(target)
                }
            }
        } else {
            if (
                !pagerState.isScrollInProgress &&
                currentIndex in queue.indices &&
                toQueueIndex(pagerState.settledPage) != currentIndex
            ) {
                pagerState.scrollToPage(currentIndex)
            }
        }
    }

    LaunchedEffect(navigationRequest) {
        if (navigationRequest == 0) {
            return@LaunchedEffect
        }
        if (isWrapEnabled) {
            val target = pagerState.currentPage + if (navigationRequest > 0) 1 else -1
            pagerState.animateScrollToPage(target)
        } else {
            val currentMapped = if (isWrapEnabled) toQueueIndex(pagerState.settledPage) else pagerState.settledPage
            val target =
                if (navigationRequest > 0) {
                    (currentMapped + 1).coerceAtMost(queue.lastIndex)
                } else {
                    (currentMapped - 1).coerceAtLeast(0)
                }
            if (target != currentMapped) {
                pagerState.animateScrollToPage(target)
            }
        }
    }

    LaunchedEffect(
        pagerState,
        queue
    ) {
        snapshotFlow {
            pagerState.currentPage to
                pagerState.currentPageOffsetFraction
        }.collect { (page, fraction) ->
            val baseIndex = toQueueIndex(page)
            val base =
                queue.getOrNull(baseIndex)
                    ?: return@collect

            val adjacentIndex = when {
                fraction > 0f -> toQueueIndex(page + 1)
                fraction < 0f -> toQueueIndex(page - 1)
                else -> null
            }
            val adjacent = adjacentIndex?.let { queue.getOrNull(it) }

            onSwipePalette(
                base,
                adjacent,
                abs(fraction)
                    .coerceIn(0f, 1f)
            )
        }
    }

    LaunchedEffect(
        pagerState,
        queue
    ) {
        snapshotFlow {
            if (
                pagerState.isScrollInProgress ||
                pagerState.currentPageOffsetFraction != 0f
            ) {
                null
            } else {
                toQueueIndex(pagerState.settledPage)
            }
        }
            .distinctUntilChanged()
            .collect { page ->
                if (
                    page != null &&
                    page in queue.indices &&
                    page != latestIndex.value
                ) {
                    latestCommit.value(page)
                }
            }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .requiredWidth(screenWidth)
                .fillMaxSize(),
            pageSize = PageSize.Fill,
            pageSpacing = 14.dp,
            beyondViewportPageCount = 1,
            verticalAlignment =
                Alignment.CenterVertically
        ) { page ->
            val qIndex = toQueueIndex(page)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 12.dp
                    )
                    .clip(
                        RoundedCornerShape(
                            20.dp
                        )
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                XvoxSongArtwork(
                    artwork =
                        queue[qIndex].artworkUri,
                    requestSize =
                        XvoxRecentArtworkSize,
                    modifier =
                        Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            queue[qIndex].id
                        ) {
                            detectTapGestures(
                                onTap = {
                                    if (
                                        !pagerState
                                            .isScrollInProgress
                                    ) {
                                        onArtworkTap()
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}
