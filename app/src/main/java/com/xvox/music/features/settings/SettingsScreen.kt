package com.xvox.music.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.sections.AboutSettingsSection
import com.xvox.music.features.settings.sections.AppearanceSettingsSection
import com.xvox.music.features.settings.sections.AppResetSettingsSection
import com.xvox.music.features.settings.sections.BackupSettingsSection
import com.xvox.music.features.settings.sections.BatteryOptimizationSection
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.sections.HeadsetSettingsSection
import com.xvox.music.features.settings.sections.HiddenSongsSettingsSection
import com.xvox.music.features.settings.sections.HomeSettingsSection
import com.xvox.music.features.settings.sections.HowToUseSettingsSection
import com.xvox.music.features.settings.sections.LibraryFilterSettingsSection
import com.xvox.music.features.settings.sections.LyricsSettingsSection
import com.xvox.music.features.settings.sections.NotifySettingsSection
import com.xvox.music.features.settings.sections.PlaybackSettingsEditor
import com.xvox.music.features.settings.sections.ThreeDSoundSettingsSection
import com.xvox.music.features.settings.sections.WidgetSettingsSection

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    topResetKey: Long = 0L,
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val state by settingsViewModel.state.collectAsState()
    val scrollState = rememberLazyListState()

    var expandedSection by remember { mutableStateOf(state.lastSettingsTab.ifBlank { "Appearance" }) }

    LaunchedEffect(topResetKey) {
        runCatching { scrollState.scrollToItem(0) }
    }

    LaunchedEffect(expandedSection) {
        settingsViewModel.setLastSettingsTab(expandedSection)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = LocalXvoxTopInset.current + 4.dp)
    ) {
        Text(
            text = "Settings",
            color = colors.primaryAccent,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
        )

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 10.dp,
                end = 10.dp,
                bottom = LocalXvoxBottomInset.current + 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "section_appearance") {
                SettingsSectionCard(
                    title = "Appearance",
                    expanded = expandedSection == "Appearance",
                    onToggle = { expandedSection = if (expandedSection == "Appearance") "" else "Appearance" }
                ) {
                    AppearanceSettingsSection(state, settingsViewModel)
                }
            }

            item(key = "section_home") {
                SettingsSectionCard(
                    title = "Home",
                    expanded = expandedSection == "Home",
                    onToggle = { expandedSection = if (expandedSection == "Home") "" else "Home" }
                ) {
                    HomeSettingsSection(state, settingsViewModel)
                }
            }

            item(key = "section_lyrics") {
                SettingsSectionCard(
                    title = "Lyrics",
                    expanded = expandedSection == "Lyrics",
                    onToggle = { expandedSection = if (expandedSection == "Lyrics") "" else "Lyrics" }
                ) {
                    LyricsSettingsSection(state, settingsViewModel)
                }
            }

            item(key = "section_eq") {
                SettingsSectionCard(
                    title = "Equalizer",
                    expanded = expandedSection == "Equalizer",
                    onToggle = { expandedSection = if (expandedSection == "Equalizer") "" else "Equalizer" }
                ) {
                    EqualizerSettingsSection(state, settingsViewModel)
                }
            }

            item(key = "section_3d") {
                SettingsSectionCard(
                    title = "3D Sound",
                    expanded = expandedSection == "3D sound",
                    onToggle = { expandedSection = if (expandedSection == "3D sound") "" else "3D sound" }
                ) {
                    ThreeDSoundSettingsSection(state, settingsViewModel)
                }
            }

            item(key = "section_playback") {
                SettingsSectionCard(
                    title = "Playback & Crossfade",
                    expanded = expandedSection == "Crossfade",
                    onToggle = { expandedSection = if (expandedSection == "Crossfade") "" else "Crossfade" }
                ) {
                    PlaybackSettingsEditor(state, settingsViewModel)
                }
            }

            item(key = "section_headset") {
                SettingsSectionCard(
                    title = "Headset",
                    expanded = expandedSection == "Headset",
                    onToggle = { expandedSection = if (expandedSection == "Headset") "" else "Headset" }
                ) {
                    HeadsetSettingsSection(state, settingsViewModel)
                }
            }

            item(key = "section_widget") {
                SettingsSectionCard(
                    title = "Widget",
                    expanded = expandedSection == "Widget",
                    onToggle = { expandedSection = if (expandedSection == "Widget") "" else "Widget" }
                ) {
                    WidgetSettingsSection(state, settingsViewModel)
                }
            }

            item(key = "section_notify") {
                SettingsSectionCard(
                    title = "Notifications",
                    expanded = expandedSection == "Notify",
                    onToggle = { expandedSection = if (expandedSection == "Notify") "" else "Notify" }
                ) {
                    NotifySettingsSection(state, settingsViewModel)
                }
            }

            item(key = "section_filter") {
                SettingsSectionCard(
                    title = "Library Filters",
                    expanded = expandedSection == "Library filter",
                    onToggle = { expandedSection = if (expandedSection == "Library filter") "" else "Library filter" }
                ) {
                    LibraryFilterSettingsSection(state, settingsViewModel, homeViewModel)
                }
            }

            item(key = "section_deleted") {
                SettingsSectionCard(
                    title = "Deleted Songs & Artists",
                    expanded = expandedSection == "Deleted songs",
                    onToggle = { expandedSection = if (expandedSection == "Deleted songs") "" else "Deleted songs" }
                ) {
                    HiddenSongsSettingsSection(homeViewModel)
                }
            }

            item(key = "section_backup") {
                SettingsSectionCard(
                    title = "Backup & Restore",
                    expanded = expandedSection == "Backup",
                    onToggle = { expandedSection = if (expandedSection == "Backup") "" else "Backup" }
                ) {
                    BackupSettingsSection(state, settingsViewModel)
                }
            }

            item(key = "section_battery") {
                SettingsSectionCard(
                    title = "Battery Optimization",
                    expanded = expandedSection == "Don't kill app",
                    onToggle = { expandedSection = if (expandedSection == "Don't kill app") "" else "Don't kill app" }
                ) {
                    BatteryOptimizationSection()
                }
            }

            item(key = "section_reset") {
                SettingsSectionCard(
                    title = "App Reset",
                    expanded = expandedSection == "Reset",
                    onToggle = { expandedSection = if (expandedSection == "Reset") "" else "Reset" }
                ) {
                    AppResetSettingsSection()
                }
            }

            item(key = "section_how_to") {
                SettingsSectionCard(
                    title = "How to Use",
                    expanded = expandedSection == "How to use",
                    onToggle = { expandedSection = if (expandedSection == "How to use") "" else "How to use" }
                ) {
                    HowToUseSettingsSection()
                }
            }

            item(key = "section_about") {
                SettingsSectionCard(
                    title = "About",
                    expanded = expandedSection == "About",
                    onToggle = { expandedSection = if (expandedSection == "About") "" else "About" }
                ) {
                    AboutSettingsSection()
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.tap()
                    onToggle()
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = if (expanded) colors.primaryAccent else colors.primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Icon(
                painter = painterResource(
                    if (expanded) R.drawable.ic_xvox_chevron_up else R.drawable.ic_xvox_chevron_down
                ),
                contentDescription = null,
                tint = if (expanded) colors.primaryAccent else colors.secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(220)),
            exit = shrinkVertically(tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
            ) {
                content()
            }
        }
    }
}
