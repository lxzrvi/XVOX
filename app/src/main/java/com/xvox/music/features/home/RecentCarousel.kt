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
import androidx.compose.foundation.lazy.LazyListState
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
import kotlinx.coroutines.yield
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
    onSongClick: (Song) -> Unit
) {
    /*
     * This state belongs to the carousel.
     *
     * Playback changes do NOT recreate it.
     * Recent order changes do NOT recreate it.
     */
    val listState =
        rememberLazyListState()

    val flingBehavior =
        rememberSnapFlingBehavior(
            listState
        )

    /*
     * Last presentation that the user was actually
     * looking at.
     *
     * This is retained before an All Songs event so
     * we can animate exactly:
     *
     * OLD -> RIGHT
     * NEW <- LEFT
     *
     * without moving hidden history cards through
     * the viewport.
     */
    var previousVisual by remember {
        mutableStateOf<RecentVisual>(
            RecentVisual.Empty
        )
    }

    /*
     * Used only while the explicit All Songs
     * transition is running.
     */
    var transitionVisual by remember {
        mutableStateOf<RecentVisual>(
            RecentVisual.Empty
        )
    }

    var transitioning by remember {
        mutableStateOf(false)
    }

    var handledTransitionId by remember {
        mutableLongStateOf(0L)
    }

    /*
     * ========================================================
     * TRACK USER'S NORMAL CAROUSEL FOCUS
     * ========================================================
     *
     * When the user manually swipes Recent, keep a snapshot
     * of the card currently closest to the viewport center.
     *
     * This does NOT modify history order.
     */
    LaunchedEffect(
        listState.isScrollInProgress,
        songs,
        transitioning
    ) {
        if (transitioning) {
            return@LaunchedEffect
        }

        val focused =
            focusedSong(
                songs = songs,
                listState = listState
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

    /*
     * ========================================================
     * ALL SONGS -> RECENT TRANSITION
     * ========================================================
     *
     * ONLY RecentTransitionMode.LIBRARY enters here.
     *
     * Tapping inside Recent itself never triggers this
     * transition.
     */
    LaunchedEffect(
        transition.id
    ) {
        if (
            transition.id == 0L ||
            transition.id ==
                handledTransitionId ||
            transition.mode !=
                RecentTransitionMode.LIBRARY
        ) {
            return@LaunchedEffect
        }

        val incoming =
            songs.firstOrNull {
                it.id ==
                    transition.songId
            }
                ?: songs.firstOrNull()
                ?: return@LaunchedEffect

        handledTransitionId =
            transition.id

        /*
         * Freeze the presentation layer.
         */
        transitioning = true

        /*
         * Start AnimatedContent from the OLD content.
         *
         * This can be:
         *
         * Nothing played yet
         *
         * OR the Recent card the user was currently
         * looking at.
         */
        transitionVisual =
            previousVisual

        /*
         * Underlying history is already newest-first.
         *
         * Jump it silently to index 0 underneath the
         * temporary presentation layer.
         *
         * DO NOT animateScrollToItem here.
         *
         * Otherwise every intermediate history item can
         * become visible.
         */
        listState.scrollToItem(0)

        /*
         * Give Compose one frame/chance to establish the
         * retained OLD target before changing AnimatedContent
         * to the incoming one.
         */
        yield()

        /*
         * Now trigger exactly one transition:
         *
         * OLD -> RIGHT
         * NEW <- LEFT
         */
        transitionVisual =
            RecentVisual.SongCard(
                song = incoming,
                eventId =
                    transition.id
            )

        delay(
            RecentSlideMillis.toLong()
        )

        /*
         * Transition finished.
         *
         * The real LazyRow underneath is already at the
         * correct newest/front song.
         */
        previousVisual =
            RecentVisual.SongCard(
                song = incoming,
                eventId = 0L
            )

        transitioning = false
    }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        val edge =
            6.dp

        /*
         * At rest:
         *
         * 6dp | CURRENT CARD | 6dp
         *
         * No intentional adjacent-card peek.
         */
        val itemWidth =
            maxWidth -
                edge * 2

        /*
         * Combined with viewport edge padding, this keeps
         * settled cards visually isolated.
         */
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
                if (transitioning) {
                    /*
                     * =================================================
                     * TEMPORARY PRESENTATION
                     * =================================================
                     *
                     * The LazyRow is NOT rendered over this.
                     *
                     * Only old/new visible content participates.
                     */
                    AnimatedContent(
                        targetState =
                            transitionVisual,
                        contentKey = {
                            visual ->

                            when (visual) {
                                RecentVisual.Empty ->
                                    "empty"

                                is RecentVisual.SongCard ->
                                    "song_${visual.eventId}_${visual.song.id}"
                            }
                        },
                        transitionSpec = {
                            (
                                slideInHorizontally(
                                    animationSpec =
                                        tween(
                                            durationMillis =
                                                RecentSlideMillis
                                        ),
                                    initialOffsetX = {
                                        fullWidth ->
                                        -fullWidth
                                    }
                                ) +
                                    fadeIn(
                                        animationSpec =
                                            tween(
                                                durationMillis =
                                                    170
                                            )
                                    )
                                )
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec =
                                            tween(
                                                durationMillis =
                                                    RecentSlideMillis
                                            ),
                                        targetOffsetX = {
                                            fullWidth ->
                                            fullWidth
                                        }
                                    ) +
                                        fadeOut(
                                            animationSpec =
                                                tween(
                                                    durationMillis =
                                                        170
                                                )
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
                            edge =
                                edge
                        )
                    }
                } else {
                    /*
                     * =================================================
                     * NORMAL RECENT CAROUSEL
                     * =================================================
                     *
                     * This owns all normal horizontal swiping.
                     *
                     * No invisible animation overlay.
                     * No animateItem().
                     * No pointer interception.
                     */
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
                                flingBehavior,
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
                                    song ->
                                    song.id
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
                                        /*
                                         * RECENT TAP:
                                         *
                                         * No visual Recent transition.
                                         *
                                         * HomeViewModel silently:
                                         * - promotes selected song to 0
                                         * - persists order
                                         *
                                         * Playback is handled separately.
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

            /*
             * =========================================================
             * INDICATOR
             * =========================================================
             *
             * It follows the real carousel scroll state.
             *
             * During an external transition the real row is silently
             * prepared at item zero, so the rail also resolves to front
             * without stepping through hidden songs.
             */
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
    edge: androidx.compose.ui.unit.Dp
) {
    when (visual) {
        RecentVisual.Empty -> {
            /*
             * Same exact 122dp presentation viewport.
             *
             * Therefore first playback can animate:
             *
             * Nothing played yet -> RIGHT
             * song             <- LEFT
             */
            RecentEmpty(
                modifier =
                    Modifier.fillMaxSize()
            )
        }

        is RecentVisual.SongCard -> {
            Box(
                modifier = Modifier
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
                    /*
                     * Presentation is intentionally
                     * non-interactive.
                     */
                    onClick = {},
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
            fontSize =
                12.sp
        )
    }
}

/*
 * Returns the song that is actually closest to the
 * center of the current Recent viewport.
 *
 * This is presentation state only.
 * It never changes history ordering.
 */
private fun focusedSong(
    songs: List<Song>,
    listState: LazyListState
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

    val viewportCenter =
        (
            layout.viewportStartOffset +
                layout.viewportEndOffset
            ) / 2

    val focusedIndex =
        visible
            .minByOrNull {
                item ->

                abs(
                    (
                        item.offset +
                            item.size / 2
                        ) -
                        viewportCenter
                )
            }
            ?.index
            ?: listState
                .firstVisibleItemIndex

    return songs.getOrNull(
        focusedIndex
    )
}
