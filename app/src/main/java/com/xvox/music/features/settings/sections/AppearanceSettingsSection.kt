package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsSectionCard
import com.xvox.music.features.settings.components.SettingsToggle

@Composable
fun AppearanceSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    SettingsSectionCard(title = "Themes & Appearance", iconRes = R.drawable.ic_xvox_settings) {
        Text(
            text = "App Theme: ${state.theme}",
            color = colors.secondaryText,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("System", "Light", "Dark", "AMOLED").forEach { t ->
                val isSel = state.theme == t
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) colors.primaryAccent else colors.cardElevated)
                        .border(1.dp, if (isSel) colors.primaryAccent else colors.cardBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            haptics.tap()
                            viewModel.setTheme(t)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t,
                        color = if (isSel) colors.background else colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Accent Color: ${state.accentColor}",
            color = colors.secondaryText,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        val accentOptions = listOf(
            "Default" to Color(0xFFF5F5F5),
            "XVOX Red" to Color(0xFFFA2D48),
            "XVOX Blue" to Color(0xFF007AFF)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            accentOptions.forEach { (name, colorVal) ->
                val isSel = state.accentColor == name
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.cardElevated)
                        .border(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) colorVal else colors.cardBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            haptics.tap()
                            viewModel.setAccentColor(name)
                        }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(colorVal)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = name,
                            color = if (isSel) colors.primaryText else colors.secondaryText,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Font Scale: ${state.fontSizeScale}x",
            color = colors.secondaryText,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0.85f to "Small", 1.0f to "Normal", 1.15f to "Large", 1.25f to "XL").forEach { (scale, label) ->
                val isSel = state.fontSizeScale == scale
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) colors.primaryAccent else colors.cardElevated)
                        .border(1.dp, if (isSel) colors.primaryAccent else colors.cardBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptics.tap()
                            viewModel.setFontSizeScale(scale)
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSel) colors.background else colors.primaryText,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            title = "Haptic feedback",
            subtitle = "Vibrate gently on button taps and sliders",
            checked = state.hapticFeedback
        ) {
            haptics.toggle()
            viewModel.setHapticFeedback(it)
        }

        if (state.hapticFeedback) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Haptic Strength: ${state.hapticStrength}",
                color = colors.secondaryText,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Light", "Medium", "Strong").forEach { s ->
                    val isSel = state.hapticStrength.equals(s, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) colors.primaryAccent else colors.cardElevated)
                            .border(1.dp, if (isSel) colors.primaryAccent else colors.cardBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.heavy()
                                viewModel.setHapticStrength(s)
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s,
                            color = if (isSel) colors.background else colors.primaryText,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
