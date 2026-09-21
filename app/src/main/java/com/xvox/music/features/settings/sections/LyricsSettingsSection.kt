package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(
            title = "Lines Alignment"
        ) {
            SettingsChoiceRow(
                listOf("left" to "Left", "center" to "Centre", "right" to "Right"),
                settings.alignment
            ) { chosen -> viewModel.updateLyrics { it.copy(alignment = chosen) } }
        }

        SettingsAccordionItem(
            title = "Line Transition Style"
        ) {
            val currentAnim = when (settings.animation) {
                "wave" -> "rise"
                "drift" -> "glide"
                "aurora" -> "pop"
                "classic" -> "off"
                else -> settings.animation
            }
            SettingsChoiceRow(
                listOf(
                    "rise" to "Rise", "glide" to "Glide", "pop" to "Pop", "off" to "Classic"
                ),
                currentAnim
            ) { value -> viewModel.updateLyrics { it.copy(animation = value) } }
        }

        SettingsAccordionItem(
            title = "Font Size & Weight"
        ) {
            Label("Text Size")
            val sizePresets = listOf(
                "Small" to (18 to 12),
                "Normal" to (22 to 14),
                "Large" to (26 to 16),
                "Extra" to (30 to 18)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sizePresets.forEach { (name, sizes) ->
                    val isSelected = settings.currentSize in (sizes.first - 1)..(sizes.first + 1)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .xvoxPressScale {
                                viewModel.updateLyrics {
                                    it.copy(
                                        currentSize = sizes.first,
                                        topSize = sizes.second,
                                        bottomSize = sizes.second,
                                        otherSize = sizes.second
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Label("Font Weight")
            val weightPresets = listOf(
                "Normal" to 400,
                "Medium" to 500,
                "Semi-Bold" to 600,
                "Bold" to 700
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weightPresets.forEach { (name, weight) ->
                    val isSelected = settings.fontWeight in (weight - 50)..(weight + 50)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .xvoxPressScale {
                                viewModel.updateLyrics { it.copy(fontWeight = weight) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        SettingsAccordionItem(
            title = "Line Spacing & Gap"
        ) {
            val gapPresets = listOf(
                "Tight" to 8,
                "Normal" to 14,
                "Relaxed" to 22
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gapPresets.forEach { (name, gap) ->
                    val isSelected = settings.lineGap in (gap - 2)..(gap + 2)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .xvoxPressScale {
                                viewModel.updateLyrics { it.copy(lineGap = gap) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$name (${gap}dp)",
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        SettingsAccordionItem(
            title = "Color & Contrast"
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
            title = "Fading & Focus"
        ) {
            SettingsToggle(
                title = "Equal fade",
                subtitle = "Fade every line above and below — only current line stays clear",
                checked = settings.fadeEqual,
                onChange = { on -> viewModel.updateLyrics { it.copy(fadeEqual = on) } }
            )

            Spacer(Modifier.height(8.dp))
            Label("Fade strength · ${(settings.fadeIntensity * 100).roundToInt()}%")
            XvoxThinLineSlider(
                settings.fadeIntensity,
                { v -> viewModel.updateLyrics { it.copy(fadeIntensity = (v * 100).roundToInt() / 100f, fadeTop = v * 0.35f, fadeBottom = v * 0.35f) } },
                0f..1f,
                defaultValue = 1f
            )
        }

        SettingsAccordionItem(
            title = if (settings.offsetMs == 0) "Timing" else if (settings.offsetMs > 0) "Timing +${settings.offsetMs} ms" else "Timing ${settings.offsetMs} ms"
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
                            .border(0.8.dp, colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
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
