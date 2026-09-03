package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val colors =
        XvoxTheme.colors

    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp),
        content = {
            ControlButton(
                R.drawable.ic_xvox_shuffle,
                20,
                onShuffle
            )

            ControlButton(
                R.drawable.ic_xvox_skip_previous,
                24,
                onPrevious
            )

            ControlButton(
                if (isPlaying) {
                    R.drawable.ic_xvox_pause
                } else {
                    R.drawable.ic_xvox_play
                },
                25,
                onTogglePlay,
                prominent = true
            )

            ControlButton(
                R.drawable.ic_xvox_skip_next,
                24,
                onNext
            )

            ControlButton(
                R.drawable.ic_xvox_repeat,
                20,
                onRepeat
            )
        }
    ) {
        measurables,
        constraints ->

        val placeables =
            measurables.map {
                it.measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0
                    )
                )
            }

        val width =
            constraints.maxWidth

        val height =
            constraints.maxHeight

        val centers =
            floatArrayOf(
                0.15f,
                0.35f,
                0.50f,
                0.65f,
                0.85f
            )

        layout(
            width,
            height
        ) {
            placeables
                .forEachIndexed {
                    index,
                    placeable ->

                    val centerX =
                        width *
                            centers[index]

                    placeable.placeRelative(
                        x =
                            (
                                centerX -
                                    placeable.width /
                                    2f
                                ).toInt(),
                        y =
                            (
                                height -
                                    placeable.height
                                ) / 2
                    )
                }
        }
    }
}

@Composable
private fun ControlButton(
    resource: Int,
    iconSize: Int,
    onClick: () -> Unit,
    prominent: Boolean = false
) {
    val colors =
        XvoxTheme.colors

    val buttonSize =
        if (prominent) {
            56.dp
        } else {
            42.dp
        }

    Box(
        modifier = Modifier
            .size(buttonSize)
            .background(
                colors.card.copy(
                    alpha =
                        if (prominent) {
                            0.58f
                        } else {
                            0.26f
                        }
                ),
                CircleShape
            )
            .border(
                0.65.dp,
                Color.White.copy(
                    alpha = 0.14f
                ),
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
                    resource
                ),
            contentDescription = null,
            tint = Color.White,
            modifier =
                Modifier.size(
                    iconSize.dp
                )
        )
    }
}
