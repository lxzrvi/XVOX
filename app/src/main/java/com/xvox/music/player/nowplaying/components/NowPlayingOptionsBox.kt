package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
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
import com.xvox.music.player.nowplaying.XvoxBackgroundTransitionOption
import com.xvox.music.player.nowplaying.XvoxBackgroundTransitionStyles
import com.xvox.music.player.nowplaying.XvoxCoverTransitionOption
import com.xvox.music.player.nowplaying.XvoxCoverTransitionStyles

private enum class NowPlayingTransitionEditor { COVER, BACKGROUND }

/**
 * First-level three-dot interface.  It is always a bottom sheet; its secondary transition pickers
 * open as centred dialogs so the hierarchy never becomes a stack of sheets.
 */
@Composable
fun NowPlayingOptionsBox(
    onDismiss: () -> Unit,
    dominant: Color,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val state by settingsViewModel.state.collectAsState()
    var editor by remember { mutableStateOf<NowPlayingTransitionEditor?>(null) }
    val coverKey = XvoxCoverTransitionStyles.normalize(state.nowPlayingCoverTransition)
    val backgroundKey = XvoxBackgroundTransitionStyles.normalize(state.nowPlayingBackgroundTransition)
    val selectedCover = XvoxCoverTransitionStyles.options.first { it.key == coverKey }
    val selectedBackground = XvoxBackgroundTransitionStyles.options.first { it.key == backgroundKey }
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
            // The retired ten-style menu is intentionally represented by one persistent Default.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(colors.cardElevated)
                    .border(.9.dp, colors.primaryAccent.copy(alpha = .72f), RoundedCornerShape(15.dp))
                    .padding(13.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Background · Default", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Artwork-derived Default background",
                        color = colors.secondaryText,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_check),
                    contentDescription = "Default background selected",
                    tint = colors.primaryAccent,
                    modifier = Modifier.align(Alignment.CenterEnd).size(18.dp)
                )
            }

            TransitionSettingRow(
                title = "Cover transition",
                value = selectedCover.title,
                subtitle = selectedCover.subtitle,
                onClick = { haptics.tap(); editor = NowPlayingTransitionEditor.COVER }
            )
            TransitionSettingRow(
                title = "Background transition",
                value = selectedBackground.title,
                subtitle = selectedBackground.subtitle,
                onClick = { haptics.tap(); editor = NowPlayingTransitionEditor.BACKGROUND }
            )
        }
    }

    when (editor) {
        NowPlayingTransitionEditor.COVER -> XvoxBox(
            onDismiss = { editor = null },
            title = "Cover transition",
            presentation = XvoxBoxPresentation.CENTERED
        ) {
            val pickerScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(pickerScroll),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                XvoxCoverTransitionStyles.options.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { option ->
                            CoverTransitionCard(
                                option = option,
                                selected = option.key == coverKey,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.tap()
                                    settingsViewModel.setNowPlayingCoverTransition(option.key)
                                    editor = null
                                }
                            )
                        }
                    }
                }
            }
        }
        NowPlayingTransitionEditor.BACKGROUND -> XvoxBox(
            onDismiss = { editor = null },
            title = "Background transition",
            presentation = XvoxBoxPresentation.CENTERED
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Text(value, color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("›", color = colors.secondaryText, fontSize = 22.sp, modifier = Modifier.padding(start = 7.dp))
    }
}

@Composable
private fun CoverTransitionCard(
    option: XvoxCoverTransitionOption,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .height(78.dp)
            .clip(shape)
            .background(if (selected) colors.primaryAccent.copy(alpha = .18f) else colors.cardElevated)
            .border(if (selected) 1.5.dp else .7.dp, if (selected) colors.primaryAccent else colors.cardBorder.copy(alpha = .72f), shape)
            .xvoxPressScale(onClick = onClick)
            .padding(10.dp)
    ) {
        Text(option.title, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            option.subtitle,
            color = colors.secondaryText,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_check),
                contentDescription = "Selected ${option.title}",
                tint = colors.primaryAccent,
                modifier = Modifier.align(Alignment.TopEnd).size(16.dp)
            )
        }
    }
}

@Composable
private fun BackgroundTransitionRow(
    option: XvoxBackgroundTransitionOption,
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
            Text(option.title, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(option.subtitle, color = colors.secondaryText, fontSize = 11.sp)
        }
        if (selected) Icon(
            painter = painterResource(R.drawable.ic_xvox_check),
            contentDescription = "Selected ${option.title}",
            tint = colors.primaryAccent,
            modifier = Modifier.size(17.dp)
        )
    }
}
