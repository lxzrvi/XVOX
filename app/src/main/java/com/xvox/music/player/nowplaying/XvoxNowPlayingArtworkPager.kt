package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
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

    val scope =
        rememberCoroutineScope()

    val density =
        LocalDensity.current

    val screenWidthPx =
        with(density) {
            LocalConfiguration.current
                .screenWidthDp.dp
                .toPx()
        }

    val drag =
        remember(
            current.id
        ) {
            Animatable(0f)
        }

    val shape =
        RoundedCornerShape(
            20.dp
        )

    Box(
        modifier = modifier
            /*
             * Do not clip the pager root.
             *
             * Incoming artwork can travel from the physical
             * screen edge.
             */
            .detectArtworkSwipe(
                dragValue = {
                    drag.value
                },
                onDrag = {
                    amount ->

                    scope.launch {
                        drag.snapTo(
                            constrainedDrag(
                                current =
                                    drag.value +
                                        amount,
                                hasPrevious =
                                    previous != null,
                                hasNext =
                                    next != null
                            )
                        )
                    }
                },
                onEnd = {
                    val value =
                        drag.value

                    when {
                        value <=
                            -XvoxNowPlayingMotion
                                .ArtworkSwipeThreshold &&
                            next != null -> {

                            scope.launch {
                                drag.animateTo(
                                    -screenWidthPx,
                                    XvoxNowPlayingMotion
                                        .returnToRest
                                )

                                onNext()
                            }
                        }

                        value >=
                            XvoxNowPlayingMotion
                                .ArtworkSwipeThreshold &&
                            previous != null -> {

                            scope.launch {
                                drag.animateTo(
                                    screenWidthPx,
                                    XvoxNowPlayingMotion
                                        .returnToRest
                                )

                                onPrevious()
                            }
                        }

                        else -> {
                            scope.launch {
                                drag.animateTo(
                                    0f,
                                    XvoxNowPlayingMotion
                                        .returnToRest
                                )
                            }
                        }
                    }
                },
                onCancel = {
                    scope.launch {
                        drag.animateTo(
                            0f,
                            XvoxNowPlayingMotion
                                .returnToRest
                        )
                    }
                }
            )
    ) {
        /*
         * PREVIOUS enters from LEFT screen edge.
         */
        previous?.let {
            song ->

            ArtworkPage(
                song = song,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX =
                            drag.value -
                                screenWidthPx
                    }
            )
        }

        /*
         * NEXT enters from RIGHT screen edge.
         */
        next?.let {
            song ->

            ArtworkPage(
                song = song,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX =
                            drag.value +
                                screenWidthPx
                    }
            )
        }

        /*
         * Current artwork physically follows finger.
         */
        ArtworkPage(
            song = current,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX =
                        drag.value
                }
        )
    }
}

private fun Modifier.detectArtworkSwipe(
    dragValue: () -> Float,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit
): Modifier {
    return pointerInput(
        Unit
    ) {
        detectHorizontalDragGestures(
            onHorizontalDrag = {
                change,
                amount ->

                change.consume()
                onDrag(amount)
            },
            onDragEnd =
                onEnd,
            onDragCancel =
                onCancel
        )
    }
}

private fun constrainedDrag(
    current: Float,
    hasPrevious: Boolean,
    hasNext: Boolean
): Float {
    return when {
        current > 0f &&
            !hasPrevious ->
            current * 0.18f

        current < 0f &&
            !hasNext ->
            current * 0.18f

        else ->
            current
    }
}

@Composable
private fun ArtworkPage(
    song: Song,
    modifier: Modifier = Modifier
) {
    val shape =
        RoundedCornerShape(
            20.dp
        )

    Box(
        modifier =
            modifier.clip(
                shape
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
