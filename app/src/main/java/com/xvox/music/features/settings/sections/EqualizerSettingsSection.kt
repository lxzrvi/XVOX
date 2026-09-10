package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.*
import kotlin.math.roundToInt

@Composable
fun EqualizerSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    var expandedGroup by remember { mutableStateOf<String?>("Equalizer") }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(
            title = "Equalizer",
            expanded = expandedGroup == "Equalizer",
            onToggle = { toggle("Equalizer") }
        ) {
            SettingsToggle("Enable equalizer", null, state.equalizerEnabled, viewModel::setEqualizerEnabled)

            if (state.equalizerEnabled) {
                Spacer(Modifier.height(8.dp))
                EqLabel("Bands")
                SettingsChoiceRow(listOf("5" to "5 bands", "10" to "10 bands"), state.eqBandCount.toString()) {
                    viewModel.setEqBandCount(it.toInt())
                }

                Spacer(Modifier.height(8.dp))
                EqLabel("Presets")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.xvox.music.audio.EqPresets.presets(state.eqBandCount).forEach { preset ->
                        val active = state.eqPreset == preset.name
                        Text(
                            preset.name,
                            color = if (active) colors.background else colors.primaryText,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (active) colors.primaryAccent else colors.card)
                                .xvoxPressScale { viewModel.setEqPreset(preset.name) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                val frequencies = com.xvox.music.audio.EqBands.frequencies(state.eqBandCount)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.card)
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    frequencies.forEachIndexed { index, frequency ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            VerticalEqBandSlider(com.xvox.music.audio.EqBands.label(frequency), state.eqBands.getOrElse(index) { 0 }) {
                                viewModel.setEqBand(index, it)
                            }
                        }
                    }
                }
            }
        }

        SettingsAccordionItem(
            title = "Reverb & Space",
            expanded = expandedGroup == "Reverb",
            onToggle = { toggle("Reverb") }
        ) {
            EqLabel("Reverb preset")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "Off" to (0f to 0.1f),
                    "Small Room" to (0.18f to 0.25f),
                    "Medium Room" to (0.32f to 0.45f),
                    "Large Room" to (0.48f to 0.65f),
                    "Hall" to (0.62f to 0.8f),
                    "Cathedral" to (0.85f to 0.95f)
                ).forEach { (name, values) ->
                    val active = kotlin.math.abs(state.reverbAmount - values.first) < .04f
                    Text(
                        name,
                        color = if (active) colors.background else colors.primaryText,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (active) colors.primaryAccent else colors.card)
                            .xvoxPressScale {
                                viewModel.setReverbAmount(values.first)
                                viewModel.setRoomAmount(values.second)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            EqLabel("Reverb amount · ${(state.reverbAmount * 100).roundToInt()}%")
            XvoxThinLineSlider(state.reverbAmount, viewModel::setReverbAmount, 0f..1f, defaultValue = 0f)

            Spacer(Modifier.height(10.dp))
            EqLabel("Room size · ${(state.roomAmount * 100).roundToInt()}%")
            XvoxThinLineSlider(state.roomAmount, viewModel::setRoomAmount, 0f..1f, defaultValue = .5f)
        }

        SettingsAccordionItem(
            title = "Clarity & Protection",
            expanded = expandedGroup == "Clarity",
            onToggle = { toggle("Clarity") }
        ) {
            EqLabel("Noise reduction · ${(state.noiseReduction * 100).roundToInt()}%")
            XvoxThinLineSlider(state.noiseReduction, viewModel::setNoiseReduction, 0f..1f)
            Spacer(Modifier.height(8.dp))
            EqLabel("Soften highs · ${(state.softenHighs * 100).roundToInt()}%")
            XvoxThinLineSlider(state.softenHighs, viewModel::setSoftenHighs, 0f..1f)
            Spacer(Modifier.height(8.dp))
            EqLabel("Boost protection · ${state.eqHeadroomDb.roundToInt()} dB")
            XvoxThinLineSlider(state.eqHeadroomDb, viewModel::setEqHeadroomDb, 0f..18f, defaultValue = 0f)
        }

        SettingsAccordionItem(
            title = "Volume & Balance",
            expanded = expandedGroup == "Volume",
            onToggle = { toggle("Volume") }
        ) {
            EqLabel("App volume · ${(state.appVolume * 100).roundToInt()}%")
            XvoxThinLineSlider(state.appVolume, viewModel::setAppVolume, 0f..1.5f, defaultValue = 1f)
            Spacer(Modifier.height(8.dp))
            EqLabel("Volume limit · ${(state.volumeLimit * 100).roundToInt()}%")
            XvoxThinLineSlider(state.volumeLimit, viewModel::setVolumeLimit, 0.1f..1f, defaultValue = 1f)
            Spacer(Modifier.height(8.dp))
            val balanceText = if (state.balance < -0.05f) "Left ${(-state.balance * 100).roundToInt()}%"
                else if (state.balance > 0.05f) "Right ${(state.balance * 100).roundToInt()}%"
                else "Center"
            EqLabel("Balance · $balanceText")
            XvoxThinLineSlider(state.balance, viewModel::setBalance, -1f..1f, defaultValue = 0f)
        }

        SettingsAccordionItem(
            title = "Speed & Pitch",
            expanded = expandedGroup == "Speed",
            onToggle = { toggle("Speed") }
        ) {
            EqLabel("Speed " + String.format("%.2f", state.playbackSpeed) + "×")
            XvoxThinLineSlider(state.playbackSpeed, viewModel::setPlaybackSpeed, .25f..3f, defaultValue = 1f)
            Spacer(Modifier.height(8.dp))
            EqLabel("Pitch " + String.format("%.2f", state.playbackPitch) + "×")
            XvoxThinLineSlider(state.playbackPitch, viewModel::setPlaybackPitch, .5f..2f, defaultValue = 1f)
        }
    })
}

@Composable
private fun EqLabel(text: String) {
    Text(
        text,
        color = XvoxTheme.colors.primaryAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
