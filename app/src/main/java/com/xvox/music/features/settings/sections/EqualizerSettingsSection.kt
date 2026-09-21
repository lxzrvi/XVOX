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
            SettingsToggle("Enable equalizer", "Boost dynamic clarity and output loudness", state.equalizerEnabled) { on ->
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isOffActive = state.roomAmount < 0.05f
                // "Off" in Circular Shape
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isOffActive) colors.primaryAccent else colors.cardElevated)
                        .border(0.8.dp, if (isOffActive) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), CircleShape)
                        .xvoxPressScale {
                            viewModel.setRoomAmount(0f)
                            viewModel.setReverbAmount(0f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Off",
                        color = if (isOffActive) colors.background else colors.primaryText,
                        fontSize = 11.5.sp,
                        fontWeight = if (isOffActive) FontWeight.Bold else FontWeight.Medium
                    )
                }

                listOf(
                    Pair("Small Room", 0.20f),
                    Pair("Medium Room", 0.40f),
                    Pair("Large Room", 0.60f),
                    Pair("Hall", 0.80f),
                    Pair("Cathedral", 1.00f)
                ).forEach { (name, revAmount) ->
                    val isPresetActive = !isOffActive && kotlin.math.abs(state.roomAmount - revAmount) < 0.10f
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isPresetActive) colors.primaryAccent else colors.cardElevated)
                            .border(0.8.dp, if (isPresetActive) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                            .xvoxPressScale {
                                viewModel.setRoomAmount(revAmount)
                                viewModel.setReverbAmount(revAmount)
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (isPresetActive) colors.background else colors.primaryText,
                            fontSize = 11.5.sp,
                            fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        SettingsAccordionItem(
            title = "Clarity & Protection",
            expanded = expandedGroup == "Clarity",
            onToggle = { toggle("Clarity") }
        ) {
            EqLabel("Noise reduction · ${(state.noiseReduction * 100).roundToInt()}%")
            SevenButtonLevelSelector(
                value = state.noiseReduction,
                onValueChange = viewModel::setNoiseReduction
            )
            Spacer(Modifier.height(10.dp))
            EqLabel("Grain control · ${(state.softenHighs * 100).roundToInt()}%")
            SevenButtonLevelSelector(
                value = state.softenHighs,
                onValueChange = viewModel::setSoftenHighs
            )
        }

        SettingsAccordionItem(
            title = "Volume",
            expanded = expandedGroup == "Volume",
            onToggle = { toggle("Volume") }
        ) {
            EqLabel("App volume · ${(state.appVolume * 100).roundToInt()}%")
            XvoxThinLineSlider(state.appVolume, viewModel::setAppVolume, 0f..1f, defaultValue = 1f)
        }
    })
}

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
        levels.forEachIndexed { index, lvl ->
            val isSelected = (index == 0 && value < 0.08f) ||
                    (index == levels.lastIndex && value > 0.92f) ||
                    (index in 1 until levels.lastIndex && kotlin.math.abs(value - lvl) < 0.08f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                    .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(17.dp))
                    .xvoxPressScale {
                        onValueChange(lvl)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labels[index],
                    color = if (isSelected) colors.background else colors.primaryText,
                    fontSize = 9.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
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
