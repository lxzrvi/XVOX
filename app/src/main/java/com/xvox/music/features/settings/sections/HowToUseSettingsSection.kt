package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun HowToUseSettingsSection() {
    val colors = XvoxTheme.colors

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GuideTipItem(
            iconRes = R.drawable.ic_xvox_home,
            title = "Multi-Select & Bulk Actions",
            description = "Tap a song once to play. Open ⋯ or hold it for options, then choose Select. Tap more songs and use the toolbar directly below the header for playlist, like/unlike, remove or share actions."
        )

        GuideTipItem(
            iconRes = R.drawable.ic_xvox_disc,
            title = "NowPlaying Gestures",
            description = "Swipe horizontally across the album art to skip tracks. The full-screen background color dynamically interpolates with your swipe gesture in real time. Drag down from the top handle to dismiss."
        )

        GuideTipItem(
            iconRes = R.drawable.ic_xvox_equalizer,
            title = "XvoxMix Audio DSP",
            description = "Five smoothly ramped EQ bands shape your music. Boost protection adds headroom; raise it if bass strains your speaker. Try the ear-delay, rear pinna cues and early-reflection orbit with headphones, then adjust depth and speed."
        )

        GuideTipItem(
            iconRes = R.drawable.ic_xvox_refresh,
            title = "Library & Folders",
            description = "Tap the refresh icon in the top header to rescan your device for newly added music. Filter out short voice clips, small files, or exclude entire folders in Library Filter settings."
        )

        GuideTipItem(
            iconRes = R.drawable.ic_xvox_queue,
            title = "Queue Reordering",
            description = "Open the queue box and drag songs up or down. The list smoothly auto-scrolls when dragging near the edges to effortlessly reposition tracks."
        )
    }
}

@Composable
private fun GuideTipItem(
    iconRes: Int,
    title: String,
    description: String
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colors.primaryAccent,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.primaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                color = colors.secondaryText,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
