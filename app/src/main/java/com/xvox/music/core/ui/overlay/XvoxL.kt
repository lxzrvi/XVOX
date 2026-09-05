package com.xvox.music.core.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val XvoxLEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

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
    var isExpanded by remember { mutableStateOf(false) }

    fun close() {
        if (closing) return
        closing = true
        visible = false
        scope.launch {
            delay(260L)
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
        val maxAvailableHeight = if (isExpanded) maxHeight * 0.94f else maxHeight * 0.85f
        var dragOffset by remember { mutableFloatStateOf(0f) }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = XvoxLEasing)
            ) + fadeIn(tween(240, easing = XvoxLEasing)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(260, easing = XvoxLEasing)
            ) + fadeOut(tween(200, easing = XvoxLEasing))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(min = 100.dp, max = maxAvailableHeight)
                    .animateContentSize(animationSpec = tween(200, easing = XvoxLEasing))
                    .graphicsLayer {
                        translationY = dragOffset.coerceAtLeast(0f)
                    }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(colors.cardElevated.copy(alpha = 0.98f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { tryAwaitRelease() })
                    }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            ) {
                // Top Drag Handle Indicator: drag up to expand, drag down to dismiss
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 8.dp)
                        .size(width = 44.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.cardBorder)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount < -10f) {
                                        isExpanded = true
                                    } else {
                                        dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                                    }
                                },
                                onDragEnd = {
                                    if (dragOffset > 90f) {
                                        close()
                                    } else {
                                        dragOffset = 0f
                                    }
                                },
                                onDragCancel = { dragOffset = 0f }
                            )
                        }
                )

                content()
            }
        }
    }
}
