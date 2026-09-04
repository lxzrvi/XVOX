package com.xvox.music.core.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay

private val XvoxPEasing =
    CubicBezierEasing(
        0.20f,
        0.75f,
        0.22f,
        1f
    )

@Composable
fun XvoxP(
    message: XvoxPopupMessage,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    var visible by remember(
        message.id
    ) {
        mutableStateOf(false)
    }

    LaunchedEffect(message.id) {
        visible = true
        delay(3000L)
        visible = false
        delay(190L)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.statusBars
            )
            .padding(top = 7.dp),
        enter =
            fadeIn(
                tween(150)
            ) +
                slideInVertically(
                    initialOffsetY = {
                        -it
                    },
                    animationSpec =
                        tween(
                            210,
                            easing =
                                XvoxPEasing
                        )
                ),
        exit =
            fadeOut(
                tween(150)
            ) +
                slideOutVertically(
                    targetOffsetY = {
                        -it
                    },
                    animationSpec =
                        tween(
                            190,
                            easing =
                                XvoxPEasing
                        )
                )
    ) {
        Box(
            modifier = Modifier
                .background(
                    colors.cardElevated,
                    RoundedCornerShape(
                        20.dp
                    )
                )
                .padding(
                    horizontal = 15.dp,
                    vertical = 8.dp
                )
        ) {
            Text(
                text = message.text,
                color = colors.primaryText,
                fontSize = 11.sp,
                lineHeight = 13.sp
            )
        }
    }
}
