package com.xvox.music.features.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.home.HomeGeometry
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.sections.AboutSettingsSection
import com.xvox.music.features.settings.sections.AppearanceSettingsSection
import com.xvox.music.features.settings.sections.BatteryOptimizationSection
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.sections.HomeSettingsSection
import com.xvox.music.features.settings.sections.BackupSettingsSection
import com.xvox.music.features.settings.sections.HeadsetSettingsSection
import com.xvox.music.features.settings.sections.HowToUseSettingsSection
import com.xvox.music.features.settings.sections.LibraryFilterSettingsSection
import com.xvox.music.features.settings.sections.NotifySettingsSection
import com.xvox.music.features.settings.sections.ThreeDSoundSettingsSection
import com.xvox.music.features.settings.sections.WidgetSettingsSection

enum class SettingsAccordionKey {
    NONE,
    APPEARANCE,
    HOME,
    LYRICS,
    XVOX_MIX,
    THREE_D,
    CROSSFADE,
    HEADSET,
    BACKUP,
    NOTIFY,
    FILTER,
    BATTERY,
    WIDGET,
    HOW_TO_USE,
    ABOUT,
    HIDDEN_SONGS
}

/**
 * Settings: short labels, no paragraphs.
 *
 * Two navigation rules the app now keeps:
 *  - Back collapses whatever is open before it ever leaves Settings.
 *  - Opening the tab always shows the top of the list with every row collapsed, regardless of
 *    where the previous visit was left.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    topResetKey: Long = 0L,
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val overlays = com.xvox.music.core.ui.overlay.LocalXvoxOverlayController.current
    val colors = XvoxTheme.colors
    val state by settingsViewModel.state.collectAsState()
    var expandedKey by remember { mutableStateOf(SettingsAccordionKey.NONE) }
    val listState = rememberLazyListState()

    // Every fresh entry into the Settings tab lands on the top of the list, fully collapsed.
    LaunchedEffect(topResetKey) {
        expandedKey = SettingsAccordionKey.NONE
        listState.scrollToItem(0)
    }

    // Back closes the open row first; only a collapsed Settings hands Back to the shell.
    BackHandler(enabled = expandedKey != SettingsAccordionKey.NONE) {
        expandedKey = SettingsAccordionKey.NONE
    }

    fun openEditor(title: String) {
        overlays.showBox(title) {
            val live by settingsViewModel.state.collectAsState()
            when (title) {
                "Home" -> HomeSettingsSection(live, settingsViewModel)
                "Lyrics" -> com.xvox.music.features.settings.sections.LyricsSettingsSection(live, settingsViewModel)
                "Equalizer" -> EqualizerSettingsSection(live, settingsViewModel)
                "3D sound" -> ThreeDSoundSettingsSection(live, settingsViewModel)
                "Crossfade" -> com.xvox.music.features.settings.sections.PlaybackSettingsEditor(live, settingsViewModel)
                "Headset" -> HeadsetSettingsSection(live, settingsViewModel)
                "Backup" -> BackupSettingsSection(live, settingsViewModel)
                "Notify" -> NotifySettingsSection(live, settingsViewModel)
                "Widgets" -> WidgetSettingsSection(live, settingsViewModel)
            }
        }
    }

    fun toggle(key: SettingsAccordionKey) {
        expandedKey = if (expandedKey == key) SettingsAccordionKey.NONE else key
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = com.xvox.music.core.ui.navigation.LocalXvoxTopInset.current + 4.dp,
            bottom = com.xvox.music.core.ui.navigation.LocalXvoxBottomInset.current),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "settings_header_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = HomeGeometry.sectionGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    color = colors.primaryAccent,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item(key = "accordion_appearance") {
            SettingsAccordionItem(
                title = "Appearance",
                iconRes = R.drawable.ic_xvox_sparkle,
                expanded = expandedKey == SettingsAccordionKey.APPEARANCE,
                onToggle = { toggle(SettingsAccordionKey.APPEARANCE) }
            ) {
                AppearanceSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        // Playlists now live inside Home, where their layout actually applies.
        item(key = "accordion_home") {
            SettingsAccordionItem("Home", R.drawable.ic_xvox_home, false, { openEditor("Home") }) { }
        }

        item(key = "accordion_lyrics") {
            SettingsAccordionItem("Lyrics", R.drawable.ic_xvox_lyrics, false, { openEditor("Lyrics") }) { }
        }

        item(key = "accordion_equalizer") {
            SettingsAccordionItem("Equalizer", R.drawable.ic_xvox_equalizer, false, { openEditor("Equalizer") }) { }
        }

        item(key = "accordion_3d") {
            SettingsAccordionItem("3D sound", R.drawable.ic_xvox_waveform, false, { openEditor("3D sound") }) { }
        }

        item(key = "accordion_crossfade") {
            SettingsAccordionItem("Crossfade", R.drawable.ic_xvox_disc, false, { openEditor("Crossfade") }) { }
        }

        item(key = "accordion_headset") {
            SettingsAccordionItem("Headset", R.drawable.ic_xvox_music_note, false, { openEditor("Headset") }) { }
        }

        item(key = "accordion_widget") {
            SettingsAccordionItem("Widget", R.drawable.ic_xvox_settings, false, { openEditor("Widgets") }) { }
        }

        item(key = "accordion_backup") {
            SettingsAccordionItem("Backup & Restore", R.drawable.ic_xvox_folder, false, { openEditor("Backup") }) { }
        }

        item(key = "accordion_notify") {
            SettingsAccordionItem("Notify", R.drawable.ic_xvox_timer, false, { openEditor("Notify") }) { }
        }

        item(key = "accordion_filter") {
            SettingsAccordionItem(
                title = "Library filter",
                iconRes = R.drawable.ic_xvox_folder,
                expanded = expandedKey == SettingsAccordionKey.FILTER,
                onToggle = { toggle(SettingsAccordionKey.FILTER) }
            ) {
                LibraryFilterSettingsSection(
                    state = state,
                    viewModel = settingsViewModel,
                    homeViewModel = homeViewModel
                )
            }
        }

        item(key = "accordion_hidden_songs") {
            SettingsAccordionItem("Deleted songs", R.drawable.ic_xvox_music_note,
                expandedKey == SettingsAccordionKey.HIDDEN_SONGS, { toggle(SettingsAccordionKey.HIDDEN_SONGS) }) {
                com.xvox.music.features.settings.sections.HiddenSongsSettingsSection(homeViewModel)
            }
        }

        item(key = "accordion_battery") {
            SettingsAccordionItem(
                title = "Don't kill app",
                iconRes = R.drawable.ic_xvox_timer,
                expanded = expandedKey == SettingsAccordionKey.BATTERY,
                onToggle = { toggle(SettingsAccordionKey.BATTERY) }
            ) {
                BatteryOptimizationSection()
            }
        }

        item(key = "accordion_how_to_use") {
            SettingsAccordionItem(
                title = "How to use",
                iconRes = R.drawable.ic_xvox_info,
                expanded = expandedKey == SettingsAccordionKey.HOW_TO_USE,
                onToggle = { toggle(SettingsAccordionKey.HOW_TO_USE) }
            ) {
                HowToUseSettingsSection()
            }
        }

        item(key = "accordion_about") {
            SettingsAccordionItem(
                title = "About",
                iconRes = R.drawable.ic_xvox_info,
                expanded = expandedKey == SettingsAccordionKey.ABOUT,
                onToggle = { toggle(SettingsAccordionKey.ABOUT) }
            ) {
                AboutSettingsSection()
            }
        }

        item(key = "settings_bottom_spacing") {
            Spacer(Modifier.height(8.dp))
        }
    }
}
