package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.audio.AudioEffectsManager
import com.xvox.music.audio.EqBands
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsControlsEditor
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun EqualizerSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val saveError by AudioEffectsManager.persistenceError.collectAsState()
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    SettingsControlsEditor(controls = {
        saveError?.let { Text(it, color = colors.secondaryText, fontSize = 11.sp) }

        SettingsAccordionItem(
            title = "Equalizer & Bands",
            expanded = expandedGroup == "Equalizer",
            onToggle = { toggle("Equalizer") }
        ) {
            SettingsToggle("Enable equalizer", null, state.equalizerEnabled) { on ->
                if (on) viewModel.setEqBandCount(5)
                viewModel.setEqualizerEnabled(on)
            }
            if (state.equalizerEnabled) {
                Spacer(Modifier.height(10.dp))
                SettingsChoiceRow(
                    options = (AudioEffectsManager.PRESETS.keys + "Custom").map { it to it },
                    selected = state.eqPreset,
                    onSelect = viewModel::setEqPreset
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().height(196.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.card)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Row(Modifier.fillMaxWidth().fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        EqBands.frequencies(5).forEachIndexed { index, frequency ->
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                                VerticalEqBandSlider(
                                    label = EqBands.label(frequency),
                                    value = state.eqBands.getOrElse(index) { 0 }
                                ) {
                                    viewModel.setEqBand(index, it)
                                }
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
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("Off", 0f, 0f),
                    Triple("Small Room", 0.16f, 0.25f),
                    Triple("Medium Room", 0.32f, 0.50f),
                    Triple("Large Room", 0.48f, 0.75f),
                    Triple("Hall", 0.65f, 0.85f),
                    Triple("Cathedral", 0.88f, 1.0f)
                ).forEach { (name, revAmount, roomSz) ->
                    val active = kotlin.math.abs(state.reverbAmount - revAmount) < 0.04f
                    Text(
                        text = name,
                        color = if (active) colors.background else colors.primaryText,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (active) colors.primaryAccent else colors.card)
                            .xvoxPressScale {
                                viewModel.setReverbAmount(revAmount)
                                viewModel.setRoomAmount(roomSz)
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
            EqLabel("Grain control · ${(state.softenHighs * 100).roundToInt()}%")
            XvoxThinLineSlider(state.softenHighs, viewModel::setSoftenHighs, 0f..1f)
            Spacer(Modifier.height(8.dp))
            EqLabel("Anti-clip protection · ${state.eqHeadroomDb.roundToInt()} dB")
            XvoxThinLineSlider(state.eqHeadroomDb, viewModel::setEqHeadroomDb, 0f..18f, defaultValue = 0f)
        }

        SettingsAccordionItem(
            title = "Volume",
            expanded = expandedGroup == "Volume",
            onToggle = { toggle("Volume") }
        ) {
            EqLabel("App volume · ${(state.appVolume * 100).roundToInt()}%")
            XvoxThinLineSlider(state.appVolume, viewModel::setAppVolume, 0f..1f)
        }

        SettingsAccordionItem(
            title = "Speed & Pitch",
            expanded = expandedGroup == "Speed",
            onToggle = { toggle("Speed") }
        ) {
            val speedNormal = kotlin.math.abs(state.playbackSpeed - 1f) < 0.005f
            EqLabel(if (speedNormal) "Playback speed · Normal" else "Playback speed " + String.format("%.2f", state.playbackSpeed) + "×")
            XvoxThinLineSlider(state.playbackSpeed, viewModel::setPlaybackSpeed, .5f..2f, defaultValue = 1f)
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

@Composable
fun VerticalEqBandSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val colors = XvoxTheme.colors
    val minDb = -12
    val maxDb = 12
    var localValue by remember(value) { mutableIntStateOf(value) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val fraction = ((localValue - minDb).toFloat() / (maxDb - minDb).toFloat()).coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "${if (localValue > 0) "+" else ""}$localValue",
            color = colors.primaryAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .width(32.dp)
                .height(120.dp)
                .semantics {
                    contentDescription = "$label equalizer band"
                    progressBarRangeInfo = ProgressBarRangeInfo(localValue.toFloat(), -12f..12f, 23)
                    setProgress { requested ->
                        localValue = requested.roundToInt().coerceIn(minDb, maxDb)
                        currentOnValueChange(localValue)
                        true
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val f = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                        val newDb = (minDb + f * (maxDb - minDb)).roundToInt().coerceIn(minDb, maxDb)
                        localValue = newDb
                        currentOnValueChange(newDb)
                    }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val f = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                            val newDb = (minDb + f * (maxDb - minDb)).roundToInt().coerceIn(minDb, maxDb)
                            if (newDb != localValue) {
                                localValue = newDb
                                currentOnValueChange(newDb)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.cardBorder)
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((110 * fraction).dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.primaryAccent)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (98 * fraction).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.primaryAccent)
            )
        }

        Text(
            text = label,
            color = colors.secondaryText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
