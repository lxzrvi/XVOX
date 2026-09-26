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
import com.xvox.music.core.design.theme.XvoxTheme
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
    // Mirror the loader's full song/artwork identity here too. A media source can update an
    // artwork/content URI without changing the numeric row ID.
    private val cache = mutableStateMapOf<String, Color>()
    private val loadingSongKeys = mutableSetOf<String>()

    /** Current visual identity allowed to finish an asynchronous palette request. */
    private var pinnedSongKey: String? = null
    /** Invalidates a late palette result after a swipe, shuffle reflow, or another song selection. */
    private var visualGeneration = 0L

    var color by mutableStateOf(initial)
        private set

    /** True only while the pager is between two covers, so the backdrop can follow the finger. */
    var isCoverTransitionInProgress by mutableStateOf(false)
        private set

    private fun knownColor(song: Song): Color? {
        val key = song.xvoxArtworkPaletteKey()
        return cache[key] ?: loader.cachedColor(song.artworkUri, key)
    }

    fun getOrFallback(song: Song?): Color {
        if (song == null) return color
        // An unprepared cover keeps the live player backdrop until its IO palette is available.
        // This avoids flashing a synthetic hash colour when browsing or opening a selected song.
        return knownColor(song) ?: color
    }

    suspend fun preload(song: Song?) {
        song ?: return
        val key = song.xvoxArtworkPaletteKey()
        if (cache.containsKey(key) || !loadingSongKeys.add(key)) return
        try {
            val resolved = loader.load(song.artworkUri, key)
            cache[key] = resolved
            // A neighbor that becomes the settled visible cover may finish after its pager frame.
            // Update only when that identity is still pinned and not between two covers.
            if (pinnedSongKey == key && !isCoverTransitionInProgress) {
                color = resolved
            }
        } finally {
            loadingSongKeys.remove(key)
        }
    }

    /** Prioritise the immediate neighbours, then warm several covers in both directions. */
    suspend fun preloadNeighborhood(queue: List<Song>, currentIndex: Int) {
        val offsets = listOf(0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6, 7, -7, 8, -8)
        val neighbours = offsets.mapNotNull { offset -> queue.getOrNull(currentIndex + offset) }
            .distinctBy { it.xvoxArtworkPaletteKey() }

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

    /**
     * Pins the backdrop to a stable song before a queue order change. This is used by Shuffle so
     * transient pager indices cannot briefly blend an unrelated cover into the live background.
     */
    fun pin(song: Song) {
        pinnedSongKey = song.xvoxArtworkPaletteKey()
        visualGeneration++
        isCoverTransitionInProgress = false
        knownColor(song)?.let { color = it }
    }

    suspend fun show(song: Song) {
        pin(song)
        val key = song.xvoxArtworkPaletteKey()
        val requestGeneration = visualGeneration
        // A visible song waits for its own palette result rather than returning early behind a
        // neighbour-prefetch job. The loader's shared cache keeps duplicate work inexpensive, and
        // this guarantees the current backdrop is eventually refreshed.
        val resolved = cache[key] ?: loader.load(song.artworkUri, key)
        cache[key] = resolved
        // A cancelled/older song request is not allowed to write its completed color over the
        // song now on screen. This closes the late-IO race that could make some covers look wrong.
        if (
            pinnedSongKey == key &&
            requestGeneration == visualGeneration &&
            !isCoverTransitionInProgress
        ) {
            color = resolved
        }
    }

    /** Called every pager frame, producing a direct colour blend with no animation lag. */
    fun blend(base: Song, adjacent: Song?, fraction: Float) {
        val baseKey = base.xvoxArtworkPaletteKey()
        // A pager frame for the same base cover must not invalidate that cover's pending IO load;
        // only crossing onto another base artwork creates a new visual identity.
        if (pinnedSongKey != baseKey) {
            pinnedSongKey = baseKey
            visualGeneration++
        }
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
    val colors = XvoxTheme.colors
    val state = remember {
        // A first-frame miss keeps the themed canvas stable until the real, per-song palette is
        // ready. It intentionally does not use a hash-derived placeholder colour.
        val initialColor = loader.cachedColor(song.artworkUri, song.xvoxArtworkPaletteKey()) ?: colors.background
        XvoxNowPlayingPaletteState(loader, initialColor)
    }

    LaunchedEffect(song.xvoxArtworkPaletteKey()) {
        state.show(song)
    }

    LaunchedEffect(queue, currentIndex) {
        state.preloadNeighborhood(queue, currentIndex)
    }

    return state
}
