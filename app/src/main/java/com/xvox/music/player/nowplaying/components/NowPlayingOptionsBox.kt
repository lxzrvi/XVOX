package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.components.SettingsChoiceRow

/**
 * The header three-dot control opens this compact editor without moving itself. The user can tune
 * the action lanes and outside transport buttons, while the central Play circle always remains at
 * the true visual center of Now Playing.
 */
@Composable
fun NowPlayingOptionsBox(
    chrome: XvoxChromeStyle,
    onChromeChange: (XvoxChromeStyle) -> Unit,
    onCustomize: () -> Unit,
    onDismiss: () -> Unit
) {
    XvoxBox(
        onDismiss = onDismiss,
        title = "Now Playing controls"
    ) {
        val colors = XvoxTheme.colors
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .xvoxBoxScroll(scrollState)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Seek bar style",
                color = colors.primaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            SettingsChoiceRow(
                options = listOf(
                    "classic" to "Classic",
                    "pill" to "Capsule",
                    "android_wave" to "Android Wave",
                    "pulse" to "Pulse Bars",
                    "aurora" to "Aurora"
                ),
                selected = when (chrome.nowPlayingSeekStyle) {
                    "pill", "android_wave", "pulse", "aurora" -> chrome.nowPlayingSeekStyle
                    else -> "classic"
                },
                onSelect = { style -> onChromeChange(chrome.copy(nowPlayingSeekStyle = style)) }
            )

            Text(
                "Customize",
                color = colors.primaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.cardElevated)
                    .xvoxPressScale { onCustomize() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Customize Now Playing layout",
                    color = colors.primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "Control placement",
                color = colors.primaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            PlacementChoice(
                label = "Utility pill",
                selected = chrome.nowPlayingPillSide,
                onSelect = { side -> onChromeChange(chrome.copy(nowPlayingPillSide = side)) }
            )
            PlacementChoice(
                label = "Action controls",
                selected = chrome.nowPlayingChangingActionsSide,
                onSelect = { side -> onChromeChange(chrome.copy(nowPlayingChangingActionsSide = side)) }
            )
            PlacementChoice(
                label = "Shuffle / repeat",
                selected = chrome.nowPlayingShuffleRepeatSide,
                onSelect = { side -> onChromeChange(chrome.copy(nowPlayingShuffleRepeatSide = side)) }
            )
            PlacementChoice(
                label = "Header options",
                selected = chrome.nowPlayingOptionsGroupSide,
                onSelect = { side -> onChromeChange(chrome.copy(nowPlayingOptionsGroupSide = side)) }
            )
            Text(
                "Customize opens a full-screen layout canvas. Android Wave keeps a stable wave while its progress beam travels through it.",
                color = colors.mutedText,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun PlacementChoice(
    label: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    val colors = XvoxTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = colors.secondaryText, fontSize = 11.sp)
        SettingsChoiceRow(
            options = listOf("left" to "Left", "right" to "Right"),
            selected = if (selected == "right") "right" else "left",
            onSelect = onSelect
        )
    }
}
