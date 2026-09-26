package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.data.preferences.LyricsSettings
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsControlsEditor
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxSlider
import kotlin.math.roundToInt

/** Legacy/direct settings-page entry point. Sheets use [LyricsSettingsDraftSection] instead. */
@Composable
fun LyricsSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    LyricsSettingsDraftSection(
        settings = state.lyrics,
        onSettingsChange = { next -> viewModel.updateLyrics { next } }
    )
}

/**
 * Controls-only lyrics editor. The caller owns the snapshot: every gesture changes [settings]
 * locally, so a surrounding sheet can show a genuine Cancel/Okay transaction.
 */
@Composable
fun LyricsSettingsDraftSection(
    settings: LyricsSettings,
    onSettingsChange: (LyricsSettings) -> Unit
) {
    fun update(change: (LyricsSettings) -> LyricsSettings) {
        onSettingsChange(change(settings).sanitized())
    }

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(title = "Lines Alignment") {
            SettingsChoiceRow(
                options = listOf("left" to "Left", "center" to "Centre", "right" to "Right"),
                selected = settings.alignment,
                onSelect = { alignment -> update { it.copy(alignment = alignment) } }
            )
        }

        SettingsAccordionItem(title = "Lyric Text Colour") {
            SettingsChoiceRow(
                options = listOf(
                    "cover" to "Cover matching",
                    "black" to "Black",
                    "white" to "White"
                ),
                selected = settings.textColorMode,
                onSelect = { mode -> update { it.copy(textColorMode = mode) } }
            )
        }

        SettingsAccordionItem(title = "Line Transition · ${settings.animation.xvoxTransitionLabel()}") {
            val transitions = listOf("rise", "glide", "pop", "off")
            val selected = transitions.indexOf(settings.animation).takeIf { it >= 0 } ?: 0
            XvoxSlider(
                value = selected.toFloat(),
                onValueChange = { raw ->
                    val index = raw.roundToInt().coerceIn(transitions.indices)
                    update { it.copy(animation = transitions[index]) }
                },
                valueRange = 0f..(transitions.lastIndex.toFloat()),
                defaultValue = 0f,
                steps = transitions.lastIndex,
                valueLabel = { raw -> transitions[raw.roundToInt().coerceIn(transitions.indices)].xvoxTransitionLabel() }
            )
        }

        SettingsAccordionItem(title = "Text Size & Font Weight") {
            SettingsToggle(
                title = "Individual Line Sizes",
                subtitle = "Use separate top, active, and bottom line sizes",
                checked = settings.individualLineSizes,
                onChange = { enabled -> update { it.copy(individualLineSizes = enabled) } }
            )

            Spacer(Modifier.height(8.dp))
            if (settings.individualLineSizes) {
                LyricsSizeSlider("Top Line Size", settings.topSize, minValue = 10, defaultValue = 14) { value ->
                    update { it.copy(topSize = value) }
                }
                Spacer(Modifier.height(8.dp))
                LyricsSizeSlider("Middle (Active) Line Size", settings.currentSize, minValue = 14, defaultValue = 23) { value ->
                    update { it.copy(currentSize = value) }
                }
                Spacer(Modifier.height(8.dp))
                LyricsSizeSlider("Bottom Line Size", settings.bottomSize, minValue = 10, defaultValue = 14) { value ->
                    update { it.copy(bottomSize = value) }
                }
            } else {
                LyricsSizeSlider("Master Text Size", settings.currentSize, minValue = 14, defaultValue = 23) { value ->
                    update { current ->
                        // Preserve individually chosen sizes for a later re-enable; renderers use
                        // currentSize as the master while individual sizing is off.
                        current.copy(currentSize = value)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            val weight = settings.fontWeight.coerceIn(400, 800)
            SliderLabel("Font Weight · ${weight.xvoxWeightLabel()}")
            XvoxSlider(
                value = weight.toFloat(),
                onValueChange = { value -> update { it.copy(fontWeight = value.roundToInt()) } },
                valueRange = 400f..800f,
                defaultValue = 600f,
                steps = 4,
                valueLabel = { value -> value.roundToInt().xvoxWeightLabel() }
            )
        }

        SettingsAccordionItem(title = "Line Spacing · ${settings.lineGap} dp") {
            XvoxSlider(
                value = settings.lineGap.toFloat(),
                onValueChange = { value -> update { it.copy(lineGap = value.roundToInt()) } },
                valueRange = 4f..40f,
                defaultValue = 14f,
                steps = 18,
                valueLabel = { value -> "${value.roundToInt()} dp" }
            )
        }

        SettingsAccordionItem(title = "Fading & Focus") {
            val fadeToggleValue = if (settings.fadeEqual) 1f else 0f
            SliderLabel("Equal Fade")
            XvoxSlider(
                value = fadeToggleValue,
                onValueChange = { value -> update { it.copy(fadeEqual = value >= .5f) } },
                valueRange = 0f..1f,
                defaultValue = 0f,
                steps = 1,
                valueLabel = { value -> if (value >= .5f) "On" else "Off" }
            )

            Spacer(Modifier.height(8.dp))
            if (settings.fadeEqual) {
                SliderLabel("Fade Strength · ${(settings.fadeIntensity * 100).roundToInt()}%")
                XvoxSlider(
                    value = settings.fadeIntensity,
                    onValueChange = { value ->
                        update {
                            it.copy(
                                fadeIntensity = value,
                                fadeTop = value * .35f,
                                fadeBottom = value * .35f
                            )
                        }
                    },
                    valueRange = 0f..1f,
                    defaultValue = 1f,
                    steps = 20,
                    valueLabel = { value -> "${(value * 100).roundToInt()}%" }
                )
            } else {
                SliderLabel("Top Lines Fade · ${(settings.fadeTop * 100).roundToInt()}%")
                XvoxSlider(
                    value = settings.fadeTop,
                    onValueChange = { value -> update { it.copy(fadeTop = value) } },
                    valueRange = 0f..0.45f,
                    defaultValue = .22f,
                    steps = 18,
                    valueLabel = { value -> "${(value * 100).roundToInt()}%" }
                )
                Spacer(Modifier.height(8.dp))
                SliderLabel("Bottom Lines Fade · ${(settings.fadeBottom * 100).roundToInt()}%")
                XvoxSlider(
                    value = settings.fadeBottom,
                    onValueChange = { value -> update { it.copy(fadeBottom = value) } },
                    valueRange = 0f..0.45f,
                    defaultValue = .22f,
                    steps = 18,
                    valueLabel = { value -> "${(value * 100).roundToInt()}%" }
                )
            }
        }

        SettingsAccordionItem(
            title = if (settings.offsetMs == 0) "Timing" else "Timing · ${settings.offsetMs.xvoxTimingLabel()}"
        ) {
            XvoxSlider(
                value = settings.offsetMs.toFloat(),
                onValueChange = { value -> update { it.copy(offsetMs = value.roundToInt()) } },
                valueRange = -1000f..1000f,
                defaultValue = 0f,
                snapRadius = 0f,
                steps = 40,
                valueLabel = { value -> value.roundToInt().xvoxTimingLabel() }
            )
        }

        // These sit at the natural end of the long editor, immediately above the sheet footer,
        // mirroring the reset location in the Equalizer sheet.
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LyricsDraftAction("Reset All", Modifier.weight(1f)) {
                onSettingsChange(LyricsSettings())
            }
            LyricsDraftAction("Reset Timing", Modifier.weight(1f)) {
                update { it.copy(offsetMs = 0) }
            }
        }
    })
}

