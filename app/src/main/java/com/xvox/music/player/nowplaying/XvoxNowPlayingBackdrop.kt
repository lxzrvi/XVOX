package com.xvox.music.player.nowplaying

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.xvox.music.core.model.Song

@Composable
fun XvoxNowPlayingBackdrop(
    song: Song,
    modifier: Modifier = Modifier
) {
    val context =
        LocalContext.current

    val loader =
        remember {
            XvoxArtworkPaletteLoader(
                context
            )
        }

    var palette by remember {
        mutableStateOf(
            XvoxArtworkPalette(
                primary =
                    Color(0xFF31201C),
                secondary =
                    Color(0xFF120D0C)
            )
        )
    }

    var paletteSongId by remember {
        mutableStateOf(
            -1L
        )
    }

    LaunchedEffect(
        song.id,
        song.artworkUri
    ) {
        palette =
            loader.load(
                song.artworkUri
            )

        paletteSongId =
            song.id
    }

    AnimatedContent(
        targetState =
            paletteSongId to
                palette,
        contentKey = {
            it.first
        },
        transitionSpec = {
            fadeIn(
                tween(850)
            ).togetherWith(
                fadeOut(
                    tween(850)
                )
            )
        },
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Color.Black
                ),
        label =
            "nowPlayingPalette"
    ) {
        state ->

        val colors =
            state.second

        Canvas(
            modifier =
                Modifier.fillMaxSize()
        ) {
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors =
                            listOf(
                                colors.primary,
                                colors.secondary
                            ),
                        start =
                            Offset.Zero,
                        end =
                            Offset(
                                size.width,
                                size.height
                            )
                    )
            )

            drawRect(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                colors.primary.copy(
                                    alpha = 0.78f
                                ),
                                Color.Transparent
                            ),
                        center =
                            Offset(
                                size.width *
                                    0.22f,
                                size.height *
                                    0.22f
                            ),
                        radius =
                            size.maxDimension *
                                0.72f
                    )
            )

            drawRect(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                colors.secondary.copy(
                                    alpha = 0.92f
                                ),
                                Color.Transparent
                            ),
                        center =
                            Offset(
                                size.width *
                                    0.82f,
                                size.height *
                                    0.72f
                            ),
                        radius =
                            size.maxDimension *
                                0.74f
                    )
            )

            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color.Black.copy(
                                    alpha = 0.08f
                                ),
                                Color.Transparent,
                                Color.Black.copy(
                                    alpha = 0.26f
                                )
                            )
                    )
            )
        }
    }
}
