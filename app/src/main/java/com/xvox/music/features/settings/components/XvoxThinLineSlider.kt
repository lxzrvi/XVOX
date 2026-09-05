package com.xvox.music.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics

@Composable
fun XvoxThinLineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    defaultValue: Float? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val totalSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)
    val fraction = ((value - valueRange.start) / totalSpan).coerceIn(0f, 1f)
    val snapThreshold = totalSpan * 0.045f

    val defaultFraction = if (defaultValue != null) {
        ((defaultValue - valueRange.start) / totalSpan).coerceIn(0f, 1f)
    } else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(valueRange, defaultValue) {
                detectTapGestures { offset ->
                    val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    var newValue = valueRange.start + newFraction * totalSpan
                    if (defaultValue != null && kotlin.math.abs(newValue - defaultValue) < snapThreshold) {
                        newValue = defaultValue
                        haptics.sliderTick()
                    }
                    onValueChange(newValue)
                }
            }
            .pointerInput(valueRange, defaultValue) {
                detectDragGestures { change, _ ->
                    val newFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    var newValue = valueRange.start + newFraction * totalSpan
                    if (defaultValue != null && kotlin.math.abs(newValue - defaultValue) < snapThreshold) {
                        if (value != defaultValue) {
                            haptics.sliderTick()
                        }
                        newValue = defaultValue
                    }
                    onValueChange(newValue)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Inactive background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.cardBorder)
        )

        // Default value vertical notch indicator / symbol
        if (defaultFraction != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(defaultFraction)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(10.dp)
                        .fillMaxWidth(0.015f)
                        .clip(RoundedCornerShape(1.dp))
                        .background(colors.primaryText.copy(alpha = 0.65f))
                )
            }
        }

        // Active accent track (NO THUMB CIRCLE, NO DOT)
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.primaryAccent)
        )
    }
}
