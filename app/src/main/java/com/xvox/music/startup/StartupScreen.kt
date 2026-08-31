package com.xvox.music.startup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay

@Composable
fun StartupScreen(
    onFinished: () -> Unit
) {
    val colors = XvoxTheme.colors

    var revealVox by remember {
        mutableStateOf(false)
    }

    var centered by remember {
        mutableStateOf(false)
    }

    val centerProgress by animateFloatAsState(
        targetValue = if (centered) 1f else 0f,
        animationSpec = tween(
            durationMillis = 450
        ),
        label = "startupCenter"
    )

    LaunchedEffect(Unit) {
        delay(300)

        revealVox = true
        centered = true

        delay(1450)

        StartupState.animationShown = true
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        StartupAnimation(
            revealVox = revealVox,
            centerProgress = centerProgress
        )
    }
}
