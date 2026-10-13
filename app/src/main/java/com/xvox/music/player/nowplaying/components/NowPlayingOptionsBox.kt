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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.features.settings.components.XvoxTransactionalFooterActions
import com.xvox.music.features.settings.sections.XvoxThinAmountSlider
import com.xvox.music.player.nowplaying.XvoxNowPlayingBackgroundStyles
import kotlin.math.roundToInt

/**
 * Three-dot overflow for the local Now Playing canvas. Its controls are deliberately draft-only:
 * background changes stay in this sheet until Okay, while Cancel/back/scrim dismisses untouched.
 */
@Composable
fun NowPlayingOptionsBox(
    backgroundStyle: String,
    coverBlur: Float,
    onApplyBackground: (style: String, coverBlur: Float) -> Unit,
    onDismiss: () -> Unit
) {
    val blurMin = 12f
    val blurMax = 50f
    // The displayed rail reads 20%–100%. 50% maps to this retained local cover-blur range.
    val resetCoverBlur = 26.25f
    val initialStyle = if (backgroundStyle == XvoxNowPlayingBackgroundStyles.COVER) {
        XvoxNowPlayingBackgroundStyles.COVER
    } else {
        XvoxNowPlayingBackgroundStyles.DEFAULT
    }
    var draftBackgroundStyle by remember(backgroundStyle) { mutableStateOf(initialStyle) }
    var draftCoverBlur by remember(coverBlur) {
        mutableFloatStateOf(coverBlur.coerceIn(blurMin, blurMax))
    }

    XvoxBox(
        onDismiss = onDismiss,
        title = "Now Playing Background",
        bottomAction = {
            XvoxTransactionalFooterActions(
                onCancel = onDismiss,
                onReset = {
                    // Reset is intentionally observable: choose Cover and place its displayed rail
                    // at exactly 50%, rather than quietly resetting a hidden Default-only value.
                    draftBackgroundStyle = XvoxNowPlayingBackgroundStyles.COVER
                    draftCoverBlur = resetCoverBlur
                },
                onOkay = {
                    onApplyBackground(draftBackgroundStyle, draftCoverBlur)
                    onDismiss()
                }
            )
        }
    ) {
        val colors = XvoxTheme.colors
        val shape = RoundedCornerShape(14.dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Choose the player background. Changes apply when you tap Okay.",
                color = colors.secondaryText,
                fontSize = 12.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                XvoxBackgroundChoice(
                    label = "Default",
                    description = "XVOX canvas",
                    selected = draftBackgroundStyle == XvoxNowPlayingBackgroundStyles.DEFAULT,
                    shape = shape,
                    modifier = Modifier.weight(1f),
                    onClick = { draftBackgroundStyle = XvoxNowPlayingBackgroundStyles.DEFAULT }
                )
                XvoxBackgroundChoice(
                    label = "Cover",
                    description = "Artwork canvas",
                    selected = draftBackgroundStyle == XvoxNowPlayingBackgroundStyles.COVER,
                    shape = shape,
                    modifier = Modifier.weight(1f),
                    onClick = { draftBackgroundStyle = XvoxNowPlayingBackgroundStyles.COVER }
                )
            }

            if (draftBackgroundStyle == XvoxNowPlayingBackgroundStyles.COVER) {
                // Storage remains in the local 12–50dp blur range. The rail speaks in an
                // intuitive 20%–100% strength scale instead of leaking implementation radii.
                val normalizedBlur = draftCoverBlur.coerceIn(blurMin, blurMax)
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
                    // Keep the established thin, thumb-free XVOX rail.
                    XvoxThinAmountSlider(
                        value = blurFraction,
                        onValueChange = { fraction ->
                            draftCoverBlur = (
                                blurMin + fraction.coerceIn(0f, 1f) * (blurMax - blurMin)
                            ).coerceIn(blurMin, blurMax)
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
    description: String,
    selected: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(if (selected) colors.accentSoft else colors.cardElevated.copy(alpha = .58f))
            .border(
                if (selected) 1.dp else .8.dp,
                if (selected) colors.primaryAccent else colors.cardBorder.copy(alpha = .84f),
                shape
            )
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = if (selected) colors.primaryAccent else colors.primaryText,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = description,
                color = if (selected) colors.primaryAccent.copy(alpha = .74f) else colors.mutedText,
                fontSize = 9.5.sp,
                maxLines = 1
            )
        }
    }
}
