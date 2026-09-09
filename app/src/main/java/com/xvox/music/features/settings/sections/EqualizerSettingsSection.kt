package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.audio.AudioEffectsManager
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun EqualizerSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val saveError by AudioEffectsManager.persistenceError.collectAsState()
    var previewMode by remember { androidx.compose.runtime.mutableStateOf("eq") }
    com.xvox.music.features.settings.components.PinnedSettingsEditor(preview = {
        if (state.equalizerEnabled && state.stereoWidening) com.xvox.music.features.settings.components.SettingsChoiceRow(
            listOf("eq" to "Equalizer", "space" to "3D"), previewMode) { previewMode = it }
        when {
            state.stereoWidening && (previewMode == "space" || !state.equalizerEnabled) -> com.xvox.music.features.settings.components.SurroundSettingsPreview(state)
            state.equalizerEnabled -> com.xvox.music.features.settings.components.EqSettingsPreview(state)
            else -> Text("Turn on EQ or 3D to preview", color = colors.secondaryText, fontSize = 12.sp)
        }
    }, controls = {
        saveError?.let { Text(it, color = colors.secondaryText, fontSize = 11.sp) }
        SettingsToggle("Equalizer", null, state.equalizerEnabled, viewModel::setEqualizerEnabled)
        if (state.equalizerEnabled) {
            com.xvox.music.features.settings.components.SettingsChoiceRow(listOf("5" to "5 bands", "10" to "10 bands"), state.eqBandCount.toString()) {
                viewModel.setEqBandCount(it.toInt())
            }
            Spacer(Modifier.height(12.dp))
            com.xvox.music.features.settings.components.SettingsChoiceRow(
                (AudioEffectsManager.PRESETS.keys + "Custom").map { it to it }, state.eqPreset, viewModel::setEqPreset)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(180.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                com.xvox.music.audio.EqBands.frequencies(state.eqBandCount).forEachIndexed { index, frequency ->
                    VerticalEqBandSlider(com.xvox.music.audio.EqBands.label(frequency), state.eqBands.getOrElse(index) { 0 }) {
                        previewMode = "eq"; viewModel.setEqBand(index, it)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        EqLabel("Boost protection ${state.eqHeadroomDb.roundToInt()} dB")
        XvoxThinLineSlider(state.eqHeadroomDb, { previewMode = "eq"; viewModel.setEqHeadroomDb(it) }, 0f..18f, defaultValue = 0f)
        EqLabel("Noise reduction ${(state.noiseReduction * 100).roundToInt()}%")
        XvoxThinLineSlider(state.noiseReduction, viewModel::setNoiseReduction, 0f..1f)
        EqLabel("Soften highs ${(state.softenHighs * 100).roundToInt()}%")
        XvoxThinLineSlider(state.softenHighs, viewModel::setSoftenHighs, 0f..1f)

        Spacer(Modifier.height(12.dp))
        SettingsToggle("3D sound", null, state.stereoWidening, { previewMode = "space"; viewModel.setStereoWidening(it) })
        if (state.stereoWidening) {
            EqLabel("Width ${(state.surroundWidth * 100).roundToInt()}%")
            XvoxThinLineSlider(state.surroundWidth, viewModel::setSurroundWidth, .05f..1f, defaultValue = .78f)
            EqLabel("Depth ${(state.surroundDepth * 100).roundToInt()}%")
            XvoxThinLineSlider(state.surroundDepth, viewModel::setSurroundDepth, 0f..1f, defaultValue = .65f)
            EqLabel("Position ${(state.surroundPosition * 57.2958f).roundToInt()}°")
            XvoxThinLineSlider(state.surroundPosition, viewModel::setSurroundPosition, -1.5f..1.5f, defaultValue = 0f)
            EqLabel("Room ${(state.roomAmount * 100).roundToInt()}%")
            XvoxThinLineSlider(state.roomAmount, viewModel::setRoomAmount, 0f..1f, defaultValue = .5f)
            EqLabel("Reverb ${(state.reverbAmount * 100).roundToInt()}%")
            XvoxThinLineSlider(state.reverbAmount, viewModel::setReverbAmount, 0f..1f, defaultValue = 0f)
            EqLabel("Movement ${state.surroundPanSpeed}s per orbit")
            XvoxThinLineSlider(state.surroundPanSpeed.toFloat(), { viewModel.setSurroundPanSpeed(it.roundToInt()) }, 2f..10f, defaultValue = 6f)
            EqLabel("HRTF / Spatial ${(state.hrtf * 100).roundToInt()}%")
            XvoxThinLineSlider(state.hrtf, viewModel::setHrtf, 0f..1f, defaultValue = .6f)
            EqLabel("Center preservation ${(state.centerPreservation * 100).roundToInt()}%")
            XvoxThinLineSlider(state.centerPreservation, viewModel::setCenterPreservation, 0f..1f, defaultValue = 0f)
        }

        EqLabel("App volume ${(state.appVolume * 100).roundToInt()}%")
        XvoxThinLineSlider(state.appVolume, viewModel::setAppVolume, 0f..1f)
        EqLabel("Output ceiling ${(state.volumeLimit * 100).roundToInt()}%")
        XvoxThinLineSlider(state.volumeLimit, viewModel::setVolumeLimit, 0f..1f)
        EqLabel("Balance ${if (state.balance < -.05f) "Left" else if (state.balance > .05f) "Right" else "Centre"}")
        XvoxThinLineSlider(state.balance, viewModel::setBalance, -1f..1f, defaultValue = 0f)
    })
}

@Composable
private fun EqLabel(text: String) {
    Text(text, color = XvoxTheme.colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
            .width(52.dp)
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