@Composable
private fun LyricsSizeSlider(
    label: String,
    size: Int,
    minValue: Int,
    defaultValue: Int,
    onChange: (Int) -> Unit
) {
    SliderLabel("$label · $size sp")
    XvoxSlider(
        value = size.toFloat(),
        onValueChange = { value -> onChange(value.roundToInt()) },
        valueRange = minValue.toFloat()..50f,
        defaultValue = defaultValue.toFloat(),
        steps = (50 - minValue),
        valueLabel = { value -> "${value.roundToInt()} sp" }
    )
}

@Composable
private fun SliderLabel(text: String) {
    Text(
        text = text,
        color = XvoxTheme.colors.primaryAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun LyricsDraftAction(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(colors.cardElevated)
            .border(.8.dp, colors.cardBorder.copy(alpha = .65f), shape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun String.xvoxTransitionLabel(): String = when (this) {
    "rise" -> "Rise"
    "glide" -> "Glide"
    "pop" -> "Pop"
    else -> "Classic"
}

private fun Int.xvoxWeightLabel(): String = when {
    this <= 425 -> "Regular"
    this <= 525 -> "Medium"
    this <= 625 -> "Semi-Bold"
    this <= 725 -> "Bold"
    else -> "Extra Bold"
}

private fun Int.xvoxTimingLabel(): String = when {
    this > 0 -> "+${this} ms"
    this < 0 -> "$this ms"
    else -> "0 ms"
}
