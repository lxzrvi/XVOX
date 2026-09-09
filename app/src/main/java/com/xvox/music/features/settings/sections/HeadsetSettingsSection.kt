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
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsSectionCard
import com.xvox.music.features.settings.components.SettingsToggle

/**
 * Headset. First choose where a song should play — always the headset, always the phone speaker,
 * or Auto (the phone decides, pausing when nothing is plugged in). Below that: what happens when
 * a headset connects and when it unplugs. The same three choices are reachable from Now Playing's
 * three-dot menu.
 */
@Composable
fun HeadsetSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors

    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
        SettingsChoiceRow(
            listOf("auto" to "Auto", "headset" to "Headset", "phone" to "Phone"),
            state.audioOutputRoute
        ) { route -> viewModel.setAudioOutputRoute(route) }
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (state.audioOutputRoute) {
                "headset" -> "Music always stays in your headset/earbuds."
                "phone" -> "Music plays on the phone speaker even when a headset is connected."
                else -> "Music follows your headset; pausing when it is unplugged."
            },
            color = colors.secondaryText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        SettingsSectionCard(title = "Headset connected") {
            SettingsChoiceRow(
                listOf("none" to "Nothing", "play" to "Start playing"),
                state.btConnectAction
            ) { key ->
                viewModel.setBtConnectAction(key)
                viewModel.setPlayOnHeadsetConnect(key == "play")
            }
        }

        Spacer(Modifier.height(10.dp))
        SettingsSectionCard(title = "Headset unplugged") {
            SettingsChoiceRow(
                listOf("pause" to "Pause", "keep" to "Keep playing"),
                state.btDisconnectAction
            ) { key ->
                viewModel.setBtDisconnectAction(key)
                viewModel.setPauseOnHeadphoneDisconnect(key == "pause")
            }
        }

    }
}

@Composable
private fun HeadLabel(text: String) {
    Text(text, color = XvoxTheme.colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}
