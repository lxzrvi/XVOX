package com.xvox.music.player.nowplaying

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

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val latestIndex = rememberUpdatedState(currentIndex)
    val latestCommit = rememberUpdatedState(onSettledPage)

    val safeIndex = currentIndex.coerceIn(0, queue.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = safeIndex,
        pageCount = { queue.size }
    )

    LaunchedEffect(currentIndex, queue.size) {
        if (queue.isNotEmpty() && currentIndex in queue.indices) {
            if (!pagerState.isScrollInProgress && pagerState.currentPage != currentIndex) {
                if (abs(pagerState.currentPage - currentIndex) == 1) {
                    pagerState.animateScrollToPage(currentIndex)
                } else {
                    pagerState.scrollToPage(currentIndex)
                }
            }
        }
    }

    LaunchedEffect(navigationRequest) {
        if (navigationRequest == 0 || queue.isEmpty()) return@LaunchedEffect
        val target = if (navigationRequest > 0) {
            if (pagerState.currentPage >= queue.lastIndex) {
                if (repeatMode == RepeatMode.ALL) 0 else queue.lastIndex
            } else {
                pagerState.currentPage + 1
            }
        } else {
            if (pagerState.currentPage <= 0) {
                if (repeatMode == RepeatMode.ALL) queue.lastIndex else 0
            } else {
                pagerState.currentPage - 1
            }
        }
        if (target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState, queue) {
        snapshotFlow { pagerState.currentPage to pagerState.currentPageOffsetFraction }
            .collect { (page, fraction) ->
                val base = queue.getOrNull(page) ?: return@collect
                val adjacentIndex = when {
                    fraction > 0f -> page + 1
                    fraction < 0f -> page - 1
                    else -> null
                }
                val adjacent = adjacentIndex?.let { queue.getOrNull(it) }
                onSwipePalette(base, adjacent, abs(fraction).coerceIn(0f, 1f))
            }
    }

    LaunchedEffect(pagerState, queue) {
        snapshotFlow {
            if (pagerState.isScrollInProgress || pagerState.currentPageOffsetFraction != 0f) null
            else pagerState.settledPage
        }
            .distinctUntilChanged()
            .collect { page ->
                if (page != null && page in queue.indices && page != latestIndex.value) {
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
            key = { page -> queue.getOrNull(page)?.id ?: page },
            modifier = Modifier
                .requiredWidth(screenWidth)
                .fillMaxSize(),
            pageSize = PageSize.Fill,
            pageSpacing = 0.dp, // Zero peeking
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.CenterVertically
        ) { page ->
            val song = queue.getOrNull(page)
            if (song != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp) // Exact tight margin matching All Songs card gap
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    XvoxSongArtwork(
                        artwork = song.artworkUri,
                        requestSize = XvoxRecentArtworkSize,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(song.id) {
                                detectTapGestures(
                                    onTap = {
                                        if (!pagerState.isScrollInProgress) {
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
}
