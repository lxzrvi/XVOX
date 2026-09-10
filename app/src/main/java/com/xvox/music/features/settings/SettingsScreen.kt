package com.xvox.music.features.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.chrome.parseHexColor
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.features.home.HomeGeometry
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.settings.components.CrossfadeSettingsPreview
import com.xvox.music.features.settings.components.EqSettingsPreview
import com.xvox.music.features.settings.components.HomeSettingsPreview
import com.xvox.music.features.settings.components.SurroundSettingsPreview
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

/**
 * The choices Settings offers, laid out horizontally under the title.
 *
 * There is no accordion list any more: picking a choice shows that choice's controls in the lower
 * part of the screen while the top part keeps showing a live preview of whatever is being changed.
 */
private enum class SettingsChoice(val label: String) {
    APPEARANCE("Appearance"),
    HOME("Home"),
    LYRICS("Lyrics"),
    EQUALIZER("Equalizer"),
    THREE_D("3D sound"),
    CROSSFADE("Crossfade"),
    HEADSET("Headset"),
    WIDGET("Widget"),
    NOTIFY("Notify"),
    FILTER("Library filter"),
    DELETED("Deleted songs"),
    BACKUP("Backup"),
    BATTERY("Don't kill app"),
    HOW_TO_USE("How to use"),
    ABOUT("About")
}

/**
 * Settings: short labels, no paragraphs.
 *
 * Layout rule the app now keeps: the title stays at the top, the choices sit in one horizontal
 * row directly under it, the selected choice's controls fill the lower part of the screen and the
 * top 40% always shows a live preview of the setting being changed. Appearance is open first, and
 * opening the tab always comes back to Appearance at the top of its controls.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    topResetKey: Long = 0L,
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val state by settingsViewModel.state.collectAsState()
    // Appearance is the section the tab opens on.
    var choice by remember { mutableStateOf(SettingsChoice.APPEARANCE) }
    val controlsState = rememberLazyListState()
    val chipsState = rememberLazyListState()

    // Every fresh entry into the Settings tab lands on Appearance, scrolled to the top.
    LaunchedEffect(topResetKey) {
        choice = SettingsChoice.APPEARANCE
        runCatching { controlsState.scrollToItem(0) }
        runCatching { chipsState.scrollToItem(0) }
    }

    // Keep the active chip on screen when the choice changes.
    LaunchedEffect(choice) {
        runCatching { chipsState.animateScrollToItem(SettingsChoice.entries.indexOf(choice)) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = LocalXvoxTopInset.current + 4.dp,
                bottom = LocalXvoxBottomInset.current
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

        // Live preview: the top 40% of this same screen, following whatever is being changed.
        SettingsPreviewPane(
            choice = choice,
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
                .padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(HomeGeometry.sectionGap))

        // Controls for the selected choice.
        LazyColumn(
            state = controlsState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
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
                    SettingsChoice.FILTER ->
                        LibraryFilterSettingsSection(state, settingsViewModel, homeViewModel)
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

/** One horizontal choice under the Settings title. */
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

/**
 * The live preview shown in the top 40% of the Settings screen. Visual sections reuse the same
 * previews the app already had; the non-visual ones show a compact read-out of the live values,
 * so something on screen always answers "what did I just change?".
 */
@Composable
private fun SettingsPreviewPane(choice: SettingsChoice, state: SettingsState, modifier: Modifier) {
    val colors = XvoxTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        when (choice) {
            SettingsChoice.APPEARANCE -> ChromePreview(state.chromeStyle, state.accentColor)
            SettingsChoice.HOME -> HomeSettingsPreview(state)
            SettingsChoice.LYRICS -> LyricsPreview(state)
            SettingsChoice.EQUALIZER -> EqSettingsPreview(state)
            SettingsChoice.THREE_D -> SurroundSettingsPreview(state)
            SettingsChoice.CROSSFADE -> CrossfadeSettingsPreview(state)
            SettingsChoice.WIDGET -> WidgetPreview(state)
            else -> SettingsSummaryPreview(choice, state)
        }
    }
}

