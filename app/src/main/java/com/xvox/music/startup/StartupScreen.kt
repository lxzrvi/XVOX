fgpackage com.xvox.music.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
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

    var reveal by remember {
        mutableStateOf(false)
    }

    var visible by remember {
        mutableStateOf(true)
    }

    val progress by animateFloatAsState(
        targetValue = if (reveal) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1100,
            easing = CubicBezierEasing(
                0.22f,
                1f,
                0.36f,
                1f
            )
        ),
        label = "xvoxReveal"
    )

    LaunchedEffect(Unit) {
        reveal = true

        delay(1100)
        delay(1000)

        visible = false

        delay(220)

        StartupState.animationShown = true
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 220
                )
            )
        ) {
            StartupAnimation(
                progress = progress
            )
        }
    }
}
