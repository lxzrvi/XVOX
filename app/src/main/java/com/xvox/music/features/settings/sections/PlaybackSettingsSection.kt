package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsControlsEditor
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxSlider
import kotlin.math.roundToInt

/** Direct-persistence form retained for the standalone Settings page. */
@Composable
fun PlaybackSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    showPreview: Boolean = true
) {
    PlaybackSettingsContent(
        state = state,
        showPreview = showPreview,
        onCrossfade = viewModel::setCrossfade,
        onGapless = viewModel::setGapless,
        onDuration = viewModel::setCrossfadeDuration,
        onSmartBlend = viewModel::setCrossfadeSmart,
        onClashControl = viewModel::setCrossfadeClashControl,
        onBeatSync = viewModel::setCrossfadeBeatSync
    )
}

/**
 * Local-draft form used by the Now Playing sheet. It deliberately makes no ViewModel calls;
 * callers persist [onStateChange] only after their Okay action.
 */
@Composable
fun PlaybackSettingsDraftSection(
    state: SettingsState,
    onStateChange: (SettingsState) -> Unit
) {
    PlaybackSettingsContent(
        state = state,
        showPreview = false,
        onCrossfade = { value -> onStateChange(state.copy(crossfade = value)) },
        onGapless = { value -> onStateChange(state.copy(gapless = value)) },
        onDuration = { value -> onStateChange(state.copy(crossfadeDuration = value)) },
        onSmartBlend = { value -> onStateChange(state.copy(crossfadeSmart = value)) },
        onClashControl = { value -> onStateChange(state.copy(crossfadeClashControl = value)) },
        onBeatSync = { value -> onStateChange(state.copy(crossfadeBeatSync = value)) }
    )
}

@Composable
private fun PlaybackSettingsContent(
    state: SettingsState,
    showPreview: Boolean,
    onCrossfade: (Boolean) -> Unit,
    onGapless: (Boolean) -> Unit,
    onDuration: (Int) -> Unit,
    onSmartBlend: (Boolean) -> Unit,
    onClashControl: (Float) -> Unit,
    onBeatSync: (Boolean) -> Unit
) {
    var expandedGroup by remember { mutableStateOf<String?>(null) }
    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showPreview) com.xvox.music.features.settings.components.CrossfadeSettingsPreview(state)

        SettingsToggle(
            title = "Crossfade",
            subtitle = "Smoothly transition between consecutive songs",
            checked = state.crossfade,
            onChange = onCrossfade
        )
        SettingsToggle(
            title = "Gapless Playback",
            subtitle = "Trim empty silence at the end of songs for instant seamless playback",
            checked = state.gapless,
            onChange = onGapless
        )

        if (state.crossfade) {
            SettingsAccordionItem(
                title = "Transition Length · ${state.crossfadeDuration}s",
                expanded = expandedGroup == "Duration",
                onToggle = { toggle("Duration") }
            ) {
                XvoxSlider(
                    value = state.crossfadeDuration.toFloat(),
                    onValueChange = { onDuration(it.roundToInt()) },
                    valueRange = 1f..20f,
                    defaultValue = 3f,
                    steps = 19,
                    valueLabel = { "${it.roundToInt()} s" }
                )
            }

            SettingsAccordionItem(
                title = "Smart Blend & Beat Align",
                expanded = expandedGroup == "Blend",
                onToggle = { toggle("Blend") }
            ) {
                SettingsToggle("Seamless Blend", null, state.crossfadeSmart, onSmartBlend)
                if (state.crossfadeSmart) {
                    Spacer(Modifier.height(8.dp))
                    XvoxSlider(
                        value = state.crossfadeClashControl,
                        onValueChange = onClashControl,
                        valueRange = 0f..1f,
                        defaultValue = .7f,
                        steps = 20,
                        valueLabel = { "Bass hand-off · ${(it * 100).roundToInt()}%" }
                    )
                }
                Spacer(Modifier.height(8.dp))
                SettingsToggle("Beat Align", null, state.crossfadeBeatSync, onBeatSync)
            }
        }
    }
}

@Composable
fun PlaybackSettingsEditor(state: SettingsState, viewModel: SettingsViewModel) {
    SettingsControlsEditor(
        controls = { PlaybackSettingsSection(state, viewModel, showPreview = false) }
    )
}
