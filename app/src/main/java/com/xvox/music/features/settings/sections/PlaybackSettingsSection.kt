package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
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
 * State-hoisted form used by the Now Playing sheet. Its host forwards every [onStateChange]
 * immediately to Settings, while this composable stays reusable without owning a ViewModel.
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
            // Each live amount has its own clear material rather than leaving a rail loose among
            // toggles.  This holds both the label and slider together in short and tall sheets.
            CrossfadeOptionBox(title = "Transition length · ${state.crossfadeDuration}s") {
                XvoxSlider(
                    value = state.crossfadeDuration.toFloat(),
                    onValueChange = { onDuration(it.roundToInt()) },
                    valueRange = 1f..20f,
                    defaultValue = 3f,
                    steps = 19,
                    valueLabel = { "${it.roundToInt()} s" }
                )
            }

            CrossfadeOptionBox(title = "Smart blend & beat align") {
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

/** A boxed crossfade control group keeps its label and slider visibly associated. */
@Composable
private fun CrossfadeOptionBox(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = com.xvox.music.core.design.theme.XvoxTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated.copy(alpha = .64f))
            .border(.8.dp, colors.cardBorder.copy(alpha = .78f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        content = {
            androidx.compose.material3.Text(
                text = title,
                color = colors.primaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    )
}
