package com.xvox.music.player.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.xvox.music.core.model.Song

class XvoxNowPlayingPaletteState internal constructor(
    initial: Color
) {
    var current by
        mutableStateOf(initial)

    var adjacent by
        mutableStateOf(initial)

    var fraction by
        mutableFloatStateOf(0f)

    internal var adjacentSong:
        Song? = null
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
                Color(0xFF9A756C)
            )
        }

    LaunchedEffect(
        song.id,
        song.artworkUri
    ) {
        state.current =
            loader.load(
                song.artworkUri
            )

        state.adjacent =
            state.current

        state.fraction =
            0f

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
