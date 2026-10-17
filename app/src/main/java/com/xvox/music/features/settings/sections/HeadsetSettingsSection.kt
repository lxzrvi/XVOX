package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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

        // Connection and disconnection behavior are independent but equally important. Put their
        // complete controls side by side so neither action spills onto a detached next line.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeadsetActionBox(
                title = "When headset connects",
                detail = "Choose what XVOX does after a wired, Bluetooth, or USB route is ready.",
                options = listOf("none" to "Nothing", "play" to "Start playing"),
                selected = state.btConnectAction,
                onSelect = onConnectAction,
                modifier = Modifier.weight(1f)
            )
            HeadsetActionBox(
                title = "When headset disconnects",
                detail = "Choose whether playback pauses when the private route goes away.",
                options = listOf("pause" to "Pause", "keep" to "Keep playing"),
                selected = state.btDisconnectAction,
                onSelect = onDisconnectAction,
                modifier = Modifier.weight(1f)
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
private fun HeadsetActionBox(
    title: String,
    detail: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.cardElevated.copy(alpha = .70f))
            .border(.8.dp, colors.cardBorder.copy(alpha = .78f), shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(title, color = colors.primaryText, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        Text(
            detail,
            color = colors.secondaryText,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            minLines = 3,
            maxLines = 3
        )
        options.forEach { (key, label) ->
            val active = selected == key
            val optionShape = RoundedCornerShape(11.dp)
            Text(
                text = label,
                color = if (active) colors.background else colors.primaryText,
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(optionShape)
                    .background(if (active) colors.primaryAccent else colors.card.copy(alpha = .78f))
                    .border(.7.dp, if (active) colors.primaryAccent else colors.cardBorder.copy(alpha = .65f), optionShape)
                    .xvoxPressScale { onSelect(key) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
