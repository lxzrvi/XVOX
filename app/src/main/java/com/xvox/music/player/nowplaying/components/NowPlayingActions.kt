package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun NowPlayingActions(
    isLiked: Boolean = false,
    isInPlaylist: Boolean = false,
    onTimer: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onToggleLiked: (() -> Unit)? = null,
    onStarPlaylist: (() -> Unit)? = null,
    timerProgress: Float? = null
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(colors.card.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        NowPlayingCircleAction(
            resource = R.drawable.ic_xvox_star,
            tint = if (isInPlaylist) colors.primaryAccent else colors.primaryText,
            onClick = onStarPlaylist
        )

        Spacer(Modifier.size(10.dp))

        NowPlayingCircleAction(
            resource = if (isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline,
            tint = if (isLiked) colors.primaryAccent else colors.primaryText,
            onClick = onToggleLiked
        )
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
            .size(42.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
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
    onClick: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(42.dp)
            .background(colors.card.copy(alpha = 0.22f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = tint ?: colors.primaryAccent,
            modifier = Modifier.size(19.dp)
        )
    }
}
