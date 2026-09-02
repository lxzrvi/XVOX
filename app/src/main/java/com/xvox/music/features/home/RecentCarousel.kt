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
import kotlin.math.abs

@Composable
fun RecentCarousel(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    transition: RecentTransitionRequest,
    onSongClick:
        (
            Song,
            RecentTransitionMode
        ) -> Unit
) {
    val listState =
        rememberLazyListState()

    val fling =
        rememberSnapFlingBehavior(
            listState
        )

    var lastHandledTransition by remember {
        mutableLongStateOf(0L)
    }

    /*
     * Reconciliation happens only for an explicit
     * transition request, never merely because the
     * history List object changed.
     */
    LaunchedEffect(
        transition.id
    ) {
        if (
            transition.id == 0L ||
            transition.id ==
            lastHandledTransition
        ) {
            return@LaunchedEffect
        }

        lastHandledTransition =
            transition.id

        when (
            transition.mode
        ) {
            RecentTransitionMode.NONE ->
                Unit

            RecentTransitionMode
                .ADJACENT_SWAP -> {

                /*
                 * Underlying order is already promoted.
                 * Smoothly settle the new first item.
                 */
                listState
                    .animateScrollToItem(0)
            }

            RecentTransitionMode
                .FRONT_REPLACE -> {

                /*
                 * FAR/external transition has its own
                 * front visual. Do not parade intermediate
                 * history items through the viewport.
                 */
                listState
                    .scrollToItem(0)
            }
        }
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
            /*
             * This AnimatedContent handles ONLY:
             *
             * empty -> first song
             *
             * and explicit FRONT_REPLACE of the front
             * presentation.
             *
             * The actual LazyRow remains the carousel.
             */
            AnimatedContent(
                targetState =
                    if (
                        songs.isEmpty()
                    ) {
                        null
                    } else {
                        songs.first()
                    },
                contentKey = { song ->
                    when {
                        song == null ->
                            "empty"

                        transition.mode ==
                            RecentTransitionMode
                                .FRONT_REPLACE ->
                            "front_${transition.id}_${song.id}"

                        else ->
                            "row"
                    }
                },
                transitionSpec = {
                    (
                        slideInHorizontally(
                            animationSpec =
                                tween(260),
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
                                    tween(260),
                                targetOffsetX = {
                                    it
                                }
                            ) +
                                fadeOut(
                                    tween(150)
                                )
                        )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(122.dp),
                label =
                    "recentPresentation"
            ) { front ->

                if (
                    front == null
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
                    /*
                     * Real horizontal carousel.
                     *
                     * No parent pointer overlay.
                     * No animateItem.
                     */
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
                            items = songs,
                            key = {
                                it.id
                            },
                            contentType = {
                                "recent_song"
                            }
                        ) { song ->
                            RecentArtwork(
                                song = song,
                                current =
                                    song.id ==
                                        currentSongId,
                                playing =
                                    song.id ==
                                        currentSongId &&
                                        isPlaying,
                                onClick = {
                                    val selectedIndex =
                                        songs.indexOfFirst {
                                            it.id ==
                                                song.id
                                        }

                                    val focusedIndex =
                                        focusedRecentIndex(
                                            listState =
                                                listState
                                        )

                                    val distance =
                                        abs(
                                            selectedIndex -
                                                focusedIndex
                                        )

                                    val mode =
                                        when {
                                            song.id ==
                                                currentSongId ->
                                                RecentTransitionMode
                                                    .NONE

                                            distance == 0 ->
                                                RecentTransitionMode
                                                    .NONE

                                            distance == 1 ->
                                                RecentTransitionMode
                                                    .ADJACENT_SWAP

                                            else ->
                                                RecentTransitionMode
                                                    .FRONT_REPLACE
                                        }

                                    onSongClick(
                                        song,
                                        mode
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

private fun focusedRecentIndex(
    listState:
        androidx.compose.foundation.lazy.LazyListState
): Int {
    val layout =
        listState.layoutInfo

    val visible =
        layout.visibleItemsInfo

    if (
        visible.isEmpty()
    ) {
        return listState
            .firstVisibleItemIndex
    }

    val center =
        (
            layout.viewportStartOffset +
                layout.viewportEndOffset
            ) / 2

    return visible
        .minByOrNull {
            item ->

            abs(
                (
                    item.offset +
                        item.size / 2
                    ) -
                    center
            )
        }
        ?.index
        ?: listState
            .firstVisibleItemIndex
}
