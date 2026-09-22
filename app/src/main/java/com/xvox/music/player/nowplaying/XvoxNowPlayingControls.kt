package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Transport controls use a preview/commit interaction for previous and next:
 * pressing moves only the cover pager, holding keeps stepping through covers, and releasing
 * starts the selected song. The currently audible song therefore never cuts out while browsing.
 */
@Composable
fun XvoxNowPlayingControls(
    isPlaying: Boolean,
    onShuffle: () -> Unit,
    onPreviewPrevious: () -> Boolean,
    onTogglePlay: () -> Unit,
    onPreviewNext: () -> Boolean,
    onCommitPreview: () -> Unit,
    onCancelPreview: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    isShuffleEnabled: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    previewIndex: Int = -1,
    queueSize: Int = 0
) {
    val colors = XvoxTheme.colors
    val isRepeatOne = repeatMode == RepeatMode.ONE
    val prevEnabled = !isRepeatOne && (repeatMode == RepeatMode.ALL || previewIndex > 0)
    val nextEnabled = !isRepeatOne && (repeatMode == RepeatMode.ALL || (queueSize > 0 && previewIndex < queueSize - 1))

    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp),
        content = {
            SimpleControl(
                resource = R.drawable.ic_xvox_shuffle,
                iconSize = 20,
                onClick = onShuffle,
                tint = if (isShuffleEnabled) colors.primaryAccent else colors.primaryText,
                showDot = isShuffleEnabled
            )

            PreviewNavigationControl(
                resource = R.drawable.ic_xvox_skip_previous,
                iconSize = 25,
                enabled = prevEnabled,
                tint = if (prevEnabled) colors.primaryText else colors.primaryText.copy(alpha = 0.28f),
                contentDescription = "Previous track preview",
                onStep = onPreviewPrevious,
                onCommit = onCommitPreview,
                onCancel = onCancelPreview
            )

            PlayControl(
                isPlaying = isPlaying,
                onClick = onTogglePlay
            )

            PreviewNavigationControl(
                resource = R.drawable.ic_xvox_skip_next,
                iconSize = 25,
                enabled = nextEnabled,
                tint = if (nextEnabled) colors.primaryText else colors.primaryText.copy(alpha = 0.28f),
                contentDescription = "Next track preview",
                onStep = onPreviewNext,
                onCommit = onCommitPreview,
                onCancel = onCancelPreview
            )

            SimpleControl(
                resource = when (repeatMode) {
                    RepeatMode.ONE -> R.drawable.ic_xvox_repeat_one
                    else -> R.drawable.ic_xvox_repeat
                },
                iconSize = 20,
                onClick = onRepeat,
                tint = if (repeatMode != RepeatMode.OFF) colors.primaryAccent else colors.primaryText
            )
        }
    ) { measurables, constraints ->
        val placeables = measurables.map {
            it.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }
        val centers = floatArrayOf(0.15f, 0.35f, 0.50f, 0.65f, 0.85f)

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val x = (constraints.maxWidth * centers[index] - placeable.width / 2f).toInt()
                val y = (constraints.maxHeight - placeable.height) / 2
                placeable.placeRelative(x, y)
            }
        }
    }
}

@Composable
private fun SimpleControl(
    resource: Int,
    iconSize: Int,
    onClick: () -> Unit,
    tint: Color? = null,
    showDot: Boolean = false
) {
    val colors = XvoxTheme.colors
    Box(
        modifier = Modifier
            .size(42.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = tint ?: colors.primaryText,
            modifier = Modifier.size(iconSize.dp)
        )
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .size(4.dp)
                    .background(colors.primaryAccent, CircleShape)
            )
        }
    }
}

@Composable
private fun PreviewNavigationControl(
    resource: Int,
    iconSize: Int,
    enabled: Boolean,
    tint: Color,
    contentDescription: String,
    onStep: () -> Boolean,
    onCommit: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalXvoxHaptics.current
    val latestEnabled = rememberUpdatedState(enabled)
    val latestStep = rememberUpdatedState(onStep)
    val latestCommit = rememberUpdatedState(onCommit)
    val latestCancel = rememberUpdatedState(onCancel)

    Box(
        modifier = Modifier
            .size(42.dp)
            // The key deliberately stays stable while a held button walks across the queue.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (!latestEnabled.value) {
                        waitForUpOrCancellation(PointerEventPass.Initial)
                        return@awaitEachGesture
                    }

                    var hasPreviewed = latestStep.value()
                    var canKeepStepping = hasPreviewed
                    val repeatJob = scope.launch {
                        delay(360)
                        if (hasPreviewed) haptics.heavy()
                        while (isActive && canKeepStepping) {
                            delay(190)
                            val moved = latestStep.value()
                            hasPreviewed = hasPreviewed || moved
                            canKeepStepping = moved
                        }
                    }
                    val released = try {
                        waitForUpOrCancellation(PointerEventPass.Initial)
                    } finally {
                        repeatJob.cancel()
                    }

                    if (hasPreviewed && released != null) {
                        haptics.tap()
                        latestCommit.value()
                    } else if (hasPreviewed) {
                        latestCancel.value()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

@Composable
private fun PlayControl(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val darkMode = colors.background.luminance() < 0.5f
    val circleColor = if (darkMode) Color.Black.copy(alpha = 0.22f) else colors.card.copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .size(56.dp)
            .background(circleColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(if (isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = colors.primaryText,
            modifier = Modifier.size(25.dp)
        )
    }
}
