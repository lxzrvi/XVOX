package com.xvox.music.features.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
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
import com.xvox.music.features.settings.components.ColorPickerRow
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.sections.AboutSettingsSection
import com.xvox.music.features.settings.sections.BackupSettingsSection
import com.xvox.music.features.settings.sections.BatteryOptimizationSection
import com.xvox.music.features.settings.sections.HiddenSongsSettingsSection
import com.xvox.music.features.settings.sections.LibraryFilterSettingsSection
import com.xvox.music.features.settings.sections.NotifySettingsSection

/**
 * Settings Screen:
 * - Theme-outlined bordered section boxes (colors.cardBorder, thin theme-based border).
 * - Accent color with preset buttons (White, Red, Blue) and dedicated Custom button opening the wheel.
 * - Text scale preset boxes (0.15x, 0.25x, 0.50x, 0.75x, 1.0x, 1.25x).
 * - Playback & Crossfade and Library Scan removed.
 * - Single-line side-by-side Backup Export / Import.
 * - Background playback toggle and daily reminder switch.
 * - Share XVOX button and comprehensive About section.
 * - 2-column grid in landscape mode.
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
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var showCustomColorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(topResetKey) {
        runCatching { scrollState.scrollToItem(0) }
    }

    if (showCustomColorDialog) {
        overlays.showBox("Custom Accent Color") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ColorPickerRow(
                    label = "Pick Accent Color",
                    hex = if (state.accentColor.startsWith("#")) state.accentColor else "#FFFFFF",
                    onColorChange = { hex -> settingsViewModel.setAccentColor(hex) },
                    subtitle = "Applies across all buttons and highlights"
                )
            }
        }
        showCustomColorDialog = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 2.dp)
    ) {
        Text(
            text = "Settings",
            color = colors.primaryAccent,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp)
        )

        val bottomPadding = LocalXvoxBottomInset.current + if (isLandscape) 16.dp else 40.dp

        if (isLandscape) {
            // Balanced 2-Column Vertical Stacks in Landscape Mode
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = bottomPadding)
            ) {
                item(key = "landscape_grid") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left Column: Appearance -> System & Reminders -> About XVOX
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppearanceSectionCard(state, settingsViewModel, onOpenColorWheel = { showCustomColorDialog = true })
                            SystemSectionCard(state, settingsViewModel)
                            AboutSectionCard()
                        }

                        // Right Column: Library Filters -> Backup & Restore
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LibrarySectionCard(state, settingsViewModel, homeViewModel, overlays)
                            BackupSectionCard(homeViewModel)
                        }
                    }
                }
            }
        } else {
            // Portrait Mode Single Column
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "section_appearance") {
                    AppearanceSectionCard(state, settingsViewModel, onOpenColorWheel = { showCustomColorDialog = true })
                }

                item(key = "section_library") {
                    LibrarySectionCard(state, settingsViewModel, homeViewModel, overlays)
                }

                item(key = "section_backup") {
                    BackupSectionCard(homeViewModel)
                }

                item(key = "section_system") {
                    SystemSectionCard(state, settingsViewModel)
                }

                item(key = "section_about") {
                    AboutSectionCard()
                }
            }
        }
    }
}

@Composable
private fun AppearanceSectionCard(
    state: SettingsState,
    viewModel: SettingsViewModel,
    onOpenColorWheel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    SettingsCardFrame(title = "Appearance & Theme", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Theme", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            SettingsChoiceRow(
                listOf("System" to "System", "Light" to "Light", "Dark" to "Dark", "AMOLED" to "AMOLED"),
                state.theme
            ) { viewModel.setTheme(it) }

            Spacer(Modifier.height(4.dp))

            Text("Accent Color", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("White", "Red", "Blue").forEach { colorKey ->
                    val isSelected = state.accentColor.equals(colorKey, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) colors.primaryAccent else colors.cardBorder.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .xvoxPressScale { viewModel.setAccentColor(colorKey) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = colorKey,
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Dedicated Custom Color Button next to Blue
                val isCustomActive = state.accentColor.startsWith("#")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCustomActive) colors.primaryAccent else colors.cardElevated)
                        .border(
                            width = 1.dp,
                            color = if (isCustomActive) colors.primaryAccent else colors.cardBorder.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .xvoxPressScale(onClick = onOpenColorWheel),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCustomActive) state.accentColor.uppercase() else "+ Custom",
                        color = if (isCustomActive) colors.background else colors.primaryText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text("Text Scale", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val scalePresets = listOf(
                    0.75f to "0.75x",
                    0.85f to "0.85x",
                    0.95f to "0.95x",
                    1.00f to "1.0x",
                    1.25f to "1.25x",
                    1.50f to "1.50x"
                )
                scalePresets.forEach { (scaleValue, label) ->
                    val isSelected = kotlin.math.abs(state.fontSizeScale - scaleValue) < 0.04f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) colors.primaryAccent else colors.cardBorder.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .xvoxPressScale { viewModel.setFontSizeScale(scaleValue) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySectionCard(
    state: SettingsState,
    viewModel: SettingsViewModel,
    homeViewModel: HomeViewModel,
    overlays: com.xvox.music.core.ui.overlay.XvoxOverlayController,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    SettingsCardFrame(title = "Library & Filters", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LibraryFilterSettingsSection(state, viewModel, homeViewModel)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated)
                    .xvoxPressScale {
                        overlays.showBox("Deleted Songs & Artists") {
                            HiddenSongsSettingsSection(homeViewModel)
                        }
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hidden & Deleted Tracks", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Restore songs or artists hidden from library", color = colors.secondaryText, fontSize = 11.sp)
                }
                Icon(painterResource(R.drawable.ic_xvox_caret_right), null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun BackupSectionCard(homeViewModel: HomeViewModel, modifier: Modifier = Modifier) {
    SettingsCardFrame(title = "Backup & Restore", modifier = modifier) {
        BackupSettingsSection(viewModel = homeViewModel)
    }
}

@Composable
private fun SystemSectionCard(state: SettingsState, viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    SettingsCardFrame(title = "System & Background", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BatteryOptimizationSection()
            NotifySettingsSection(state, viewModel)
        }
    }
}

@Composable
private fun AboutSectionCard(modifier: Modifier = Modifier) {
    SettingsCardFrame(title = "About XVOX", modifier = modifier) {
        AboutSettingsSection()
    }
}

@Composable
private fun SettingsCardFrame(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(0.9.dp, colors.cardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = colors.primaryAccent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        content()
    }
}
