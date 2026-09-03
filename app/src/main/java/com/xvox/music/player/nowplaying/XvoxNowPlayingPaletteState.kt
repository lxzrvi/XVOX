package com.xvox.music.player.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.xvox.music.core.model.Song

class XvoxNowPlayingPaletteState
internal constructor(
    initial: Color
) {
    var current by
        mutableStateOf(
            initial
        )

    var adjacent by
        mutableStateOf(
            initial
        )

    var fraction by
        mutableFloatStateOf(
            0f
        )

    internal var adjacentSong by
        mutableStateOf<Song?>(
            null
        )

    internal var currentSongId:
        Long = -1L

    fun renderedColor():
        Color {
        return lerp(
            current,
            adjacent,
            fraction
                .coerceIn(
                    0f,
                    1f
                )
        )
    }
}

@Composable
fun rememberXvoxNowPlayingPalette(
    song: Song
): XvoxNowPlayingPaletteState {
    val context =
        LocalContext.current

    val loader =
        remember {
            XvoxArtworkPaletteLoader(
                context
            )
        }

    val state =
        remember {
            XvoxNowPlayingPaletteState(
                Color(0xFF8C7772)
            )
        }

    LaunchedEffect(
        song.id,
        song.artworkUri
    ) {
        /*
         * Preserve whatever color was actually on screen
         * during the previous swipe.
         */
        val retained =
            state.renderedColor()

        if (
            state.currentSongId !=
            -1L
        ) {
            state.current =
                retained

            state.adjacent =
                retained

            state.fraction =
                0f
        }

        val loaded =
            loader.load(
                song.artworkUri
            )

        state.current =
            loaded

        state.adjacent =
            loaded

        state.fraction =
            0f

        state.currentSongId =
            song.id

        state.adjacentSong =
            null
    }

    LaunchedEffect(
        state.adjacentSong?.id
    ) {
        val adjacent =
            state.adjacentSong

        state.adjacent =
            if (
                adjacent == null
            ) {
                state.current
            } else {
                loader.load(
                    adjacent.artworkUri
                )
            }
    }

    return state
}
