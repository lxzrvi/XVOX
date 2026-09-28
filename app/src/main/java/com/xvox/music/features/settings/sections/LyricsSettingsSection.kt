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
 * Controls-only lyrics editor. State is hoisted so each gesture can be applied immediately by a
 * settings page or live sheet host; Cancel/Okay only close the host surface.
 */
@Composable
fun LyricsSettingsDraftSection(
    settings: LyricsSettings,
    onSettingsChange: (LyricsSettings) -> Unit,
    /** Standalone settings pages retain inline resets; XvoxBox moves them into its fixed footer. */
    showInlineActions: Boolean = true
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
            // Three direct choices are clearer and more reliable than a stepped slider: every
            // label maps to a real live lyric motion profile.
            SettingsChoiceRow(
                options = listOf("rise" to "Rise", "glide" to "Glide", "pop" to "Pop"),
                selected = settings.animation.takeIf { it in setOf("rise", "glide", "pop") } ?: "rise",
                onSelect = { animation -> update { it.copy(animation = animation) } }
            )
        }

        // Font weight sits immediately beneath transition, where its active-line emphasis is
        // easiest to judge before editing the individual text-size controls below.
        SettingsAccordionItem(title = "Font Weight · ${settings.fontWeight.coerceIn(400, 800).xvoxWeightLabel()}") {
            val weight = settings.fontWeight.coerceIn(400, 800)
            XvoxSlider(
                value = weight.toFloat(),
                onValueChange = { value -> update { it.copy(fontWeight = value.roundToInt()) } },
                valueRange = 400f..800f,
                defaultValue = 600f,
                steps = 4,
                valueLabel = { value -> value.roundToInt().xvoxWeightLabel() }
            )
        }

        SettingsAccordionItem(title = "Text Size") {
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
            // These are intentional binary presentation modes, not misleading 0/1 sliders.
            SettingsToggle(
                title = "Fading",
                subtitle = "Dim non-active lines with the standard readable fade",
                checked = settings.fadeEnabled,
                onChange = { enabled ->
                    update {
                        it.copy(
                            fadeEnabled = enabled,
                            // Keep the old wire field meaningful for downgraded builds.
                            fadeEqual = enabled,
                            fadeIntensity = .82f,
                            fadeTop = .29f,
                            fadeBottom = .29f
                        )
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            SettingsToggle(
                title = "Focus active line",
                subtitle = "Emphasise the current line with weight, scale, and context contrast",
                checked = settings.focusActiveLine,
                onChange = { enabled -> update { it.copy(focusActiveLine = enabled) } }
            )
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

        // The original former-Experimental style-20 fullscreen motion is now permanent. Its
        // selector intentionally has no UI or persisted choice any more.
        if (showInlineActions) {
            // Direct settings-page use has no XvoxBox footer. The Now Playing sheet passes false
            // and provides these same actions in its fixed bottom bar instead.
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
        }
    })
}

/** Fixed bottom bar used by the Now Playing lyrics sheet. */
@Composable
fun LyricsFooterActions(
    onCancel: () -> Unit,
    onResetAll: () -> Unit,
    onResetTiming: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LyricsDraftAction("Cancel", Modifier.weight(1f), onCancel)
        LyricsDraftAction("Reset\nAll", Modifier.weight(1f), onResetAll)
        LyricsDraftAction("Reset\nTiming", Modifier.weight(1f), onResetTiming)
        LyricsDraftAction("Okay", Modifier.weight(1f), onDone, prominent = true)
    }
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
private fun LyricsDraftAction(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    prominent: Boolean = false
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(if (prominent) colors.primaryAccent else colors.cardElevated)
            .border(.8.dp, if (prominent) Color.Transparent else colors.cardBorder.copy(alpha = .65f), shape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (prominent) colors.background else colors.primaryText,
            fontSize = if (label.contains('\n')) 9.5.sp else 11.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun String.xvoxTransitionLabel(): String = when (this) {
    "rise" -> "Rise"
    "glide" -> "Glide"
    "pop" -> "Pop"
    else -> "Rise"
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
