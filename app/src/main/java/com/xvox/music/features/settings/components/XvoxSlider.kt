package com.xvox.music.features.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Shared settings slider in the Equalizer's thin, continuous rail language.
 *
 * This intentionally replaces the former heavy stepped rail and +/- button pair everywhere it
 * was used. Callers can still opt into discrete values with [steps], retain an exact default
 * marker, preserve a default snap radius, and receive their finish callback after a gesture.
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
    val latestChange = rememberUpdatedState(onValueChange)
    val latestFinish = rememberUpdatedState(onValueChangeFinished)
    val start = valueRange.start
    val end = valueRange.endInclusive
    val span = (end - start).coerceAtLeast(.0001f)

    fun normalize(raw: Float): Float {
        var next = raw.coerceIn(valueRange)
        // Only an explicit steps argument requests quantisation. The default is intentionally a
        // smooth Equalizer-style rail rather than the legacy plus/minus picker.
        steps?.takeIf { it > 0 }?.let { count ->
            val fraction = ((next - start) / span).coerceIn(0f, 1f)
            next = (start + (fraction * count).roundToInt() * span / count).coerceIn(valueRange)
        }
        val default = defaultValue?.coerceIn(valueRange)
        if (default != null && snapRadius != null && abs(next - default) <= snapRadius) {
            next = default
        }
        return next
    }

    val displayed = normalize(value)
    XvoxContinuousSlider(
        value = displayed,
        onValueChange = { raw -> latestChange.value(normalize(raw)) },
        valueRange = valueRange,
        modifier = modifier,
        defaultValue = defaultValue,
        enabled = enabled,
        // Keep an accessible human-readable value even though the visual value label was removed
        // along with the thick legacy rail.
        contentDescription = "Selected value ${valueLabel(displayed)}",
        onValueChangeFinished = { latestFinish.value?.invoke() }
    )
}

private fun defaultXvoxSliderValueLabel(value: Float): String {
    val whole = value.roundToInt()
    return if (abs(value - whole) < .001f) whole.toString() else value.toString()
}
