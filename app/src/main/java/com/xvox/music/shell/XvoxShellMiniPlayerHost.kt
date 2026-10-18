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
    /** Settings owns a delayed, visibly downward route handoff instead of keeping this card. */
    settingsExit: Boolean = false,
    currentSongId: Long?,
    queue: List<Song>,
    currentIndex: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    riseKey: Int,
    /** The card can stay composed below the viewport while Now Playing is open. */
    entranceEnabled: Boolean = true,
    onTogglePlay: () -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onStopAndDismiss: () -> Unit,
    onPrepareNowPlaying: () -> Unit = {},
    onOpenPlayer: () -> Unit,
    isLiked: Boolean = false,
    onLike: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onOpenMiniPlayerSettings: () -> Unit = {},
    navigationBarHeight: Dp = 62.dp
) {
    // Let the incoming Settings page become visible before the card descends, so the card reads
    // as part of that route handoff rather than disappearing before Settings has opened.
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
        // Home/Search stay mounted. Settings is the one deliberate route exit and begins only
        // after its page has started to enter; Now Playing keeps its separate handoff lifecycle.
        enter = EnterTransition.None,
        exit = if (settingsExit) {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight + settingsExitExtraPx },
                animationSpec = tween(260, delayMillis = 72)
            ) + fadeOut(tween(150, delayMillis = 72))
        } else {
            ExitTransition.None
        },
        // The shell paints the navbar after this host. Keep the card at the natural layer so its
        // complete arrival/departure passes behind the navbar pill instead of over that surface.
        modifier = Modifier
            .align(Alignment.BottomCenter)
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
            // As the keyboard closes, stop treating a small residual inset as an active keyboard.
            // This snaps the Mini Player straight back to its resting lane instead of leaving it
            // briefly parked above the navbar during the final IME animation frames.
            val imeCloseSnapThresholdPx = with(density) { 48.dp.roundToPx() }
            val imeIsActuallyOccluding = WindowInsets.isImeVisible &&
                imeBottomPx > navBottomPx && imeAboveNavigationPx > imeCloseSnapThresholdPx
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
                entranceEnabled = entranceEnabled,
                togglePlay = onTogglePlay,
                playQueueIndex = onPlayQueueIndex,
                stopAndDismiss = onStopAndDismiss,
                prepareNowPlaying = onPrepareNowPlaying,
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
