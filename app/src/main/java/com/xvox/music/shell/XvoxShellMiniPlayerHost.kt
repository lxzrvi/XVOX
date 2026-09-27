@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.xvox.music.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
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

@Composable
fun BoxScope.XvoxShellMiniPlayerHost(
    visible: Boolean,
    /** Only the Settings route gets the visible downward shell handoff. */
    settingsExit: Boolean = false,
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
    navigationBarHeight: Dp = 62.dp
) {
    val settingsExitExtraPx = with(LocalDensity.current) { 18.dp.roundToPx() }
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
        // Playback/Now Playing continues to use the Mini Player's own sequential handoff. Only
        // Settings receives a shell-level downward exit so users can actually see it leave.
        enter = EnterTransition.None,
        exit = if (settingsExit) {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight + settingsExitExtraPx },
                animationSpec = tween(260)
            ) + fadeOut(tween(150))
        } else {
            ExitTransition.None
        },
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        if (currentSongId != null) {
            val density = LocalDensity.current
            // Read the live IME inset rather than using a fixed keyboard height. Android reports
            // different bottom insets for Gboard, OEM keyboards, split layouts, hardware-keyboard
            // transitions, and gesture/navigation modes. navigationBarsPadding below owns the
            // system-bar portion, so only the IME height above that baseline is added here.
            val imeBottomPx = WindowInsets.ime.getBottom(density)
            val navBottomPx = WindowInsets.navigationBars.getBottom(density)
            val imeAboveNavigationPx = (imeBottomPx - navBottomPx).coerceAtLeast(0)
            // isImeVisible flips at the close request rather than after the inset's closing
            // animation has trickled through. That lets the card snap directly to its resting
            // lane instead of lingering a little high and then dropping a second time.
            val imeIsActuallyOccluding = WindowInsets.isImeVisible &&
                imeBottomPx > navBottomPx && imeAboveNavigationPx > 1
            val effectiveImeDp = with(density) { imeAboveNavigationPx.toDp() }

            val restingBottomPadding = XvoxMiniPlayerPlacement.miniPlayerBottom(navigationBarHeight)
            // Keep the card above the *actual* keyboard edge with the shared visible 5dp air.
            // The physical navigation inset is applied exactly once by navigationBarsPadding.
            val keyboardBottomPadding = if (imeIsActuallyOccluding) {
                effectiveImeDp + XvoxMiniPlayerPlacement.controlGap
            } else {
                restingBottomPadding
            }
            val currentBottomPadding = max(restingBottomPadding, keyboardBottomPadding)

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
                // The product placement is a fixed −12dp lift while the IME actually occludes
                // content. Once it closes this is immediately zero, so the resting card cannot
                // retain or get stuck at a keyboard-only position.
                keyboardOffsetY = if (imeIsActuallyOccluding) (-12).dp else 0.dp,
                modifier = miniModifier
            )
        }
    }
}
