package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.sections.PlaybackSettingsSection

@Composable
fun NowPlayingOptionsBox(onDismiss: () -> Unit, settingsViewModel: SettingsViewModel = viewModel()) {
    val state by settingsViewModel.state.collectAsState()
    XvoxBox(onDismiss = onDismiss, title = "Now playing · XvoxMix") {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            EqualizerSettingsSection(state, settingsViewModel)
            Spacer(Modifier.height(20.dp))
            PlaybackSettingsSection(state, settingsViewModel)
        }
    }
}
