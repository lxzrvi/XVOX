package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
            Animatable(0f)
        }

    val translation =
        dragX +
            settle.value

    Box(
        modifier = modifier
            .pointerInput(
                current.id,
                previous?.id,
                next?.id
            ) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        scope.launch {
                            settle.snapTo(
                                0f
                            )
                        }

                        dragX = 0f

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
                                candidate > 0f &&
                                    previous ==
                                    null -> {

                                    candidate *
                                        0.18f
                                }

                                candidate < 0f &&
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
                                dragX < 0f
                            ) {
                                next
                            } else {
                                previous
                            }

                        onSwipeProgress(
                            adjacent,
                            (
                                abs(dragX) /
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

                        when {
                            final <=
                                -XvoxNowPlayingMotion
                                    .ArtworkSwipeThreshold &&
                                next != null -> {

                                dragX = 0f

                                scope.launch {
                                    settle.snapTo(
                                        final
                                    )

                                    settle.animateTo(
                                        -screenWidth,
                                        XvoxNowPlayingMotion
                                            .returnToRest
                                    )

                                    onNext()

                                    settle.snapTo(
                                        0f
                                    )

                                    onSwipeProgress(
                                        null,
                                        0f
                                    )
                                }
                            }

                            final >=
                                XvoxNowPlayingMotion
                                    .ArtworkSwipeThreshold &&
                                previous != null -> {

                                dragX = 0f

                                scope.launch {
                                    settle.snapTo(
                                        final
                                    )

                                    settle.animateTo(
                                        screenWidth,
                                        XvoxNowPlayingMotion
                                            .returnToRest
                                    )

                                    onPrevious()

                                    settle.snapTo(
                                        0f
                                    )

                                    onSwipeProgress(
                                        null,
                                        0f
                                    )
                                }
                            }

                            else -> {
                                dragX = 0f

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
                        }
                    },

                    onDragCancel = {
                        val final =
                            dragX

                        dragX = 0f

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
                song = song,
                modifier = Modifier
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
                song = song,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX =
                            translation +
                                screenWidth
                    }
            )
        }

        ArtworkPage(
            song = current,
            modifier = Modifier
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
    modifier: Modifier = Modifier
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
