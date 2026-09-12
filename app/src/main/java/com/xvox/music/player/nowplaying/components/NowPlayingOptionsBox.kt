package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsSectionPreview
import com.xvox.music.features.settings.sections.AudioOutputContent
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.sections.HeadsetSettingsSection
import com.xvox.music.features.settings.sections.LyricsSettingsSection
import com.xvox.music.features.settings.sections.PlaybackSettingsEditor
import com.xvox.music.features.settings.sections.ThreeDSoundSettingsSection

@Composable
fun NowPlayingOptionsBox(
    onDismiss: () -> Unit,
    initialPage: String? = null,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val state by settingsViewModel.state.collectAsState()
    var page by remember { mutableStateOf(initialPage) }
    val scrollState = rememberScrollState()

    XvoxBox(
        onDismiss = onDismiss,
        title = page ?: "Now Playing options",
        onBack = if (page != null) ({ page = null }) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // Single live preview at the top
            page?.takeIf { it != "Bluetooth" && it != "Lyrics" }?.let { title ->
                SettingsSectionPreview(
                    title = title,
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .padding(bottom = 12.dp)
                )
            }

            when (page) {
                "Equalizer" -> EqualizerSettingsSection(state, settingsViewModel)
                "Crossfade" -> PlaybackSettingsEditor(state, settingsViewModel)
                "3D sound" -> ThreeDSoundSettingsSection(state, settingsViewModel)
                "Headset" -> HeadsetSettingsSection(state, settingsViewModel)
                "Bluetooth" -> Column(Modifier.fillMaxWidth()) {
                    Text("Audio output", color = XvoxTheme.colors.primaryAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    SettingsSectionPreview(
                        title = "Headset",
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .padding(bottom = 12.dp)
                    )
                    AudioOutputContent(state, settingsViewModel)
                }
                "Lyrics" -> LyricsSettingsSection(state, settingsViewModel)
                else -> Column(Modifier.fillMaxWidth()) {
                    for (name in listOf("Equalizer", "Crossfade", "3D sound", "Headset", "Bluetooth", "Lyrics")) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .xvoxPressScale { page = name }
                                .padding(vertical = 14.dp)
                        ) {
                            Text(
                                text = name,
                                color = XvoxTheme.colors.primaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
