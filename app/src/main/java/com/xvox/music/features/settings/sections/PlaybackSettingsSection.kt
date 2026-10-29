package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
        onBeatSync = viewModel::setCrossfadeBeatSync,
        onCurveStyle = viewModel::setCrossfadeCurveStyle
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
        onBeatSync = { value -> onStateChange(state.copy(crossfadeBeatSync = value)) },
        onCurveStyle = { value -> onStateChange(state.copy(crossfadeCurveStyle = value)) }
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
    onBeatSync: (Boolean) -> Unit,
    onCurveStyle: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showPreview) com.xvox.music.features.settings.components.CrossfadeSettingsPreview(state)

        // Crossfade owns one complete group: its primary switch, label, timing rail, Smart Blend,
        // Seamless Blend and Beat Align all remain in the same box when it is active.
        CrossfadeOptionBox(title = "Playback & crossfade") {
            SettingsToggle(
                title = "Crossfade",
                subtitle = "Smoothly transition between consecutive songs",
                checked = state.crossfade,
                onChange = onCrossfade
            )
            if (state.crossfade) {
                androidx.compose.material3.Text(
                    text = "Transition length · ${state.crossfadeDuration}s",
                    color = com.xvox.music.core.design.theme.XvoxTheme.colors.primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                XvoxSlider(
                    value = state.crossfadeDuration.toFloat(),
                    onValueChange = { onDuration(it.roundToInt()) },
                    valueRange = 1f..20f,
                    defaultValue = 3f,
                    steps = 19,
                    valueLabel = { "${it.roundToInt()} s" }
                )
                androidx.compose.material3.Text(
                    text = "Blend curve",
                    color = com.xvox.music.core.design.theme.XvoxTheme.colors.primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                CrossfadeCurveChoice(state.crossfadeCurveStyle, onCurveStyle)

                androidx.compose.material3.Text(
                    text = "Smart blend",
                    color = com.xvox.music.core.design.theme.XvoxTheme.colors.primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                SettingsToggle("Seamless Blend", null, state.crossfadeSmart, onSmartBlend)
                if (state.crossfadeSmart) {
                    androidx.compose.material3.Text(
                        text = "Bass hand-off · ${(state.crossfadeClashControl * 100).roundToInt()}%",
                        color = com.xvox.music.core.design.theme.XvoxTheme.colors.secondaryText,
                        fontSize = 11.sp
                    )
                    XvoxSlider(
                        value = state.crossfadeClashControl,
                        onValueChange = onClashControl,
                        valueRange = 0f..1f,
                        defaultValue = .7f,
                        steps = 20,
                        valueLabel = { "Bass hand-off · ${(it * 100).roundToInt()}%" }
                    )
                }
                SettingsToggle("Beat Align", null, state.crossfadeBeatSync, onBeatSync)
            }
        }

        // Gapless Playback is the next distinct option group, never visually detached from the
        // playback sheet or accidentally nested in Crossfade's active-only controls.
        CrossfadeOptionBox(title = "Gapless playback") {
            SettingsToggle(
                title = "Gapless Playback",
                subtitle = "Trim empty silence at the end of songs for instant seamless playback",
                checked = state.gapless,
                onChange = onGapless
            )
        }
    }
}


/** Three timing families, deliberately rendered as one compact segment group. */
@Composable
private fun CrossfadeCurveChoice(selected: Int, onSelect: (Int) -> Unit) {
    val colors = com.xvox.music.core.design.theme.XvoxTheme.colors
    val labels = listOf(
        com.xvox.music.player.playback.CrossfadeMath.CURVE_EQUAL_POWER to "Equal power",
        com.xvox.music.player.playback.CrossfadeMath.CURVE_LATE to "Late",
        com.xvox.music.player.playback.CrossfadeMath.CURVE_SOFT to "Soft"
    )
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEach { (style, label) ->
            val active = selected == style
            val shape = RoundedCornerShape(10.dp)
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (active) colors.primaryAccent else colors.card)
                    .border(.7.dp, if (active) colors.primaryAccent else colors.cardBorder.copy(alpha = .72f), shape)
                    .com.xvox.music.core.ui.effects.xvoxPressScale { onSelect(style) }
                    .padding(horizontal = 5.dp, vertical = 8.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = label,
                    color = if (active) colors.background else colors.secondaryText,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
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

/** A single material group keeps each playback option, its labels, and any slider together. */
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
