package com.xvox.music.features.settings.sections

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * Appearance.
 *
 * Every option is outlined, so the unselected choices are visible instead of dissolving into the
 * background; the selected one is filled and its border thickens. Both states animate.
 */
@Composable
fun AppearanceSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {

        SectionLabel("Theme")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("System", "Dark", "AMOLED", "Light").forEach { themeName ->
                OptionChip(
                    label = themeName,
                    selected = state.theme == themeName,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTheme(themeName) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionLabel("Accent")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val accents = listOf(
                "Default" to Color(0xFFF5F5F5),
                "XVOX Red" to Color(0xFFFA2D48),
                "XVOX Blue" to Color(0xFF007AFF)
            )
            accents.forEach { (name, swatch) ->
                val isSelected = state.accentColor == name
                val border by animateColorAsState(
                    if (isSelected) colors.primaryAccent else colors.cardBorder,
                    tween(180), label = "accent_border"
                )
                val width by animateDpAsState(if (isSelected) 1.6.dp else 0.9.dp, tween(180), label = "accent_width")
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.primaryAccent.copy(alpha = 0.14f) else colors.cardElevated)
                        .border(width, border, RoundedCornerShape(10.dp))
                        .xvoxPressScale { viewModel.setAccentColor(name) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(swatch, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                    Text(
                        text = name.replace("XVOX ", ""),
                        color = colors.primaryText,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionLabel("Text size")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.75f to "0.75x", 0.85f to "0.85x", 1.0f to "1x", 1.15f to "1.15x", 1.25f to "1.25x")
                .forEach { (scaleValue, scaleLabel) ->
                    OptionChip(
                        label = scaleLabel,
                        selected = kotlin.math.abs(state.fontSizeScale - scaleValue) < 0.04f,
                        modifier = Modifier.weight(1f),
                        fontSize = 11.sp,
                        onClick = { viewModel.setFontSizeScale(scaleValue) }
                    )
                }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = XvoxTheme.colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

/** Outlined in every state; filled and emphasised when selected. */
@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val fill by animateColorAsState(
        if (selected) colors.primaryAccent else colors.cardElevated, tween(180), label = "chip_fill"
    )
    val border by animateColorAsState(
        if (selected) colors.primaryAccent else colors.cardBorder, tween(180), label = "chip_border"
    )
    val borderWidth by animateDpAsState(if (selected) 1.6.dp else 0.9.dp, tween(180), label = "chip_border_w")
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(fill)
            .border(borderWidth, border, shape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) colors.background else colors.primaryText,
            fontSize = fontSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
