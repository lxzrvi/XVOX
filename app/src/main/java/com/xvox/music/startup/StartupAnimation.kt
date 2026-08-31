package com.xvox.music.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun StartupAnimation(
    revealVox: Boolean,
    centerProgress: Float
) {
    val colors = XvoxTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer {
            translationX = -centerProgress * 20f
        }
    ) {
        Text(
            text = "X",
            color = colors.primaryText,
            fontFamily = XvoxLogoFont,
            fontSize = 38.sp,
            textAlign = TextAlign.Center
        )

        AnimatedVisibility(
            visible = revealVox,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 450,
                    easing = FastOutSlowInEasing
                )
            ) + slideInHorizontally(
                animationSpec = tween(
                    durationMillis = 450,
                    easing = FastOutSlowInEasing
                ),
                initialOffsetX = {
                    -it
                }
            )
        ) {
            Text(
                text = "VOX",
                color = colors.primaryText,
                fontFamily = XvoxLogoFont,
                fontSize = 38.sp
            )
        }
    }
}
