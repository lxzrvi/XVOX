package com.xvox.music.features.settings

import androidx.compose.foundation.layout.Arrangement
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
import com.xvox.music.features.settings.sections.HowToUseSettingsSection
import com.xvox.music.features.settings.sections.LibraryFilterSettingsSection
import com.xvox.music.features.settings.sections.PlaybackSettingsSection
import com.xvox.music.features.settings.sections.WidgetSettingsSection

enum class SettingsAccordionKey {
    NONE,
    APPEARANCE,
    HOME,
    XVOX_MIX,
    PLAYBACK,
    FILTER,
    BATTERY,
    WIDGET,
    HOW_TO_USE,
    ABOUT
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val state by settingsViewModel.state.collectAsState()
    var expandedKey by remember { mutableStateOf(SettingsAccordionKey.NONE) }

    fun toggle(key: SettingsAccordionKey) {
        expandedKey = if (expandedKey == key) SettingsAccordionKey.NONE else key
    }

    LazyColumn(
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
                subtitle = "Themes, accent colors & font scale",
                iconRes = R.drawable.ic_xvox_sparkle,
                expanded = expandedKey == SettingsAccordionKey.APPEARANCE,
                onToggle = { toggle(SettingsAccordionKey.APPEARANCE) }
            ) {
                AppearanceSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        item(key = "accordion_home") {
            SettingsAccordionItem(
                title = "Home",
                subtitle = "Generated mosaics, free pan & Recents placement",
                iconRes = R.drawable.ic_xvox_home,
                expanded = expandedKey == SettingsAccordionKey.HOME,
                onToggle = { toggle(SettingsAccordionKey.HOME) }
            ) {
                HomeSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        item(key = "accordion_xvoxmix") {
            SettingsAccordionItem(
                title = "XvoxMix",
                subtitle = "Smooth EQ, boost protection & spatial orbit",
                iconRes = R.drawable.ic_xvox_equalizer,
                expanded = expandedKey == SettingsAccordionKey.XVOX_MIX,
                onToggle = { toggle(SettingsAccordionKey.XVOX_MIX) }
            ) {
                EqualizerSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        item(key = "accordion_playback") {
            SettingsAccordionItem(
                title = "Playback",
                subtitle = "Two-track blend & headset behaviour",
                iconRes = R.drawable.ic_xvox_disc,
                expanded = expandedKey == SettingsAccordionKey.PLAYBACK,
                onToggle = { toggle(SettingsAccordionKey.PLAYBACK) }
            ) {
                PlaybackSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        item(key = "accordion_filter") {
            SettingsAccordionItem(
                title = "Library Filter",
                subtitle = "Ignore short audio, small files & exclude folders",
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

        item(key = "accordion_battery") {
            SettingsAccordionItem(
                title = "Don't Kill App",
                subtitle = "Background playback & battery optimization exemption",
                iconRes = R.drawable.ic_xvox_timer,
                expanded = expandedKey == SettingsAccordionKey.BATTERY,
                onToggle = { toggle(SettingsAccordionKey.BATTERY) }
            ) {
                BatteryOptimizationSection()
            }
        }

        item(key = "accordion_widget") {
            SettingsAccordionItem(
                title = "Widget Customizer",
                subtitle = "Home screen widget styling, transparency & corners",
                iconRes = R.drawable.ic_xvox_settings,
                expanded = expandedKey == SettingsAccordionKey.WIDGET,
                onToggle = { toggle(SettingsAccordionKey.WIDGET) }
            ) {
                WidgetSettingsSection(state = state, viewModel = settingsViewModel)
            }
        }

        item(key = "accordion_how_to_use") {
            SettingsAccordionItem(
                title = "How To Use",
                subtitle = "Gestures, shortcuts, multi-select & feature guide",
                iconRes = R.drawable.ic_xvox_info,
                expanded = expandedKey == SettingsAccordionKey.HOW_TO_USE,
                onToggle = { toggle(SettingsAccordionKey.HOW_TO_USE) }
            ) {
                HowToUseSettingsSection()
            }
        }

        item(key = "accordion_about") {
            SettingsAccordionItem(
                title = "About XVOX",
                subtitle = "Local music, made personal",
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
