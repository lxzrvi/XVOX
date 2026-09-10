package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxTapOrScrub
import com.xvox.music.player.playback.RepeatMode

@Composable
fun XvoxNowPlayingControls(
    isPlaying: Boolean,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    isShuffleEnabled: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    currentIndex: Int = -1,
    queueSize: Int = 0,
    positionMs: Long = 0L,
    durationMs: Long = 0L,
    onScrubTo: ((Long) -> Unit)? = null,
) {
    val colors = XvoxTheme.colors
    val prevEnabled = repeatMode == RepeatMode.ALL || currentIndex > 0
    val nextEnabled = repeatMode == RepeatMode.ALL || (queueSize > 0 && currentIndex < queueSize - 1)
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp),
        content = {
            BareControl(
                R.drawable.ic_xvox_shuffle,
                20,
                onShuffle,
                tint = if (isShuffleEnabled) colors.primaryAccent else colors.primaryText,
                showDot = isShuffleEnabled
            )

            BareControl(
                R.drawable.ic_xvox_skip_previous,
                25,
                onPrevious,
                tint = if (prevEnabled) colors.primaryText else colors.primaryText.copy(alpha = 0.28f),
                enabled = prevEnabled,
                scrubTo = onScrubTo,
                scrubDirection = -1,
                // Rewinding cannot be seamless on a forward-only decoder, so step back in fewer,
                // bigger jumps instead of 40 ms ticks — far less chopping of the voice.
                scrubTickEveryMs = 320,
                scrubPositionMs = { positionMs },
                scrubDurationMs = { durationMs }
            )

            PlayControl(
                isPlaying = isPlaying,
                onClick = onTogglePlay
            )

            BareControl(
                R.drawable.ic_xvox_skip_next,
                25,
                onNext,
                boostHold = true,
                tint = if (nextEnabled) colors.primaryText else colors.primaryText.copy(alpha = 0.28f),
                enabled = nextEnabled,
                scrubTo = onScrubTo,
                scrubDirection = 1,
                scrubPositionMs = { positionMs },
                scrubDurationMs = { durationMs }
            )

            BareControl(
                when (repeatMode) {
                    RepeatMode.ONE -> R.drawable.ic_xvox_repeat_one
                    else -> R.drawable.ic_xvox_repeat
                },
                20,
                onRepeat,
                tint = if (repeatMode != RepeatMode.OFF) colors.primaryAccent else colors.primaryText
            )
        }
    ) { measurables, constraints ->
        val placeables =
            measurables.map {
                it.measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0
                    )
                )
            }

        val centers =
            floatArrayOf(
                0.15f,
                0.35f,
                0.50f,
                0.65f,
                0.85f
            )

        layout(
            constraints.maxWidth,
            constraints.maxHeight
        ) {
            placeables.forEachIndexed {
                    index,
                    placeable ->

                val x =
                    (
                        constraints.maxWidth *
                            centers[index] -
                            placeable.width / 2f
                        ).toInt()

                val y =
                    (
                        constraints.maxHeight -
                            placeable.height
                        ) / 2

                placeable.placeRelative(
                    x,
                    y
                )
            }
        }
    }
}

@Composable
private fun BareControl(
    resource: Int,
    iconSize: Int,
    onClick: () -> Unit,
    tint: Color? = null,
    showDot: Boolean = false,
    enabled: Boolean = true,
    scrubTo: ((Long) -> Unit)? = null,
    scrubDirection: Int = 1,
    scrubPositionMs: () -> Long = { 0L },
    scrubDurationMs: () -> Long = { 0L },
    scrubTickEveryMs: Long = 40,
    /** Next only: holding plays faster instead of re-seeking, so the audio never breaks. */
    boostHold: Boolean = false
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(42.dp)
            .then(
                if (boostHold) {
                    Modifier.xvoxTapOrBoost(
                        enabled = enabled,
                        onTap = onClick,
                        onBoostChange = { boosting ->
                            if (boosting) com.xvox.music.player.session.XvoxTransportBoost.set(2f)
                            else com.xvox.music.player.session.XvoxTransportBoost.release()
                        }
                    )
                } else if (scrubTo != null && scrubDurationMs() > 0L) {
                    // Tap skips the track; holding scrubs at 2× real time in [scrubDirection].
                    Modifier.xvoxTapOrScrub(
                        enabled = true,
                        onTap = { if (enabled) onClick() },
                        onScrubTo = scrubTo,
                        direction = scrubDirection,
                        positionMs = scrubPositionMs,
                        durationMs = scrubDurationMs,
                        tickEvery = scrubTickEveryMs
                    )
                } else {
                    Modifier.clickable(
                        interactionSource =
                            remember {
                                MutableInteractionSource()
                            },
                        indication = null,
                        enabled = enabled,
                        onClick = onClick
                    )
                }
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(resource),
            contentDescription = null,
            tint = tint ?: colors.primaryText,
            modifier =
                Modifier.size(iconSize.dp)
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
private fun PlayControl(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    val darkMode =
        colors.background.luminance() < 0.5f

    val circleColor =
        if (darkMode) {
            Color.Black.copy(
                alpha = 0.22f
            )
        } else {
            colors.card.copy(
                alpha = 0.25f
            )
        }

    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                circleColor,
                CircleShape
            )
            .clickable(
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
                indication = null,
                onClick = onClick
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(
                    if (isPlaying) {
                        R.drawable
                            .ic_xvox_pause
                    } else {
                        R.drawable
                            .ic_xvox_play
                    }
                ),
            contentDescription = null,
            tint = colors.primaryText,
            modifier =
                Modifier.size(25.dp)
        )
    }
}
