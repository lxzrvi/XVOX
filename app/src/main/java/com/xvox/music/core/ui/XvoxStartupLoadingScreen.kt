package com.xvox.music.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme

/**
 * Determinate startup screen built around the real XVOX mark.
 *
 * The bar is fed by actual startup milestones and creeps between them, so a slow stage still
 * looks alive. It never sits at a fixed value and never waits on an artificial timer.
 */
@Composable
fun XvoxStartupLoadingScreen(
    progress: Float = 0f,
    stage: String = ""
) {
    val colors = XvoxTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "startup")

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_breathe"
    )

    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "startup_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.xvox_mark),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.primaryText),
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer { scaleX = breathe; scaleY = breathe }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "XVOX",
                fontFamily = XvoxLogoFont,
                fontSize = 20.sp,
                letterSpacing = 7.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText
            )

            Spacer(Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .width(168.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.primaryText.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shown.coerceIn(0.02f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.primaryAccent)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stage.ifBlank { "Preparing" },
                color = colors.secondaryText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(20.dp)
            ) {
                val heights = listOf(11.dp, 18.dp, 8.dp, 15.dp, 7.dp)
                val durations = listOf(600, 800, 500, 750, 650)

                heights.forEachIndexed { index, targetHeight ->
                    val barScale by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durations[index], easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bar_$index"
                    )

                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(targetHeight)
                            .graphicsLayer { scaleY = barScale }
                            .background(
                                color = colors.primaryAccent.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}
