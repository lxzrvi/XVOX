package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun NowPlayingPlaybackSettingsSection(
    gapless: Boolean,
    crossfade: Boolean,
    crossfadeDuration: Int,
    fadeInEnabled: Boolean,
    fadeOutEnabled: Boolean,
    skipSilence: Boolean,
    onToggleGapless: (Boolean) -> Unit,
    onToggleCrossfade: (Boolean) -> Unit,
    onCrossfadeDurationChange: (Int) -> Unit,
    onToggleFadeIn: (Boolean) -> Unit,
    onToggleFadeOut: (Boolean) -> Unit,
    onToggleSkipSilence: (Boolean) -> Unit
) {
    val colors = XvoxTheme.colors

    Text(
        text = "Playback Settings",
        color = colors.primaryText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(6.dp))

    PlaybackOptionSwitchRow(
        title = "Gapless Playback",
        checked = gapless,
        onCheckedChange = onToggleGapless
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Crossfade Tracks", color = colors.primaryText, fontSize = 13.sp)
            if (crossfade) {
                Text("${crossfadeDuration}s duration", color = colors.secondaryText, fontSize = 11.sp)
            }
        }

        Switch(
            checked = crossfade,
            onCheckedChange = onToggleCrossfade,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.primaryAccent
            )
        )
    }

    if (crossfade) {
        XvoxThinLineSlider(
            value = crossfadeDuration.toFloat(),
            onValueChange = { onCrossfadeDurationChange(it.roundToInt()) },
            valueRange = 1f..12f,
            modifier = Modifier.fillMaxWidth()
        )
    }

    PlaybackOptionSwitchRow(
        title = "Fade In on Start",
        checked = fadeInEnabled,
        onCheckedChange = onToggleFadeIn
    )

    PlaybackOptionSwitchRow(
        title = "Fade Out on Pause",
        checked = fadeOutEnabled,
        onCheckedChange = onToggleFadeOut
    )

    PlaybackOptionSwitchRow(
        title = "Skip Silence",
        checked = skipSilence,
        onCheckedChange = onToggleSkipSilence
    )
}

@Composable
fun PlaybackOptionSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = XvoxTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.primaryText,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.primaryAccent
            )
        )
    }
}
