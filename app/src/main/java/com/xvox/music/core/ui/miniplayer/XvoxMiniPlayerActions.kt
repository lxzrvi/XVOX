package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.spacedBy
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxMiniPlayerActions(
    visible: Boolean,
    onLike: () -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            fadeIn() +
                scaleIn(
                    initialScale = 0.72f,
                    animationSpec =
                        spring(
                            dampingRatio = 0.72f,
                            stiffness = 500f
                        )
                ) +
                slideInVertically {
                    28
                },
        exit =
            fadeOut() +
                scaleOut(
                    targetScale = 0.82f
                ) +
                slideOutVertically {
                    16
                }
    ) {
        Row(
            horizontalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            ActionCircle(
                type = XvoxMiniIconType.HEART,
                onClick = onLike
            )

            ActionCircle(
                type = XvoxMiniIconType.ADD,
                onClick = onAdd
            )

            ActionCircle(
                type = XvoxMiniIconType.CLOSE,
                onClick = onClose
            )
        }
    }
}

@Composable
private fun ActionCircle(
    type: XvoxMiniIconType,
    onClick: () -> Unit
) {
    val colors =
        XvoxTheme.colors

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                colors.cardElevated,
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
        XvoxMiniIcon(
            type = type,
            color = colors.primaryText,
            modifier =
                Modifier.size(19.dp)
        )
    }
}
