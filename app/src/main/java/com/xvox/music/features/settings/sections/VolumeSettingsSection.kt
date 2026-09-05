package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsSectionCard
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun VolumeSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    SettingsSectionCard(title = "Volume & Output", iconRes = R.drawable.ic_xvox_volume_high) {
        Text(
            text = "In-App Stream Volume: ${(state.appVolume * 100).roundToInt()}%",
            color = colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        XvoxThinLineSlider(
            value = state.appVolume,
            onValueChange = {
                haptics.sliderTick()
                viewModel.setAppVolume(it)
            },
            valueRange = 0f..1f,
            defaultValue = 1.0f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(6.dp))

        SettingsToggle(
            title = "Remember Volume",
            subtitle = "Restore volume level on next app start",
            checked = state.rememberVolume
        ) {
            haptics.toggle()
            viewModel.setRememberVolume(it)
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Volume Limiter Cap: ${(state.volumeLimit * 100).roundToInt()}%",
            color = colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        XvoxThinLineSlider(
            value = state.volumeLimit,
            onValueChange = {
                haptics.sliderTick()
                viewModel.setVolumeLimit(it)
            },
            valueRange = 0.5f..1.0f,
            defaultValue = 1.0f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
