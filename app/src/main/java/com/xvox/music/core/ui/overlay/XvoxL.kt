package com.xvox.music.core.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val XvoxLEasing =
    CubicBezierEasing(
        0.2f,
        0.9f,
        0.1f,
        1f
    )

@Composable
fun XvoxL(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var visible by remember {
        mutableStateOf(false)
    }

    var closing by remember {
        mutableStateOf(false)
    }

    fun close() {
        if (closing) return

        closing = true
        visible = false

        scope.launch {
            delay(300L)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    BackHandler {
        close()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures {
                    close()
                }
            }
    ) {
        val screenHeightPx =
            with(density) {
                maxHeight.toPx()
            }

        val minimumOpenPx =
            screenHeightPx * 0.25f

        val maximumHeightPx =
            screenHeightPx * 0.94f

        var sheetHeightPx by remember(
            screenHeightPx
        ) {
            mutableFloatStateOf(
                screenHeightPx * 0.72f
            )
        }

        var contentMeasured by remember {
            mutableStateOf(false)
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(
                Alignment.BottomCenter
            ),
            enter =
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(
                        300,
                        easing = XvoxLEasing
                    )
                ) +
                    fadeIn(
                        tween(
                            300,
                            easing = XvoxLEasing
                        )
                    ),
            exit =
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(
                        300,
                        easing = XvoxLEasing
                    )
                ) +
                    fadeOut(
                        tween(
                            300,
                            easing = XvoxLEasing
                        )
                    )
        ) {
            
            val sheetCornerShape = RoundedCornerShape(
                topStart = 26.dp,
                topEnd = 26.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        with(density) {
                            Modifier.size(
                                width = maxWidth,
                                height =
                                    sheetHeightPx
                                        .coerceIn(
                                            minimumOpenPx,
                                            maximumHeightPx
                                        )
                                        .toDp()
                            )
                        }
                    )
                    .xvoxGlass(
                        shape = sheetCornerShape,
                        tint = colors.cardElevated.copy(alpha = 0.94f),
                        solidFallback = colors.cardElevated,
                        borderWidth = 0.8.dp,
                        borderColor = Color.White.copy(alpha = 0.12f)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                tryAwaitRelease()
                            }
                        )
                    }
            ) {
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp,
                            bottom = 8.dp
                        )
                        .pointerInput(
                            screenHeightPx
                        ) {
                            var rawHeight =
                                sheetHeightPx

                            detectVerticalDragGestures(
                                onDragStart = {
                                    rawHeight =
                                        sheetHeightPx
                                },
                                onVerticalDrag = {
                                        change,
                                        dragAmount ->

                                    change.consume()

                                    rawHeight -=
                                        dragAmount

                                    sheetHeightPx =
                                        rawHeight
                                            .coerceIn(
                                                0f,
                                                maximumHeightPx
                                            )
                                },
                                onDragEnd = {
                                    if (
                                        sheetHeightPx <
                                        minimumOpenPx
                                    ) {
                                        close()
                                    } else {
                                        sheetHeightPx =
                                            sheetHeightPx
                                                .coerceAtMost(
                                                    maximumHeightPx
                                                )
                                    }
                                },
                                onDragCancel = {
                                    if (
                                        sheetHeightPx <
                                        minimumOpenPx
                                    ) {
                                        sheetHeightPx =
                                            minimumOpenPx
                                    }
                                }
                            )
                        },
                    contentAlignment =
                        Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(
                                width = 44.dp,
                                height = 4.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    2.dp
                                )
                            )
                            .background(
                                colors.cardBorder
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.navigationBars
                        )
                        .imePadding()
                        .padding(
                            start = 14.dp,
                            end = 14.dp,
                            bottom = 8.dp
                        )
                        .onSizeChanged {
                            
                            if (!contentMeasured) {
                                contentMeasured = true
                            }
                        }
                ) {
                    content()
                }
            }
        }
    }
}
