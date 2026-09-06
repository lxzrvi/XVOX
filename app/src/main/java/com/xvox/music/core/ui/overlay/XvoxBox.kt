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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val XvoxBoxEasing = CubicBezierEasing(0.2f, 0.9f, 0.1f, 1f)

@Composable
fun XvoxBox(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onAddClick: (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val density = LocalDensity.current
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
                detectTapGestures { close() }
            }
    ) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val maximumHeightPx = screenHeightPx * 0.92f
        var dragDeltaPx by remember { mutableFloatStateOf(0f) }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding(),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(280, easing = XvoxBoxEasing)
            ) + fadeIn(tween(280, easing = XvoxBoxEasing)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(280, easing = XvoxBoxEasing)
            ) + fadeOut(tween(280, easing = XvoxBoxEasing))
        ) {
            val sheetCornerShape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = with(density) { maximumHeightPx.toDp() })
                    .clip(sheetCornerShape)
                    .background(colors.cardElevated.copy(alpha = 0.96f))
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { tryAwaitRelease() })
                    }
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                        .pointerInput(screenHeightPx) {
                            detectVerticalDragGestures(
                                onDragStart = { dragDeltaPx = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragDeltaPx += dragAmount
                                    if (dragDeltaPx > screenHeightPx * 0.15f) {
                                        close()
                                    }
                                },
                                onDragEnd = {
                                    if (dragDeltaPx > screenHeightPx * 0.15f) {
                                        close()
                                    }
                                    dragDeltaPx = 0f
                                },
                                onDragCancel = { dragDeltaPx = 0f }
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

                // Transparent Top Header
                if (title != null || onAddClick != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title.orEmpty(),
                            color = colors.primaryText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onAddClick != null) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colors.card)
                                        .clickable { onAddClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_xvox_add),
                                        contentDescription = "Add",
                                        tint = colors.primaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.card)
                                    .clickable { close() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_close),
                                    contentDescription = "Close",
                                    tint = colors.primaryText,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                // Main Scrollable / Dynamic Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    content()
                }

                // Floating Bottom Action Button
                if (bottomAction != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        bottomAction()
                    }
                } else {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .height(8.dp)
                    )
                }
            }
        }
    }
}
