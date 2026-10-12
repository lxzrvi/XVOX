package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.features.settings.sections.XvoxThinAmountSlider
import com.xvox.music.player.nowplaying.XvoxNowPlayingBackgroundStyles
import kotlin.math.roundToInt

/** Three-dot overflow: select the normal player canvas or a locally blurred current-cover canvas. */
@Composable
fun NowPlayingOptionsBox(
    backgroundStyle: String,
    coverBlur: Float,
    onBackgroundStyleChange: (String) -> Unit,
    onCoverBlurChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    XvoxBox(onDismiss = onDismiss, title = "Now Playing Background") {
        val colors = XvoxTheme.colors
        val shape = RoundedCornerShape(14.dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Choose the player background",
                color = colors.secondaryText,
                fontSize = 12.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                XvoxBackgroundChoice(
                    label = "Default",
                    selected = backgroundStyle == XvoxNowPlayingBackgroundStyles.DEFAULT,
                    shape = shape,
                    modifier = Modifier.weight(1f),
                    onClick = { onBackgroundStyleChange(XvoxNowPlayingBackgroundStyles.DEFAULT) }
                )
                XvoxBackgroundChoice(
                    label = "Cover",
                    selected = backgroundStyle == XvoxNowPlayingBackgroundStyles.COVER,
                    shape = shape,
                    modifier = Modifier.weight(1f),
                    onClick = { onBackgroundStyleChange(XvoxNowPlayingBackgroundStyles.COVER) }
                )
            }

            if (backgroundStyle == XvoxNowPlayingBackgroundStyles.COVER) {
                // Storage stays in the local 12–50dp blur range. The control deliberately speaks
                // in an intuitive 20%–100% strength scale instead of exposing implementation
                // radius numbers to listeners.
                val blurMin = 12f
                val blurMax = 50f
                val normalizedBlur = coverBlur.coerceIn(blurMin, blurMax)
                val blurFraction = (normalizedBlur - blurMin) / (blurMax - blurMin)
                val blurPercent = (20f + blurFraction * 80f).roundToInt().coerceIn(20, 100)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.cardElevated.copy(alpha = .62f))
                        .border(.8.dp, colors.cardBorder.copy(alpha = .82f), shape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cover blur",
                            color = colors.primaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$blurPercent%",
                            color = colors.secondaryText,
                            fontSize = 11.sp
                        )
                    }
                    // Reuse the thin, thumb-free EQ rail so Cover blur belongs to the same
                    // XVOX control family rather than looking like a Material-system outlier.
                    XvoxThinAmountSlider(
                        value = blurFraction,
                        onValueChange = { fraction ->
                            onCoverBlurChange(
                                (blurMin + fraction.coerceIn(0f, 1f) * (blurMax - blurMin))
                                    .coerceIn(blurMin, blurMax)
                            )
                        },
                        defaultValue = null,
                        contentDescription = "Cover blur $blurPercent percent"
                    )
                }
            }
        }
    }
}

@Composable
private fun XvoxBackgroundChoice(
    label: String,
    selected: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(if (selected) colors.accentSoft else colors.cardElevated.copy(alpha = .58f))
            .border(
                if (selected) 1.dp else .8.dp,
                if (selected) colors.primaryAccent else colors.cardBorder.copy(alpha = .84f),
                shape
            )
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) colors.primaryAccent else colors.primaryText,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
