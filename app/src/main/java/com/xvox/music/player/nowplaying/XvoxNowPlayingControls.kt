package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp),
        content = {
            BareControl(
                R.drawable.ic_xvox_shuffle,
                20,
                onShuffle
            )

            BareControl(
                R.drawable.ic_xvox_skip_previous,
                25,
                onPrevious
            )

            PlayControl(
                isPlaying = isPlaying,
                onClick = onTogglePlay
            )

            BareControl(
                R.drawable.ic_xvox_skip_next,
                25,
                onNext
            )

            BareControl(
                R.drawable.ic_xvox_repeat,
                20,
                onRepeat
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
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .size(42.dp)
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
                painterResource(resource),
            contentDescription = null,
            tint = colors.primaryText,
            modifier =
                Modifier.size(iconSize.dp)
        )
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
