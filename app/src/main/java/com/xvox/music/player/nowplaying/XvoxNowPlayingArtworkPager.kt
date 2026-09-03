package com.xvox.music.player.nowplaying

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.RecentArtworkSize
import com.xvox.music.features.home.SongArtwork
import kotlin.math.abs

@Composable
fun XvoxNowPlayingArtworkPager(
    song: Song,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var drag by remember {
        mutableFloatStateOf(0f)
    }

    var direction by remember {
        mutableIntStateOf(0)
    }

    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    20.dp
                )
            )
            .pointerInput(
                song.id,
                canPrevious,
                canNext
            ) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        drag = 0f
                    },
                    onHorizontalDrag = {
                        change,
                        amount ->

                        change.consume()
                        drag += amount
                    },
                    onDragEnd = {
                        if (
                            abs(drag) >=
                            XvoxNowPlayingMotion
                                .ArtworkSwipeThreshold
                        ) {
                            if (
                                drag < 0f &&
                                canNext
                            ) {
                                direction = 1
                                onNext()
                            } else if (
                                drag > 0f &&
                                canPrevious
                            ) {
                                direction = -1
                                onPrevious()
                            }
                        }

                        drag = 0f
                    },
                    onDragCancel = {
                        drag = 0f
                    }
                )
            }
    ) {
        AnimatedContent(
            targetState = song,
            contentKey = {
                it.id
            },
            transitionSpec = {
                when {
                    direction > 0 -> {
                        (
                            slideInHorizontally(
                                tween(270)
                            ) {
                                it
                            } +
                                fadeIn(
                                    tween(210)
                                )
                            )
                            .togetherWith(
                                slideOutHorizontally(
                                    tween(270)
                                ) {
                                    -it
                                } +
                                    fadeOut(
                                        tween(190)
                                    )
                            )
                    }

                    direction < 0 -> {
                        (
                            slideInHorizontally(
                                tween(270)
                            ) {
                                -it
                            } +
                                fadeIn(
                                    tween(210)
                                )
                            )
                            .togetherWith(
                                slideOutHorizontally(
                                    tween(270)
                                ) {
                                    it
                                } +
                                    fadeOut(
                                        tween(190)
                                    )
                            )
                    }

                    else -> {
                        fadeIn(
                            tween(200)
                        ).togetherWith(
                            fadeOut(
                                tween(180)
                            )
                        )
                    }
                }
            },
            modifier =
                Modifier.fillMaxSize(),
            label =
                "nowPlayingArtwork"
        ) { visualSong ->
            SongArtwork(
                artwork =
                    visualSong.artworkUri,
                requestSize =
                    RecentArtworkSize,
                modifier =
                    Modifier.fillMaxSize()
            )
        }
    }
}
