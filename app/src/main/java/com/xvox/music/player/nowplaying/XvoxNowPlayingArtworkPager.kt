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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.RecentArtworkSize
import com.xvox.music.features.home.SongArtwork
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
    modifier: Modifier = Modifier
) {
    if (queue.isEmpty()) return

    val screenWidth =
        LocalConfiguration.current
            .screenWidthDp.dp

    val pageWidth =
        screenWidth - 24.dp

    val latestIndex =
        rememberUpdatedState(
            currentIndex
        )

    val latestCommit =
        rememberUpdatedState(
            onSettledPage
        )

    val pagerState =
        rememberPagerState(
            initialPage =
                currentIndex.coerceIn(
                    0,
                    queue.lastIndex
                ),
            pageCount = {
                queue.size
            }
        )

    LaunchedEffect(
        currentIndex,
        queue.size
    ) {
        if (
            !pagerState.isScrollInProgress &&
            currentIndex in queue.indices &&
            pagerState.settledPage != currentIndex
        ) {
            pagerState.scrollToPage(
                currentIndex
            )
        }
    }

    LaunchedEffect(navigationRequest) {
        if (navigationRequest == 0) {
            return@LaunchedEffect
        }

        val target =
            if (navigationRequest > 0) {
                (pagerState.settledPage + 1)
                    .coerceAtMost(
                        queue.lastIndex
                    )
            } else {
                (pagerState.settledPage - 1)
                    .coerceAtLeast(0)
            }

        if (target != pagerState.settledPage) {
            pagerState.animateScrollToPage(
                target
            )
        }
    }

    LaunchedEffect(
        pagerState,
        queue
    ) {
        snapshotFlow {
            pagerState.currentPage to
                pagerState.currentPageOffsetFraction
        }.collect {
            (page, fraction) ->

            val base =
                queue.getOrNull(page)
                    ?: return@collect

            val adjacent =
                when {
                    fraction > 0f ->
                        queue.getOrNull(
                            page + 1
                        )

                    fraction < 0f ->
                        queue.getOrNull(
                            page - 1
                        )

                    else -> null
                }

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
                pagerState.settledPage
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
            pageSize =
                PageSize.Fixed(pageWidth),
            pageSpacing = 14.dp,
            beyondViewportPageCount = 1,
            verticalAlignment =
                Alignment.CenterVertically
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 0.dp
                    )
                    .clip(
                        RoundedCornerShape(
                            20.dp
                        )
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                SongArtwork(
                    artwork =
                        queue[page].artworkUri,
                    requestSize =
                        RecentArtworkSize,
                    modifier =
                        Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            queue[page].id
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
