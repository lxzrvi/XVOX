package com.xvox.music.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.home.HomeFooter
import com.xvox.music.features.home.HomeGeometry
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.sections.AppearanceSettingsSection
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.sections.NotificationSettingsSection
import com.xvox.music.features.settings.sections.PlaybackSettingsSection
import com.xvox.music.features.settings.sections.VolumeSettingsSection
import com.xvox.music.features.settings.sections.WidgetSettingsSection

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val state by settingsViewModel.state.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "settings_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = HomeGeometry.sectionGap,
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    color = colors.primaryAccent,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item(key = "appearance_section") {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                AppearanceSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        item(key = "equalizer_section") {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                EqualizerSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        item(key = "playback_section") {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                PlaybackSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        item(key = "volume_section") {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                VolumeSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        item(key = "notification_section") {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                NotificationSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        item(key = "widget_customizer") {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                WidgetSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        item(key = "settings_brand") {
            Spacer(Modifier.height(16.dp))
            HomeFooter(modifier = Modifier.fillMaxWidth().height(180.dp).padding(bottom = 20.dp))
        }

        item(key = "settings_bottom_inset") { Spacer(Modifier.height(80.dp)) }
    }
}
