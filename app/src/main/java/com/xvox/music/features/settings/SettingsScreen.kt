package com.xvox.music.features.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            // Balanced Symmetrical Rows in Landscape Mode + Full Width About Card
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Appearance & Library
                item(key = "row_1") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AppearanceSectionCard(state, settingsViewModel, onOpenColorWheel = { showCustomColorDialog = true }, modifier = Modifier.fillMaxHeight())
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            LibrarySectionCard(state, settingsViewModel, homeViewModel, overlays, modifier = Modifier.fillMaxHeight())
                        }
                    }
                }

                // Row 2: System & Backup
                item(key = "row_2") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            SystemSectionCard(state, settingsViewModel, modifier = Modifier.fillMaxHeight())
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            BackupSectionCard(homeViewModel, modifier = Modifier.fillMaxHeight())
                        }
                    }
                }

                // Full Width About Box
                item(key = "row_about") {
                    AboutSectionCard()
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
            Text("Theme", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            val themeOptions = listOf(
                "System" to "System",
                "Light" to "Light",
                "Dark" to "Dark",
                "AMOLED" to "AMOLED"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                themeOptions.forEach { (key, label) ->
                    val isSelected = state.theme.equals(key, ignoreCase = true)
                    val btnBg = when (key) {
                        "Light" -> Color(0xFFF2F2F7)
                        "Dark" -> Color(0xFF1C1C1E)
                        "AMOLED" -> Color(0xFF000000)
                        else -> colors.cardElevated
                    }
                    val textColor = when (key) {
                        "Light" -> Color(0xFF111111)
                        "Dark" -> Color(0xFFEBEBF5)
                        "AMOLED" -> Color(0xFFFFFFFF)
                        else -> colors.primaryText
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(btnBg)
                            .then(
                                if (isSelected) Modifier.border(2.dp, colors.primaryAccent, RoundedCornerShape(10.dp))
                                else Modifier.border(0.8.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                            )
                            .xvoxPressScale { viewModel.setTheme(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text("Accent Color", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            val accentOptions = listOf(
                "White" to "White",
                "Red" to "Red",
                "Blue" to "Blue",
                "custom" to "Custom"
            )
            val isCustomActive = state.accentColor.startsWith("#")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                accentOptions.forEach { (key, label) ->
                    val isSelected = if (key == "custom") isCustomActive else (!isCustomActive && state.accentColor.equals(key, ignoreCase = true))
                    val multiGradient = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFF3B30),
                            Color(0xFFFF9500),
                            Color(0xFF34C759),
                            Color(0xFF007AFF),
                            Color(0xFFAF52DE)
                        )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected && key != "custom") colors.primaryAccent else colors.cardElevated)
                            .then(
                                if (!isSelected) Modifier.border(0.8.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                                else if (key == "custom") Modifier.border(2.dp, colors.primaryAccent, RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .xvoxPressScale {
                                if (key == "custom") onOpenColorWheel()
                                else viewModel.setAccentColor(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "Red" -> Text(
                                text = label,
                                color = if (isSelected) colors.background else Color(0xFFFF453A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            "Blue" -> Text(
                                text = label,
                                color = if (isSelected) colors.background else Color(0xFF0A84FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            "custom" -> Text(
                                text = label,
                                style = androidx.compose.material3.LocalTextStyle.current.copy(
                                    brush = multiGradient,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            else -> Text(
                                text = label,
                                color = if (isSelected) colors.background else colors.primaryText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text("Text Scale", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            val sizeOptions = listOf(
                Triple(0.80f, "Small", 10.5.sp),
                Triple(1.00f, "Medium", 12.5.sp),
                Triple(1.20f, "Large", 14.5.sp),
                Triple(1.40f, "Extra", 16.5.sp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sizeOptions.forEach { (scale, label, fontSize) ->
                    val isSelected = kotlin.math.abs(state.fontSizeScale - scale) < 0.10f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                            .then(
                                if (!isSelected) Modifier.border(0.8.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .xvoxPressScale {
                                viewModel.setFontSizeScale(scale)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) colors.background else colors.primaryText,
                            fontSize = fontSize,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Hide Notification Bar",
                        color = colors.primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Hide system status bar for immersive view",
                        color = colors.secondaryText,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = state.hideStatusBar,
                    onCheckedChange = { viewModel.setHideStatusBar(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.background,
                        checkedTrackColor = colors.primaryAccent,
                        uncheckedThumbColor = colors.secondaryText,
                        uncheckedTrackColor = colors.card
                    )
                )
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
