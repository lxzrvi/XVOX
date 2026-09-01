package com.xvox.music.core.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun XvoxNavigationItem(
    destination: XvoxDestination,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors =
        XvoxTheme.colors

    val interaction =
        remember {
            MutableInteractionSource()
        }

    val pressed by
        interaction
            .collectIsPressedAsState()

    val motion =
        rememberNavigationItemMotion(
            active = active,
            pressed = pressed
        )

    val color =
        navigationColor(
            inactive =
                colors.mutedText,
            active =
                colors.primaryText,
            progress =
                motion.progress
        )

    Box(
        modifier = Modifier
            .width(
                XvoxNavigationGeometry
                    .slotWidth
            )
            .fillMaxSize()
            .clickable(
                interactionSource =
                    interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment =
            Alignment.Center
    ) {
        XvoxNavigationIcon(
            destination =
                destination,
            color =
                color,
            modifier = Modifier
                .size(
                    XvoxNavigationGeometry
                        .iconSize
                )
                .graphicsLayer {
                    translationX =
                        XvoxNavigationGeometry
                            .activeIconCenter(
                                destination
                            )
                            .toPx() *
                            motion
                                .easedProgress

                    scaleX =
                        motion.pressScale

                    scaleY =
                        motion.pressScale
                }
        )

        XvoxNavigationLabel(
            destination =
                destination,
            alpha =
                motion.labelAlpha,
            progress =
                motion.easedProgress,
            color =
                color
        )
    }
}

@Composable
private fun XvoxNavigationLabel(
    destination: XvoxDestination,
    alpha: Float,
    progress: Float,
    color: androidx.compose.ui.graphics.Color
) {
    val labelWidth =
        XvoxNavigationGeometry
            .labelWidth(
                destination
            )

    val finalCenter =
        XvoxNavigationGeometry
            .activeLabelCenter(
                destination
            )

    Text(
        text =
            destination.label,
        color =
            color,
        fontSize = 14.sp,
        lineHeight = 17.sp,
        fontWeight =
            FontWeight.SemiBold,
        maxLines = 1,
        overflow =
            TextOverflow.Visible,
        modifier = Modifier
            .width(labelWidth)
            .graphicsLayer {
                this.alpha =
                    alpha

                translationX =
                    finalCenter.toPx() +
                        (
                            1f -
                                progress
                            ) *
                        5.dp.toPx()

                val scale =
                    0.97f +
                        0.03f *
                        progress

                scaleX =
                    scale

                scaleY =
                    scale
            }
    )
}
