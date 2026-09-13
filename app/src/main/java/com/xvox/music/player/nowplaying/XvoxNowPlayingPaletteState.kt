package com.xvox.music.player.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.xvox.music.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Stable
class XvoxNowPlayingPaletteState internal constructor(
    private val loader: XvoxArtworkPaletteLoader,
    initial: Color
) {
    private val cache = mutableStateMapOf<Long, Color>()

    var color by mutableStateOf(initial)
        private set

    fun getOrFallback(song: Song?): Color {
        if (song == null) return color
        return cache[song.id] ?: loader.fastEstimate(song.artworkUri, "${song.title}_${song.artist}")
    }

    suspend fun preload(song: Song?) {
        song ?: return
        if (cache.containsKey(song.id)) return
        val fast = loader.fastEstimate(song.artworkUri, "${song.title}_${song.artist}")
        cache[song.id] = fast
        val extracted = withContext(Dispatchers.IO) {
            loader.load(song.artworkUri, "${song.title}_${song.artist}")
        }
        cache[song.id] = extracted
    }

    suspend fun show(song: Song) {
        val targetColor = getOrFallback(song)
        color = targetColor
        preload(song)
        cache[song.id]?.let {
            color = it
        }
    }

    suspend fun blend(base: Song, adjacent: Song?, fraction: Float) {
        val from = getOrFallback(base)
        val to = adjacent?.let { getOrFallback(it) } ?: from
        color = lerp(from, to, fraction.coerceIn(0f, 1f))
    }
}

@Composable
fun rememberXvoxNowPlayingPalette(
    song: Song,
    queue: List<Song>,
    currentIndex: Int
): XvoxNowPlayingPaletteState {
    val context = LocalContext.current
    val loader = remember { XvoxArtworkPaletteLoader(context) }
    val state = remember {
        val initialColor = loader.fastEstimate(song.artworkUri, "${song.title}_${song.artist}")
        XvoxNowPlayingPaletteState(loader, initialColor)
    }

    LaunchedEffect(song.id, song.artworkUri) {
        state.show(song)
    }

    LaunchedEffect(queue, currentIndex) {
        for (offset in -8..8) {
            val s = queue.getOrNull(currentIndex + offset)
            if (s != null) {
                state.preload(s)
            }
        }
    }

    return state
}
