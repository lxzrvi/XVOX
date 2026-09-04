package com.xvox.music.core.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.launch

private val XvoxLEasing =
    CubicBezierEasing(
        0.20f,
        0.75f,
        0.22f,
        1f
    )

@Composable
fun XvoxL(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val hidden =
            constraints.maxHeight.toFloat()

        val offset =
            remember(hidden) {
                Animatable(hidden)
            }

        fun dismiss() {
            scope.launch {
                offset.animateTo(
                    hidden,
                    tween(
                        250,
                        easing = XvoxLEasing
                    )
                )

                onDismiss()
            }
        }

        LaunchedEffect(hidden) {
            offset.snapTo(hidden)

            offset.animateTo(
                0f,
                tween(
                    290,
                    easing = XvoxLEasing
                )
            )
        }

        BackHandler {
            dismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.18f
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures {
                        dismiss()
                    }
                }
        )

        Column(
            modifier = Modifier
                .align(
                    Alignment.BottomCenter
                )
                .fillMaxWidth()
                .heightIn(
                    max = maxHeight * 0.5f
                )
                .graphicsLayer {
                    translationY =
                        offset.value
                }
                .background(
                    colors.background.copy(
                        alpha = 0.96f
                    ),
                    RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp
                    )
                )
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                )
                .pointerInput(hidden) {
                    detectVerticalDragGestures(
                        onVerticalDrag = {
                                change,
                                amount ->

                            change.consume()

                            scope.launch {
                                offset.snapTo(
                                    (
                                        offset.value +
                                            amount
                                        )
                                        .coerceAtLeast(
                                            0f
                                        )
                                )
                            }
                        },
                        onDragEnd = {
                            if (
                                offset.value >
                                hidden * 0.10f
                            ) {
                                dismiss()
                            } else {
                                scope.launch {
                                    offset.animateTo(
                                        0f,
                                        tween(
                                            190,
                                            easing =
                                                XvoxLEasing
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp
                )
        ) {
            Box(
                modifier = Modifier
                    .align(
                        Alignment.CenterHorizontally
                    )
                    .padding(
                        vertical = 10.dp
                    )
                    .size(
                        width = 38.dp,
                        height = 4.dp
                    )
                    .background(
                        colors.mutedText.copy(
                            alpha = 0.48f
                        ),
                        RoundedCornerShape(
                            2.dp
                        )
                    )
            )

            content()
        }
    }
}
