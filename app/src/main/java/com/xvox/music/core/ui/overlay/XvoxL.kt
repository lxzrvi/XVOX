package com.xvox.music.core.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val XvoxLEasing = CubicBezierEasing(0.2f, 0.9f, 0.1f, 1f)

@Composable
fun XvoxL(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }

    fun close() {
        if (closing) return
        closing = true
        visible = false
        scope.launch {
            delay(280L)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) { visible = true }
    BackHandler { close() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) { detectTapGestures { close() } }
    ) {
        val density = LocalDensity.current
        val totalScreenHeightPx = with(density) { maxHeight.toPx() }

        var heightFraction by remember { mutableFloatStateOf(0.72f) }
        var dragDismissOffsetY by remember { mutableFloatStateOf(0f) }

        val animatedFraction by animateFloatAsState(
            targetValue = heightFraction,
            animationSpec = tween(220, easing = XvoxLEasing),
            label = "sheetHeightFraction"
        )

        val currentHeightDp = maxHeight * animatedFraction

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(340, easing = XvoxLEasing)
            ) + fadeIn(tween(240, easing = XvoxLEasing)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300, easing = XvoxLEasing)
            ) + fadeOut(tween(200, easing = XvoxLEasing))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentHeightDp)
                    .graphicsLayer {
                        translationY = dragDismissOffsetY.coerceAtLeast(0f)
                    }
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                    .background(colors.cardElevated.copy(alpha = 0.98f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { tryAwaitRelease() })
                    }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
            ) {
                // Top Drag Handle Indicator: drag up to grow taller, drag down to shrink or close past threshold
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 8.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount < 0f) {
                                        // Dragging UP -> Grow
                                        dragDismissOffsetY = 0f
                                        val deltaFraction = -dragAmount / totalScreenHeightPx
                                        heightFraction = (heightFraction + deltaFraction * 1.5f).coerceIn(0.42f, 0.94f)
                                    } else {
                                        // Dragging DOWN
                                        if (heightFraction > 0.46f) {
                                            val deltaFraction = dragAmount / totalScreenHeightPx
                                            heightFraction = (heightFraction - deltaFraction * 1.5f).coerceIn(0.42f, 0.94f)
                                        } else {
                                            dragDismissOffsetY += dragAmount
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (dragDismissOffsetY > 100f || heightFraction < 0.38f) {
                                        close()
                                    } else {
                                        dragDismissOffsetY = 0f
                                        if (heightFraction > 0.80f) {
                                            heightFraction = 0.92f
                                        } else if (heightFraction < 0.55f) {
                                            heightFraction = 0.45f
                                        } else {
                                            heightFraction = 0.70f
                                        }
                                    }
                                },
                                onDragCancel = {
                                    dragDismissOffsetY = 0f
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.cardBorder)
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}
