package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Compact live Equalizer editor modeled on the supplied reference. Every toggle, preset, and
 * slider updates immediately; Cancel and Okay merely close or acknowledge the ordinary sheet.
 */
@Composable
fun EqualizerSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    onCancel: () -> Unit = {},
    onDone: () -> Unit = {},
    /** XvoxBox hosts the fixed footer when this editor is used inside Now Playing. */
    showFooter: Boolean = true
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        saveError?.let {
            Text(
                text = it,
                color = XvoxRed,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }

        // Enable remains available at all times, but a disabled EQ has no preset or band box
        // to imply that those controls are currently active.
        EqualizerCard {
            EqualizerToggleRow(
                title = "Enable equalizer",
                subtitle = "Boost or cut each band without weakening the full mix",
                checked = state.equalizerEnabled,
                onCheckedChange = viewModel::setEqualizerEnabled
            )
            if (state.equalizerEnabled) {
                EqualizerSectionLabel("EQ preset")
                EqualizerChipRow(
                    options = AudioEffectsManager.EQ_PRESET_NAMES,
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
                        .background(colors.background)
                        .border(0.8.dp, colors.cardBorder.copy(alpha = .72f), RoundedCornerShape(14.dp))
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
        }

        // The remaining controls intentionally sit directly on the sheet rather than each being
        // boxed into a card. That preserves the compact single-surface reference hierarchy.
        EqualizerSliderRow(
            label = "App volume",
            valueText = "${(state.appVolume * 100).roundToInt()}%",
            value = (state.appVolume / 2f).coerceIn(0f, 1f),
            onValueChange = { viewModel.setAppVolume(it * 2f) },
            defaultValue = .50f
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EqualizerSectionLabel("Reverb preset")
            EqualizerChipRow(
                options = ReverbPresets.names,
                selected = state.reverbPreset,
                onSelect = viewModel::setReverbPreset,
                circularOption = ReverbPresets.OFF
            )
            if (state.reverbPreset != ReverbPresets.OFF) {
                EqualizerSliderRow(
                    label = "Amount",
                    valueText = "${(state.reverbAmount * 100).roundToInt()}%",
                    value = state.reverbAmount,
                    onValueChange = viewModel::setReverbAmount,
                    defaultValue = .50f
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EqualizerToggleRow(
                title = "Noise reduction",
                subtitle = "Reduce hiss and quiet background noise",
                checked = state.noiseReductionEnabled,
                onCheckedChange = viewModel::setNoiseReductionEnabled
            )
            if (state.noiseReductionEnabled) {
                EqualizerSliderRow(
                    label = "Amount",
                    valueText = "${(state.noiseReduction * 100).roundToInt()}%",
                    value = state.noiseReduction,
                    onValueChange = viewModel::setNoiseReduction,
                    defaultValue = .50f
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EqualizerToggleRow(
                title = "Grain control",
                subtitle = "Smooth harsh highs and digital grain",
                checked = state.grainControlEnabled,
                onCheckedChange = viewModel::setGrainControlEnabled
            )
            if (state.grainControlEnabled) {
                EqualizerSliderRow(
                    label = "Amount",
                    valueText = "${(state.softenHighs * 100).roundToInt()}%",
                    value = state.softenHighs,
                    onValueChange = viewModel::setSoftenHighs,
                    defaultValue = .50f
                )
            }
        }

        if (showFooter) {
            EqualizerFooterActions(
                onCancel = onCancel,
                onReset = viewModel::resetEqualizerControls,
                onDone = onDone
            )
        }
    }
}

/** Fixed action row supplied to XvoxBox when the overlay owns its viewport. */
@Composable
fun EqualizerFooterActions(
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EqualizerFooterButton("Cancel", onCancel, Modifier.weight(1f))
        EqualizerFooterButton("Reset", onReset, Modifier.weight(1f))
        EqualizerFooterButton("Okay", onDone, Modifier.weight(1f), prominent = true)
    }
}

@Composable
private fun EqualizerCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Match the Settings-page cards exactly in both light and dark palettes.
            .clip(shape)
            .background(colors.cardElevated)
            .border(0.8.dp, colors.cardBorder.copy(alpha = if (colors.isLight) .80f else .72f), shape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun EqualizerHairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(.8.dp)
            .background(XvoxTheme.colors.cardBorder.copy(alpha = .72f))
    )
}

@Composable
private fun EqualizerSectionLabel(text: String) {
    Text(
        text = text,
        color = XvoxTheme.colors.primaryText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
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
    onSelect: (String) -> Unit,
    /** Off in the reverb row is intentionally a compact circular preset control. */
    circularOption: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        options.distinct().forEach { option ->
            val selectedChip = option == selected
            val colors = XvoxTheme.colors
            val circular = option == circularOption
            val shape = if (circular) CircleShape else RoundedCornerShape(18.dp)
            Box(
                modifier = Modifier
                    .then(if (circular) Modifier.size(34.dp) else Modifier.height(34.dp))
                    .clip(shape)
                    .background(if (selectedChip) colors.primaryAccent else colors.cardElevated)
                    .border(
                        0.8.dp,
                        if (selectedChip) Color.Transparent else colors.cardBorder.copy(alpha = .85f),
                        shape
                    )
                    .xvoxPressScale { onSelect(option) }
                    .padding(horizontal = if (circular) 0.dp else 13.dp),
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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
        EqualizerThinSlider(
            value = value,
            onValueChange = onValueChange,
            defaultValue = defaultValue,
            contentDescription = "$label amount"
        )
    }
}

/** Thin, rounded amount slider with an intentional snap at its default/midpoint. */
@Composable
private fun EqualizerThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    defaultValue: Float,
    contentDescription: String
) {
    val colors = XvoxTheme.colors
    val latestChange by rememberUpdatedState(onValueChange)
    val current = value.coerceIn(0f, 1f)
    fun snap(raw: Float): Float {
        val safe = raw.coerceIn(0f, 1f)
        return if (abs(safe - defaultValue) <= .035f) defaultValue else safe
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(current, 0f..1f, 100)
                setProgress { requested ->
                    latestChange(snap(requested))
                    true
                }
            }
            .pointerInput(latestChange, defaultValue) {
                detectTapGestures { offset ->
                    val raw = offset.x / size.width.toFloat().coerceAtLeast(1f)
                    latestChange(snap(raw))
                }
            }
            .pointerInput(latestChange, defaultValue) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val raw = change.position.x / size.width.toFloat().coerceAtLeast(1f)
                    latestChange(snap(raw))
                }
            }
    ) {
        val shape = RoundedCornerShape(50)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(4.dp)
                .clip(shape)
                .background(colors.progressTrack)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(maxWidth * current)
                .height(4.dp)
                .clip(shape)
                .background(colors.primaryAccent)
        )
        // Marker stays inside the 4dp track thickness instead of protruding above or below it.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (maxWidth * defaultValue) - .5.dp)
                .width(1.dp)
                .height(4.dp)
                .background(colors.primaryText.copy(alpha = .42f))
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

/** Five-band vertical control with rounded track tips, pointer, tap, and accessibility support. */
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
    fun snappedDb(raw: Float): Int = raw.roundToInt().coerceIn(minDb, maxDb)

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
                        val db = snappedDb(requested)
                        localValue = db
                        currentOnValueChange(db)
                        true
                    }
                }
                .pointerInput(label) {
                    detectTapGestures { offset ->
                        val f = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                        val db = snappedDb(minDb + f * (maxDb - minDb))
                        localValue = db
                        currentOnValueChange(db)
                    }
                }
                .pointerInput(label) {
                    detectVerticalDragGestures(onVerticalDrag = { change, _ ->
                        change.consume()
                        val f = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                        val db = snappedDb(minDb + f * (maxDb - minDb))
                        if (db != localValue) {
                            localValue = db
                            currentOnValueChange(db)
                        }
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            // The active segment grows away from the zero line: up for boost, down for cut.
            // It therefore represents sign as well as magnitude instead of pretending every
            // adjustment starts from the bottom of the range.
            val magnitude = (kotlin.math.abs(localValue).toFloat() / maxDb.toFloat()).coerceIn(0f, 1f)
            val activeHeight = 55.dp * magnitude
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(110.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.progressTrack)
            ) {
                if (localValue != 0) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(activeHeight)
                            .align(Alignment.Center)
                            .offset(y = if (localValue > 0) -(activeHeight / 2) else activeHeight / 2)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.primaryAccent)
                    )
                }
                // Zero/default marker never extends beyond the track's own thickness.
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(4.dp)
                        .height(1.dp)
                        .background(colors.cardBorder.copy(alpha = .72f))
                )
            }
        }

        Text(
            text = label,
            color = colors.secondaryText,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
