package com.xvox.music.features.settings

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.features.home.HomeGeometry
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.components.SettingsSectionPreview
import com.xvox.music.features.settings.sections.AboutSettingsSection
import com.xvox.music.features.settings.sections.AppearanceSettingsSection
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

private enum class SettingsChoice(val label: String, val hasPreview: Boolean = true) {
    APPEARANCE("Appearance", hasPreview = true),
    HOME("Home", hasPreview = true),
    LYRICS("Lyrics", hasPreview = true),
    EQUALIZER("Equalizer", hasPreview = true),
    THREE_D("3D sound", hasPreview = true),
    CROSSFADE("Crossfade", hasPreview = true),
    HEADSET("Headset", hasPreview = true),
    WIDGET("Widget", hasPreview = true),
    NOTIFY("Notify", hasPreview = false),
    FILTER("Library filter", hasPreview = false),
    DELETED("Deleted songs", hasPreview = false),
    BACKUP("Backup", hasPreview = false),
    BATTERY("Don't kill app", hasPreview = false),
    HOW_TO_USE("How to use", hasPreview = false),
    ABOUT("About", hasPreview = false)
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    topResetKey: Long = 0L,
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val state by settingsViewModel.state.collectAsState()
    var choice by remember { mutableStateOf(SettingsChoice.APPEARANCE) }
    var restored by remember { mutableStateOf(false) }
    val controlsState = rememberLazyListState()
    val chipsState = rememberLazyListState()

    LaunchedEffect(state.lastSettingsTab) {
        if (!restored) {
            val saved = SettingsChoice.entries.firstOrNull { it.label == state.lastSettingsTab }
            if (saved != null) choice = saved
            restored = true
        }
    }

    LaunchedEffect(topResetKey) {
        runCatching { controlsState.scrollToItem(0) }
        runCatching { chipsState.scrollToItem(0) }
    }

    LaunchedEffect(choice) { settingsViewModel.setLastSettingsTab(choice.label) }

    LaunchedEffect(choice) {
        runCatching { chipsState.animateScrollToItem(SettingsChoice.entries.indexOf(choice)) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = LocalXvoxTopInset.current + 4.dp,
                bottom = 0.dp
            )
    ) {
        Text(
            text = "Settings",
            color = colors.primaryAccent,
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = HomeGeometry.sectionGap)
        )

        LazyRow(
            state = chipsState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(SettingsChoice.entries, key = { it.name }) { entry ->
                SettingsChoiceChip(entry.label, entry == choice) { choice = entry }
            }
        }

        Spacer(Modifier.height(HomeGeometry.sectionGap))

        val showPreview = choice.hasPreview && !state.previewHidden
        if (showPreview) {
            SettingsPreviewPane(
                choice = choice,
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f)
                    .padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(HomeGeometry.sectionGap))
        }

        LazyColumn(
            state = controlsState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (showPreview) 0.58f else 1f),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = LocalXvoxBottomInset.current + 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "controls_${choice.name}") {
                when (choice) {
                    SettingsChoice.APPEARANCE -> AppearanceSettingsSection(state, settingsViewModel)
                    SettingsChoice.HOME -> HomeSettingsSection(state, settingsViewModel)
                    SettingsChoice.LYRICS -> LyricsSettingsSection(state, settingsViewModel)
                    SettingsChoice.EQUALIZER -> EqualizerSettingsSection(state, settingsViewModel)
                    SettingsChoice.THREE_D -> ThreeDSoundSettingsSection(state, settingsViewModel)
                    SettingsChoice.CROSSFADE -> PlaybackSettingsEditor(state, settingsViewModel)
                    SettingsChoice.HEADSET -> HeadsetSettingsSection(state, settingsViewModel)
                    SettingsChoice.WIDGET -> WidgetSettingsSection(state, settingsViewModel)
                    SettingsChoice.NOTIFY -> NotifySettingsSection(state, settingsViewModel)
                    SettingsChoice.FILTER -> LibraryFilterSettingsSection(state, settingsViewModel, homeViewModel)
                    SettingsChoice.DELETED -> HiddenSongsSettingsSection(homeViewModel)
                    SettingsChoice.BACKUP -> BackupSettingsSection(state, settingsViewModel)
                    SettingsChoice.BATTERY -> BatteryOptimizationSection()
                    SettingsChoice.HOW_TO_USE -> HowToUseSettingsSection()
                    SettingsChoice.ABOUT -> AboutSettingsSection()
                }
            }
        }
    }
}

@Composable
private fun SettingsChoiceChip(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) colors.primaryAccent else colors.card)
            .border(1.dp, if (active) colors.primaryAccent else colors.cardBorder, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) colors.background else colors.primaryText,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingsPreviewPane(choice: SettingsChoice, state: SettingsState, modifier: Modifier) {
    Box(modifier) {
        SettingsSectionPreview(
            title = choice.label,
            state = state,
            modifier = Modifier.fillMaxSize()
        )
    }
}
