package com.xvox.music.player.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.xvox.music.core.model.Song
import java.util.concurrent.ConcurrentHashMap

@Stable
class XvoxNowPlayingPaletteState internal constructor(
    private val loader: XvoxArtworkPaletteLoader,
    initial: Color
) {
    private val colors =
        ConcurrentHashMap<Long, Color>()

    var color by mutableStateOf(initial)
        private set

    suspend fun preload(
        song: Song?
    ) {
        song ?: return

        if (colors.containsKey(song.id)) {
            return
        }

        colors[song.id] =
            loader.load(song.artworkUri)
    }

    suspend fun show(
        song: Song
    ) {
        preload(song)
        color =
            colors[song.id] ?: color
    }

    suspend fun blend(
        base: Song,
        adjacent: Song?,
        fraction: Float
    ) {
        preload(base)
        preload(adjacent)

        val from =
            colors[base.id] ?: color

        val to =
            adjacent?.let {
                colors[it.id]
            } ?: from

        color =
            lerp(
                from,
                to ?: from,
                fraction.coerceIn(0f, 1f)
            )
    }
}

@Composable
fun rememberXvoxNowPlayingPalette(
    song: Song,
    queue: List<Song>,
    currentIndex: Int
): XvoxNowPlayingPaletteState {
    val context = LocalContext.current

    val loader =
        remember {
            XvoxArtworkPaletteLoader(context)
        }

    val state =
        remember {
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
            queue.getOrNull(currentIndex)
        )
        state.preload(
            queue.getOrNull(currentIndex - 1)
        )
        state.preload(
            queue.getOrNull(currentIndex + 1)
        )
        state.preload(
            queue.getOrNull(currentIndex - 2)
        )
        state.preload(
            queue.getOrNull(currentIndex + 2)
        )
    }

    return state
}
