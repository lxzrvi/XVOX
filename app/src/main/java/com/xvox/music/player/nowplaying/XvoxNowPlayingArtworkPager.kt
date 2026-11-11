// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/player/nowplaying/XvoxNowPlayingArtworkPager.kt
package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.artwork.XvoxArtworkPreloader
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxFallbackBrand
import com.xvox.music.features.home.XvoxNowPlayingArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.ArrayDeque
import java.util.IdentityHashMap
import kotlin.math.abs

/**
 * Stable composition keys for queue occurrences. Index-based pager keys make a reordered list
 * dispose and recreate the active cover exactly when it must remain visibly unchanged. Object
 * identity is preserved by PlaybackController across a local drag; a per-object FIFO also keeps
 * multiple queued appearances of the same Song distinct.
 */
private class ArtworkOccurrenceKeyLedger {
    private var serial = 0L
    private var previous: List<Pair<String, Song>> = emptyList()

    fun resolve(queue: List<Song>): List<String> {
        val reusable = IdentityHashMap<Song, ArrayDeque<String>>()
        previous.forEach { (key, song) ->
            val bucket = reusable[song] ?: ArrayDeque<String>().also { reusable[song] = it }
            bucket.addLast(key)
        }
        val resolved = queue.map { song ->
            reusable[song]?.pollFirst() ?: "artwork_occurrence_${serial++}"
        }
        previous = queue.indices.map { index -> resolved[index] to queue[index] }
        return resolved
    }
}

/**
 * Full-queue artwork pager for Now Playing.
 * - Portrait uses horizontal paging; landscape can opt into vertical paging so previous artwork
 *   arrives from the top and next artwork arrives from the bottom.
 * - Fast Next/Previous taps animate the cover without touching audio until the settled 300 ms
 *   occurrence commit, preserving duplicate-song identity and swipe stability.
 */
