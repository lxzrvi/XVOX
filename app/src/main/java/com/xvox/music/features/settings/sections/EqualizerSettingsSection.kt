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
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun EqualizerSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val saveError by AudioEffectsManager.persistenceError.collectAsState()
    com.xvox.music.features.settings.components.PinnedSettingsEditor(preview = {
        when {
            state.equalizerEnabled -> com.xvox.music.features.settings.components.EqSettingsPreview(state)
            else -> Text("Turn on the equalizer to preview", color = colors.secondaryText, fontSize = 12.sp)
        }
    }, controls = {
        saveError?.let { Text(it, color = colors.secondaryText, fontSize = 11.sp) }
        SettingsToggle("Equalizer", "5 bands · presets · reverb · protect", state.equalizerEnabled) { on ->
            if (on) viewModel.setEqBandCount(5)
            viewModel.setEqualizerEnabled(on)
        }
        if (state.equalizerEnabled) {
            Spacer(Modifier.height(12.dp))
            com.xvox.music.features.settings.components.SettingsChoiceRow(
                (AudioEffectsManager.PRESETS.keys + "Custom").map { it to it }, state.eqPreset, viewModel::setEqPreset)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(180.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                com.xvox.music.audio.EqBands.frequencies(5).forEachIndexed { index, frequency ->
                    VerticalEqBandSlider(com.xvox.music.audio.EqBands.label(frequency), state.eqBands.getOrElse(index) { 0 }) {
                        viewModel.setEqBand(index, it)
                    }
                }
            }
        }

        // Reverb + room always stay reachable, even while the equalizer itself is off.
        Spacer(Modifier.height(10.dp))
        EqLabel("Reverb")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Off" to 0f, "Room" to .16f, "Small hall" to .3f, "Hall" to .45f, "Large hall" to .62f, "Cathedral" to .85f)
                .forEach { (name, value) ->
                    val active = kotlin.math.abs(state.reverbAmount - value) < .03f
                    Text(name, color = if (active) colors.background else colors.primaryText, fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.clip(RoundedCornerShape(11.dp))
                            .background(if (active) colors.primaryAccent else colors.card)
                            .xvoxPressScale { viewModel.setReverbAmount(value) }
                            .padding(horizontal = 12.dp, vertical = 8.dp))
                }
        }
        XvoxThinLineSlider(state.reverbAmount, viewModel::setReverbAmount, 0f..1f, defaultValue = 0f)

        EqLabel("Room size")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Dry" to 0f, "Small" to .25f, "Mid" to .5f, "Large" to .78f, "Huge" to 1f)
                .forEach { (name, value) ->
                    val active = kotlin.math.abs(state.roomAmount - value) < .05f
                    Text(name, color = if (active) colors.background else colors.primaryText, fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.clip(RoundedCornerShape(11.dp))
                            .background(if (active) colors.primaryAccent else colors.card)
                            .xvoxPressScale { viewModel.setRoomAmount(value) }
                            .padding(horizontal = 12.dp, vertical = 8.dp))
                }
        }
        XvoxThinLineSlider(state.roomAmount, viewModel::setRoomAmount, 0f..1f, defaultValue = .5f)

        Spacer(Modifier.height(12.dp))
        EqLabel("Noise reduction ${(state.noiseReduction * 100).roundToInt()}%")
        XvoxThinLineSlider(state.noiseReduction, viewModel::setNoiseReduction, 0f..1f)
        EqLabel("Soften highs ${(state.softenHighs * 100).roundToInt()}%")
        XvoxThinLineSlider(state.softenHighs, viewModel::setSoftenHighs, 0f..1f)
        EqLabel("Boost protection ${state.eqHeadroomDb.roundToInt()} dB")
        XvoxThinLineSlider(state.eqHeadroomDb, viewModel::setEqHeadroomDb, 0f..18f, defaultValue = 0f)
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
    Text(text, color = XvoxTheme.colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
