package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.sections.*

@Composable
fun NowPlayingOptionsBox(onDismiss: () -> Unit, settingsViewModel: SettingsViewModel = viewModel()) {
    val state by settingsViewModel.state.collectAsState()
    var page by remember { mutableStateOf<String?>(null) }
    XvoxBox(onDismiss = onDismiss, title = page ?: "Now Playing options", onBack = if (page != null) ({ page = null }) else null) {
        when (page) {
            "XvoxMix" -> EqualizerSettingsSection(state, settingsViewModel)
            "Playback" -> PlaybackSettingsEditor(state, settingsViewModel)
            "Lyrics" -> LyricsSettingsSection(state, settingsViewModel)
            "XvoxSplit" -> com.xvox.music.split.XvoxSplitPanel()
            else -> Column(Modifier.fillMaxWidth()) {
                for ((name, detail) in listOf("XvoxMix" to "Equalizer, protection & 3D", "Playback" to "Crossfade and headset behaviour", "Lyrics" to "Timing, fades & five animations", "XvoxSplit" to "Real vocal / instrumental separation")) {
                    Column(Modifier.fillMaxWidth().xvoxPressScale { page = name }.padding(14.dp)) {
                        Text(name, color = XvoxTheme.colors.primaryText, fontSize = 15.sp)
                        Text(detail, color = XvoxTheme.colors.secondaryText, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
