package com.xvox.music.features.settings.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsControlsEditor

/** Direct-persistence entry point retained for the regular Settings page. */
@Composable
fun HeadsetSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    HeadsetSettingsContent(
        state = state,
        audioOutput = { AudioOutputContent(state, viewModel) },
        onConnectAction = { key ->
            viewModel.setBtConnectAction(key)
            viewModel.setPlayOnHeadsetConnect(key == "play")
        },
        onDisconnectAction = { key ->
            viewModel.setBtDisconnectAction(key)
            viewModel.setPauseOnHeadphoneDisconnect(key == "pause")
        }
    )
}

/** Local-draft form used by the transactional Now Playing Bluetooth sheet. */
@Composable
fun HeadsetSettingsDraftSection(
    state: SettingsState,
    onStateChange: (SettingsState) -> Unit
) {
    HeadsetSettingsContent(
        state = state,
        audioOutput = { AudioOutputDraftContent(state, onStateChange) },
        onConnectAction = { key ->
            onStateChange(
                state.copy(
                    btConnectAction = key,
                    playOnHeadsetConnect = key == "play"
                )
            )
        },
        onDisconnectAction = { key ->
            onStateChange(
                state.copy(
                    btDisconnectAction = key,
                    pauseOnHeadphoneDisconnect = key == "pause"
                )
            )
        }
    )
}

@Composable
private fun HeadsetSettingsContent(
    state: SettingsState,
    audioOutput: @Composable () -> Unit,
    onConnectAction: (String) -> Unit,
    onDisconnectAction: (String) -> Unit
) {
    var expandedGroup by remember { mutableStateOf<String?>(null) }
    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(
            title = "Audio Output Routing",
            expanded = expandedGroup == "Routing",
            onToggle = { toggle("Routing") }
        ) {
            audioOutput()
        }

        SettingsAccordionItem(
            title = "When Headset Connects",
            expanded = expandedGroup == "Connect",
            onToggle = { toggle("Connect") }
        ) {
            SettingsChoiceRow(
                options = listOf("none" to "Nothing", "play" to "Start playing"),
                selected = state.btConnectAction,
                onSelect = onConnectAction
            )
        }

        SettingsAccordionItem(
            title = "When Headset Disconnects",
            expanded = expandedGroup == "Disconnect",
            onToggle = { toggle("Disconnect") }
        ) {
            SettingsChoiceRow(
                options = listOf("pause" to "Pause", "keep" to "Keep playing"),
                selected = state.btDisconnectAction,
                onSelect = onDisconnectAction
            )
        }
    })
}
