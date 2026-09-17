package com.xvox.music.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.showLibraryRefresh
import com.xvox.music.features.settings.components.ColorPickerRow
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import com.xvox.music.features.settings.sections.AboutSettingsSection
import com.xvox.music.features.settings.sections.AppResetSettingsSection
import com.xvox.music.features.settings.sections.BackupSettingsSection
import com.xvox.music.features.settings.sections.BatteryOptimizationSection
import com.xvox.music.features.settings.sections.HiddenSongsSettingsSection
import com.xvox.music.features.settings.sections.LibraryFilterSettingsSection
import com.xvox.music.features.settings.sections.NotifySettingsSection

/**
 * Redesigned Settings Screen:
 * - Prominent large header title.
 * - Flat unboxed clean layout with open section headers.
 * - Direct inline toggles / switches for boolean options.
 * - Duplicated overlay settings filtered out (lyrics, equalizer, 3d sound, home layouts).
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    topResetKey: Long = 0L,
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current
    val state by settingsViewModel.state.collectAsState()
    val scrollState = rememberLazyListState()

    LaunchedEffect(topResetKey) {
        runCatching { scrollState.scrollToItem(0) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp)
    ) {
        // Large Prominent Header Text
        Text(
            text = "Settings",
            color = colors.primaryAccent,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp)
        )

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                bottom = LocalXvoxBottomInset.current + 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Appearance & Theme
            item(key = "section_appearance") {
                SettingsSectionTitle("Appearance & Theme")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Theme", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    SettingsChoiceRow(
                        listOf("System" to "System", "Light" to "Light", "Dark" to "Dark", "AMOLED" to "AMOLED"),
                        state.theme
                    ) { settingsViewModel.setTheme(it) }

                    Spacer(Modifier.height(4.dp))

                    Text("Accent Colour", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    SettingsChoiceRow(
                        listOf("White" to "White", "Red" to "Red", "Blue" to "Blue"),
                        if (state.accentColor.startsWith("#")) "custom" else state.accentColor
                    ) { key -> if (key != "custom") settingsViewModel.setAccentColor(key) }

                    ColorPickerRow(
                        label = "Custom accent",
                        hex = if (state.accentColor.startsWith("#")) state.accentColor else "",
                        onColorChange = { hex -> settingsViewModel.setAccentColor(hex) },
                        subtitle = if (state.accentColor.startsWith("#")) "Active" else "Pick colour"
                    )

                    Spacer(Modifier.height(4.dp))

                    Text("Text Scale", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A", color = colors.mutedText, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                        XvoxThinLineSlider(
                            value = state.fontSizeScale,
                            onValueChange = settingsViewModel::setFontSizeScale,
                            valueRange = 0.8f..1.4f,
                            defaultValue = 1f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("A", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                    }
                }
            }

            // 2. Playback & Crossfade Engine
            item(key = "section_playback") {
                SettingsSectionTitle("Playback & Crossfade")

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsRowSwitch(
                        title = "Crossfade audio",
                        subtitle = "Smoothly blend track transitions without gap",
                        checked = state.crossfade,
                        onCheckedChange = settingsViewModel::setCrossfade
                    )

                    if (state.crossfade) {
                        Text(
                            "Transition length · ${state.crossfadeDuration}s",
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 4, 6, 8, 10, 12).forEach { sec ->
                                val isSelected = state.crossfadeDuration == sec
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                                        .xvoxPressScale { settingsViewModel.setCrossfadeDuration(sec) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        color = if (isSelected) colors.background else colors.primaryText,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        SettingsRowSwitch(
                            title = "Smart energy blend",
                            subtitle = "Automatic bass hand-off & frequency clash suppression",
                            checked = state.crossfadeSmart,
                            onCheckedChange = settingsViewModel::setCrossfadeSmart
                        )

                        SettingsRowSwitch(
                            title = "Beat align",
                            subtitle = "Align tempo beats during track transitions",
                            checked = state.crossfadeBeatSync,
                            onCheckedChange = settingsViewModel::setCrossfadeBeatSync
                        )
                    }

                    SettingsRowSwitch(
                        title = "Pause on disconnect",
                        subtitle = "Pause playback when headphones or Bluetooth disconnect",
                        checked = state.pauseOnHeadphoneDisconnect,
                        onCheckedChange = settingsViewModel::setPauseOnHeadphoneDisconnect
                    )

                    SettingsRowSwitch(
                        title = "Auto-play on connect",
                        subtitle = "Resume playback when headphones or Bluetooth connect",
                        checked = state.playOnHeadsetConnect,
                        onCheckedChange = settingsViewModel::setPlayOnHeadsetConnect
                    )
                }
            }

            // 3. Library & Storage
            item(key = "section_library") {
                SettingsSectionTitle("Library & Storage")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .xvoxPressScale {
                                showLibraryRefresh(overlays, homeViewModel)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Scan Library", color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text("Rescan device audio files and indexed metadata", color = colors.secondaryText, fontSize = 11.sp)
                        }
                        Icon(painterResource(R.drawable.ic_xvox_refresh), null, tint = colors.primaryAccent, modifier = Modifier.size(19.dp))
                    }

                    LibraryFilterSettingsSection(state, settingsViewModel, homeViewModel)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .xvoxPressScale {
                                overlays.showBox("Deleted Songs & Artists") {
                                    HiddenSongsSettingsSection(homeViewModel)
                                }
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hidden & Deleted Tracks", color = colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text("Restore songs or artists hidden from library", color = colors.secondaryText, fontSize = 11.sp)
                        }
                        Icon(painterResource(R.drawable.ic_xvox_caret_right), null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 4. Backup & Restore
            item(key = "section_backup") {
                SettingsSectionTitle("Backup & Restore")
                BackupSettingsSection(state, settingsViewModel)
            }

            // 5. System & Battery
            item(key = "section_system") {
                SettingsSectionTitle("System")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BatteryOptimizationSection()
                    NotifySettingsSection(state, settingsViewModel)
                }
            }

            // 6. About & Reset
            item(key = "section_about") {
                SettingsSectionTitle("About & App Reset")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AboutSettingsSection()
                    AppResetSettingsSection()
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    val colors = XvoxTheme.colors
    Text(
        text = title,
        color = colors.primaryAccent,
        fontSize = 15.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SettingsRowSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable {
                haptics.tap()
                onCheckedChange(!checked)
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = colors.primaryText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = colors.secondaryText,
                    fontSize = 11.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.tap()
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.primaryAccent,
                uncheckedThumbColor = colors.secondaryText,
                uncheckedTrackColor = colors.cardElevated
            )
        )
    }
}
