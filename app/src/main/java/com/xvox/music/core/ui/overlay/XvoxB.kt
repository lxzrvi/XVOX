package com.xvox.music.core.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay

@Composable
fun XvoxB(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors

    var visible by remember {
        mutableStateOf(false)
    }

    suspend fun close() {
        visible = false
        delay(150L)
        onDismiss()
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    BackHandler {
        visible = false
        onDismiss()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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
                        visible = false
                        onDismiss()
                    }
                }
        )

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                tween(180)
            ),
            exit = fadeOut(
                tween(150)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp
                    )
                    .background(
                        colors.background.copy(
                            alpha = 0.97f
                        ),
                        RoundedCornerShape(
                            24.dp
                        )
                    )
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}
