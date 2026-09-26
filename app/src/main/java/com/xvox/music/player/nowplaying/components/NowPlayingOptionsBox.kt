package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.core.ui.overlay.XvoxBoxPresentation
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.player.nowplaying.XvoxBackgroundMethodOption
import com.xvox.music.player.nowplaying.XvoxBackgroundMethodStyles
import com.xvox.music.player.nowplaying.XvoxBackgroundTransitionOption
import com.xvox.music.player.nowplaying.XvoxBackgroundTransitionStyles

private enum class NowPlayingBackgroundEditor { TRANSITION, METHOD }

/**
 * First-level Now Playing options remain a sheet. The two 20-choice detail pickers are centred
 * boxes, keeping deeper choices separate from the player without exposing a cover-transition
 * setting—the artwork treatment is intentionally fixed to Depth.
 */
@Composable
fun NowPlayingOptionsBox(
    onDismiss: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val state by settingsViewModel.state.collectAsState()
    var editor by remember { mutableStateOf<NowPlayingBackgroundEditor?>(null) }
    val backgroundKey = XvoxBackgroundTransitionStyles.normalize(state.nowPlayingBackgroundTransition)
    val methodKey = XvoxBackgroundMethodStyles.normalize(state.nowPlayingBackgroundMethod)
    val selectedBackground = XvoxBackgroundTransitionStyles.options.first { it.key == backgroundKey }
    val selectedMethod = XvoxBackgroundMethodStyles.options.first { it.key == methodKey }
    val scrollState = rememberScrollState()

    XvoxBox(
        onDismiss = onDismiss,
        title = "Now Playing"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .xvoxBoxScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // This is informational rather than selectable: the revised artwork treatment is
            // deliberately consistent across all songs and all Now Playing layouts.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(colors.cardElevated)
                    .border(.9.dp, colors.primaryAccent.copy(alpha = .72f), RoundedCornerShape(15.dp))
                    .padding(13.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Artwork · Depth", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Depth is fixed for Now Playing cover changes", color = colors.secondaryText, fontSize = 11.sp)
                }
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_check),
                    contentDescription = "Depth artwork treatment active",
                    tint = colors.primaryAccent,
                    modifier = Modifier.align(Alignment.CenterEnd).size(18.dp)
                )
            }

            TransitionSettingRow(
                title = "Background Transition",
                value = selectedBackground.title,
                subtitle = selectedBackground.subtitle,
                onClick = { haptics.tap(); editor = NowPlayingBackgroundEditor.TRANSITION }
            )
            TransitionSettingRow(
                title = "BG Method",
                value = selectedMethod.title,
                subtitle = selectedMethod.subtitle,
                onClick = { haptics.tap(); editor = NowPlayingBackgroundEditor.METHOD }
            )
        }
    }

    when (editor) {
        NowPlayingBackgroundEditor.TRANSITION -> XvoxBox(
            onDismiss = { editor = null },
            title = "Background Transition",
            presentation = XvoxBoxPresentation.CENTERED
        ) {
            val pickerScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(pickerScroll)
                    .xvoxBoxScroll(pickerScroll),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                XvoxBackgroundTransitionStyles.options.forEach { option ->
                    BackgroundTransitionRow(
                        option = option,
                        selected = option.key == backgroundKey,
                        onClick = {
                            haptics.tap()
                            settingsViewModel.setNowPlayingBackgroundTransition(option.key)
                            editor = null
                        }
                    )
                }
            }
        }
        NowPlayingBackgroundEditor.METHOD -> XvoxBox(
            onDismiss = { editor = null },
            title = "BG Method",
            presentation = XvoxBoxPresentation.CENTERED
        ) {
            val pickerScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(pickerScroll)
                    .xvoxBoxScroll(pickerScroll),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                XvoxBackgroundMethodStyles.options.forEach { option ->
                    BackgroundMethodRow(
                        option = option,
                        selected = option.key == methodKey,
                        onClick = {
                            haptics.tap()
                            settingsViewModel.setNowPlayingBackgroundMethod(option.key)
                            editor = null
                        }
                    )
                }
            }
        }
        null -> Unit
    }
}

@Composable
private fun TransitionSettingRow(
    title: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated)
            .border(.7.dp, colors.cardBorder.copy(alpha = .72f), shape)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.secondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(value, color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("›", color = colors.secondaryText, fontSize = 22.sp, modifier = Modifier.padding(start = 7.dp))
    }
}

@Composable
private fun BackgroundTransitionRow(
    option: XvoxBackgroundTransitionOption,
    selected: Boolean,
    onClick: () -> Unit
) = BackgroundChoiceRow(
    title = option.title,
    subtitle = option.subtitle,
    selected = selected,
    onClick = onClick
)

@Composable
private fun BackgroundMethodRow(
    option: XvoxBackgroundMethodOption,
    selected: Boolean,
    onClick: () -> Unit
) = BackgroundChoiceRow(
    title = option.title,
    subtitle = option.subtitle,
    selected = selected,
    onClick = onClick
)

@Composable
private fun BackgroundChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.primaryAccent.copy(alpha = .16f) else colors.cardElevated)
            .border(if (selected) 1.4.dp else .7.dp, if (selected) colors.primaryAccent else colors.cardBorder.copy(alpha = .72f), shape)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.secondaryText, fontSize = 11.sp)
        }
        if (selected) Icon(
            painter = painterResource(R.drawable.ic_xvox_check),
            contentDescription = "Selected $title",
            tint = colors.primaryAccent,
            modifier = Modifier.size(17.dp)
        )
    }
}
