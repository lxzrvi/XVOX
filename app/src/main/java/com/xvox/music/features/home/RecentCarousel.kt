package com.xvox.music.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import kotlinx.coroutines.delay

private const val RecentTransitionDuration =
    260L

private data class RecentFrontPresentation(
    val eventId: Long,
    val song: Song?
)

@Composable
fun RecentCarousel(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    transition:
        RecentTransitionRequest,
    onSongClick:
        (Song) -> Unit
) {
    val listState =
        rememberLazyListState()

    val fling =
        rememberSnapFlingBehavior(
            listState
        )

    var presentedEventId by remember {
        mutableLongStateOf(0L)
    }

    var transitionVisible by remember {
        androidx.compose.runtime
            .mutableStateOf(false)
    }

    LaunchedEffect(
        transition.id
    ) {
        if (
            transition.id == 0L ||
            transition.id ==
            presentedEventId ||
            transition.mode !=
            RecentTransitionMode.LIBRARY
        ) {
            return@LaunchedEffect
        }

        presentedEventId =
            transition.id

        /*
         * Underlying history immediately becomes correct.
         *
         * No animateScrollToItem here:
         * hidden cards must never parade across the screen.
         */
        if (
            songs.isNotEmpty()
        ) {
            listState.scrollToItem(0)
        }

        transitionVisible =
            true

        delay(
            RecentTransitionDuration
        )

        transitionVisible =
            false
    }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        val edge =
            6.dp

        val itemWidth =
            maxWidth -
                edge * 2

        val itemGap =
            edge * 2

        val railWidth =
            itemWidth *
                0.22f

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        122.dp
                    )
            ) {
                if (
                    songs.isEmpty()
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                "Nothing played yet",
                            color =
                                XvoxTheme.colors
                                    .secondaryText,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyRow(
                        state =
                            listState,
                        flingBehavior =
                            fling,
                        modifier =
                            Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                horizontal =
                                    edge
                            ),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                itemGap
                            )
                    ) {
                        items(
                            items =
                                songs,
                            key = {
                                it.id
                            },
                            contentType = {
                                "recent_song"
                            }
                        ) { song ->
                            RecentArtwork(
                                song =
                                    song,
                                current =
                                    song.id ==
                                        currentSongId,
                                playing =
                                    song.id ==
                                        currentSongId &&
                                        isPlaying,
                                onClick = {
                                    onSongClick(
                                        song
                                    )
                                },
                                modifier =
                                    Modifier
                                        .width(
                                            itemWidth
                                        )
                                        .height(
                                            122.dp
                                        )
                            )
                        }
                    }
                }

                /*
                 * ALL SONGS transition only.
                 *
                 * This presenter doesn't add click handlers,
                 * and disappears after 260ms.
                 */
                if (
                    transitionVisible
                ) {
                    AnimatedContent(
                        targetState =
                            RecentFrontPresentation(
                                eventId =
                                    transition.id,
                                song =
                                    songs.firstOrNull()
                            ),
                        contentKey = {
                            it.eventId
                        },
                        transitionSpec = {
                            (
                                slideInHorizontally(
                                    animationSpec =
                                        tween(
                                            RecentTransitionDuration
                                                .toInt()
                                        ),
                                    initialOffsetX = {
                                        -it
                                    }
                                ) +
                                    fadeIn(
                                        tween(150)
                                    )
                                )
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec =
                                            tween(
                                                RecentTransitionDuration
                                                    .toInt()
                                            ),
                                        targetOffsetX = {
                                            it
                                        }
                                    ) +
                                        fadeOut(
                                            tween(150)
                                        )
                                )
                        },
                        modifier =
                            Modifier.fillMaxSize(),
                        label =
                            "recentLibraryTransition"
                    ) { front ->
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(
                                        horizontal =
                                            edge
                                    )
                        ) {
                            front.song
                                ?.let { song ->
                                    RecentArtwork(
                                        song =
                                            song,
                                        current =
                                            song.id ==
                                                currentSongId,
                                        playing =
                                            song.id ==
                                                currentSongId &&
                                                isPlaying,
                                        onClick = {},
                                        modifier =
                                            Modifier.fillMaxSize()
                                    )
                                }
                        }
                    }
                }
            }

            RecentPositionRail(
                songCount =
                    songs.size,
                listState =
                    listState,
                itemWidth =
                    itemWidth,
                itemGap =
                    itemGap,
                railWidth =
                    railWidth,
                modifier =
                    Modifier.padding(
                        top =
                            HomeGeometry
                                .sectionGap
                    )
            )
        }
    }
}
