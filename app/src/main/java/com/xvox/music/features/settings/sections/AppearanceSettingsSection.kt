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
import androidx.compose.foundation.layout.size
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
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel

@Composable
fun AppearanceSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        com.xvox.music.features.settings.components.SettingsPreviewFrame("Appearance · live preview") {
            Text("Made for your music", color = colors.primaryAccent, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("${state.theme} · ${state.accentColor} · ${(state.fontSizeScale * 100).toInt()}% text",
                color = colors.primaryText, fontSize = 12.sp)
            Text("Scrolling content remains visible behind the glass header, navigation and mini player.",
                color = colors.secondaryText, fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))

        Text(
            text = "Theme",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themes = listOf("System", "Dark", "AMOLED", "Light")
            themes.forEach { themeName ->
                val isSelected = state.theme == themeName
                val shape = RoundedCornerShape(10.dp)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(shape)
                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                        .clickable { viewModel.setTheme(themeName) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = themeName,
                        color = if (isSelected) colors.background else colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Accent Color",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val accents = listOf(
                "Default" to Color(0xFFF5F5F5),
                "XVOX Red" to Color(0xFFFA2D48),
                "XVOX Blue" to Color(0xFF007AFF)
            )

            accents.forEach { (name, color) ->
                val isSelected = state.accentColor == name
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.cardElevated)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) colors.primaryAccent else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.setAccentColor(name) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(color, CircleShape)
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Font Scale",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val scales = listOf(
                0.75f to "0.75x",
                0.85f to "0.85x",
                1.0f to "1.0x",
                1.15f to "1.15x",
                1.25f to "1.25x"
            )

            scales.forEach { (scaleValue, scaleLabel) ->
                val isSelected = kotlin.math.abs(state.fontSizeScale - scaleValue) < 0.04f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                        .clickable { viewModel.setFontSizeScale(scaleValue) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scaleLabel,
                        color = if (isSelected) colors.background else colors.primaryText,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
