package com.xvox.music.player.nowplaying

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.player.styles.XvoxPlayerStyle
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@Composable
fun XvoxNowPlayingArtworkForStyle(
    song: Song,
    queue: List<Song>,
    currentIndex: Int,
    isPlaying: Boolean,
    style: XvoxPlayerStyle,
    navigationRequest: Int,
    onArtworkTap: () -> Unit,
    onSwipePalette: (Song, Song?, Float) -> Unit,
    onSettledPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    repeatMode: RepeatMode = RepeatMode.OFF
) {
    when (style) {
        XvoxPlayerStyle.NORMAL -> {
            XvoxNowPlayingArtworkPager(
                queue = queue,
                currentIndex = currentIndex,
                navigationRequest = navigationRequest,
                onArtworkTap = onArtworkTap,
                onSwipePalette = onSwipePalette,
                onSettledPage = onSettledPage,
                modifier = modifier,
                repeatMode = repeatMode
            )
        }
        XvoxPlayerStyle.FULL_ART -> {
            XvoxFullArtPager(
                queue = queue,
                currentIndex = currentIndex,
                navigationRequest = navigationRequest,
                onSwipePalette = onSwipePalette,
                onSettledPage = onSettledPage,
                modifier = modifier,
                repeatMode = repeatMode
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun XvoxFullArtPager(
    queue: List<Song>,
    currentIndex: Int,
    navigationRequest: Int,
    onSwipePalette: (Song, Song?, Float) -> Unit,
    onSettledPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    repeatMode: RepeatMode = RepeatMode.OFF
) {
    if (queue.isEmpty()) return
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val latestIndex = rememberUpdatedState(currentIndex)
    val latestCommit = rememberUpdatedState(onSettledPage)
    val isWrapEnabled = repeatMode == RepeatMode.ALL && queue.size > 1
    val infinitePageCount = if (isWrapEnabled) Int.MAX_VALUE else queue.size
    val startPage = if (isWrapEnabled) {
        val middle = Int.MAX_VALUE / 2
        middle - (middle % queue.size) + currentIndex.coerceIn(0, queue.lastIndex)
    } else currentIndex.coerceIn(0, queue.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { infinitePageCount }
    )
    fun toQueueIndex(page: Int): Int = ((page % queue.size) + queue.size) % queue.size
    fun targetInfinitePage(targetQueueIndex: Int, currentPagerPage: Int): Int {
        if (!isWrapEnabled) return targetQueueIndex
        val currentMapped = toQueueIndex(currentPagerPage)
        var delta = targetQueueIndex - currentMapped
        if (kotlin.math.abs(delta) > queue.size / 2) {
            delta = if (delta > 0) delta - queue.size else delta + queue.size
        }
        return currentPagerPage + delta
    }
    LaunchedEffect(currentIndex, queue.size, isWrapEnabled) {
        if (queue.isEmpty() || currentIndex !in queue.indices) return@LaunchedEffect
        if (isWrapEnabled) {
            val target = targetInfinitePage(currentIndex, pagerState.currentPage)
            if (!pagerState.isScrollInProgress && pagerState.currentPage != target) {
                val distance = kotlin.math.abs(target - pagerState.currentPage)
                if (distance == 1) pagerState.animateScrollToPage(target) else if (pagerState.settledPage != target) pagerState.scrollToPage(target)
            }
        } else {
            if (!pagerState.isScrollInProgress && currentIndex in queue.indices && toQueueIndex(pagerState.settledPage) != currentIndex) {
                pagerState.scrollToPage(currentIndex)
            }
        }
    }
    LaunchedEffect(navigationRequest) {
        if (navigationRequest == 0) return@LaunchedEffect
        if (isWrapEnabled) {
            val target = pagerState.currentPage + if (navigationRequest > 0) 1 else -1
            pagerState.animateScrollToPage(target)
        } else {
            val currentMapped = if (isWrapEnabled) toQueueIndex(pagerState.settledPage) else pagerState.settledPage
            val target = if (navigationRequest > 0) (currentMapped + 1).coerceAtMost(queue.lastIndex) else (currentMapped - 1).coerceAtLeast(0)
            if (target != currentMapped) pagerState.animateScrollToPage(target)
        }
    }
    LaunchedEffect(pagerState, queue) {
        snapshotFlow { pagerState.currentPage to pagerState.currentPageOffsetFraction }
            .collect { (page, fraction) ->
                val baseIndex = toQueueIndex(page)
                val base = queue.getOrNull(baseIndex) ?: return@collect
                val adjacentIndex = when {
                    fraction > 0f -> toQueueIndex(page + 1)
                    fraction < 0f -> toQueueIndex(page - 1)
                    else -> null
                }
                val adjacent = adjacentIndex?.let { queue.getOrNull(it) }
                onSwipePalette(base, adjacent, abs(fraction).coerceIn(0f, 1f))
            }
    }
    LaunchedEffect(pagerState, queue) {
        snapshotFlow {
            if (pagerState.isScrollInProgress || pagerState.currentPageOffsetFraction != 0f) null else toQueueIndex(pagerState.settledPage)
        }.distinctUntilChanged().collect { page ->
            if (page != null && page in queue.indices && page != latestIndex.value) latestCommit.value(page)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSize = PageSize.Fill,
            pageSpacing = 0.dp,
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.CenterVertically
        ) { page ->
            val qIndex = toQueueIndex(page)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                XvoxSongArtwork(
                    artwork = queue[qIndex].artworkUri,
                    requestSize = 2048,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    return this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
