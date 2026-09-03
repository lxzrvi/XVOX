package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.xvox.music.core.model.Song
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Stable
class XvoxNowPlayingPaletteState internal constructor(
    private val loader: XvoxArtworkPaletteLoader,
    initial: Color
) {
    private val red = Animatable(initial.red)
    private val green = Animatable(initial.green)
    private val blue = Animatable(initial.blue)

    private var visualSongId = -1L
    private var requestId = 0L

    val color: Color
        get() = Color(
            red = red.value,
            green = green.value,
            blue = blue.value,
            alpha = 1f
        )

    suspend fun show(
        song: Song,
        immediate: Boolean = false
    ) {
        if (visualSongId == song.id) return

        visualSongId = song.id
        val request = ++requestId
        val target = loader.load(song.artworkUri)

        if (
            request != requestId ||
            visualSongId != song.id
        ) {
            return
        }

        if (immediate) {
            red.snapTo(target.red)
            green.snapTo(target.green)
            blue.snapTo(target.blue)
            return
        }

        coroutineScope {
            launch {
                red.animateTo(
                    target.red,
                    tween(230)
                )
            }
            launch {
                green.animateTo(
                    target.green,
                    tween(230)
                )
            }
            launch {
                blue.animateTo(
                    target.blue,
                    tween(230)
                )
            }
        }
    }

    suspend fun preload(
        song: Song?
    ) {
        song ?: return
        loader.load(song.artworkUri)
    }
}

@Composable
fun rememberXvoxNowPlayingPalette(
    song: Song,
    queue: List<Song>,
    currentIndex: Int
): XvoxNowPlayingPaletteState {
    val context = LocalContext.current

    val loader = remember {
        XvoxArtworkPaletteLoader(context)
    }

    val state = remember {
        XvoxNowPlayingPaletteState(
            loader = loader,
            initial = Color(0xFF8C7772)
        )
    }

    LaunchedEffect(
        song.id,
        song.artworkUri
    ) {
        state.show(song)
    }

    LaunchedEffect(
        queue,
        currentIndex
    ) {
        state.preload(
            queue.getOrNull(
                currentIndex - 1
            )
        )

        state.preload(
            queue.getOrNull(
                currentIndex + 1
            )
        )
    }

    return state
}
