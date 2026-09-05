package com.xvox.music.core.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val XvoxBEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

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
            delay(260L)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) { visible = true }
    BackHandler { close() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent) // Clean transparent backdrop
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
            enter = fadeIn(tween(240, easing = XvoxBEasing)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(280, easing = XvoxBEasing)) +
                slideInVertically(initialOffsetY = { it / 16 }, animationSpec = tween(280, easing = XvoxBEasing)),
            exit = fadeOut(tween(220, easing = XvoxBEasing)) +
                scaleOut(targetScale = 0.92f, animationSpec = tween(240, easing = XvoxBEasing)) +
                slideOutVertically(targetOffsetY = { it / 16 }, animationSpec = tween(240, easing = XvoxBEasing))
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                val maxH = (maxHeight - 24.dp).coerceAtLeast(140.dp)
                val cardShape = RoundedCornerShape(24.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(max = maxH)
                        .animateContentSize(animationSpec = tween(240, easing = XvoxBEasing))
                        .clip(cardShape)
                        .background(colors.cardElevated.copy(alpha = 0.98f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), cardShape)
                        .pointerInput(Unit) { detectTapGestures(onPress = { tryAwaitRelease() }) },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        content()
                    }

                    // Top-right Close Button with clean inset padding
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.card.copy(alpha = 0.94f))
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
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}
