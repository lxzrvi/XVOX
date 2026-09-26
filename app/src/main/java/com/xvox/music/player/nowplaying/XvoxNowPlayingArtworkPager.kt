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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxNowPlayingArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * Full-queue smooth HorizontalPager for Now Playing.
 * - Instant cover swiping with zero delay.
 * - Fast Next/Previous button taps immediately animate the cover without getting stuck.
 * - Currently playing audio remains playing while swiping fast; only when the user finishes a
 *   manual touch swipe on a stable song does the parent commit that song identity to playback.
 */
@Composable
fun XvoxNowPlayingArtworkPager(
    queue: List<Song>,
    currentIndex: Int,
    @Suppress("UNUSED_PARAMETER") navigationRequest: Int,
    /** Page currently being previewed by the previous/next controls. */
    previewIndex: Int = currentIndex,
    /** Stable visual target used to survive a queue reorder while this pager is mounted. */
    previewSongId: Long? = queue.getOrNull(previewIndex)?.id,
    onPreviewIndexChange: (Int) -> Unit = {},
    onArtworkTap: () -> Unit,
    onSwipePalette: (Song, Song?, Float) -> Unit,
    /** Supplies the stable visible song, never only an index that a shuffled queue can invalidate. */
    onSettledPage: (Song) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSpacing: Dp = 12.dp,
    /** Insets the cover inside an edge-clipped page without exposing neighbouring artwork. */
    artworkHorizontalInset: Dp = 0.dp,
    /** Landscape uses a vertical deck so no left/right neighbouring covers can appear. */
    verticalPaging: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF
) {
    if (queue.isEmpty()) return
    val initialIdx = currentIndex.coerceIn(0, queue.lastIndex)
    val queueIdentity = remember(queue) { queue.map { it.xvoxArtworkPaletteKey() } }
    val stableInitialPage = previewSongId
        ?.let { targetId -> queue.indexOfFirst { it.id == targetId } }
        ?.takeIf { it in queue.indices }
        ?: initialIdx
    // A reordered queue creates a fresh pager at the stable target song rather than retaining an
    // old numeric page that now belongs to a different cover.
    val pager = key(queueIdentity) {
        rememberPagerState(initialPage = stableInitialPage, pageCount = { queue.size })
    }

    val settled by rememberUpdatedState(onSettledPage)
    val previewChanged by rememberUpdatedState(onPreviewIndexChange)
    val latestCurrentIndex by rememberUpdatedState(currentIndex)
    val palette by rememberUpdatedState(onSwipePalette)
    val tap by rememberUpdatedState(onArtworkTap)

    val isUserDragging by pager.interactionSource.collectIsDraggedAsState()
    var userSwiped by remember(queueIdentity) { mutableStateOf(false) }
    // Prevent a stale 300 ms release commit when the user begins a fresh fast swipe.
    var swipeEpoch by remember(queueIdentity) { mutableIntStateOf(0) }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            swipeEpoch++
            userSwiped = true
        }
    }

    // The visible deck follows the preview song rather than trusting a numerical page after a
    // queue reorder. This is especially important when Shuffle moves the current item to index 0.
    val targetPreviewSongId = previewSongId ?: queue.getOrNull(previewIndex)?.id
    LaunchedEffect(targetPreviewSongId, queue) {
        userSwiped = false
        val targetPage = targetPreviewSongId?.let { id -> queue.indexOfFirst { it.id == id } } ?: -1
        if (targetPage in queue.indices && targetPage != pager.currentPage) {
            // Never scrollToPage here: a held or rapidly tapped control can move more than one
            // index, but the visible cover and the live palette must still travel continuously.
            pager.animateScrollToPage(
                page = targetPage,
                animationSpec = tween(
                    durationMillis = com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion.Duration,
                    easing = com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion.easing
                )
            )
        }
    }

    // Real-time backdrop color crossfading matching finger/pager position with zero latency.
    LaunchedEffect(pager, queue) {
        snapshotFlow {
            Pair(pager.currentPage, pager.currentPageOffsetFraction)
        }.collect { (page, offset) ->
            val curr = queue.getOrNull(page)
            val adj = if (offset > 0.0001f) queue.getOrNull(page + 1)
            else if (offset < -0.0001f) queue.getOrNull(page - 1)
            else null

            if (curr != null) {
                palette(curr, adj, abs(offset))
            }
        }
    }

    // Playback changes only after a manual swipe has genuinely settled for 300 ms. Capture the
    // target Song itself before waiting: an index can mean a different cover after Shuffle.
    LaunchedEffect(pager, queue) {
        snapshotFlow {
            Triple(pager.settledPage, pager.isScrollInProgress, isUserDragging)
        }.distinctUntilChanged().collect { (settledIndex, inProgress, dragging) ->
            val targetSong = queue.getOrNull(settledIndex)
            val currentSongId = queue.getOrNull(latestCurrentIndex)?.id
            if (!inProgress && !dragging && userSwiped && targetSong != null && targetSong.id != currentSongId) {
                userSwiped = false
                previewChanged(settledIndex)
                val settledEpoch = swipeEpoch
                val settledSongId = targetSong.id
                delay(300)
                if (
                    settledEpoch == swipeEpoch &&
                    !pager.isScrollInProgress &&
                    !isUserDragging &&
                    queue.getOrNull(pager.settledPage)?.id == settledSongId &&
                    queue.getOrNull(latestCurrentIndex)?.id != settledSongId
                ) {
                    settled(targetSong)
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
            beyondViewportPageCount = 3,
            snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Center,
            flingBehavior = pagerFlingBehavior,
            contentPadding = contentPadding,
            pageSpacing = pageSpacing,
            modifier = modifier.fillMaxSize(),
            key = { page -> queue.getOrNull(page)?.xvoxArtworkPaletteKey() ?: page }
        ) { page ->
            XvoxNowPlayingArtworkPage(
                page = page,
                queue = queue,
                pager = pager,
                artworkHorizontalInset = artworkHorizontalInset,
                onArtworkTap = { if (!pager.isScrollInProgress) tap() }
            )
        }
    } else {
        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 3,
            snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Center,
            flingBehavior = pagerFlingBehavior,
            contentPadding = contentPadding,
            pageSpacing = pageSpacing,
            modifier = modifier.fillMaxSize(),
            key = { page -> queue.getOrNull(page)?.xvoxArtworkPaletteKey() ?: page }
        ) { page ->
            XvoxNowPlayingArtworkPage(
                page = page,
                queue = queue,
                pager = pager,
                artworkHorizontalInset = artworkHorizontalInset,
                onArtworkTap = { if (!pager.isScrollInProgress) tap() }
            )
        }
    }
}

@Composable
private fun XvoxNowPlayingArtworkPage(
    page: Int,
    queue: List<Song>,
    pager: PagerState,
    artworkHorizontalInset: Dp,
    onArtworkTap: () -> Unit
) {
    val song = queue.getOrNull(page) ?: return
    val pageOffset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
    val amount = abs(pageOffset)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Now Playing artwork has one intentional treatment: Depth. It remains
                // responsive to native pager drag while the background owns its own variety.
                scaleX = 1f - .20f * amount
                scaleY = scaleX
                alpha = 1f - .36f * amount
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onArtworkTap
            ),
        contentAlignment = Alignment.Center
    ) {
        // The page retains its full edge-to-edge clipping boundary. In landscape an inner
        // inset reduces cover width, while adjacent covers remain outside that boundary.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = artworkHorizontalInset)
                .clip(RoundedCornerShape(20.dp))
        ) {
            XvoxSongArtwork(
                artwork = song.artworkUri,
                requestSize = XvoxNowPlayingArtworkSize,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