@Composable
fun XvoxNowPlayingArtworkPager(
    queue: List<Song>,
    /** Controller queue-occurrence tokens, preferred over object/FIFO reconstruction. */
    occurrenceIds: List<String> = emptyList(),
    currentIndex: Int,
    @Suppress("UNUSED_PARAMETER") navigationRequest: Int,
    /** Page currently being previewed by the previous/next controls. */
    previewIndex: Int = currentIndex,
    /** Temporarily pins the deck to the audible occurrence while a queue reorder settles. */
    forceCurrentIndex: Boolean = false,
    /**
     * Retained for source compatibility with callers that restore a single non-repeated song.
     * [previewIndex] is authoritative: an ID alone cannot identify one repeated occurrence.
     */
    previewSongId: Long? = queue.getOrNull(previewIndex)?.id,
    onPreviewIndexChange: (Int) -> Unit = {},
    onArtworkTap: () -> Unit,
    onSwipePalette: (Song, Song?, Float) -> Unit,
    /** Supplies the settled queue occurrence as well as its visible Song. */
    onSettledPage: (Int, Song) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    // Zero spacing keeps the incoming cover opaque through the handoff instead of revealing a
    // transient backdrop strip between pages.
    pageSpacing: Dp = 0.dp,
    /** Insets the cover inside an edge-clipped page without exposing neighbouring artwork. */
    artworkHorizontalInset: Dp = 0.dp,
    /** 0dp lets an edge-to-edge landscape deck touch all screen boundaries. */
    artworkCornerRadius: Dp = 20.dp,
    /** Vertical mode makes previous/next covers travel top/bottom rather than side-to-side. */
    verticalPaging: Boolean = false,
    /** Applies the same portrait-like inset/depth treatment in either pager orientation. */
    applyDepth: Boolean = true,
    repeatMode: RepeatMode = RepeatMode.OFF
) {
    if (queue.isEmpty()) return
    // ONE preference collector for the whole pager (each page used to open its own DataStore flow).
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    androidx.compose.runtime.LaunchedEffect(appContext) {
        com.xvox.music.data.preferences.UserPreferencesRepository(appContext).coverTransition
            .collect { XvoxCoverTransition.current = it }
    }
    val initialIdx = currentIndex.coerceIn(0, queue.lastIndex)
    val repeatAll = repeatMode == RepeatMode.ALL && queue.size > 1
    val occurrenceKeyLedger = remember { ArtworkOccurrenceKeyLedger() }
    val hasOccurrenceIds = occurrenceIds.size == queue.size && occurrenceIds.toSet().size == queue.size
    // Media3 occurrence IDs survive a drag of identical/same-reference songs. The legacy ledger
    // remains only for callers without the active controller's token list.
    val occurrenceKeys = remember(queue, occurrenceIds) {
        if (hasOccurrenceIds) occurrenceIds else occurrenceKeyLedger.resolve(queue)
    }
    // Repeat All adds one virtual copy at each edge: [last, real queue…, first]. It lets the real
    // Compose pager settle beyond an edge before snapping to the canonical page, rather than
    // faking a wrap with a discontinuous artwork jump.
    val pagerPageCount = if (repeatAll) queue.size + 2 else queue.size
    fun pageForIndex(index: Int): Int = if (repeatAll) index + 1 else index
    fun indexForPage(page: Int): Int = when {
        !repeatAll -> page.coerceIn(0, queue.lastIndex)
        page <= 0 -> queue.lastIndex
        page >= pagerPageCount - 1 -> 0
        else -> (page - 1).coerceIn(0, queue.lastIndex)
    }
    fun occurrenceKeyForPage(page: Int): String {
        val index = indexForPage(page)
        val base = occurrenceKeys.getOrElse(index) { "artwork_missing_$index" }
        return when {
            repeatAll && page == 0 -> "artwork_wrap_before_$base"
            repeatAll && page == pagerPageCount - 1 -> "artwork_wrap_after_$base"
            else -> base
        }
    }
    // Page indices identify queue occurrences, except for the two explicit Repeat All edge pages.
    // Do not recreate the deck on a reorder: it briefly paints a neighbouring cover.
    val pager = rememberPagerState(initialPage = pageForIndex(initialIdx), pageCount = { pagerPageCount })
    val context = LocalContext.current
    val artworkPreloader = remember(context) { XvoxArtworkPreloader(context) }
    // Button and held-navigation previews move before audio commits. Warm around that visual
    // target, not just the audible index, so the incoming original is ready while its lower-res
    // cache bridge is still on screen.
    val prewarmIndex = previewIndex.takeIf { it in queue.indices } ?: initialIdx
    LaunchedEffect(queue, prewarmIndex) {
        artworkPreloader.warm(
            songs = queue,
            fromIndex = (prewarmIndex - 1).coerceAtLeast(0),
            count = 3,
            requestSize = XvoxNowPlayingArtworkSize
        )
    }

    val settled by rememberUpdatedState(onSettledPage)
    val previewChanged by rememberUpdatedState(onPreviewIndexChange)
    val latestCurrentIndex by rememberUpdatedState(currentIndex)
    val palette by rememberUpdatedState(onSwipePalette)
    val tap by rememberUpdatedState(onArtworkTap)

    val isUserDragging by pager.interactionSource.collectIsDraggedAsState()
    var userSwiped by remember { mutableStateOf(false) }
    // Prevent a stale 300 ms release commit when the user begins a fresh fast swipe or a queue
    // reorder changes the occurrence layout beneath an existing preview.
    var swipeEpoch by remember { mutableIntStateOf(0) }
    // A current index can change for two very different reasons: playback advanced to another
    // occurrence (animate like Next), or the same audible occurrence moved after a reorder
    // (snap to avoid inventing a false cover transition). Keep the prior token to tell them apart.
    fun occurrenceAtIndex(index: Int): String? = occurrenceIds.getOrNull(index)
        ?: queue.getOrNull(index)?.xvoxArtworkPaletteKey()
    var previousAudibleOccurrence by remember {
        mutableStateOf(occurrenceAtIndex(initialIdx))
    }
    var priorRepeatAll by remember { mutableStateOf(repeatAll) }
    LaunchedEffect(repeatAll) {
        if (priorRepeatAll != repeatAll) {
            // Repeat was toggled while the player was open. Preserve the audible real occurrence
            // and map it into/out of the virtual edge layout without an accidental page change.
            pager.scrollToPage(pageForIndex(initialIdx))
            priorRepeatAll = repeatAll
        }
    }

    LaunchedEffect(queue, occurrenceIds) {
        swipeEpoch++
        userSwiped = false
    }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            swipeEpoch++
            userSwiped = true
        }
    }

    // The visible deck follows the preview occurrence. A numerical real index is deliberately
    // used because a repeated Song.id cannot tell the second queue occurrence from the first.
    val targetPreviewIndex = if (forceCurrentIndex) initialIdx
    else previewIndex.takeIf { it in queue.indices } ?: initialIdx
    val targetPreviewPage = pageForIndex(targetPreviewIndex)
    LaunchedEffect(targetPreviewPage, targetPreviewIndex, queue, occurrenceIds, forceCurrentIndex, currentIndex, repeatAll) {
        userSwiped = false
        val targetPage = targetPreviewPage
        if (targetPage in 0 until pagerPageCount && targetPage != pager.currentPage) {
            val targetOccurrence = occurrenceAtIndex(targetPreviewIndex)
            val isSameAudibleOccurrenceRemap =
                targetPreviewIndex == currentIndex && targetOccurrence != null &&
                    targetOccurrence == previousAudibleOccurrence && !isUserDragging
            val isVirtualWrapPage = repeatAll && pager.currentPage in setOf(0, pagerPageCount - 1)
            if (forceCurrentIndex || isSameAudibleOccurrenceRemap || isVirtualWrapPage) {
                // A queue drag/shuffle remaps the same occurrence. A Repeat All edge has already
                // shown its wrap animation, so canonicalize it without reversing the gesture.
                pager.scrollToPage(targetPage)
            } else {
                // Button navigation, a swipe preview, and natural completion share one cover
                // motion between canonical queue occurrences.
                pager.animateScrollToPage(
                    page = targetPage,
                    animationSpec = tween(
                        durationMillis = com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion.Duration,
                        easing = com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion.easing
                    )
                )
            }
        }
        // Preview state can lag one composition behind a playback index. Advance the remembered
        // audible token only once this pager target has caught up, otherwise natural completion
        // would be mistaken for a same-occurrence remap on the next frame.
        if (targetPreviewIndex == currentIndex) {
            previousAudibleOccurrence = occurrenceAtIndex(currentIndex)
        }
    }

    // Keep one outgoing/incoming pair for an entire drag/Next/Previous travel. Pager.currentPage
    // switches at the midpoint, so deriving the pair directly from it used to reverse base and
    // adjacent covers halfway through the same gesture—the source of the Cover-background flash.
    LaunchedEffect(pager, queue, occurrenceIds, repeatAll) {
        var stableBasePage = pager.settledPage.coerceIn(0, pagerPageCount - 1)
        snapshotFlow {
            Triple(
                pager.currentPage + pager.currentPageOffsetFraction,
                pager.isScrollInProgress,
                pager.settledPage
            )
        }.collect { (rawPagePosition, moving, settledPage) ->
            if (!moving) {
                stableBasePage = settledPage.coerceIn(0, pagerPageCount - 1)
                queue.getOrNull(indexForPage(stableBasePage))?.let { settledSong ->
                    palette(settledSong, null, 0f)
                }
            } else {
                val basePage = stableBasePage.coerceIn(0, pagerPageCount - 1)
                val deltaFromBase = rawPagePosition - basePage
                val direction = when {
                    deltaFromBase > .0001f -> 1
                    deltaFromBase < -.0001f -> -1
                    else -> 0
                }
                val baseIndex = indexForPage(basePage)
                val adjacentPage = (basePage + direction).takeIf { it in 0 until pagerPageCount }
                val adjacentIndex = adjacentPage?.let(::indexForPage)
                val amount = abs(deltaFromBase).coerceIn(0f, 1f)
                queue.getOrNull(baseIndex)?.let { baseSong ->
                    palette(baseSong, adjacentIndex?.let(queue::get), amount)
                }
            }
        }
    }

    // Playback changes only after a manual swipe has genuinely settled for 300 ms. Capture the
    // queue occurrence before waiting so two equal Song IDs never collapse to the first copy.
    LaunchedEffect(pager, queue, occurrenceIds, repeatAll) {
        snapshotFlow {
            Triple(pager.settledPage, pager.isScrollInProgress, isUserDragging)
        }.distinctUntilChanged().collect { (settledPage, inProgress, dragging) ->
            val settledIndex = indexForPage(settledPage)
            val targetSong = queue.getOrNull(settledIndex)
            if (!inProgress && !dragging && userSwiped && targetSong != null && settledIndex != latestCurrentIndex) {
                userSwiped = false
                previewChanged(settledIndex)
                val settledEpoch = swipeEpoch
                val settledOccurrence = settledIndex
                delay(300)
                // A Repeat All edge page has been snapped back to its canonical page by now. The
                // real queue index, rather than the virtual page number, remains the identity.
                if (
                    settledEpoch == swipeEpoch &&
                    !pager.isScrollInProgress &&
                    !isUserDragging &&
                    indexForPage(pager.settledPage) == settledOccurrence &&
                    latestCurrentIndex != settledOccurrence
                ) {
                    settled(settledOccurrence, targetSong)
                }
            } else if (!inProgress && !dragging) {
                userSwiped = false
            }
        }
    }

    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pager,
        snapAnimationSpec = tween(120, easing = FastOutSlowInEasing),
        snapPositionalThreshold = 0.35f
    )
    if (verticalPaging) {
        VerticalPager(
            state = pager,
            beyondViewportPageCount = 1,
            snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Center,
            flingBehavior = pagerFlingBehavior,
            contentPadding = contentPadding,
            pageSpacing = pageSpacing,
            modifier = modifier.fillMaxSize(),
            key = { page -> occurrenceKeyForPage(page) }
        ) { page ->
            XvoxNowPlayingArtworkPage(
                page = page,
                songIndex = indexForPage(page),
                queue = queue,
                pager = pager,
                artworkHorizontalInset = artworkHorizontalInset,
                artworkCornerRadius = artworkCornerRadius,
                applyDepth = applyDepth,
                verticalPaging = verticalPaging,
                onArtworkTap = { if (!pager.isScrollInProgress) tap() }
            )
        }
    } else {
        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 1,
            snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Center,
            flingBehavior = pagerFlingBehavior,
            contentPadding = contentPadding,
            pageSpacing = pageSpacing,
            modifier = modifier.fillMaxSize(),
            key = { page -> occurrenceKeyForPage(page) }
        ) { page ->
            XvoxNowPlayingArtworkPage(
                page = page,
                songIndex = indexForPage(page),
                queue = queue,
                pager = pager,
                artworkHorizontalInset = artworkHorizontalInset,
                artworkCornerRadius = artworkCornerRadius,
                applyDepth = applyDepth,
                verticalPaging = verticalPaging,
                onArtworkTap = { if (!pager.isScrollInProgress) tap() }
            )
        }
    }
}

