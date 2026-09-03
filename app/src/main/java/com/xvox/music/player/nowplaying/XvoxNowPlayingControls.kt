package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xvox.music.R

@Composable
fun XvoxNowPlayingControls(
    isPlaying: Boolean,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier =
            modifier,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        ControlIcon(
            resource =
                R.drawable.ic_xvox_shuffle,
            size = 20,
            onClick =
                onShuffle
        )

        androidx.compose.foundation.layout.Spacer(
            Modifier.weight(1f)
        )

        ControlIcon(
            resource =
                R.drawable
                    .ic_xvox_skip_previous,
            size = 24,
            onClick =
                onPrevious
        )

        androidx.compose.foundation.layout.Spacer(
            Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    Color.White.copy(
                        alpha = 0.18f
                    ),
                    CircleShape
                )
                .clickable(
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    indication = null,
                    onClick =
                        onTogglePlay
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
                tint = Color.White,
                modifier =
                    Modifier.size(
                        24.dp
                    )
            )
        }

        androidx.compose.foundation.layout.Spacer(
            Modifier.weight(1f)
        )

        ControlIcon(
            resource =
                R.drawable
                    .ic_xvox_skip_next,
            size = 24,
            onClick =
                onNext
        )

        androidx.compose.foundation.layout.Spacer(
            Modifier.weight(1f)
        )

        ControlIcon(
            resource =
                R.drawable.ic_xvox_repeat,
            size = 20,
            onClick =
                onRepeat
        )
    }
}

@Composable
private fun ControlIcon(
    resource: Int,
    size: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clickable(
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
                indication = null,
                onClick =
                    onClick
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            painter =
                painterResource(
                    resource
                ),
            contentDescription =
                null,
            tint = Color.White,
            modifier =
                Modifier.size(
                    size.dp
                )
        )
    }
}
