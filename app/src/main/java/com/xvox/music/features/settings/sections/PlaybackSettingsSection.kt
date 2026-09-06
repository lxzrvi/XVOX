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
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsToggle(
            title = "Crossfade",
            subtitle = "Seamless linear volume crossfade between consecutive tracks",
            checked = state.crossfade,
            onChange = viewModel::setCrossfade
        )

        if (state.crossfade) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Crossfade Duration",
                color = colors.secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

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
