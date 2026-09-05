package com.xvox.music.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayer
import com.xvox.music.core.ui.miniplayer.XvoxMiniPlayerPlacement
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion

private val MainEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

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
    onLike: () -> Unit,
    onAdd: () -> Unit
) {
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
            val keyboardOpen = imeBottomPx > 0
            val effectiveImeDp = with(density) {
                (imeBottomPx - navBottomPx).coerceAtLeast(0).toDp()
            }

            val animatedBottomPadding by animateDpAsState(
                targetValue = if (keyboardOpen) effectiveImeDp + 24.dp else 106.dp,
                animationSpec = tween(durationMillis = 280, easing = MainEase),
                label = "miniPlayerBottomGap"
            )

            val miniModifier = Modifier
                .navigationBarsPadding()
                .padding(
                    start = XvoxMiniPlayerPlacement.horizontalEdge,
                    end = XvoxMiniPlayerPlacement.horizontalEdge,
                    bottom = animatedBottomPadding
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
                onLike = onLike,
                onAdd = onAdd,
                modifier = miniModifier
            )
        }
    }
}
