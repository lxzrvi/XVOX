package com.xvox.music.features.settings.sections

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsControlsEditor
import com.xvox.music.features.settings.components.SettingsToggle
import com.xvox.music.features.settings.components.XvoxThinLineSlider

@Composable
fun PlaybackSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    showPreview: Boolean = true
) {
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showPreview) com.xvox.music.features.settings.components.CrossfadeSettingsPreview(state)

        SettingsToggle("Crossfade", null, state.crossfade, viewModel::setCrossfade)

        if (state.crossfade) {
            SettingsAccordionItem(
                title = "Transition length · ${state.crossfadeDuration}s",
                expanded = expandedGroup == "Duration",
                onToggle = { toggle("Duration") }
            ) {
                XvoxThinLineSlider(
                    value = state.crossfadeDuration.toFloat(),
                    valueRange = 1f..12f,
                    onValueChange = { viewModel.setCrossfadeDuration(kotlin.math.round(it).toInt()) }
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 5, 7, 10, 12).forEach { sec ->
                        Choice("${sec}s", state.crossfadeDuration == sec, Modifier.weight(1f)) {
                            viewModel.setCrossfadeDuration(sec)
                        }
                    }
                }
            }

            SettingsAccordionItem(
                title = "Smart blend & Beat align",
                expanded = expandedGroup == "Blend",
                onToggle = { toggle("Blend") }
            ) {
                SettingsToggle("Seamless blend", null, state.crossfadeSmart, viewModel::setCrossfadeSmart)
                if (state.crossfadeSmart) {
                    Spacer(Modifier.height(8.dp))
                    Label("Bass hand-off · ${(state.crossfadeClashControl * 100).toInt()}%")
                    XvoxThinLineSlider(
                        state.crossfadeClashControl,
                        viewModel::setCrossfadeClashControl,
                        0f..1f
                    )
                }
                Spacer(Modifier.height(8.dp))
                SettingsToggle("Beat align", null, state.crossfadeBeatSync, viewModel::setCrossfadeBeatSync)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        color = XvoxTheme.colors.primaryAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun Choice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val fill by animateColorAsState(if (selected) colors.primaryAccent else colors.cardElevated, tween(180), label = "fill")
    val border by animateColorAsState(if (selected) colors.primaryAccent else colors.cardBorder, tween(180), label = "border")
    Box(
        modifier = modifier.height(38.dp).clip(shape).background(fill)
            .border(if (selected) 1.6.dp else 0.9.dp, border, shape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, color = if (selected) colors.background else colors.primaryText,
            fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PlaybackSettingsEditor(state: SettingsState, viewModel: SettingsViewModel) {
    SettingsControlsEditor(
        controls = { PlaybackSettingsSection(state, viewModel, showPreview = false) }
    )
}
