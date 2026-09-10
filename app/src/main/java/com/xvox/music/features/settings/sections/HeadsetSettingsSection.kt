package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsControlsEditor

@Composable
fun HeadsetSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    var expandedGroup by remember { mutableStateOf<String?>("Routing") }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(
            title = "Audio output routing",
            expanded = expandedGroup == "Routing",
            onToggle = { toggle("Routing") }
        ) {
            AudioOutputContent(state, viewModel)
        }

        SettingsAccordionItem(
            title = "When headset connects",
            expanded = expandedGroup == "Connect",
            onToggle = { toggle("Connect") }
        ) {
            SettingsChoiceRow(
                listOf("none" to "Nothing", "play" to "Start playing"),
                state.btConnectAction
            ) { key ->
                viewModel.setBtConnectAction(key)
                viewModel.setPlayOnHeadsetConnect(key == "play")
            }
        }

        SettingsAccordionItem(
            title = "When headset disconnects",
            expanded = expandedGroup == "Disconnect",
            onToggle = { toggle("Disconnect") }
        ) {
            SettingsChoiceRow(
                listOf("pause" to "Pause", "keep" to "Keep playing"),
                state.btDisconnectAction
            ) { key ->
                viewModel.setBtDisconnectAction(key)
                viewModel.setPauseOnHeadphoneDisconnect(key == "pause")
            }
        }
    })
}
