package com.xvox.music.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xvox.music.features.settings.components.SettingsSectionCard
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider as CoreThinLineSlider

@Composable
fun XvoxThinLineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    defaultValue: Float? = null
) {
    CoreThinLineSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        defaultValue = defaultValue
    )
}
