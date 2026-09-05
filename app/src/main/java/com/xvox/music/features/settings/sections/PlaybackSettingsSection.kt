package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
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
    val haptics = LocalXvoxHaptics.current

    SettingsSectionCard(title = "Playback", iconRes = R.drawable.ic_xvox_play) {
        SettingsToggle(
            title = "Gapless playback",
            subtitle = "Seamless transition between tracks",
            checked = state.gaplessPlayback
        ) {
            haptics.toggle()
            viewModel.setGaplessPlayback(it)
        }

        SettingsToggle(
            title = "Crossfade",
            subtitle = "iOS-level beat sync blend between tracks",
            checked = state.crossfade
        ) {
            haptics.toggle()
            viewModel.setCrossfade(it)
        }
        if (state.crossfade) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Duration", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                XvoxThinLineSlider(
                    value = state.crossfadeDuration.toFloat(),
                    onValueChange = {
                        haptics.sliderTick()
                        viewModel.setCrossfadeDuration(it.roundToInt())
                    },
                    valueRange = 1f..12f,
                    defaultValue = 3f,
                    modifier = Modifier.weight(1f)
                )
                Text("${state.crossfadeDuration}s", color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(32.dp))
            }
        }

        SettingsToggle(
            title = "Fade in",
            subtitle = "Gradual volume rise on playback start",
            checked = state.fadeIn
        ) {
            haptics.toggle()
            viewModel.setFadeIn(it)
        }

        SettingsToggle(
            title = "Fade out",
            subtitle = "Gradual volume fall on pause/stop",
            checked = state.fadeOut
        ) {
            haptics.toggle()
            viewModel.setFadeOut(it)
        }

        SettingsToggle(
            title = "ReplayGain",
            subtitle = "Normalize loudness via metadata tags",
            checked = state.replayGain
        ) {
            haptics.toggle()
            viewModel.setReplayGain(it)
        }

        SettingsToggle(
            title = "Loudness normalization",
            subtitle = "EBU R128 standard volume leveling",
            checked = state.loudnessNormalization
        ) {
            haptics.toggle()
            viewModel.setLoudnessNormalization(it)
        }

        SettingsToggle(
            title = "Skip silence",
            subtitle = "Trim leading and trailing silent gaps",
            checked = state.skipSilence
        ) {
            haptics.toggle()
            viewModel.setSkipSilence(it)
        }

        SettingsToggle(
            title = "Audio focus",
            subtitle = "React to phone calls, navigation & other apps",
            checked = state.audioFocus
        ) {
            haptics.toggle()
            viewModel.setAudioFocus(it)
        }

        SettingsToggle(
            title = "Pause on headphone disconnect",
            subtitle = "Auto pause when headset or Bluetooth disconnects",
            checked = state.pauseOnHeadphoneDisconnect
        ) {
            haptics.toggle()
            viewModel.setPauseOnHeadphoneDisconnect(it)
        }

        SettingsToggle(
            title = "Play on headset connect",
            subtitle = "Resume playback when headset or Bluetooth connects",
            checked = state.playOnHeadsetConnect
        ) {
            haptics.toggle()
            viewModel.setPlayOnHeadsetConnect(it)
        }

        SettingsToggle(
            title = "Clear queue after playback",
            subtitle = "Empty queue when current list completes",
            checked = state.clearQueueAfterPlayback
        ) {
            haptics.toggle()
            viewModel.setClearQueueAfterPlayback(it)
        }

        SettingsToggle(
            title = "Remember queue",
            subtitle = "Restore playing queue on next launch",
            checked = state.rememberQueue
        ) {
            haptics.toggle()
            viewModel.setRememberQueue(it)
        }
    }
}