@Composable
private fun XvoxNowPlayingArtworkPage(
    /** Physical pager page; may be one of Repeat All's virtual edge pages. */
    page: Int,
    /** Real queue index displayed by this page. */
    songIndex: Int,
    queue: List<Song>,
    pager: PagerState,
    artworkHorizontalInset: Dp,
    artworkCornerRadius: Dp,
    applyDepth: Boolean,
    verticalPaging: Boolean,
    onArtworkTap: () -> Unit
) {
    val song = queue.getOrNull(songIndex) ?: return
    val shape = remember(artworkCornerRadius) { RoundedCornerShape(artworkCornerRadius) }
    val transition = XvoxCoverTransition.current
    Box(
        Modifier
            // Draw order follows the swipe, read in the PLACEMENT phase (not composition) so a drag
            // never recomposes the page: crossfade/zoom raise the arriving page, page/cascade
            // choose which of the two sits on top.
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    val o = ((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
                    val leaving = page == pager.settledPage
                    val z = when (transition) {
                        // arriving cover sits ON TOP, solid
                        "crossfade", "cascade", "zoom", "fan" -> if (leaving) 0f else 1f
                        // leaving cover stays on top while it turns/folds away
                        "rotate", "page" -> if (leaving) 2f else 0f
                        else -> 0f
                    }
                    placeable.place(0, 0, zIndex = z)
                }
            }
            .fillMaxSize()
            .graphicsLayer {
                // One bitmap-free cover: fade by modulating its draw alpha, no offscreen buffer.
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction)
                    .coerceIn(-1f, 1f)
                val a = abs(offset)
                var scale = if (applyDepth) 1f - .08f * a else 1f
                val w = if (verticalPaging) size.height else size.width
                // offset > 0: page sits before centre; offset < 0: page sits after centre.
                // The pager already slides pages by -offset*extent; `cancel(f)` removes that
                // fraction of the slide (f = 1 pins the page in place).
                fun cancel(f: Float) {
                    if (verticalPaging) translationY = offset * w * f else translationX = offset * w * f
                }
                var accX = 1f
                var accY = 1f
                // Smootherstep easing for opacity/scale (motion that eases in AND out, so the swipe
                // feels weighted); rotations/translations stay linear so 3D geometry stays exact.
                val e = a * a * (3f - 2f * a)
                val sg = if (offset >= 0f) 1f else -1f
                val leaving = page == pager.settledPage
                when (transition) {
                    "crossfade" -> {
                        cancel(1f)
                        // e(p) + e(1-p) = 1, so the two covers always sum to exactly one opaque cover.
                        alpha = 1f - e
                        scale = 1f - .05f * e
                    }
                    "tumble" -> {
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, 1.25f)
                        rotationZ = -sg * e * 24f
                        alpha = 1f - .4f * e
                        scale = 1f - .1f * e
                    }
                    "page" -> {
                        cameraDistance = 14f * density
                        if (leaving) {
                            // Leaving page folds about the hinge edge, above the static one.
                            transformOrigin = if (verticalPaging) androidx.compose.ui.graphics.TransformOrigin(.5f, if (offset > 0f) 0f else 1f)
                            else androidx.compose.ui.graphics.TransformOrigin(if (offset > 0f) 0f else 1f, .5f)
                            if (verticalPaging) rotationX = offset * 74f else rotationY = -offset * 74f
                            cancel(-.4f)
                        } else {
                            cancel(1f)
                            scale = 1f - .07f * (1f - e)
                        }
                    }
                    "cascade" -> {
                        // Solid: the leaving cover sinks back, the arriving one slides over it.
                        if (leaving) {
                            cancel(.3f)
                            scale = 1f - .16f * e
                        }
                    }
                    "rotate" -> {
                        // Leaving cover turns away fully opaque; the next cover waits solid beneath.
                        if (leaving) {
                            cancel(.6f)
                            rotationZ = -sg * e * 75f
                            scale = 1f - .12f * e
                        } else {
                            cancel(1f)
                        }
                    }
                    "cube" -> {
                        cameraDistance = 16f * density
                        transformOrigin = if (verticalPaging) androidx.compose.ui.graphics.TransformOrigin(.5f, if (offset > 0f) 1f else 0f)
                        else androidx.compose.ui.graphics.TransformOrigin(if (offset > 0f) 1f else 0f, .5f)
                        if (verticalPaging) rotationX = offset * 90f else rotationY = -offset * 90f
                        alpha = 1f - .2f * e
                    }
                    "zoom" -> {
                        // Solid zoom: the current cover shrinks back, the next grows in over it.
                        if (leaving) {
                            cancel(.5f)
                            scale = 1f - .15f * e
                        } else {
                            cancel(.5f)
                            scale = 1f + .18f * e
                        }
                    }
                    "flip" -> {
                        cameraDistance = 18f * density
                        cancel(1f)
                        if (verticalPaging) rotationX = offset * 180f else rotationY = -offset * 180f
                        alpha = if (a > .5f) 0f else 1f
                    }
                    "fan" -> {
                        // The current cover stays put; the next one fans in SOLID on top of it.
                        if (leaving) {
                            cancel(1f)
                            scale = 1f - .04f * e
                        } else {
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, 1.7f)
                            cancel(.35f)
                            rotationZ = -sg * e * 22f
                        }
                    }
                    "accordion" -> {
                        // Each cover squeezes toward the seam it shares with its neighbour, so the
                        // two travel together with no gap between them.
                        transformOrigin = if (verticalPaging) androidx.compose.ui.graphics.TransformOrigin(.5f, if (offset > 0f) 1f else 0f)
                        else androidx.compose.ui.graphics.TransformOrigin(if (offset > 0f) 1f else 0f, .5f)
                        if (verticalPaging) accY = 1f - .55f * e else accX = 1f - .55f * e
                    }
                    "drift" -> {
                        cancel(.55f)
                        alpha = 1f - .9f * e
                        scale = 1f - .08f * e
                    }
                }
                scaleX = scale * accX
                scaleY = scale * accY
                // Apply one offscreen-shaped clip to the scaled pager page. This prevents the
                // cover radius from separating from its moving border while pages overlap.
                this.shape = shape
                clip = true
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onArtworkTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = artworkHorizontalInset)
                .then(if (artworkHorizontalInset > 0.dp) Modifier.clip(shape) else Modifier)
        ) {
            XvoxSongArtwork(
                artwork = song.artworkUri,
                fallbackSeed = song.id.hashCode(),
                fallbackBrand = XvoxFallbackBrand.NOW_PLAYING,
                requestSize = XvoxNowPlayingArtworkSize,
                keepPreviousOnLoading = true,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


/** Cover-transition style shared by every pager page; written by the pager's single collector. */
internal object XvoxCoverTransition {
    var current: String by androidx.compose.runtime.mutableStateOf("slide")
}
