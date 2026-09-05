package com.xvox.music.player.nowplaying

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

    val wrapEnabled =
        repeatMode == RepeatMode.ALL &&
            queue.size > 1

    val virtualPageCount =
        if (wrapEnabled) {
            Int.MAX_VALUE
        } else {
            queue.size
        }

    fun queueIndexForPage(
        page: Int
    ): Int {
        if (!wrapEnabled) {
            return page.coerceIn(
                0,
                queue.lastIndex
            )
        }

        return (
            (page % queue.size) +
                queue.size
            ) % queue.size
    }

    val safeCurrentIndex =
        currentIndex.coerceIn(
            0,
            queue.lastIndex
        )

    val initialPage =
        if (wrapEnabled) {
            val middle =
                Int.MAX_VALUE / 2

            middle -
                (middle % queue.size) +
                safeCurrentIndex
        } else {
            safeCurrentIndex
        }

    val pagerState =
        rememberPagerState(
            initialPage = initialPage,
            pageCount = {
                virtualPageCount
            }
        )

    val latestCurrentIndex =
        rememberUpdatedState(
            currentIndex
        )

    val latestSettledCallback =
        rememberUpdatedState(
            onSettledPage
        )

    fun nearestVirtualPage(
        targetQueueIndex: Int
    ): Int {
        if (!wrapEnabled) {
            return targetQueueIndex
        }

        val currentPage =
            pagerState.currentPage

        val currentQueueIndex =
            queueIndexForPage(
                currentPage
            )

        var delta =
            targetQueueIndex -
                currentQueueIndex

        if (
            abs(delta) >
            queue.size / 2
        ) {
            delta =
                if (delta > 0) {
                    delta - queue.size
                } else {
                    delta + queue.size
                }
        }

        return currentPage + delta
    }

    LaunchedEffect(
        currentIndex,
        queue.size,
        wrapEnabled
    ) {
        if (
            currentIndex !in queue.indices ||
            pagerState.isScrollInProgress
        ) {
            return@LaunchedEffect
        }

        val currentlyShown =
            queueIndexForPage(
                pagerState.settledPage
            )

        if (
            currentlyShown !=
            currentIndex
        ) {
            val target =
                nearestVirtualPage(
                    currentIndex
                )

            if (
                abs(
                    target -
                        pagerState.currentPage
                ) == 1
            ) {
                pagerState
                    .animateScrollToPage(
                        target
                    )
            } else {
                pagerState
                    .scrollToPage(
                        target
                    )
            }
        }
    }

    LaunchedEffect(
        navigationRequest
    ) {
        if (
            navigationRequest == 0 ||
            queue.isEmpty()
        ) {
            return@LaunchedEffect
        }

        val direction =
            if (
                navigationRequest > 0
            ) {
                1
            } else {
                -1
            }

        val currentPage =
            pagerState.settledPage

        val targetPage =
            if (wrapEnabled) {
                currentPage + direction
            } else {
                (
                    currentPage +
                        direction
                    ).coerceIn(
                    0,
                    queue.lastIndex
                )
            }

        if (
            targetPage !=
            currentPage
        ) {
            pagerState
                .animateScrollToPage(
                    targetPage
                )
        }
    }

    LaunchedEffect(
        pagerState,
        queue,
        wrapEnabled
    ) {
        snapshotFlow {
            pagerState.currentPage to
                pagerState
                    .currentPageOffsetFraction
        }.collect {
                (
                    page,
                    fraction
                ) ->

            val baseIndex =
                queueIndexForPage(
                    page
                )

            val baseSong =
                queue.getOrNull(
                    baseIndex
                ) ?: return@collect

            val adjacentSong =
                when {
                    fraction > 0f -> {
                        val nextPage =
                            page + 1

                        if (
                            wrapEnabled ||
                            nextPage <
                            queue.size
                        ) {
                            queue.getOrNull(
                                queueIndexForPage(
                                    nextPage
                                )
                            )
                        } else {
                            null
                        }
                    }

                    fraction < 0f -> {
                        val previousPage =
                            page - 1

                        if (
                            wrapEnabled ||
                            previousPage >= 0
                        ) {
                            queue.getOrNull(
                                queueIndexForPage(
                                    previousPage
                                )
                            )
                        } else {
                            null
                        }
                    }

                    else -> null
                }

            onSwipePalette(
                baseSong,
                adjacentSong,
                abs(fraction)
                    .coerceIn(
                        0f,
                        1f
                    )
            )
        }
    }

    LaunchedEffect(
        pagerState,
        queue,
        wrapEnabled
    ) {
        snapshotFlow {
            if (
                pagerState
                    .isScrollInProgress ||
                pagerState
                    .currentPageOffsetFraction !=
                    0f
            ) {
                null
            } else {
                queueIndexForPage(
                    pagerState.settledPage
                )
            }
        }
            .distinctUntilChanged()
            .collect { settledIndex ->

                if (
                    settledIndex != null &&
                    settledIndex in
                    queue.indices &&
                    settledIndex !=
                    latestCurrentIndex.value
                ) {
                    latestSettledCallback
                        .value(
                            settledIndex
                        )
                }
            }
    }

    Box(
        modifier =
            modifier.fillMaxSize(),
        contentAlignment =
            Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier.fillMaxSize(),
            pageSize =
                PageSize.Fill,
            pageSpacing =
                0.dp,
            beyondViewportPageCount =
                1,
            verticalAlignment =
                Alignment.CenterVertically,
            key = { page ->
                if (wrapEnabled) {
                    page
                } else {
                    queue[
                        queueIndexForPage(
                            page
                        )
                    ].id
                }
            }
        ) { page ->

            val song =
                queue[
                    queueIndexForPage(
                        page
                    )
                ]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal =
                            8.dp,
                        vertical =
                            8.dp
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
                        song.artworkUri,
                    requestSize =
                        XvoxRecentArtworkSize,
                    modifier =
                        Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            song.id
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
