package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.RecentArtworkSize
import com.xvox.music.features.home.SongArtwork
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun XvoxNowPlayingArtworkPager(
    queue: List<Song>,
    currentIndex: Int,
    navigationRequest: Int,
    onSwipeProgress:
        (
            Song?,
            Float
        ) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val current =
        queue.getOrNull(
            currentIndex
        ) ?: return

    val previous =
        queue.getOrNull(
            currentIndex - 1
        )

    val next =
        queue.getOrNull(
            currentIndex + 1
        )

    val density =
        LocalDensity.current

    val scope =
        rememberCoroutineScope()

    val screenWidth =
        with(density) {
            LocalConfiguration.current
                .screenWidthDp.dp
                .toPx()
        }

    var dragX by remember(
        current.id
    ) {
        mutableFloatStateOf(
            0f
        )
    }

    val settle =
        remember(
            current.id
        ) {
            Animatable(
                0f
            )
        }

    val translation =
        dragX +
            settle.value

    /*
     * Positive request = Next.
     * Negative request = Previous.
     *
     * 0 means no button navigation request.
     */
    LaunchedEffect(
        navigationRequest
    ) {
        when {
            navigationRequest >
                0 &&
                next != null -> {

                onSwipeProgress(
                    next,
                    0f
                )

                settle.snapTo(
                    0f
                )

                settle.animateTo(
                    -screenWidth,
                    XvoxNowPlayingMotion
                        .returnToRest
                ) {
                    onSwipeProgress(
                        next,
                        (
                            abs(value) /
                                screenWidth
                            )
                            .coerceIn(
                                0f,
                                1f
                            )
                    )
                }

                onNext()
            }

            navigationRequest <
                0 &&
                previous != null -> {

                onSwipeProgress(
                    previous,
                    0f
                )

                settle.snapTo(
                    0f
                )

                settle.animateTo(
                    screenWidth,
                    XvoxNowPlayingMotion
                        .returnToRest
                ) {
                    onSwipeProgress(
                        previous,
                        (
                            abs(value) /
                                screenWidth
                            )
                            .coerceIn(
                                0f,
                                1f
                            )
                    )
                }

                onPrevious()
            }
        }
    }

    Box(
        modifier = modifier
            .pointerInput(
                current.id,
                previous?.id,
                next?.id
            ) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragX =
                            0f

                        onSwipeProgress(
                            null,
                            0f
                        )
                    },

                    onHorizontalDrag = {
                        change,
                        amount ->

                        change.consume()

                        val candidate =
                            dragX +
                                amount

                        dragX =
                            when {
                                candidate >
                                    0f &&
                                    previous ==
                                    null -> {

                                    candidate *
                                        0.18f
                                }

                                candidate <
                                    0f &&
                                    next ==
                                    null -> {

                                    candidate *
                                        0.18f
                                }

                                else ->
                                    candidate
                            }

                        val adjacent =
                            if (
                                dragX <
                                0f
                            ) {
                                next
                            } else {
                                previous
                            }

                        onSwipeProgress(
                            adjacent,
                            (
                                abs(
                                    dragX
                                ) /
                                    screenWidth
                                )
                                .coerceIn(
                                    0f,
                                    1f
                                )
                        )
                    },

                    onDragEnd = {
                        val final =
                            dragX

                        dragX =
                            0f

                        when {
                            final <=
                                -XvoxNowPlayingMotion
                                    .ArtworkSwipeThreshold &&
                                next != null -> {

                                scope.launch {
                                    settle.snapTo(
                                        final
                                    )

                                    settle.animateTo(
                                        -screenWidth,
                                        XvoxNowPlayingMotion
                                            .returnToRest
                                    ) {
                                        onSwipeProgress(
                                            next,
                                            (
                                                abs(
                                                    value
                                                ) /
                                                    screenWidth
                                                )
                                                .coerceIn(
                                                    0f,
                                                    1f
                                                )
                                        )
                                    }

                                    onNext()
                                }
                            }

                            final >=
                                XvoxNowPlayingMotion
                                    .ArtworkSwipeThreshold &&
                                previous !=
                                null -> {

                                scope.launch {
                                    settle.snapTo(
                                        final
                                    )

                                    settle.animateTo(
                                        screenWidth,
                                        XvoxNowPlayingMotion
                                            .returnToRest
                                    ) {
                                        onSwipeProgress(
                                            previous,
                                            (
                                                abs(
                                                    value
                                                ) /
                                                    screenWidth
                                                )
                                                .coerceIn(
                                                    0f,
                                                    1f
                                                )
                                        )
                                    }

                                    onPrevious()
                                }
                            }

                            else -> {
                                scope.launch {
                                    settle.snapTo(
                                        final
                                    )

                                    settle.animateTo(
                                        0f,
                                        XvoxNowPlayingMotion
                                            .returnToRest
                                    ) {
                                        val adjacent =
                                            if (
                                                value <
                                                0f
                                            ) {
                                                next
                                            } else {
                                                previous
                                            }

                                        onSwipeProgress(
                                            adjacent,
                                            (
                                                abs(
                                                    value
                                                ) /
                                                    screenWidth
                                                )
                                                .coerceIn(
                                                    0f,
                                                    1f
                                                )
                                        )
                                    }

                                    onSwipeProgress(
                                        null,
                                        0f
                                    )
                                }
                            }
                        }
                    },

                    onDragCancel = {
                        val final =
                            dragX

                        dragX =
                            0f

                        scope.launch {
                            settle.snapTo(
                                final
                            )

                            settle.animateTo(
                                0f,
                                XvoxNowPlayingMotion
                                    .returnToRest
                            )

                            onSwipeProgress(
                                null,
                                0f
                            )
                        }
                    }
                )
            }
    ) {
        previous?.let {
            song ->

            ArtworkPage(
                song =
                    song,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX =
                                translation -
                                    screenWidth
                        }
            )
        }

        next?.let {
            song ->

            ArtworkPage(
                song =
                    song,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX =
                                translation +
                                    screenWidth
                        }
            )
        }

        ArtworkPage(
            song =
                current,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX =
                            translation
                    }
        )
    }
}

@Composable
private fun ArtworkPage(
    song: Song,
    modifier: Modifier =
        Modifier
) {
    Box(
        modifier =
            modifier.clip(
                RoundedCornerShape(
                    20.dp
                )
            )
    ) {
        SongArtwork(
            artwork =
                song.artworkUri,
            requestSize =
                RecentArtworkSize,
            modifier =
                Modifier.fillMaxSize()
        )
    }
}
