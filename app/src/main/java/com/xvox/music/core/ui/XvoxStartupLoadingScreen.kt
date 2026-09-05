package com.xvox.music.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxStartupLoadingScreen(
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "startup_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "XVOX",
                fontFamily = XvoxLogoFont,
                fontSize = 28.sp,
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText,
                modifier = Modifier.scale(pulseScale)
            )

            Spacer(Modifier.height(32.dp))

            // Animated Waveform Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heights = listOf(14.dp, 26.dp, 36.dp, 22.dp, 12.dp)
                heights.forEachIndexed { index, targetHeight ->
                    val barScale by infiniteTransition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(550 + index * 110, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bar_$index"
                    )

                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(targetHeight * barScale)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.primaryAccent.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}
