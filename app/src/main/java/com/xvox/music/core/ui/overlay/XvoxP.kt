package com.xvox.music.core.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay

private val XvoxPEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun XvoxP(
    message: XvoxPopupMessage,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    var visible by remember(message.id) {
        mutableStateOf(false)
    }

    LaunchedEffect(message.id) {
        visible = true
        delay(2600L)
        visible = false
        delay(220L)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp),
        enter = fadeIn(tween(220, easing = XvoxPEasing)) +
            slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(260, easing = XvoxPEasing)
            ),
        exit = fadeOut(tween(220, easing = XvoxPEasing)) +
            slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(240, easing = XvoxPEasing)
            )
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(colors.cardElevated.copy(alpha = 0.96f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 9.dp)
        ) {
            Text(
                text = message.text,
                color = colors.primaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 15.sp
            )
        }
    }
}
