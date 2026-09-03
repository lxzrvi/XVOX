package com.xvox.music.player.nowplaying

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.RecentArtworkSize
import com.xvox.music.features.home.SongArtwork
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XvoxNowPlayingArtworkPager(
    queue: List<Song>,
    currentIndex: Int,
    navigationRequest: Int,
    onVisualSong: (Song) -> Unit,
    onSettledPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (queue.isEmpty()) return

    val safeIndex =
        currentIndex
            .coerceIn(0, queue.lastIndex)

    val configuration =
        LocalConfiguration.current

    val screenWidth =
        configuration.screenWidthDp.dp

    val artworkWidth =
        (configuration.screenWidthDp.dp - 24.dp)
            .coerceAtLeast(1.dp)

    val pagerState =
        rememberPagerState(
            initialPage = safeIndex,
            pageCount = { queue.size }
        )

    LaunchedEffect(currentIndex, queue.size) {
        if (
            !pagerState.isScrollInProgress &&
            currentIndex in queue.indices &&
            pagerState.settledPage != currentIndex
        ) {
            pagerState.scrollToPage(currentIndex)
        }
    }

    LaunchedEffect(navigationRequest) {
        if (navigationRequest == 0) return@LaunchedEffect

        val target =
            if (navigationRequest > 0) {
                (pagerState.settledPage + 1)
                    .coerceAtMost(queue.lastIndex)
            } else {
                (pagerState.settledPage - 1)
                    .coerceAtLeast(0)
            }

        if (target != pagerState.settledPage) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState, queue) {
        snapshotFlow {
            pagerState.currentPage
        }
            .distinctUntilChanged()
            .collect { page ->
                queue.getOrNull(page)
                    ?.let(onVisualSong)
            }
    }

    LaunchedEffect(pagerState, queue) {
        snapshotFlow {
            pagerState.isScrollInProgress to
                pagerState.settledPage
        }
            .distinctUntilChanged()
            .collect { (scrolling, page) ->
                if (
                    !scrolling &&
                    page in queue.indices
                ) {
                    onVisualSong(queue[page])

                    if (page != currentIndex) {
                        onSettledPage(page)
                    }
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
            pageSize = PageSize.Fixed(artworkWidth),
            pageSpacing = 24.dp,
            snapPosition = SnapPosition.Center,
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.CenterVertically
        ) { page ->
            ArtworkPage(
                song = queue[page],
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ArtworkPage(
    song: Song,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clip(
            RoundedCornerShape(20.dp)
        )
    ) {
        SongArtwork(
            artwork = song.artworkUri,
            requestSize = RecentArtworkSize,
            modifier = Modifier.fillMaxSize()
        )
    }
}
