package com.xvox.music.features.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

    LaunchedEffect(value, isDragging) {
        if (!isDragging) {
            localValue = value
        }
    }

    val fraction = ((localValue - valueRange.start) / totalSpan).coerceIn(0f, 1f)
    var lastHapticStep by remember { mutableIntStateOf((fraction * 12).roundToInt()) }

    val defaultFraction = if (defaultValue != null) {
        ((defaultValue - valueRange.start) / totalSpan).coerceIn(0f, 1f)
    } else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(localValue.coerceIn(valueRange), valueRange)
                setProgress { requested ->
                    localValue = requested.coerceIn(valueRange)
                    currentOnValueChange(localValue)
                    true
                }
            }
            .pointerInput(valueRange, defaultValue) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isDragging = true

                    fun updatePosition(x: Float) {
                        val thumbRadius = 7.dp.toPx()
                        val availableWidth = (size.width - thumbRadius * 2f).coerceAtLeast(1f)
                        val newFraction = ((x - thumbRadius) / availableWidth).coerceIn(0f, 1f)
                        var newValue = valueRange.start + newFraction * totalSpan
                        if (defaultValue != null && abs(newValue - defaultValue) < (totalSpan * 0.04f)) {
                            newValue = defaultValue
                        }
                        val step = (newFraction * 12).roundToInt()
                        if (step != lastHapticStep) {
                            lastHapticStep = step
                            haptics.tap()
                        }
                        localValue = newValue
                        currentOnValueChange(newValue)
                    }

                    updatePosition(down.position.x)

                    while (true) {
                        val event = awaitPointerEvent()
                        val drag = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!drag.pressed) {
                            drag.consume()
                            break
                        }
                        drag.consume()
                        updatePosition(drag.position.x)
                    }

                    isDragging = false
                    currentOnFinish?.invoke()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        ) {
            val thumbRadius = 6.5.dp.toPx()
            val availableWidth = size.width - thumbRadius * 2f
            val trackHeight = 4.dp.toPx()
            val cy = size.height / 2f
            val trackStart = thumbRadius

            // Inactive track (full width)
            drawRoundRect(
                color = colors.cardBorder,
                topLeft = Offset(trackStart, cy - trackHeight / 2f),
                size = Size(availableWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )

            // Active track fill
            val activeWidth = availableWidth * fraction
            if (activeWidth > 0f) {
                drawRoundRect(
                    color = colors.primaryAccent,
                    topLeft = Offset(trackStart, cy - trackHeight / 2f),
                    size = Size(activeWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                )
            }

            // Default marker point (circle)
            if (defaultFraction != null) {
                val defaultX = trackStart + availableWidth * defaultFraction
                drawCircle(
                    color = colors.primaryText.copy(alpha = 0.70f),
                    radius = 3.dp.toPx(),
                    center = Offset(defaultX, cy)
                )
            }

            // Thumb (stays completely inside [trackStart, trackEnd], 0% cropped!)
            val thumbX = trackStart + availableWidth * fraction
            drawCircle(
                color = colors.primaryAccent,
                radius = thumbRadius,
                center = Offset(thumbX, cy)
            )
            drawCircle(
                color = colors.background,
                radius = 2.2.dp.toPx(),
                center = Offset(thumbX, cy)
            )
        }
    }
}
