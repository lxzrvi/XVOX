package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
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

/** State-hoisted form used by the live Now Playing Bluetooth sheet. */
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
    SettingsControlsEditor {
        HeadsetGroupBox(title = "Audio output routing") {
            audioOutput()
        }

        // These action choices are one vertical group, rather than two narrow cards. It keeps
        // their labels readable in phone portrait and makes the opposing connection lifecycle
        // actions feel like one deliberate policy.
        HeadsetGroupBox(title = "Automatic actions") {
            Text(
                text = "Choose how XVOX reacts to wired, Bluetooth, or USB audio changes.",
                color = XvoxTheme.colors.secondaryText,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
            )
            HeadsetActionSegment(
                title = "When headset connects",
                detail = "After a private output becomes ready",
                options = listOf("none" to "Nothing", "play" to "Start playing"),
                selected = state.btConnectAction,
                onSelect = onConnectAction
            )
            HeadsetActionSegment(
                title = "When headset disconnects",
                detail = "When the private output goes away",
                options = listOf("pause" to "Pause", "keep" to "Keep playing"),
                selected = state.btDisconnectAction,
                onSelect = onDisconnectAction
            )
        }
    }
}

@Composable
private fun HeadsetGroupBox(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated.copy(alpha = .70f))
            .border(.8.dp, colors.cardBorder.copy(alpha = .78f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun HeadsetActionSegment(
    title: String,
    detail: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card.copy(alpha = .60f))
            .border(.7.dp, colors.cardBorder.copy(alpha = .64f), shape)
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = colors.primaryText, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = colors.secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (key, label) ->
                val active = selected == key
                val optionShape = RoundedCornerShape(10.dp)
                Text(
                    text = label,
                    color = if (active) colors.background else colors.primaryText,
                    fontSize = 10.5.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .clip(optionShape)
                        .background(if (active) colors.primaryAccent else colors.cardElevated)
                        .border(.7.dp, if (active) colors.primaryAccent else colors.cardBorder.copy(alpha = .65f), optionShape)
                        .xvoxPressScale { onSelect(key) }
                        .padding(horizontal = 7.dp, vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
