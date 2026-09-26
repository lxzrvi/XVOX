package com.xvox.music.player.nowplaying.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.player.nowplaying.XvoxNowPlayingBackgroundOption
import com.xvox.music.player.nowplaying.XvoxNowPlayingBackgroundStyles
import com.xvox.music.player.nowplaying.xvoxNowPlayingBackgroundBrush
import com.xvox.music.player.nowplaying.xvoxNowPlayingBackgroundColor

/** The three-dot menu's exact ten artwork-derived Now Playing background treatments. */
@Composable
fun NowPlayingOptionsBox(
    onDismiss: () -> Unit,
    dominant: Color,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val state by settingsViewModel.state.collectAsState()
    val selected = XvoxNowPlayingBackgroundStyles.normalize(state.nowPlayingBackgroundStyle)

    XvoxBox(
        onDismiss = onDismiss,
        title = "Now Playing background"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Cover-derived background styles",
                color = colors.secondaryText,
                fontSize = 12.sp
            )
            XvoxNowPlayingBackgroundStyles.options.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { option ->
                        BackgroundStyleCard(
                            option = option,
                            dominant = dominant,
                            selected = option.key == selected,
                            onClick = {
                                haptics.tap()
                                settingsViewModel.setNowPlayingBackgroundStyle(option.key)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BackgroundStyleCard(
    option: XvoxNowPlayingBackgroundOption,
    dominant: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val brush = xvoxNowPlayingBackgroundBrush(option.key, dominant)
    val fill = xvoxNowPlayingBackgroundColor(option.key, dominant)
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(fill))
            .border(
                width = if (selected) 1.8.dp else .7.dp,
                color = if (selected) colors.primaryAccent else colors.cardBorder.copy(alpha = .78f),
                shape = shape
            )
            .xvoxPressScale(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = option.title,
            color = colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_check),
                contentDescription = "Selected ${option.title}",
                tint = colors.primaryAccent,
                modifier = Modifier.align(Alignment.TopEnd).size(17.dp)
            )
        }
    }
}
