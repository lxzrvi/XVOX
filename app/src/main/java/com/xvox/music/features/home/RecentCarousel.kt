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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val RecentSlideMillis =
    280

private sealed interface RecentVisual {

    data object Empty :
        RecentVisual

    data class SongCard(
        val song: Song,
        val eventId: Long
    ) : RecentVisual
}

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

    /*
     * What was actually visible before the latest
     * All Songs event.
     */
    var previousVisual by remember {
        mutableStateOf<RecentVisual>(
            RecentVisual.Empty
        )
    }

    var transitionVisual by remember {
        mutableStateOf<RecentVisual?>(null)
    }

    var handledEvent by remember {
        mutableLongStateOf(0L)
    }

    var transitioning by remember {
        mutableStateOf(false)
    }

    /*
     * Keep our previous presentation synchronized while
     * user manually scrolls Recent and there is no
     * transition.
     */
    LaunchedEffect(
        listState.isScrollInProgress,
        songs,
        transitioning
    ) {
        if (
            !transitioning
        ) {
            val focused =
                focusedSong(
                    songs = songs,
                    listState =
                        listState
                )

            previousVisual =
                if (focused == null) {
                    RecentVisual.Empty
                } else {
                    RecentVisual.SongCard(
                        song = focused,
                        eventId = 0L
                    )
                }
        }
    }

    /*
     * Only All Songs creates this animation.
     */
    LaunchedEffect(
        transition.id,
        songs
    ) {
        if (
            transition.id == 0L ||
            transition.id ==
            handledEvent ||
            transition.mode !=
            RecentTransitionMode.LIBRARY
        ) {
            return@LaunchedEffect
        }

        handledEvent =
            transition.id

        val incoming =
            songs.firstOrNull {
                it.id ==
                    transition.songId
            } ?: songs.firstOrNull()
            ?: return@LaunchedEffect

        transitioning = true

        transitionVisual =
            RecentVisual.SongCard(
                song = incoming,
                eventId =
                    transition.id
            )

        /*
         * Real history becomes ready at front without
         * scrolling intermediate songs past the screen.
         */
        listState.scrollToItem(0)

        delay(
            RecentSlideMillis.toLong()
        )

        previousVisual =
            RecentVisual.SongCard(
                song = incoming,
                eventId = 0L
            )

        transitionVisual = null
        transitioning = false
    }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        val edge = 6.dp

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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            122.dp
                        )
            ) {
                if (
                    transitioning &&
                    transitionVisual != null
                ) {
                    /*
                     * During an All Songs event only this
                     * retained presentation is rendered.
                     *
                     * outgoing:
                     * previous focused/empty -> RIGHT
                     *
                     * incoming:
                     * selected song <- LEFT
                     */
                    AnimatedContent(
                        targetState =
                            transitionVisual!!,
                        contentKey = {
                            when (it) {
                                RecentVisual.Empty ->
                                    "empty"

                                is RecentVisual.SongCard ->
                                    "event_${it.eventId}_${it.song.id}"
                            }
                        },
                        transitionSpec = {
                            (
                                slideInHorizontally(
                                    animationSpec =
                                        tween(
                                            RecentSlideMillis
                                        ),
                                    initialOffsetX = {
                                        -it
                                    }
                                ) +
                                    fadeIn(
                                        tween(170)
                                    )
                                )
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec =
                                            tween(
                                                RecentSlideMillis
                                            ),
                                        targetOffsetX = {
                                            it
                                        }
                                    ) +
                                        fadeOut(
                                            tween(170)
                                        )
                                )
                        },
                        modifier =
                            Modifier.fillMaxSize(),
                        label =
                            "recentLibrarySlide"
                    ) { visual ->
                        RecentVisualContent(
                            visual =
                                visual,
                            currentSongId =
                                currentSongId,
                            isPlaying =
                                isPlaying,
                            onSongClick = {},
                            edge = edge
                        )
                    }
                } else {
                    if (
                        songs.isEmpty()
                    ) {
                        RecentEmpty(
                            modifier =
                                Modifier.fillMaxSize()
                        )
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
                                        /*
                                         * NO Recent animation.
                                         * ViewModel promotes it
                                         * silently.
                                         */
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

@Composable
private fun RecentVisualContent(
    visual: RecentVisual,
    currentSongId: Long?,
    isPlaying: Boolean,
    onSongClick: () -> Unit,
    edge: androidx.compose.ui.unit.Dp
) {
    when (visual) {
        RecentVisual.Empty -> {
            RecentEmpty(
                modifier =
                    Modifier.fillMaxSize()
            )
        }

        is RecentVisual.SongCard -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal =
                                edge
                        )
            ) {
                RecentArtwork(
                    song =
                        visual.song,
                    current =
                        visual.song.id ==
                            currentSongId,
                    playing =
                        visual.song.id ==
                            currentSongId &&
                            isPlaying,
                    onClick =
                        onSongClick,
                    modifier =
                        Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun RecentEmpty(
    modifier: Modifier =
        Modifier
) {
    Box(
        modifier =
            modifier,
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
}

private fun focusedSong(
    songs: List<Song>,
    listState:
        androidx.compose.foundation.lazy.LazyListState
): Song? {
    if (
        songs.isEmpty()
    ) {
        return null
    }

    val layout =
        listState.layoutInfo

    val visible =
        layout.visibleItemsInfo

    if (
        visible.isEmpty()
    ) {
        return songs.getOrNull(
            listState
                .firstVisibleItemIndex
        )
    }

    val center =
        (
            layout.viewportStartOffset +
                layout.viewportEndOffset
            ) / 2

    val index =
        visible
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
            ?: 0

    return songs.getOrNull(
        index
    )
}
