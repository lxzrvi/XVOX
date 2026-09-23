package com.xvox.music.features.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A compact, discrete XVOX value picker.
 *
 * Unlike a conventional continuous slider, [XvoxSlider] makes each selectable stop visible:
 * the white, filled part of the track is the active value, while the remaining dark part keeps
 * its subtle step dividers. The minus and plus buttons move one stop at a time and the track can
 * still be tapped or dragged directly.
 */
@Composable
fun XvoxSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    defaultValue: Float? = null,
    snapRadius: Float? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    steps: Int? = null,
    valueLabel: (Float) -> String = ::defaultXvoxSliderValueLabel,
    enabled: Boolean = true
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val rangeStart = valueRange.start
    val rangeEnd = valueRange.endInclusive
    val rangeSpan = (rangeEnd - rangeStart).coerceAtLeast(0.001f)
    val stepCount = (steps ?: XvoxSliderDefaults.stepCountFor(valueRange)).coerceIn(1, 24)
    val boundedValue = value.coerceIn(valueRange)

    fun valueForStep(step: Int): Float =
        (rangeStart + rangeSpan * (step.coerceIn(0, stepCount).toFloat() / stepCount)).coerceIn(valueRange)

    fun stepForValue(rawValue: Float): Int =
        (((rawValue.coerceIn(valueRange) - rangeStart) / rangeSpan) * stepCount)
            .roundToInt()
            .coerceIn(0, stepCount)

    var selectedStep by remember(stepCount, rangeStart, rangeEnd) {
        mutableIntStateOf(stepForValue(boundedValue))
    }
    var isTracking by remember { mutableStateOf(false) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinish by rememberUpdatedState(onValueChangeFinished)

    LaunchedEffect(boundedValue, stepCount, rangeStart, rangeEnd, isTracking) {
        if (!isTracking) selectedStep = stepForValue(boundedValue)
    }

    fun selectStep(step: Int, provideHaptic: Boolean = true) {
        val clampedStep = step.coerceIn(0, stepCount)
        if (clampedStep == selectedStep) return
        selectedStep = clampedStep
        if (provideHaptic) haptics.tap()
        currentOnValueChange(valueForStep(clampedStep))
    }

    val selectedValue = valueForStep(selectedStep)
    val selectedFraction = selectedStep.toFloat() / stepCount.toFloat()
    val selectedLabel = valueLabel(selectedValue)
    val trackShape = RoundedCornerShape(22.dp)
    val fillShape = if (selectedStep >= stepCount) {
        trackShape
    } else {
        RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 0.dp, bottomEnd = 0.dp)
    }
    val trackColor = if (colors.isLight) Color(0xFF050505) else Color.Black
    val fillColor = Color.White
    val fillTextColor = Color(0xFF080808)
    val inactiveTextColor = Color.White.copy(alpha = 0.88f)
    val trackBorderColor = if (colors.isLight) Color.Black else colors.cardBorder.copy(alpha = 0.85f)
    val dividerColor = Color(0xFF8E8E93).copy(alpha = if (colors.isLight) 0.66f else 0.58f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        XvoxSliderStepButton(
            symbol = "\u2212",
            description = "Decrease value",
            enabled = enabled && selectedStep > 0,
            onClick = {
                selectStep(selectedStep - 1)
                currentOnFinish?.invoke()
            }
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(trackShape)
                .background(trackColor)
                .border(1.dp, trackBorderColor, trackShape)
                .semantics {
                    contentDescription = "Selected value $selectedLabel"
                    progressBarRangeInfo = ProgressBarRangeInfo(selectedValue, valueRange, stepCount - 1)
                    setProgress { requested ->
                        val requestedStep = stepForValue(requested)
                        selectStep(requestedStep, provideHaptic = false)
                        true
                    }
                }
                .pointerInput(enabled, stepCount, rangeStart, rangeEnd, defaultValue, snapRadius) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        isTracking = true

                        fun stepForPosition(x: Float): Int {
                            val fraction = (x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            return (fraction * stepCount).roundToInt().coerceIn(0, stepCount)
                        }

                        selectStep(stepForPosition(down.position.x))
                        val downTime = System.currentTimeMillis()
                        val startX = down.position.x
                        var resetToDefault = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!pointer.pressed) {
                                pointer.consume()
                                break
                            }
                            pointer.consume()

                            // Preserve the useful legacy gesture: hold a stationary slider to
                            // return it to its documented default value.
                            if (!resetToDefault && defaultValue != null && System.currentTimeMillis() - downTime >= 360L) {
                                val distance = abs(pointer.position.x - startX)
                                if (distance < 14.dp.toPx()) {
                                    resetToDefault = true
                                    selectedStep = stepForValue(defaultValue)
                                    haptics.heavy()
                                    currentOnValueChange(valueForStep(selectedStep))
                                    continue
                                }
                            }

                            if (!resetToDefault) selectStep(stepForPosition(pointer.position.x))
                        }

                        isTracking = false
                        currentOnFinish?.invoke()
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // The selected portion is deliberately placed below the divisions. This means every
            // divider up to and including the selected step is hidden beneath the white fill.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(selectedFraction)
                    .clip(fillShape)
                    .background(fillColor),
                contentAlignment = Alignment.Center
            ) {
                if (selectedStep > 0) {
                    Text(
                        text = selectedLabel,
                        color = fillTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Draw only the unselected dividers after the fill. A divider at the edge of the
            // filled portion is intentionally omitted so it cannot cut through the active value.
            (selectedStep + 1 until stepCount).forEach { divider ->
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = maxWidth * (divider.toFloat() / stepCount) - 0.5.dp)
                        .width(1.dp)
                        .height(22.dp)
                        .background(dividerColor)
                )
            }

            // Zero has no visible fill area, so the current value remains readable on the dark
            // base track instead of being clipped into a zero-width child.
            if (selectedStep == 0) {
                Text(
                    text = selectedLabel,
                    color = inactiveTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        XvoxSliderStepButton(
            symbol = "+",
            description = "Increase value",
            enabled = enabled && selectedStep < stepCount,
            onClick = {
                selectStep(selectedStep + 1)
                currentOnFinish?.invoke()
            }
        )
    }
}

@Composable
private fun XvoxSliderStepButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val fill = if (enabled) Color.Black else Color.Black.copy(alpha = 0.34f)
    val icon = if (enabled) Color.White else Color.White.copy(alpha = 0.38f)
    val outline = if (colors.isLight) Color.Black else colors.cardBorder.copy(alpha = 0.85f)

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, outline, CircleShape)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(21.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val halfLength = size.width * 0.28f
            val stroke = 4.dp.toPx()
            drawLine(
                color = icon,
                start = Offset(center.x - halfLength, center.y),
                end = Offset(center.x + halfLength, center.y),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            if (symbol == "+") {
                drawLine(
                    color = icon,
                    start = Offset(center.x, center.y - halfLength),
                    end = Offset(center.x, center.y + halfLength),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/** Default step density keeps the track readable while still offering practical value increments. */
object XvoxSliderDefaults {
    fun stepCountFor(valueRange: ClosedFloatingPointRange<Float>): Int {
        val span = abs(valueRange.endInclusive - valueRange.start)
        return when {
            // Six broad choices are the baseline shown in the XVOX control reference.
            span <= 0.5f -> 9
            span <= 1.1f -> 10
            span <= 2.1f -> 8
            span <= 4.1f -> 8
            span <= 12f -> 6
            span <= 32f -> 8
            span <= 64f -> 8
            span <= 120f -> 10
            span <= 300f -> 12
            span <= 2_000f -> 12
            else -> 10
        }
    }
}

private fun defaultXvoxSliderValueLabel(value: Float): String {
    val whole = value.roundToInt()
    return when {
        abs(value - whole) < 0.001f -> whole.toString()
        abs(value * 10f - (value * 10f).roundToInt()) < 0.001f ->
            String.format(Locale.US, "%.1f", value)
        else -> String.format(Locale.US, "%.2f", value)
    }
}
