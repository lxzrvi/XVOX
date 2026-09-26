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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Keeps cover colours ready before a pager reaches them. Pixel work lives in the loader's IO
 * context; the UI state only receives completed colours, so rapid cover browsing stays smooth.
 */
@Stable
class XvoxNowPlayingPaletteState internal constructor(
    private val loader: XvoxArtworkPaletteLoader,
    initial: Color
) {
    private val cache = mutableStateMapOf<Long, Color>()
    private val loadingSongIds = mutableSetOf<Long>()

    var color by mutableStateOf(initial)
        private set

    /** True only while the pager is between two covers, so the backdrop can follow the finger. */
    var isCoverTransitionInProgress by mutableStateOf(false)
        private set

    private fun knownColor(song: Song): Color? =
        cache[song.id] ?: loader.cachedColor(song.artworkUri, "${song.title}_${song.artist}")

    fun getOrFallback(song: Song?): Color {
        if (song == null) return color
        // An unprepared cover keeps the live player backdrop until its IO palette is available.
        // This avoids flashing a synthetic hash colour when browsing or opening a selected song.
        return knownColor(song) ?: color
    }

    suspend fun preload(song: Song?) {
        song ?: return
        if (cache.containsKey(song.id) || !loadingSongIds.add(song.id)) return
        try {
            cache[song.id] = loader.load(song.artworkUri, "${song.title}_${song.artist}")
        } finally {
            loadingSongIds.remove(song.id)
        }
    }

    /** Prioritise the immediate neighbours, then warm several covers in both directions. */
    suspend fun preloadNeighborhood(queue: List<Song>, currentIndex: Int) {
        val offsets = listOf(0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6, 7, -7, 8, -8)
        val neighbours = offsets.mapNotNull { offset -> queue.getOrNull(currentIndex + offset) }
            .distinctBy { it.id }

        // A few concurrent jobs make the next/previous cover ready quickly without flooding Coil
        // when a long queue is opened for the first time.
        for (batch in neighbours.chunked(3)) {
            coroutineScope {
                batch.forEach { neighbour ->
                    launch { preload(neighbour) }
                }
            }
        }
    }

    suspend fun show(song: Song) {
        isCoverTransitionInProgress = false
        // Do not replace a current cover backdrop with a synthetic fallback while the selected
        // cover is still loading; the completed palette takes over as soon as IO finishes.
        knownColor(song)?.let { color = it }
        preload(song)
        color = cache[song.id] ?: color
    }

    /** Called every pager frame, producing a direct colour blend with no animation lag. */
    fun blend(base: Song, adjacent: Song?, fraction: Float) {
        val amount = fraction.coerceIn(0f, 1f)
        val from = getOrFallback(base)
        val to = adjacent?.let { getOrFallback(it) } ?: from
        color = lerp(from, to, amount)
        isCoverTransitionInProgress = amount > .0001f
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
        val initialColor = loader.initialEstimate(song.artworkUri, "${song.title}_${song.artist}")
        XvoxNowPlayingPaletteState(loader, initialColor)
    }

    LaunchedEffect(song.id, song.artworkUri) {
        state.show(song)
    }

    LaunchedEffect(queue, currentIndex) {
        state.preloadNeighborhood(queue, currentIndex)
    }

    return state
}
