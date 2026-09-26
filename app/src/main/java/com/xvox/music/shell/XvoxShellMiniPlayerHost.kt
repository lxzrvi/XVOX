package com.xvox.music.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayer
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayerPlacement
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion

@Composable
fun BoxScope.XvoxShellMiniPlayerHost(
    visible: Boolean,
    currentSongId: Long?,
    queue: List<Song>,
    currentIndex: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    riseKey: Int,
    onTogglePlay: () -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onStopAndDismiss: () -> Unit,
    onOpenPlayer: () -> Unit,
    isLiked: Boolean = false,
    onLike: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onOpenMiniPlayerSettings: () -> Unit = {},
    navigationBarHeight: Dp = 64.dp
) {
    var quickActionsVisible by remember(currentSongId) { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (!visible) quickActionsVisible = false
    }
    if (quickActionsVisible) {
        // This sits behind the pill but above the underlying screen, so any outside tap cleanly
        // reverses the pill rather than triggering Home/Now Playing beneath it.
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) { detectTapGestures { quickActionsVisible = false } }
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = XvoxPlayerTransitionMotion.Duration,
                easing = XvoxPlayerTransitionMotion.easing
            )
        ) { it } + fadeIn(
            animationSpec = tween(
                durationMillis = XvoxPlayerTransitionMotion.Duration,
                easing = XvoxPlayerTransitionMotion.easing
            )
        ),
        exit = slideOutVertically(
            animationSpec = tween(
                durationMillis = XvoxPlayerTransitionMotion.Duration,
                easing = XvoxPlayerTransitionMotion.easing
            )
        ) { it } + fadeOut(
            animationSpec = tween(
                durationMillis = XvoxPlayerTransitionMotion.Duration,
                easing = XvoxPlayerTransitionMotion.easing
            )
        ),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        if (currentSongId != null) {
            val density = LocalDensity.current
            val imeBottomPx = WindowInsets.ime.getBottom(density)
            val navBottomPx = WindowInsets.navigationBars.getBottom(density)
            val effectiveImeDp = with(density) {
                (imeBottomPx - navBottomPx).coerceAtLeast(0).toDp()
            }

            val restingBottomPadding = XvoxMiniPlayerPlacement.miniPlayerBottom(navigationBarHeight)
            val currentBottomPadding = max(restingBottomPadding, effectiveImeDp + XvoxMiniPlayerPlacement.controlGap)

            val miniModifier = Modifier
                .navigationBarsPadding()
                .padding(
                    start = XvoxMiniPlayerPlacement.horizontalEdge,
                    end = XvoxMiniPlayerPlacement.horizontalEdge,
                    bottom = currentBottomPadding
                )

            XvoxMiniPlayer(
                queue = queue,
                currentSongId = currentSongId,
                currentIndex = currentIndex,
                isPlaying = isPlaying,
                position = position,
                duration = duration,
                riseKey = riseKey,
                togglePlay = onTogglePlay,
                playQueueIndex = onPlayQueueIndex,
                stopAndDismiss = onStopAndDismiss,
                openPlayer = onOpenPlayer,
                isLiked = isLiked,
                onLike = onLike,
                onAddToPlaylist = onAddToPlaylist,
                onOpenMiniPlayerSettings = onOpenMiniPlayerSettings,
                quickActionsVisible = quickActionsVisible,
                onQuickActionsVisibleChange = { quickActionsVisible = it },
                modifier = miniModifier
            )
        }
    }
}
