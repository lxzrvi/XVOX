package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun AboutSettingsSection() {
    val colors = XvoxTheme.colors

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "XVOX Music Engine",
            color = colors.primaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "XVOX is a high-performance offline music player designed for uncompromising sound quality and fluid usability. Key features include:",
            color = colors.secondaryText,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Text(
            text = "• Lossless Bit-Exact Local Playback\n• XvoxMix 5-Band Independent Equalizer & 3D Spatial Orbit Pan\n• Seamless Linear Audio Crossfade\n• Dynamic Restart Mosaic & Multi-Row Home Grid\n• Interactive Multi-Select & Batch Library Management\n• Smart Audio Duration, File Size & Folder Filters\n• Real-Time Synchronized Lyrics & Fullscreen Mode\n• Completely Offline & 100% Privacy Preserving",
            color = colors.primaryText,
            fontSize = 11.sp,
            lineHeight = 17.sp
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Version 1.0 (Inter Edition)",
            color = colors.mutedText,
            fontSize = 10.sp
        )
    }
}
