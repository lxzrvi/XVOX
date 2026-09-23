package com.xvox.music.features.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Compatibility bridge for older callers.
 *
 * The former thin continuous slider has been replaced by [XvoxSlider]. Keeping this forwarding
 * function avoids breaking any settings extension that still imports the old name while ensuring
 * it receives the same stepped XVOX control.
 */
@Deprecated(
    message = "Use XvoxSlider for the stepped XVOX slider treatment",
    replaceWith = ReplaceWith("XvoxSlider(value, onValueChange, valueRange, modifier, defaultValue, snapRadius, onValueChangeFinished)")
)
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
    XvoxSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        defaultValue = defaultValue,
        snapRadius = snapRadius,
        onValueChangeFinished = onValueChangeFinished
    )
}
