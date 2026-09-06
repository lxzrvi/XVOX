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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.audio.AudioEffectsManager
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsSectionCard
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun EqualizerSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors

    SettingsSectionCard(
        title = "XvoxMix (Equalizer)",
        iconRes = R.drawable.ic_xvox_equalizer
    ) {
        SettingsToggle(
            title = "XvoxMix Master",
            subtitle = "Enable audio equalizer & hardware DSP engine",
            checked = state.equalizerEnabled,
            onChange = viewModel::setEqualizerEnabled
        )

        if (state.equalizerEnabled) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Preset: ${state.eqPreset}",
                color = colors.secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf("Flat", "Bass Boost", "Treble", "Rock", "Pop", "Jazz", "Electronic", "Vocal", "Custom")
                presets.forEach { preset ->
                    val isSelected = state.eqPreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .clickable {
                                viewModel.setEqPreset(preset)
                                if (preset != "Custom" && AudioEffectsManager.PRESETS.containsKey(preset)) {
                                    viewModel.setEqBands(AudioEffectsManager.PRESETS[preset] ?: listOf(0, 0, 0, 0, 0))
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset,
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Independent Equalizer Bands",
                color = colors.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            val labels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                labels.forEachIndexed { index, label ->
                    val value = state.eqBands.getOrElse(index) { 0 }
                    VerticalEqBandSlider(
                        label = label,
                        value = value,
                        onValueChange = { newValue ->
                            val updated = state.eqBands.toMutableList()
                            while (updated.size <= index) updated.add(0)
                            updated[index] = newValue
                            viewModel.setEqBands(updated)
                            viewModel.setEqPreset("Custom")
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsToggle(
            title = "3D Surround Sound",
            subtitle = "Smooth real-time L/R spatial orbit panning for immersive listening",
            checked = state.stereoWidening,
            onChange = viewModel::setStereoWidening
        )

        if (state.stereoWidening) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "3D Orbit Pan Interval: ${state.surroundPanSpeed} seconds",
                color = colors.secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            XvoxThinLineSlider(
                value = state.surroundPanSpeed.toFloat(),
                onValueChange = { viewModel.setSurroundPanSpeed(it.roundToInt()) },
                valueRange = 2f..10f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Master Volume: ${(state.appVolume * 100).roundToInt()}%",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        XvoxThinLineSlider(
            value = state.appVolume,
            onValueChange = viewModel::setAppVolume,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        val balanceLabel = when {
            state.balance < -0.05f -> "L ${(-state.balance * 100).roundToInt()}%"
            state.balance > 0.05f -> "R ${(state.balance * 100).roundToInt()}%"
            else -> "Center"
        }

        Text(
            text = "Output Balance: $balanceLabel",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        XvoxThinLineSlider(
            value = state.balance,
            onValueChange = viewModel::setBalance,
            valueRange = -1f..1f,
            modifier = Modifier.fillMaxWidth()
        )
    }
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
