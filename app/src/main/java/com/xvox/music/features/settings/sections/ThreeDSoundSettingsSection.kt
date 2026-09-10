package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsControlsEditor
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun ThreeDSoundSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    var expandedGroup by remember { mutableStateOf<String?>("Spatial") }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    SettingsControlsEditor(controls = {
        SettingsToggle(
            title = "3D sound",
            subtitle = "Widen, move and place the sound around you",
            checked = state.stereoWidening,
            onChange = viewModel::setStereoWidening
        )

        if (state.stereoWidening) {
            SettingsAccordionItem(
                title = "Spatial Stage",
                expanded = expandedGroup == "Spatial",
                onToggle = { toggle("Spatial") }
            ) {
                EqL("Width · ${(state.surroundWidth * 100).roundToInt()}%")
                XvoxThinLineSlider(state.surroundWidth, viewModel::setSurroundWidth, .05f..1f, defaultValue = .78f)
                Spacer(Modifier.height(8.dp))
                EqL("Depth · ${(state.surroundDepth * 100).roundToInt()}%")
                XvoxThinLineSlider(state.surroundDepth, viewModel::setSurroundDepth, 0f..1f, defaultValue = .65f)
                Spacer(Modifier.height(8.dp))
                EqL("Position · ${(state.surroundPosition * 57.2958f).roundToInt()}°")
                XvoxThinLineSlider(state.surroundPosition, viewModel::setSurroundPosition, -1.5f..1.5f, defaultValue = 0f)
            }

            SettingsAccordionItem(
                title = "Motion & Orbit",
                expanded = expandedGroup == "Motion",
                onToggle = { toggle("Motion") }
            ) {
                EqL("Movement · ${state.surroundPanSpeed}s per orbit")
                XvoxThinLineSlider(state.surroundPanSpeed.toFloat(), { viewModel.setSurroundPanSpeed(it.roundToInt()) }, 2f..10f, defaultValue = 6f)
            }

            SettingsAccordionItem(
                title = "Acoustics & Center",
                expanded = expandedGroup == "Acoustics",
                onToggle = { toggle("Acoustics") }
            ) {
                EqL("HRTF / Spatial · ${(state.hrtf * 100).roundToInt()}%")
                XvoxThinLineSlider(state.hrtf, viewModel::setHrtf, 0f..1f, defaultValue = .6f)
                Spacer(Modifier.height(8.dp))
                EqL("Center preservation · ${(state.centerPreservation * 100).roundToInt()}%")
                XvoxThinLineSlider(state.centerPreservation, viewModel::setCenterPreservation, 0f..1f, defaultValue = 0f)
            }
        }
    })
}

@Composable
private fun EqL(text: String) {
    Text(
        text,
        color = XvoxTheme.colors.primaryAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    )
}
