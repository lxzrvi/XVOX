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

/**
 * Headset. Audio stays on Auto — a Bluetooth headset or earbuds take over the moment they
 * connect and the phone pauses when they unplug. The only choices left are what happens on
 * connect and on unplug.
 */
@Composable
fun HeadsetSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors

    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Auto: Bluetooth headset plays as soon as it connects; music pauses when it is unplugged.",
            color = colors.primaryAccent,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
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
