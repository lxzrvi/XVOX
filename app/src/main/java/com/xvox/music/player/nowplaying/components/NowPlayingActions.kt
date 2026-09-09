@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.xvox.music.player.nowplaying.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme

/**
 * The Now Playing action bar.
 *
 * Left side stays put: Timer / Queue / Info. The right side holds three round buttons that swap
 * in place — swipe that cluster left and Crossfade / Add-to-playlist / Heart slide out while
 * Equalizer / 3D sound / Lyrics slide into the exact same spots. A small swipe right brings the
 * first trio back. Long-press on the audio toggles opens their settings.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingActions(
    isLiked: Boolean = false,
    isInPlaylist: Boolean = false,
    onTimer: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onToggleLiked: (() -> Unit)? = null,
    onStarPlaylist: (() -> Unit)? = null,
    timerProgress: Float? = null,
    crossfadeOn: Boolean = false,
    onToggleCrossfade: (() -> Unit)? = null,
    equalizerOn: Boolean = false,
    spaceOn: Boolean = false,
    lyricsOn: Boolean = false,
    onToggleEqualizer: (() -> Unit)? = null,
    onToggleSpace: (() -> Unit)? = null,
    onToggleLyrics: (() -> Unit)? = null,
    onOpenOptions: ((String) -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    var audioToggles by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fixed left cluster.
        Row(verticalAlignment = Alignment.CenterVertically) {
            NowPlayingActionIcon(
                resource = R.drawable.ic_xvox_timer,
                onClick = onTimer,
                progress = timerProgress
            )
            NowPlayingActionIcon(
                resource = R.drawable.ic_xvox_queue,
                onClick = onQueue
            )
            NowPlayingActionIcon(
                resource = R.drawable.ic_xvox_info,
                onClick = onInfo
            )
        }

        Spacer(Modifier.weight(1f))

        // Right cluster: three circles that swap in place on a horizontal swipe.
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .pointerInput(Unit) {
                    var drag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            drag += dragAmount
                        },
                        onDragEnd = {
                            if (drag <= -28f) audioToggles = true
                            else if (drag >= 28f) audioToggles = false
                        },
                        onDragCancel = { }
                    )
                }
        ) {
            AnimatedContent(
                targetState = audioToggles,
                transitionSpec = {
                    val forward = targetState
                    (slideInHorizontally(tween(230)) { it * if (forward) -1 else 1 } + fadeIn(tween(150)))
                        .togetherWith(slideOutHorizontally(tween(230)) { -it * if (forward) -1 else 1 } + fadeOut(tween(150)))
                },
                label = "nowPlayingRightCluster"
            ) { toggles ->
                if (!toggles) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        NowPlayingCircleAction(
                            resource = R.drawable.ic_xvox_crossfade,
                            tint = if (crossfadeOn) colors.primaryAccent else colors.primaryText,
                            active = crossfadeOn,
                            contentDescription = "Crossfade",
                            onClick = onToggleCrossfade,
                            onLongClick = if (onOpenOptions != null) ({ onOpenOptions("Crossfade") }) else null
                        )
                        NowPlayingCircleAction(
                            resource = R.drawable.ic_xvox_star,
                            tint = if (isInPlaylist) colors.primaryAccent else colors.primaryText,
                            active = isInPlaylist,
                            contentDescription = "Add to playlist",
                            onClick = onStarPlaylist
                        )
                        NowPlayingCircleAction(
                            resource = if (isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
                            tint = if (isLiked) colors.primaryAccent else colors.primaryText,
                            active = isLiked,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            onClick = onToggleLiked
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        NowPlayingCircleAction(
                            resource = R.drawable.ic_xvox_equalizer,
                            tint = if (equalizerOn) colors.primaryAccent else colors.primaryText,
                            active = equalizerOn,
                            contentDescription = "Equalizer",
                            onClick = onToggleEqualizer,
                            onLongClick = if (onOpenOptions != null) ({ onOpenOptions("Equalizer") }) else null
                        )
                        NowPlayingCircleAction(
                            resource = R.drawable.ic_xvox_waveform,
                            tint = if (spaceOn) colors.primaryAccent else colors.primaryText,
                            active = spaceOn,
                            contentDescription = "3D sound",
                            onClick = onToggleSpace,
                            onLongClick = if (onOpenOptions != null) ({ onOpenOptions("3D sound") }) else null
                        )
                        NowPlayingCircleAction(
                            resource = R.drawable.ic_xvox_lyrics,
                            tint = if (lyricsOn) colors.primaryAccent else colors.primaryText,
                            active = lyricsOn,
                            contentDescription = "Lyrics",
                            onClick = onToggleLyrics,
                            onLongClick = if (onOpenOptions != null) ({ onOpenOptions("Lyrics") }) else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NowPlayingActionIcon(
    resource: Int,
    onClick: (() -> Unit)? = null,
    progress: Float? = null
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(36.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onClick != null,
                onClick = { onClick?.invoke() },
                onLongClick = null
            ),
        contentAlignment = Alignment.Center
    ) {
        if (progress != null) {
            Canvas(modifier = Modifier.size(32.dp)) {
                val stroke = 2.5.dp.toPx()
                drawArc(
                    color = colors.mutedText.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = colors.primaryAccent,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun NowPlayingCircleAction(
    resource: Int,
    tint: Color? = null,
    active: Boolean = false,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                if (active) colors.primaryAccent.copy(alpha = 0.24f)
                else colors.card.copy(alpha = 0.22f),
                CircleShape
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onClick != null || onLongClick != null,
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = contentDescription,
            tint = tint ?: colors.primaryAccent,
            modifier = Modifier.size(19.dp)
        )
    }
}
