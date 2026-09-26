package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
 * - Currently playing audio remains playing while swiping fast; only when the
 *   user finishes a manual touch swipe on a target song does it begin playback.
 */
@Composable
fun XvoxNowPlayingArtworkPager(
    queue: List<Song>,
    currentIndex: Int,
    navigationRequest: Int,
    /** Page currently being previewed by the previous/next controls. */
    previewIndex: Int = currentIndex,
    onPreviewIndexChange: (Int) -> Unit = {},
    onArtworkTap: () -> Unit,
    onSwipePalette: (Song, Song?, Float) -> Unit,
    onSettledPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSpacing: androidx.compose.ui.unit.Dp = 12.dp,
    repeatMode: RepeatMode = RepeatMode.OFF,
    /** Persisted cover-change treatment selected from Now Playing → Cover transition. */
    coverTransition: String = XvoxCoverTransitionStyles.SLIDE
) {
    if (queue.isEmpty()) return
    val initialIdx = currentIndex.coerceIn(0, queue.lastIndex)
    val pager = rememberPagerState(initialPage = initialIdx, pageCount = { queue.size })

    val settled by rememberUpdatedState(onSettledPage)
    val previewChanged by rememberUpdatedState(onPreviewIndexChange)
    val latestCurrentIndex by rememberUpdatedState(currentIndex)
    val palette by rememberUpdatedState(onSwipePalette)
    val tap by rememberUpdatedState(onArtworkTap)

    val isUserDragging by pager.interactionSource.collectIsDraggedAsState()
    var userSwiped by remember { mutableStateOf(false) }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            userSwiped = true
        }
    }

    // The visible deck follows the preview index. Player state may remain on the old song while
    // the user presses/holds next or previous, which lets the cover animate like a real swipe.
    LaunchedEffect(previewIndex, queue.size) {
        userSwiped = false
        if (previewIndex in queue.indices && previewIndex != pager.currentPage) {
            val dist = abs(previewIndex - pager.currentPage)
            if (dist > 1) {
                pager.scrollToPage(previewIndex)
            } else {
                pager.animateScrollToPage(
                    page = previewIndex,
                    animationSpec = tween(
                        durationMillis = com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion.Duration,
                        easing = com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion.easing
                    )
                )
            }
        }
    }

    // Real-time backdrop color crossfading matching finger/pager position with zero latency
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

    // Playback change triggered ONLY when the user manually swiped and pager settles
    LaunchedEffect(pager, queue) {
        snapshotFlow {
            Triple(pager.settledPage, pager.isScrollInProgress, isUserDragging)
        }.distinctUntilChanged().collect { (settledIndex, inProgress, dragging) ->
            if (!inProgress && !dragging && userSwiped && settledIndex in queue.indices && settledIndex != latestCurrentIndex) {
                userSwiped = false
                previewChanged(settledIndex)
                delay(60)
                if (
                    !pager.isScrollInProgress &&
                    !isUserDragging &&
                    pager.settledPage == settledIndex &&
                    settledIndex in queue.indices &&
                    settledIndex != latestCurrentIndex
                ) {
                    settled(settledIndex)
                }
            } else if (!inProgress && !dragging) {
                userSwiped = false
            }
        }
    }

    val selectedCoverTransition = XvoxCoverTransitionStyles.normalize(coverTransition)

    HorizontalPager(
        state = pager,
        beyondViewportPageCount = 3,
        snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Center,
        flingBehavior = PagerDefaults.flingBehavior(
            state = pager,
            snapAnimationSpec = tween(120, easing = FastOutSlowInEasing),
            snapPositionalThreshold = 0.35f
        ),
        contentPadding = contentPadding,
        pageSpacing = pageSpacing,
        modifier = modifier.fillMaxSize(),
        key = { page -> queue.getOrNull(page)?.id ?: page }
    ) { page ->
        val song = queue.getOrNull(page) ?: return@HorizontalPager
        val pageOffset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
        val amount = abs(pageOffset)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Each option remains compatible with the pager's native drag and adds a
                    // distinct, lightweight treatment rather than swapping covers abruptly.
                    when (selectedCoverTransition) {
                        "push" -> { scaleX = 1f - .08f * amount; scaleY = scaleX }
                        "parallax" -> translationX = -pageOffset * 34f
                        "fade" -> alpha = 1f - .55f * amount
                        "scale" -> { scaleX = 1f - .14f * amount; scaleY = scaleX }
                        "zoom" -> { scaleX = 1f + .14f * amount; scaleY = scaleX; alpha = 1f - .22f * amount }
                        "depth" -> { scaleX = 1f - .20f * amount; scaleY = scaleX; alpha = 1f - .36f * amount }
                        "flip_x" -> { rotationY = pageOffset * 68f; alpha = 1f - .24f * amount }
                        "flip_y" -> { rotationX = -pageOffset * 62f; alpha = 1f - .24f * amount }
                        "rotate_cw" -> rotationZ = pageOffset * 18f
                        "rotate_ccw" -> rotationZ = -pageOffset * 18f
                        "tilt" -> rotationZ = pageOffset * 10f
                        "rise" -> translationY = -pageOffset * 42f
                        "drop" -> translationY = pageOffset * 42f
                        "reveal" -> { scaleX = 1f - .22f * amount; alpha = 1f - .18f * amount }
                        "stack" -> { scaleX = 1f - .12f * amount; scaleY = scaleX; translationY = amount * 18f }
                        "pop" -> { scaleX = 1f + .08f * (1f - amount); scaleY = scaleX; alpha = 1f - .28f * amount }
                        "drift" -> translationX = pageOffset * 18f
                        "swing" -> rotationZ = pageOffset * 24f
                        "glide" -> { translationX = -pageOffset * 10f; alpha = 1f - .16f * amount }
                    }
                }
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!pager.isScrollInProgress) tap()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            XvoxSongArtwork(
                artwork = song.artworkUri,
                requestSize = XvoxNowPlayingArtworkSize,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
