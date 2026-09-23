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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.xvox.music.audio.ReverbPresets
import com.xvox.music.core.design.theme.XvoxRed
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import kotlin.math.roundToInt

/**
 * Compact live Equalizer editor modeled on the supplied reference. Controls apply as a live
 * preview; Cancel restores the snapshot held by the containing sheet and Okay keeps the changes.
 */
@Composable
fun EqualizerSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    onCancel: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    val colors = XvoxTheme.colors
    val saveError by AudioEffectsManager.persistenceError.collectAsState()

    // The compact editor is intentionally a five-band equalizer, including for older installs
    // that may still hold the retired ten-band preference.
    LaunchedEffect(state.eqBandCount) {
        if (state.eqBandCount != 5) viewModel.setEqBandCount(5)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        saveError?.let {
            Text(
                text = it,
                color = XvoxRed,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }

        EqualizerToggleRow(
            title = "Enable equalizer",
            subtitle = "Boost dynamic clarity and output loudness",
            checked = state.equalizerEnabled,
            onCheckedChange = viewModel::setEqualizerEnabled
        )

        EqualizerBlock {
            EqualizerSectionLabel("EQ preset")
            EqualizerChipRow(
                options = AudioEffectsManager.EQ_PRESET_NAMES + "Custom",
                selected = state.eqPreset,
                onSelect = viewModel::setEqPreset
            )

            Spacer(Modifier.height(12.dp))
            EqualizerSectionLabel("Bands")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardElevated.copy(alpha = if (colors.isLight) .88f else .72f))
                    .border(0.8.dp, colors.cardBorder.copy(alpha = .82f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 6.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    EqBands.frequencies(5).forEachIndexed { index, frequency ->
                        VerticalEqBandSlider(
                            label = EqBands.label(frequency),
                            value = state.eqBands.getOrElse(index) { 0 },
                            onValueChange = { viewModel.setEqBand(index, it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        EqualizerBlock {
            EqualizerSliderRow(
                label = "App volume",
                valueText = "${(state.appVolume * 100).roundToInt()}%",
                value = state.appVolume,
                onValueChange = viewModel::setAppVolume,
                defaultValue = 1f
            )
        }

        EqualizerBlock {
            EqualizerSectionLabel("Reverb preset")
            EqualizerChipRow(
                options = ReverbPresets.names,
                selected = state.reverbPreset,
                onSelect = viewModel::setReverbPreset
            )

            if (state.reverbPreset != ReverbPresets.OFF) {
                Spacer(Modifier.height(12.dp))
                EqualizerSliderRow(
                    label = "Amount",
                    valueText = "${(state.reverbAmount * 100).roundToInt()}%",
                    value = state.reverbAmount,
                    onValueChange = viewModel::setReverbAmount,
                    defaultValue = .50f
                )
            }
        }

        EqualizerBlock {
            EqualizerToggleRow(
                title = "Noise reduction",
                subtitle = "Reduce steady background noise",
                checked = state.noiseReductionEnabled,
                onCheckedChange = viewModel::setNoiseReductionEnabled
            )
            if (state.noiseReductionEnabled) {
                Spacer(Modifier.height(7.dp))
                EqualizerSliderRow(
                    label = "Amount",
                    valueText = "${(state.noiseReduction * 100).roundToInt()}%",
                    value = state.noiseReduction,
                    onValueChange = viewModel::setNoiseReduction,
                    defaultValue = .40f
                )
            }
        }

        EqualizerBlock {
            EqualizerToggleRow(
                title = "Grain control",
                subtitle = "Soften harsh highs and digital grain",
                checked = state.grainControlEnabled,
                onCheckedChange = viewModel::setGrainControlEnabled
            )
            if (state.grainControlEnabled) {
                Spacer(Modifier.height(7.dp))
                EqualizerSliderRow(
                    label = "Amount",
                    valueText = "${(state.softenHighs * 100).roundToInt()}%",
                    value = state.softenHighs,
                    onValueChange = viewModel::setSoftenHighs,
                    defaultValue = .30f
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EqualizerFooterButton(
                label = "Cancel",
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )
            EqualizerFooterButton(
                label = "Reset",
                onClick = viewModel::resetEqualizerControls,
                modifier = Modifier.weight(1f)
            )
            EqualizerFooterButton(
                label = "Okay",
                onClick = onDone,
                prominent = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EqualizerBlock(content: @Composable ColumnScope.() -> Unit) {
    val colors = XvoxTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card.copy(alpha = if (colors.isLight) .50f else .42f))
            .border(0.7.dp, colors.cardBorder.copy(alpha = .72f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        content = content
    )
}

@Composable
private fun EqualizerSectionLabel(text: String) {
    Text(
        text = text,
        color = XvoxTheme.colors.primaryText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun EqualizerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = colors.mutedText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.primaryAccent,
                uncheckedThumbColor = colors.secondaryText,
                uncheckedTrackColor = colors.progressTrack
            )
        )
    }
}

@Composable
private fun EqualizerChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        options.forEach { option ->
            val selectedChip = option == selected
            val colors = XvoxTheme.colors
            val shape = RoundedCornerShape(18.dp)
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(shape)
                    .background(if (selectedChip) colors.primaryAccent else colors.cardElevated)
                    .border(
                        0.8.dp,
                        if (selectedChip) Color.Transparent else colors.cardBorder.copy(alpha = .85f),
                        shape
                    )
                    .xvoxPressScale { onSelect(option) }
                    .padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (selectedChip) colors.background else colors.primaryText,
                    fontSize = 11.sp,
                    fontWeight = if (selectedChip) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EqualizerSliderRow(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    defaultValue: Float
) {
    val colors = XvoxTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = colors.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                color = colors.primaryAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { onValueChange(it.coerceIn(0f, 1f)) },
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            colors = SliderDefaults.colors(
                thumbColor = colors.primaryAccent,
                activeTrackColor = colors.primaryAccent,
                inactiveTrackColor = colors.progressTrack
            )
        )
        Text(
            text = "Default: ${(defaultValue * 100).roundToInt()}%",
            color = colors.mutedText.copy(alpha = .80f),
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
private fun EqualizerFooterButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val fill = if (prominent) colors.primaryAccent else colors.cardElevated
    val textColor = if (prominent) colors.background else colors.primaryText
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(fill)
            .border(0.8.dp, if (prominent) Color.Transparent else colors.cardBorder, shape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Kept as a shared stepped level selector for Playback & Crossfade as well as legacy settings.
 * The compact Equalizer uses continuous sliders for its amount controls instead.
 */
@Composable
fun SevenButtonLevelSelector(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val levels = listOf(0f, 0.16f, 0.33f, 0.50f, 0.66f, 0.83f, 1.0f)
    val labels = listOf("0%", "16%", "33%", "50%", "66%", "83%", "100%")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        levels.forEachIndexed { index, level ->
            val selected = (index == 0 && value < 0.08f) ||
                (index == levels.lastIndex && value > 0.92f) ||
                (index in 1 until levels.lastIndex && kotlin.math.abs(value - level) < 0.08f)
            val shape = RoundedCornerShape(17.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(shape)
                    .background(if (selected) colors.primaryAccent else colors.cardElevated)
                    .border(0.8.dp, if (selected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), shape)
                    .xvoxPressScale { onValueChange(level) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labels[index],
                    color = if (selected) colors.background else colors.primaryText,
                    fontSize = 9.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/** Five-band vertical control with pointer, tap, and accessibility progress support. */
@Composable
fun VerticalEqBandSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val minDb = -12
    val maxDb = 12
    var localValue by remember(value) { mutableIntStateOf(value.coerceIn(minDb, maxDb)) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val fraction = ((localValue - minDb).toFloat() / (maxDb - minDb).toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${if (localValue > 0) "+" else ""}$localValue",
            color = colors.primaryAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .width(34.dp)
                .height(120.dp)
                .semantics {
                    contentDescription = "$label equalizer band"
                    progressBarRangeInfo = ProgressBarRangeInfo(localValue.toFloat(), -12f..12f, 23)
                    setProgress { requested ->
                        val db = requested.roundToInt().coerceIn(minDb, maxDb)
                        localValue = db
                        currentOnValueChange(db)
                        true
                    }
                }
                .pointerInput(label) {
                    detectTapGestures { offset ->
                        val f = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                        val db = (minDb + f * (maxDb - minDb)).roundToInt().coerceIn(minDb, maxDb)
                        localValue = db
                        currentOnValueChange(db)
                    }
                }
                .pointerInput(label) {
                    detectVerticalDragGestures(onVerticalDrag = { change, _ ->
                        change.consume()
                        val f = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                        val db = (minDb + f * (maxDb - minDb)).roundToInt().coerceIn(minDb, maxDb)
                        if (db != localValue) {
                            localValue = db
                            currentOnValueChange(db)
                        }
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.progressTrack)
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
                    .border(1.dp, colors.background.copy(alpha = .70f), CircleShape)
            )
        }

        Text(
            text = label,
            color = colors.secondaryText,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
