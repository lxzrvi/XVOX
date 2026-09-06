package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.Column
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
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsSectionCard
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun PlaybackSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors

    SettingsSectionCard(
        title = "Playback",
        iconRes = R.drawable.ic_xvox_disc
    ) {
        SettingsToggle(
            title = "Crossfade",
            subtitle = "Seamlessly blend the end of current song with next song",
            checked = state.crossfade,
            onChange = viewModel::setCrossfade
        )

        if (state.crossfade) {
            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = "Crossfade Duration: ${state.crossfadeDuration}s",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                XvoxThinLineSlider(
                    value = state.crossfadeDuration.toFloat(),
                    onValueChange = { viewModel.setCrossfadeDuration(it.roundToInt().coerceIn(1, 12)) },
                    valueRange = 1f..12f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsToggle(
            title = "Pause on Disconnect",
            subtitle = "Automatically pause when headphones or Bluetooth disconnect",
            checked = state.pauseOnHeadphoneDisconnect,
            onChange = viewModel::setPauseOnHeadphoneDisconnect
        )

        Spacer(modifier = Modifier.height(14.dp))

        SettingsToggle(
            title = "Play on Headset Connect",
            subtitle = "Resume playback automatically when Bluetooth or headphones connect",
            checked = state.playOnHeadsetConnect,
            onChange = viewModel::setPlayOnHeadsetConnect
        )
    }
}
