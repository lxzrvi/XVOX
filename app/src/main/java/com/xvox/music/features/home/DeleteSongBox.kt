package com.xvox.music.features.home

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics

@Composable
fun DeleteSongBox(
    song: Song,
    onRemoveApp: () -> Unit,
    onDeleteDevice: () -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Delete Track",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = song.title,
            color = colors.secondaryText,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(16.dp))

        DeleteChoice(
            title = "Delete from XVOX",
            subtitle = "Moves to Deleted songs",
            onClick = {
                haptics.heavy()
                onRemoveApp()
            }
        )

        Spacer(Modifier.height(8.dp))

        DeleteChoice(
            title = "Delete from device",
            subtitle = "Removes the file",
            isDanger = true,
            onClick = {
                haptics.heavy()
                onDeleteDevice()
            }
        )
    }
}

@Composable
private fun DeleteChoice(
    title: String,
    subtitle: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            color = if (isDanger) Color(0xFFEF4444) else colors.primaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = colors.secondaryText,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
