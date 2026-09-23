package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel

@Composable
fun NotifySettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val haptics = LocalXvoxHaptics.current

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsToggleRow(
            title = "Background playback",
            subtitle = if (state.persistentBackgroundPlayback) {
                "Keep playing when app is cleared from recents"
            } else {
                "Stop playing when app is closed / cleared"
            },
            checked = state.persistentBackgroundPlayback,
            onCheckedChange = {
                haptics.tap()
                viewModel.setPersistentBackgroundPlayback(it)
            }
        )

        SettingsToggleRow(
            title = "Daily music reminders",
            subtitle = "Gentle notification to resume your favourite tracks",
            checked = state.remindersEnabled,
            onCheckedChange = {
                haptics.tap()
                viewModel.setRemindersEnabled(it)
            }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.mutedText, fontSize = 11.sp, lineHeight = 14.sp)
        }
        CompactSettingsSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun CompactSettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 24.dp)
            .clip(shape)
            .background(if (checked) colors.primaryAccent else colors.cardElevated)
            .border(1.dp, if (checked) colors.primaryAccent else colors.cardBorder.copy(alpha = 0.75f), shape)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
