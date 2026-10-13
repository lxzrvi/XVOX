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
import com.xvox.music.core.design.theme.XvoxPalette
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.xvoxWavePalette
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Keeps cover colours ready before a pager reaches them. Pixel work lives in the loader's IO
 * context; the UI state only receives completed colours, so rapid cover browsing stays smooth.
 */
@Stable
class XvoxNowPlayingPaletteState internal constructor(
    private val loader: XvoxArtworkPaletteLoader,
    initial: Color,
    initialWaveColors: XvoxPalette
) {
    // Mirror the loader's full song/artwork identity here too. A media source can update an
    // artwork/content URI without changing the numeric row ID.
    private val cache = mutableStateMapOf<String, Color>()
    private val loadingSongKeys = mutableSetOf<String>()

    /** Current visual identity allowed to finish an asynchronous palette request. */
    private var pinnedSongKey: String? = null
    /** Invalidates a late palette result after a swipe, shuffle reflow, or another song selection. */
    private var visualGeneration = 0L
    /** Current theme/accent recipe for deterministic per-song wave palettes. */
    private var waveColors by mutableStateOf(initialWaveColors)
    private var pinnedSongUsesThemeFallback = false
    private var pinnedFallbackSeed: Int? = null

    var color by mutableStateOf(initial)
        private set

    /** True only while the pager is between two covers, so the backdrop can follow the finger. */
    var isCoverTransitionInProgress by mutableStateOf(false)
        private set

    private fun usesThemeFallback(song: Song): Boolean {
        val key = song.xvoxArtworkPaletteKey()
        return song.artworkUri == null || loader.usedFallback(song.artworkUri, key)
    }

    private fun knownColor(song: Song): Color? {
        val key = song.xvoxArtworkPaletteKey()
        return if (usesThemeFallback(song)) waveFallback(song)
        else cache[key] ?: loader.cachedColor(song.artworkUri, key)
    }

    private fun waveFallback(song: Song): Color =
        xvoxWavePalette(song.id.hashCode(), waveColors).dominant

    private suspend fun resolvedColor(song: Song): Color {
        if (song.artworkUri == null) return waveFallback(song)
        val key = song.xvoxArtworkPaletteKey()
        val extracted = cache[key] ?: loader.load(song.artworkUri, key)
        // A bad URI uses exactly the same seeded, theme-aware palette as its visible wave art.
        return if (loader.usedFallback(song.artworkUri, key)) waveFallback(song) else extracted
    }

    fun getOrFallback(song: Song?): Color {
        if (song == null) return color
        // Default Now Playing intentionally takes its canvas from this exact song's patterned
        // fallback palette, so coverless songs no longer collapse into one red/accent backdrop.
        if (usesThemeFallback(song)) return waveFallback(song)
        // An unprepared cover keeps the live player backdrop until its IO palette is available.
        // This avoids flashing a synthetic hash colour when browsing or opening a selected song.
        return knownColor(song) ?: color
    }

    fun updateWaveColors(next: XvoxPalette) {
        waveColors = next
        pinnedFallbackSeed?.let { seed ->
            if (!isCoverTransitionInProgress) color = xvoxWavePalette(seed, next).dominant
        }
    }

    suspend fun preload(song: Song?) {
        song ?: return
        val key = song.xvoxArtworkPaletteKey()
        if (cache.containsKey(key) || !loadingSongKeys.add(key)) return
        try {
            val resolved = resolvedColor(song)
            cache[key] = resolved
            // A neighbor that becomes the settled visible cover may finish after its pager frame.
            // Update only when that identity is still pinned and not between two covers.
            if (pinnedSongKey == key && !isCoverTransitionInProgress) {
                pinnedSongUsesThemeFallback = usesThemeFallback(song)
                pinnedFallbackSeed = song.id.hashCode().takeIf { pinnedSongUsesThemeFallback }
                color = resolved
            }
        } finally {
            loadingSongKeys.remove(key)
        }
    }

    /** Prioritise only the immediate swipe neighbourhood so image decoding cannot tax paging. */
    suspend fun preloadNeighborhood(queue: List<Song>, currentIndex: Int) {
        val offsets = listOf(0, 1, -1, 2, -2)
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
        pinnedSongUsesThemeFallback = usesThemeFallback(song)
        pinnedFallbackSeed = song.id.hashCode().takeIf { pinnedSongUsesThemeFallback }
        visualGeneration++
        isCoverTransitionInProgress = false
        if (pinnedSongUsesThemeFallback) color = waveFallback(song)
        else knownColor(song)?.let { color = it }
    }

    suspend fun show(song: Song) {
        pin(song)
        val key = song.xvoxArtworkPaletteKey()
        val requestGeneration = visualGeneration
        // A visible song waits for its own palette result rather than returning early behind a
        // neighbour-prefetch job. The loader's shared cache keeps duplicate work inexpensive, and
        // this guarantees the current backdrop is eventually refreshed.
        val resolved = resolvedColor(song)
        cache[key] = resolved
        // A cancelled/older song request is not allowed to write its completed color over the
        // song now on screen. This closes the late-IO race that could make some covers look wrong.
        if (
            pinnedSongKey == key &&
            requestGeneration == visualGeneration &&
            !isCoverTransitionInProgress
        ) {
            pinnedSongUsesThemeFallback = usesThemeFallback(song)
            pinnedFallbackSeed = song.id.hashCode().takeIf { pinnedSongUsesThemeFallback }
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
        pinnedSongUsesThemeFallback = usesThemeFallback(base)
        pinnedFallbackSeed = base.id.hashCode().takeIf { pinnedSongUsesThemeFallback }
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
    val initialWaveFallback = xvoxWavePalette(song.id.hashCode(), colors).dominant
    val state = remember {
        // A first-frame miss keeps the canvas stable until real cover analysis is ready.
        // Coverless songs use their own exact wave dominant from the first visible frame.
        val initialColor = if (
            song.artworkUri == null || loader.usedFallback(song.artworkUri, song.xvoxArtworkPaletteKey())
        ) initialWaveFallback
        else loader.cachedColor(song.artworkUri, song.xvoxArtworkPaletteKey()) ?: colors.background
        XvoxNowPlayingPaletteState(loader, initialColor, colors)
    }

    LaunchedEffect(colors) {
        state.updateWaveColors(colors)
    }

    var hasShownInitialPalette by remember { mutableStateOf(false) }
    LaunchedEffect(song.xvoxArtworkPaletteKey()) {
        if (!hasShownInitialPalette) {
            // Establish the first visible player canvas. Subsequent track changes are driven by
            // the pager's per-frame blend so background and cover move together instead of the
            // palette jumping ahead when natural playback advances.
            hasShownInitialPalette = true
            state.show(song)
        } else {
            state.preload(song)
        }
    }

    LaunchedEffect(queue, currentIndex) {
        state.preloadNeighborhood(queue, currentIndex)
    }

    return state
}
