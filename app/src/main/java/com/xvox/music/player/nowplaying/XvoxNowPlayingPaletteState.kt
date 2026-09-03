package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.VectorConverter
import androidx.compose.ui.platform.LocalContext
import com.xvox.music.core.model.Song
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Stable
class XvoxNowPlayingPaletteState internal constructor(
    private val loader: XvoxArtworkPaletteLoader,
    initial: Color
) {
    val color =
        Animatable(
            initialValue = initial,
            typeConverter = Color.VectorConverter
        )

    private val mutex = Mutex()
    private var visualSongId = -1L

    suspend fun show(
        song: Song,
        immediate: Boolean = false
    ) {
        if (visualSongId == song.id) return

        visualSongId = song.id
        val target = loader.load(song.artworkUri)

        mutex.withLock {
            if (visualSongId != song.id) return

            if (immediate) {
                color.snapTo(target)
            } else {
                color.animateTo(
                    target,
                    tween(230)
                )
            }
        }
    }

    suspend fun preload(song: Song?) {
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

    LaunchedEffect(song.id, song.artworkUri) {
        state.show(song)
    }

    LaunchedEffect(queue, currentIndex) {
        state.preload(
            queue.getOrNull(currentIndex - 1)
        )
        state.preload(
            queue.getOrNull(currentIndex + 1)
        )
    }

    return state
}
