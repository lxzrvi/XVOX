package com.xvox.music.features.settings.sections

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics

@Composable
fun AboutSettingsSection() {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalXvoxHaptics.current
    val pillShape = RoundedCornerShape(50)

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(pillShape)
                .background(colors.primaryAccent)
                .xvoxPressScale {
                    haptics.tap()
                    shareXvoxApp(context)
                }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_share),
                contentDescription = "Share XVOX",
                tint = aboutOnAccent(colors.primaryAccent),
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Share XVOX",
                color = aboutOnAccent(colors.primaryAccent),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "XVOX Music Player",
                color = colors.primaryAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "v0.1.0",
                color = colors.mutedText,
                fontSize = 11.sp
            )
        }

        Text(
            text = "A modern, ultra-responsive music player designed with fluid swipe gestures, continuous playback engines, live synchronized lyrics, real-time audio DSP, and elegant adaptive aesthetics.",
            color = colors.secondaryText,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardElevated.copy(alpha = 0.58f))
                .border(0.8.dp, colors.cardBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Text(
                text = "Note: Feature settings and customizations (Equalizer, 3D Sound, Lyrics styling, Layout grids) are accessible directly inside their dedicated pages and long-press option menus, keeping Settings clean and minimal.",
                color = colors.mutedText,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

private fun shareXvoxApp(context: Context) {
    runCatching {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "🎵 Experience music with XVOX — The next-generation fluid music player featuring instant continuous cover swiping, smart crossfade blend, synchronized lyrics, and pro audio DSP.\n\nDownload & discover XVOX!"
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share XVOX").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

private fun aboutOnAccent(accent: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color {
    val luminance = 0.2126f * accent.red + 0.7152f * accent.green + 0.0722f * accent.blue
    return if (luminance > 0.62f) androidx.compose.ui.graphics.Color(0xFF111111) else androidx.compose.ui.graphics.Color.White
}
