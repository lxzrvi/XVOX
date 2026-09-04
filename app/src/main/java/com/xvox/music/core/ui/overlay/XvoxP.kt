package com.xvox.music.core.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
        delay(180L)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.statusBars
            )
            .padding(top = 10.dp),
        enter =
            fadeIn(
                tween(180)
            ) +
                slideInVertically(
                    initialOffsetY = {
                        -it / 2
                    },
                    animationSpec =
                        tween(
                            220,
                            easing =
                                FastOutSlowInEasing
                        )
                ),
        exit =
            fadeOut(
                tween(150)
            ) +
                slideOutVertically(
                    targetOffsetY = {
                        -it / 2
                    },
                    animationSpec =
                        tween(180)
                )
    ) {
        Box(
            modifier = Modifier
                .background(
                    colors.cardElevated.copy(
                        alpha = 0.96f
                    ),
                    RoundedCornerShape(
                        24.dp
                    )
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 11.dp
                )
        ) {
            Text(
                text = message.text,
                color = colors.primaryText,
                fontSize = 12.sp
            )
        }
    }
}
