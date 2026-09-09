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
import com.xvox.music.features.settings.components.PinnedSettingsEditor
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.SurroundSettingsPreview
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

/**
 * 3D sound — a separate settings home of its own, with only the important spatial controls
 * (width, depth, position, room, movement and spatial headroom). Reverb lives with the
 * equalizer; the XvoxMix screen no longer mixes both.
 */
@Composable
fun ThreeDSoundSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    PinnedSettingsEditor(preview = {
        if (state.stereoWidening) {
            SurroundSettingsPreview(state)
        } else {
            Text("Turn on 3D sound to preview", color = XvoxTheme.colors.secondaryText, fontSize = 12.sp)
        }
    }, controls = {
        SettingsToggle(
            title = "3D sound",
            subtitle = "Widen, move and place the sound around you",
            checked = state.stereoWidening,
            onChange = viewModel::setStereoWidening
        )
        if (state.stereoWidening) {
            Spacer(Modifier.height(4.dp))
            EqL("Width ${(state.surroundWidth * 100).roundToInt()}%")
            XvoxThinLineSlider(state.surroundWidth, viewModel::setSurroundWidth, .05f..1f, defaultValue = .78f)
            EqL("Depth ${(state.surroundDepth * 100).roundToInt()}%")
            XvoxThinLineSlider(state.surroundDepth, viewModel::setSurroundDepth, 0f..1f, defaultValue = .65f)
            EqL("Position ${(state.surroundPosition * 57.2958f).roundToInt()}°")
            XvoxThinLineSlider(state.surroundPosition, viewModel::setSurroundPosition, -1.5f..1.5f, defaultValue = 0f)
            EqL("Room ${(state.roomAmount * 100).roundToInt()}%")
            XvoxThinLineSlider(state.roomAmount, viewModel::setRoomAmount, 0f..1f, defaultValue = .5f)
            EqL("Movement ${state.surroundPanSpeed}s per orbit")
            XvoxThinLineSlider(state.surroundPanSpeed.toFloat(), { viewModel.setSurroundPanSpeed(it.roundToInt()) }, 2f..10f, defaultValue = 6f)
            EqL("HRTF / Spatial ${(state.hrtf * 100).roundToInt()}%")
            XvoxThinLineSlider(state.hrtf, viewModel::setHrtf, 0f..1f, defaultValue = .6f)
            EqL("Center preservation ${(state.centerPreservation * 100).roundToInt()}%")
            XvoxThinLineSlider(state.centerPreservation, viewModel::setCenterPreservation, 0f..1f, defaultValue = 0f)
        }
    })
}

@Composable
private fun EqL(text: String) {
    Text(text, color = XvoxTheme.colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
}
