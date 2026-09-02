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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
                    initialScale = 0.7f,
                    animationSpec =
                        spring(
                            dampingRatio = 0.72f,
                            stiffness = 500f
                        )
                ) +
                slideInVertically {
                    24
                },
        exit =
            fadeOut() +
                scaleOut(
                    targetScale = 0.8f
                ) +
                slideOutVertically {
                    14
                }
    ) {
        Row(
            horizontalArrangement =
                Arrangement.spacedBy(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            MiniAction(
                type =
                    MiniActionType.HEART,
                onClick =
                    onLike
            )

            MiniAction(
                type =
                    MiniActionType.ADD,
                onClick =
                    onAdd
            )

            MiniAction(
                type =
                    MiniActionType.CLOSE,
                onClick =
                    onClose
            )
        }
    }
}

private enum class MiniActionType {
    HEART,
    ADD,
    CLOSE
}

@Composable
private fun MiniAction(
    type: MiniActionType,
    onClick: () -> Unit
) {
    val colors =
        XvoxTheme.colors

    androidx.compose.foundation.Canvas(
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
            )
    ) {
        val color =
            colors.primaryText

        val stroke =
            2.dp.toPx()

        when (type) {
            MiniActionType.ADD -> {
                drawLine(
                    color,
                    start =
                        androidx.compose.ui.geometry.Offset(
                            size.width * 0.28f,
                            size.height * 0.5f
                        ),
                    end =
                        androidx.compose.ui.geometry.Offset(
                            size.width * 0.72f,
                            size.height * 0.5f
                        ),
                    strokeWidth = stroke,
                    cap =
                        androidx.compose.ui.graphics.StrokeCap.Round
                )

                drawLine(
                    color,
                    start =
                        androidx.compose.ui.geometry.Offset(
                            size.width * 0.5f,
                            size.height * 0.28f
                        ),
                    end =
                        androidx.compose.ui.geometry.Offset(
                            size.width * 0.5f,
                            size.height * 0.72f
                        ),
                    strokeWidth = stroke,
                    cap =
                        androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            MiniActionType.CLOSE -> {
                drawLine(
                    color,
                    start =
                        androidx.compose.ui.geometry.Offset(
                            size.width * 0.33f,
                            size.height * 0.33f
                        ),
                    end =
                        androidx.compose.ui.geometry.Offset(
                            size.width * 0.67f,
                            size.height * 0.67f
                        ),
                    strokeWidth = stroke,
                    cap =
                        androidx.compose.ui.graphics.StrokeCap.Round
                )

                drawLine(
                    color,
                    start =
                        androidx.compose.ui.geometry.Offset(
                            size.width * 0.67f,
                            size.height * 0.33f
                        ),
                    end =
                        androidx.compose.ui.geometry.Offset(
                            size.width * 0.33f,
                            size.height * 0.67f
                        ),
                    strokeWidth = stroke,
                    cap =
                        androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            MiniActionType.HEART -> {
                val path =
                    androidx.compose.ui.graphics.Path()
                        .apply {
                            moveTo(
                                size.width * 0.5f,
                                size.height * 0.72f
                            )

                            cubicTo(
                                size.width * 0.17f,
                                size.height * 0.52f,
                                size.width * 0.23f,
                                size.height * 0.29f,
                                size.width * 0.39f,
                                size.height * 0.29f
                            )

                            cubicTo(
                                size.width * 0.47f,
                                size.height * 0.29f,
                                size.width * 0.5f,
                                size.height * 0.37f,
                                size.width * 0.5f,
                                size.height * 0.37f
                            )

                            cubicTo(
                                size.width * 0.5f,
                                size.height * 0.37f,
                                size.width * 0.53f,
                                size.height * 0.29f,
                                size.width * 0.61f,
                                size.height * 0.29f
                            )

                            cubicTo(
                                size.width * 0.77f,
                                size.height * 0.29f,
                                size.width * 0.83f,
                                size.height * 0.52f,
                                size.width * 0.5f,
                                size.height * 0.72f
                            )
                        }

                drawPath(
                    path,
                    color,
                    style =
                        androidx.compose.ui.graphics.drawscope.Stroke(
                            width = stroke,
                            cap =
                                androidx.compose.ui.graphics.StrokeCap.Round,
                            join =
                                androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                )
            }
        }
    }
}
