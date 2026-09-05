package com.xvox.music.core.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateContentSize
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val XvoxBEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val XvoxBEasingOut = CubicBezierEasing(0.4f, 0f, 1f, 1f)

@Composable
fun XvoxB(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) { detectTapGestures(onPress = { tryAwaitRelease() }) }
            .imePadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        // Backdrop dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { close() } }
        )

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220, easing = XvoxBEasing)) +
                scaleIn(initialScale = 0.90f, animationSpec = tween(280, easing = XvoxBEasing)) +
                slideInVertically(initialOffsetY = { it / 14 }, animationSpec = tween(280, easing = XvoxBEasing)) +
                expandVertically(expandFrom = Alignment.Top, animationSpec = tween(280, easing = XvoxBEasing)),
            exit = fadeOut(tween(280, easing = XvoxBEasing)) +
                scaleOut(targetScale = 0.90f, animationSpec = tween(280, easing = XvoxBEasing)) +
                slideOutVertically(targetOffsetY = { it / 14 }, animationSpec = tween(280, easing = XvoxBEasing)) +
                shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(280, easing = XvoxBEasing)),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                // IME-aware max height: outer imePadding already shrinks maxHeight, ensure top/bottom fully visible - reduced padding to use available area
                val maxH = (maxHeight - 24.dp).coerceAtLeast(120.dp)
                val shape = RoundedCornerShape(24.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(max = maxH)
                        .animateContentSize(animationSpec = tween(240, easing = XvoxBEasing))
                        .background(colors.cardElevated.copy(alpha = 0.96f), shape)
                        .clip(shape)
                        .pointerInput(Unit) { detectTapGestures(onPress = { tryAwaitRelease() }) },
                ) {
                    // Scrollable content – outer remains edge-to-edge clip, inner content handles own LEFT/RIGHT margin only (13)
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                content()
                            }
                        }
                    }

                    // Global X – aligned with header row (top 8dp matches reduced content top padding + row center)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(36.dp)
                            .background(colors.card.copy(alpha = 0.98f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = ::close,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_close),
                            contentDescription = "Close",
                            tint = colors.primaryText,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}
