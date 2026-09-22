package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
    var individualSizesEnabled by remember { mutableStateOf(settings.topSize != (settings.currentSize * 0.65f).roundToInt() && settings.topSize != settings.bottomSize) }

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(
            title = "Lines Alignment"
        ) {
            val alignOptions = listOf("left" to "Left", "center" to "Centre", "right" to "Right")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                alignOptions.forEach { (key, label) ->
                    val isSelected = settings.alignment == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(19.dp))
                            .xvoxPressScale { viewModel.updateLyrics { it.copy(alignment = key) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
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
            val animOptions = listOf(
                "rise" to "Rise", "glide" to "Glide", "pop" to "Pop", "off" to "Classic"
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                animOptions.forEach { (key, label) ->
                    val isSelected = currentAnim == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(19.dp))
                            .xvoxPressScale { viewModel.updateLyrics { it.copy(animation = key) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        SettingsAccordionItem(
            title = "Text Size & Font Weight"
        ) {
            val sizePresets = listOf(14, 20, 26, 32, 38, 44, 50)

            SettingsToggle(
                title = "Individual Line Sizes",
                subtitle = "Set distinct text sizes for top, middle, and bottom lines",
                checked = individualSizesEnabled,
                onChange = { on ->
                    individualSizesEnabled = on
                    if (!on) {
                        viewModel.updateLyrics {
                            it.copy(
                                currentSize = 26,
                                topSize = 18,
                                bottomSize = 18,
                                otherSize = 18
                            )
                        }
                    }
                }
            )

            if (!individualSizesEnabled) {
                Spacer(Modifier.height(8.dp))
                Label("Master Text Size · ${settings.currentSize} sp")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sizePresets.forEach { sz ->
                        val isSelected = settings.currentSize in (sz - 2)..(sz + 2)
                        val sideSz = (sz * 0.68f).roundToInt().coerceAtLeast(12)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                                .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), CircleShape)
                            .xvoxPressScale {
                                viewModel.updateLyrics {
                                    it.copy(
                                        currentSize = sz,
                                        topSize = sideSz,
                                        bottomSize = sideSz,
                                        otherSize = sideSz
                                    )
                                }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$sz",
                                color = if (isSelected) colors.background else colors.primaryText,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Label("Top Line Size · ${settings.topSize} sp")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sizePresets.forEach { sz ->
                        val isSelected = settings.topSize in (sz - 2)..(sz + 2)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                                .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), CircleShape)
                                .xvoxPressScale { viewModel.updateLyrics { it.copy(topSize = sz) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$sz", color = if (isSelected) colors.background else colors.primaryText, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Label("Middle (Active) Line Size · ${settings.currentSize} sp")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sizePresets.forEach { sz ->
                        val isSelected = settings.currentSize in (sz - 2)..(sz + 2)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                                .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), CircleShape)
                                .xvoxPressScale { viewModel.updateLyrics { it.copy(currentSize = sz) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$sz", color = if (isSelected) colors.background else colors.primaryText, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Label("Bottom Line Size · ${settings.bottomSize} sp")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sizePresets.forEach { sz ->
                        val isSelected = settings.bottomSize in (sz - 2)..(sz + 2)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                                .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), CircleShape)
                                .xvoxPressScale { viewModel.updateLyrics { it.copy(bottomSize = sz) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$sz", color = if (isSelected) colors.background else colors.primaryText, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                        }
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
                            .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
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
            title = "Line Spacing"
        ) {
            val spacingPresets = listOf(
                "Tight" to 8,
                "Compact" to 12,
                "Normal" to 16,
                "Relaxed" to 22,
                "Wide" to 28
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                spacingPresets.forEach { (name, gap) ->
                    val isSelected = settings.lineGap in (gap - 2)..(gap + 2)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .border(0.8.dp, if (isSelected) Color.Transparent else colors.cardBorder.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                            .xvoxPressScale {
                                viewModel.updateLyrics { it.copy(lineGap = gap) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = 11.sp,
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
                subtitle = "Fade lines equally above and below active line",
                checked = settings.fadeEqual,
                onChange = { on -> viewModel.updateLyrics { it.copy(fadeEqual = on) } }
            )

            if (settings.fadeEqual) {
                Spacer(Modifier.height(8.dp))
                Label("Fade strength · ${(settings.fadeIntensity * 100).roundToInt()}%")
                XvoxThinLineSlider(
                    settings.fadeIntensity,
                    { v -> viewModel.updateLyrics { it.copy(fadeIntensity = (v * 100).roundToInt() / 100f, fadeTop = v * 0.35f, fadeBottom = v * 0.35f) } },
                    0f..1f,
                    defaultValue = 1f
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Label("Top lines fade · ${(settings.fadeTop * 100).roundToInt()}%")
                XvoxThinLineSlider(
                    settings.fadeTop,
                    { v -> viewModel.updateLyrics { it.copy(fadeTop = (v * 100).roundToInt() / 100f) } },
                    0f..0.45f,
                    defaultValue = .22f
                )

                Spacer(Modifier.height(8.dp))
                Label("Bottom lines fade · ${(settings.fadeBottom * 100).roundToInt()}%")
                XvoxThinLineSlider(
                    settings.fadeBottom,
                    { v -> viewModel.updateLyrics { it.copy(fadeBottom = (v * 100).roundToInt() / 100f) } },
                    0f..0.45f,
                    defaultValue = .22f
                )
            }
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