/** Header / mini player / nav pill, drawn with the current chrome alphas and pill colour. */
@Composable
private fun ChromePreview(chrome: XvoxChromeStyle, accentName: String) {
    val colors = XvoxTheme.colors
    val accent =
        if (accentName.startsWith("#")) parseHexColor(accentName) ?: colors.primaryAccent
        else colors.primaryAccent
    val pill = parseHexColor(chrome.pillColor) ?: colors.cardElevated

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.cardElevated.copy(alpha = chrome.headerBgAlpha))
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.cardElevated.copy(alpha = chrome.miniBgAlpha))
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardElevated.copy(alpha = chrome.navBgAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .width(64.dp)
                        .height(22.dp)
                        .clip(CircleShape)
                        .background(pill.copy(alpha = chrome.pillAlpha))
                )
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.cardElevated)
                )
            }
        }
    }
}

/** Sample lyric lines: current line in the accent at its size, the rest at their fade. */
@Composable
private fun LyricsPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val lyrics = state.lyrics
    val sample = listOf("Hold the night a little longer", "Every echo finds its way", "This is where we stay")
    val otherAlpha = if (lyrics.fadeEqual) 0.18f else (1f - lyrics.fadeIntensity).coerceIn(0.18f, 1f)

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        sample.forEachIndexed { index, line ->
            val current = index == 1
            Text(
                text = line,
                color = if (current) colors.primaryAccent else colors.primaryText.copy(alpha = otherAlpha),
                fontSize = (if (current) lyrics.currentSize else lyrics.otherSize).sp,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
        Text(
            text = "${lyrics.animation} · ${if (lyrics.offsetMs == 0) "0 ms" else "${lyrics.offsetMs} ms"}",
            color = colors.mutedText,
            fontSize = 10.sp
        )
    }
}

/** Small widget mock: cover square, labels and the live padding/margins. */
@Composable
private fun WidgetPreview(state: SettingsState) {
    val colors = XvoxTheme.colors
    val c = state.widgetCustomization
    val cover = (if (c.coverSize in 1..160) c.coverSize else 64).dp

    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardElevated.copy(alpha = (1f - state.widgetTransparency).coerceIn(0.15f, 1f)))
            .padding(
                horizontal = (20 + c.coverMarginX).coerceIn(0, 40).dp,
                vertical = (12 + c.coverMarginY).coerceIn(0, 32).dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(cover)
                .clip(RoundedCornerShape(if (c.coverRadius >= 0) c.coverRadius else state.widgetCornerRadius))
                .background(colors.card)
                .border(if (c.coverBorderWidth > 0f) c.coverBorderWidth.dp else 0.7.dp, colors.cardBorder,
                    RoundedCornerShape(if (c.coverRadius >= 0) c.coverRadius else state.widgetCornerRadius))
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Now playing", color = colors.primaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("XVOX widget", color = colors.secondaryText, fontSize = 9.sp)
        }
    }
}

/** Read-out for the sections that have no visual surface of their own. */
@Composable
private fun SettingsSummaryPreview(choice: SettingsChoice, state: SettingsState) {
    val colors = XvoxTheme.colors
    val rows = when (choice) {
        SettingsChoice.HEADSET -> listOf(
            "Output" to state.audioOutputRoute,
            "On disconnect" to state.btDisconnectAction,
            "On connect" to state.btConnectAction,
            "Pause when unplugged" to if (state.pauseOnHeadphoneDisconnect) "On" else "Off"
        )
        SettingsChoice.NOTIFY -> listOf(
            "Reminders" to if (state.remindersEnabled) "On" else "Off",
            "Notification bar" to "One media icon"
        )
        SettingsChoice.FILTER -> listOf(
            "Ignored folders" to state.ignoredFolders.size.toString(),
            "Shortest song" to "${state.ignoreBelowSec}s",
            "Smallest file" to "${state.ignoreBelowKb} KB"
        )
        SettingsChoice.DELETED -> listOf("Hidden songs" to "Managed from Home settings")
        SettingsChoice.BACKUP -> listOf(
            "Backup" to "Playlists, prefs & profile",
            "Restore" to "Pick a backup file"
        )
        SettingsChoice.BATTERY -> listOf("Background playback" to "Keep alive")
        SettingsChoice.HOW_TO_USE -> listOf("Gestures" to "Hold, swipe, scrub")
        SettingsChoice.ABOUT -> listOf("App" to "XVOX", "Build" to "Offline music player")
        else -> emptyList()
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text(value, color = colors.primaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
