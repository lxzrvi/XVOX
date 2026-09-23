package com.xvox.music.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xvox.music.features.settings.components.XvoxSlider as CoreXvoxSlider

/** Public settings-package forwarding entry point for the reusable stepped slider. */
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
    enabled: Boolean = true
) {
    CoreXvoxSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        defaultValue = defaultValue,
        snapRadius = snapRadius,
        onValueChangeFinished = onValueChangeFinished,
        steps = steps,
        enabled = enabled
    )
}

/** Legacy public name retained so existing feature modules stay source-compatible. */
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
