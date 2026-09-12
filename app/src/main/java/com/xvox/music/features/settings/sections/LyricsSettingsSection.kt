package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
fun LyricsSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val settings = state.lyrics
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(
            title = "Lines Alignment",
            expanded = expandedGroup == "Alignment",
            onToggle = { toggle("Alignment") }
        ) {
            SettingsChoiceRow(
                listOf("left" to "Left", "center" to "Centre", "right" to "Right"),
                settings.alignment
            ) { chosen -> viewModel.updateLyrics { it.copy(alignment = chosen) } }
        }

        SettingsAccordionItem(
            title = "Animation style",
            expanded = expandedGroup == "Animation",
            onToggle = { toggle("Animation") }
        ) {
            SettingsChoiceRow(
                listOf(
                    "wave" to "Wave", "string" to "String", "spring" to "Spring",
                    "slide" to "Slide", "rise" to "Rise", "fade" to "Fade"
                ),
                settings.animation
            ) { value -> viewModel.updateLyrics { it.copy(animation = value) } }
        }

        SettingsAccordionItem(
            title = "Moving gradient background",
            expanded = expandedGroup == "Gradient",
            onToggle = { toggle("Gradient") }
        ) {
            SettingsChoiceRow(
                listOf(
                    "wave" to "Wave", "aurora" to "Aurora", "pulse" to "Pulse",
                    "orbital" to "Orbital", "prism" to "Prism", "off" to "Off"
                ),
                settings.gradientAnimation
            ) { value -> viewModel.updateLyrics { it.copy(gradientAnimation = value) } }
        }

        SettingsAccordionItem(
            title = "Line spacing & Gap",
            expanded = expandedGroup == "Gap",
            onToggle = { toggle("Gap") }
        ) {
            Label("Lines gap · ${settings.lineGap} dp")
            XvoxThinLineSlider(
                settings.lineGap.toFloat(),
                { v -> viewModel.updateLyrics { it.copy(lineGap = v.roundToInt()) } },
                4f..40f,
                defaultValue = 14f
            )
        }

        SettingsAccordionItem(
            title = "Color & Contrast",
            expanded = expandedGroup == "Color",
            onToggle = { toggle("Color") }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Match text with cover color", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Extract vibrant, readable color from current song artwork", color = colors.secondaryText, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.matchCoverColor,
                    onCheckedChange = { on -> viewModel.updateLyrics { it.copy(matchCoverColor = on) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.background,
                        checkedTrackColor = colors.primaryAccent,
                        uncheckedThumbColor = colors.secondaryText,
                        uncheckedTrackColor = colors.cardElevated
                    )
                )
            }
        }

        SettingsAccordionItem(
            title = "Font sizes",
            expanded = expandedGroup == "Font sizes",
            onToggle = { toggle("Font sizes") }
        ) {
            Label("Top lines font size · ${settings.topSize} sp")
            XvoxThinLineSlider(
                settings.topSize.toFloat(),
                { v -> viewModel.updateLyrics { it.copy(topSize = v.roundToInt(), otherSize = v.roundToInt()) } },
                10f..36f,
                defaultValue = 14f
            )

            Spacer(Modifier.height(8.dp))

            Label("Current line font size · ${settings.currentSize} sp")
            XvoxThinLineSlider(
                settings.currentSize.toFloat(),
                { v -> viewModel.updateLyrics { it.copy(currentSize = v.roundToInt()) } },
                14f..44f,
                defaultValue = 23f
            )

            Spacer(Modifier.height(8.dp))

            Label("Bottom lines font size · ${settings.bottomSize} sp")
            XvoxThinLineSlider(
                settings.bottomSize.toFloat(),
                { v -> viewModel.updateLyrics { it.copy(bottomSize = v.roundToInt()) } },
                10f..36f,
                defaultValue = 14f
            )
        }

        SettingsAccordionItem(
            title = "Fading",
            expanded = expandedGroup == "Fading",
            onToggle = { toggle("Fading") }
        ) {
            Label("Top fade · ${(settings.fadeTop * 100).roundToInt()}%")
            XvoxThinLineSlider(
                settings.fadeTop,
                { v -> viewModel.updateLyrics { it.copy(fadeTop = (v * 100).roundToInt() / 100f) } },
                0f..0.45f,
                defaultValue = .22f
            )

            Spacer(Modifier.height(8.dp))

            Label("Bottom fade · ${(settings.fadeBottom * 100).roundToInt()}%")
            XvoxThinLineSlider(
                settings.fadeBottom,
                { v -> viewModel.updateLyrics { it.copy(fadeBottom = (v * 100).roundToInt() / 100f) } },
                0f..0.45f,
                defaultValue = .22f
            )

            Spacer(Modifier.height(8.dp))

            SettingsToggle(
                title = "Equal fade",
                subtitle = "Fade every line above and below — only the current line stays clear",
                checked = settings.fadeEqual,
                onChange = { on -> viewModel.updateLyrics { it.copy(fadeEqual = on) } }
            )

            if (settings.fadeEqual) {
                Spacer(Modifier.height(8.dp))
                Label("Fade strength · ${(settings.fadeIntensity * 100).roundToInt()}%")
                XvoxThinLineSlider(
                    settings.fadeIntensity,
                    { v -> viewModel.updateLyrics { it.copy(fadeIntensity = (v * 100).roundToInt() / 100f) } },
                    0f..1f,
                    defaultValue = 1f
                )
            }
        }

        SettingsAccordionItem(
            title = if (settings.offsetMs == 0) "Timing" else if (settings.offsetMs > 0) "Timing +${settings.offsetMs} ms" else "Timing ${settings.offsetMs} ms",
            expanded = expandedGroup == "Timing",
            onToggle = { toggle("Timing") }
        ) {
            XvoxThinLineSlider(
                settings.offsetMs.toFloat(),
                { v -> viewModel.updateLyrics { it.copy(offsetMs = v.roundToInt()) } },
                -1000f..1000f,
                defaultValue = 0f,
                snapRadius = 0f
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(-100, -10, -1, 1, 10, 100).forEach { step ->
                    val label = if (step < 0) "${step} ms" else "+$step ms"
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                            .background(colors.cardElevated)
                            .xvoxPressScale {
                                viewModel.updateLyrics { it.copy(offsetMs = (it.offsetMs + step).coerceIn(-1000, 1000)) }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        SettingsChoiceRow(listOf("reset" to "Reset all", "timing" to "Reset timing"), "") { key ->
            viewModel.updateLyrics { if (key == "timing") it.copy(offsetMs = 0) else com.xvox.music.data.preferences.LyricsSettings() }
        }
    })
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        color = XvoxTheme.colors.primaryAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
