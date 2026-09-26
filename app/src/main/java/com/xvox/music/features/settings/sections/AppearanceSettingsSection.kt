package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxRed
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.ColorPickerRow
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsControlsEditor

@Composable
fun AppearanceSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(
            title = "Theme",
            expanded = expandedGroup == "Theme",
            onToggle = { toggle("Theme") }
        ) {
            val themeOptions = listOf(
                "System" to "System",
                "Light" to "Light",
                "Dark" to "Dark",
                "AMOLED" to "AMOLED"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                themeOptions.forEach { (key, label) ->
                    val isSelected = state.theme.equals(key, ignoreCase = true)
                    val btnBg = when (key) {
                        "Light" -> Color(0xFFF2F2F7)
                        "Dark" -> Color(0xFF1C1C1E)
                        "AMOLED" -> Color(0xFF000000)
                        else -> colors.cardElevated
                    }
                    val textColor = when (key) {
                        "Light" -> Color(0xFF111111)
                        "Dark" -> Color(0xFFEBEBF5)
                        "AMOLED" -> Color(0xFFFFFFFF)
                        else -> colors.primaryText
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(btnBg)
                            .then(
                                if (isSelected) Modifier.border(2.dp, colors.primaryAccent, RoundedCornerShape(10.dp))
                                else Modifier.border(0.8.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                            )
                            .xvoxPressScale { viewModel.setTheme(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        SettingsAccordionItem(
            title = "Accent",
            expanded = expandedGroup == "Accent",
            onToggle = { toggle("Accent") }
        ) {
            val accentOptions = listOf(
                "White" to "White",
                "Red" to "Red",
                "Blue" to "Blue",
                "custom" to "Custom"
            )
            val isCustomActive = state.accentColor.startsWith("#")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                accentOptions.forEach { (key, label) ->
                    val isSelected = if (key == "custom") isCustomActive else (!isCustomActive && state.accentColor.equals(key, ignoreCase = true))
                    val multiGradient = Brush.horizontalGradient(
                        listOf(
                            XvoxRed,
                            Color(0xFFFF9500),
                            Color(0xFF34C759),
                            Color(0xFF007AFF),
                            Color(0xFFAF52DE)
                        )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected && key != "custom") colors.primaryAccent else colors.cardElevated)
                            .then(
                                if (!isSelected) Modifier.border(0.8.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                                else if (key == "custom") Modifier.border(2.dp, colors.primaryAccent, RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .xvoxPressScale {
                                if (key != "custom") viewModel.setAccentColor(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "Red" -> Text(
                                text = label,
                                color = if (isSelected) colors.background else XvoxRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            "Blue" -> Text(
                                text = label,
                                color = if (isSelected) colors.background else Color(0xFF0A84FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            "custom" -> Text(
                                text = label,
                                style = androidx.compose.material3.LocalTextStyle.current.copy(
                                    brush = multiGradient,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            else -> Text(
                                text = label,
                                color = if (isSelected) colors.background else colors.primaryText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ColorPickerRow(
                label = "Custom accent",
                hex = if (state.accentColor.startsWith("#")) state.accentColor else "",
                onColorChange = { hex -> viewModel.setAccentColor(hex) },
                subtitle = if (state.accentColor.startsWith("#")) "Applied everywhere" else "Pick any colour",
                showPreview = false,
                initiallyExpanded = true
            )
        }

        SettingsAccordionItem(
            title = "Text size",
            expanded = expandedGroup == "Text size",
            onToggle = { toggle("Text size") }
        ) {
            val sizeOptions = listOf(
                Triple(0.70f, "XS", 9.5.sp),
                Triple(0.80f, "S", 10.5.sp),
                Triple(0.90f, "M", 11.5.sp),
                Triple(1.00f, "L", 12.5.sp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sizeOptions.forEach { (scale, label, fontSize) ->
                    val isSelected = kotlin.math.abs(state.fontSizeScale - scale) < 0.10f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .then(
                                if (!isSelected) Modifier.border(0.8.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .xvoxPressScale {
                                viewModel.setFontSizeScale(scale)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = fontSize,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    })
}
