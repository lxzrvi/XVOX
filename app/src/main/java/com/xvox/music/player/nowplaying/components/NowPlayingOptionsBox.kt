package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun NowPlayingOptionsBox(
    onDismiss: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val state by settingsViewModel.state.collectAsState()
    val currentStyle = state.nowPlayingStyle

    XvoxBox(
        onDismiss = onDismiss,
        title = "Now Playing Style"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Choose player layout style",
                color = colors.secondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StyleOptionCard(
                    title = "Default",
                    subtitle = "Complete playback controls, info & actions",
                    selected = currentStyle == "default",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptics.tap()
                        settingsViewModel.setNowPlayingStyle("default")
                    }
                )

                StyleOptionCard(
                    title = "Immersive",
                    subtitle = "Tall expanded card with compact minimalist controls",
                    selected = currentStyle == "immersive",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptics.tap()
                        settingsViewModel.setNowPlayingStyle("immersive")
                    }
                )
            }

            Spacer(Modifier.height(14.dp))

            // Notice / Note for immersive style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated.copy(alpha = 0.6f))
                    .border(0.6.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_info),
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Note: Using this style of Now Playing you can't access all the features in default. Please set your desired settings in default first, then switch to immersive and enjoy.",
                        color = colors.secondaryText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.primaryAccent.copy(alpha = 0.16f) else colors.card)
            .border(
                width = if (selected) 1.4.dp else 0.6.dp,
                color = if (selected) colors.primaryAccent else colors.cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .xvoxPressScale(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (selected) colors.primaryAccent else colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (selected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_check),
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = colors.secondaryText,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}
