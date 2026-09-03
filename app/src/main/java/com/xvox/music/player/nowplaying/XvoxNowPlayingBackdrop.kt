package com.xvox.music.player.nowplaying

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.xvox.music.core.model.Song

@Composable
fun XvoxNowPlayingBackdrop(
    song: Song,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Color(0xFF080808)
            )
    ) {
        AnimatedContent(
            targetState = song,
            contentKey = {
                it.id
            },
            transitionSpec = {
                fadeIn(
                    tween(700)
                ).togetherWith(
                    fadeOut(
                        tween(700)
                    )
                )
            },
            modifier =
                Modifier.fillMaxSize(),
            label =
                "nowPlayingBackdrop"
        ) { visualSong ->
            AsyncImage(
                model =
                    visualSong.artworkUri,
                contentDescription = null,
                contentScale =
                    ContentScale.Crop,
                alpha = 0.36f,
                modifier =
                    Modifier.fillMaxSize()
            )
        }

        /*
         * Broad translucent color atmosphere.
         * No runtime blur.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(
                                alpha = 0.20f
                            ),
                            Color.Black.copy(
                                alpha = 0.34f
                            ),
                            Color.Black.copy(
                                alpha = 0.63f
                            )
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(
                                alpha = 0.075f
                            ),
                            Color.Transparent,
                            Color.Black.copy(
                                alpha = 0.20f
                            )
                        )
                    )
                )
        )
    }
}
