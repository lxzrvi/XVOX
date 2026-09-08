package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsToggle

@Composable
fun PlaybackSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    showPreview: Boolean = true
) {
    val colors = XvoxTheme.colors
    val overlays = com.xvox.music.core.ui.overlay.LocalXvoxOverlayController.current

    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.material3.OutlinedButton(onClick = { overlays.showBox("XvoxSplit") { com.xvox.music.split.XvoxSplitPanel() } }, modifier = Modifier.fillMaxWidth()) {
            Text("XvoxSplit · vocals / instruments")
        }
        if (showPreview) com.xvox.music.features.settings.components.CrossfadeSettingsPreview(state)
        Spacer(Modifier.height(12.dp))
        SettingsToggle(
            title = "Crossfade",
            subtitle = "Overlap current + next at equal power, including background playback",
            checked = state.crossfade,
            onChange = viewModel::setCrossfade
        )

        if (state.crossfade) {
            SettingsToggle("Smart energy blend", "Compare the outgoing tail and incoming intro graphs; prefer calmer hand-off points without skipping the intro.",
                state.crossfadeSmart, viewModel::setCrossfadeSmart)
            if (state.crossfadeSmart) {
                Text("Beat-clash control: ${(state.crossfadeClashControl * 100).toInt()}%", color = colors.secondaryText, fontSize = 12.sp)
                com.xvox.music.features.settings.components.XvoxThinLineSlider(state.crossfadeClashControl, viewModel::setCrossfadeClashControl, 0f..1f)
                Text("Stronger control hands the low end to one track at a time. No forced tempo/pitch changes.", color = colors.secondaryText, fontSize = 10.sp)
            }
            SettingsToggle("Beat alignment", "Align the incoming first beat when rhythms are confidently detected and tempos fit. Never skips the intro or waits for analysis.",
                state.crossfadeBeatSync, viewModel::setCrossfadeBeatSync)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Crossfade Duration",
                color = colors.secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            com.xvox.music.features.settings.components.XvoxThinLineSlider(
                value = state.crossfadeDuration.toFloat(), valueRange = 1f..12f,
                onValueChange = { viewModel.setCrossfadeDuration(kotlin.math.round(it).toInt()) })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val durations = listOf(2, 3, 5, 7, 10, 12)
                durations.forEach { sec ->
                    val isSelected = state.crossfadeDuration == sec
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .clickable { viewModel.setCrossfadeDuration(sec) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${sec}s",
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "When Headset / Bluetooth Disconnected",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val disconnectActions = listOf(
                "pause" to "Pause Playback",
                "keep" to "Keep Playing"
            )

            disconnectActions.forEach { (key, label) ->
                val isSelected = state.btDisconnectAction == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                        .clickable {
                            viewModel.setBtDisconnectAction(key)
                            viewModel.setPauseOnHeadphoneDisconnect(key == "pause")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.background else colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "When Headset / Bluetooth Connected",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val connectActions = listOf(
                "none" to "Do Nothing",
                "play" to "Auto Play"
            )

            connectActions.forEach { (key, label) ->
                val isSelected = state.btConnectAction == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                        .clickable {
                            viewModel.setBtConnectAction(key)
                            viewModel.setPlayOnHeadsetConnect(key == "play")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.background else colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun PlaybackSettingsEditor(state: SettingsState, viewModel: SettingsViewModel) {
    com.xvox.music.features.settings.components.PinnedSettingsEditor(
        preview = { com.xvox.music.features.settings.components.CrossfadeSettingsPreview(state) },
        controls = { PlaybackSettingsSection(state, viewModel, showPreview = false) })
}
