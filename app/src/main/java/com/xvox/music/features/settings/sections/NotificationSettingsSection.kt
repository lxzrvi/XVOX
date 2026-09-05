package com.xvox.music.features.settings.sections

import androidx.compose.runtime.Composable
import com.xvox.music.R
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsSectionCard
import com.xvox.music.features.settings.components.SettingsToggle

@Composable
fun NotificationSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val haptics = LocalXvoxHaptics.current

    SettingsSectionCard(title = "Notification", iconRes = R.drawable.ic_xvox_settings) {
        SettingsToggle(
            title = "Media notification",
            subtitle = "Show foreground player notification with controls",
            checked = state.mediaNotification
        ) {
            haptics.toggle()
            viewModel.setMediaNotification(it)
        }
    }
}
