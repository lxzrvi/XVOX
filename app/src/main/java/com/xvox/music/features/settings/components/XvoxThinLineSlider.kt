package com.xvox.music.features.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun XvoxThinLineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    defaultValue: Float? = null,
    snapRadius: Float? = null,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val totalSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)
    var localValue by remember { mutableFloatStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinish by rememberUpdatedState(onValueChangeFinished)

    LaunchedEffect(value) {
        if (!isDragging) {
            localValue = value
        }
    }

    val fraction = ((localValue - valueRange.start) / totalSpan).coerceIn(0f, 1f)
    var lastHapticStep by remember { mutableIntStateOf((fraction * 10).roundToInt()) }

    val defaultFraction = if (defaultValue != null) {
        ((defaultValue - valueRange.start) / totalSpan).coerceIn(0f, 1f)
    } else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(localValue.coerceIn(valueRange), valueRange)
                setProgress { requested ->
                    localValue = requested.coerceIn(valueRange)
                    currentOnValueChange(localValue)
                    true
                }
            }
            .pointerInput(valueRange, defaultValue) {
                detectTapGestures(
                    onPress = { },
                    onLongPress = {
                        val target = (defaultValue ?: (valueRange.start + valueRange.endInclusive) / 2f)
                            .coerceIn(valueRange)
                        localValue = target
                        haptics.heavy()
                        currentOnValueChange(target)
                        currentOnFinish?.invoke()
                    }
                ) { offset ->
                    val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    var newValue = valueRange.start + newFraction * totalSpan
                    if (defaultValue != null && abs(newValue - defaultValue) < (totalSpan * 0.05f)) {
                        newValue = defaultValue
                    }
                    localValue = newValue
                    haptics.tap()
                    currentOnValueChange(newValue)
                    currentOnFinish?.invoke()
                }
            }
            .pointerInput(valueRange, defaultValue) {
                fun settle(x: Float) {
                    val newFraction = (x / size.width.toFloat()).coerceIn(0f, 1f)
                    var newValue = valueRange.start + newFraction * totalSpan
                    if (defaultValue != null && abs(newValue - defaultValue) < (totalSpan * 0.03f)) {
                        newValue = defaultValue
                    }
                    val step = (newFraction * 10).roundToInt()
                    if (step != lastHapticStep) {
                        lastHapticStep = step
                        haptics.tap()
                    }
                    localValue = newValue
                    currentOnValueChange(newValue)
                }
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        settle(offset.x)
                    },
                    onDragEnd = {
                        isDragging = false
                        currentOnFinish?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                        currentOnFinish?.invoke()
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        settle(change.position.x)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.cardBorder)
        )

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

        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.primaryAccent)
        )
        Canvas(Modifier.fillMaxWidth().height(30.dp)) {
            drawCircle(colors.primaryAccent, 6.dp.toPx(), Offset(size.width * fraction, size.height / 2))
            drawCircle(colors.background, 2.dp.toPx(), Offset(size.width * fraction, size.height / 2))
        }
    }
}
