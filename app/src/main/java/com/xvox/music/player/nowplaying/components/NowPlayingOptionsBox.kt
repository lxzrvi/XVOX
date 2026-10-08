package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.features.settings.components.SettingsChoiceRow

/**
 * The three-dot Now Playing editor. All choices are deliberately simple left/right controls—this
 * is a testable arrangement selector, not a drag-layout editor—and persist through chrome style.
 */
@Composable
fun NowPlayingOptionsBox(
    seekStyle: String,
    timerQueueInfoSide: String,
    changingActionsSide: String,
    shuffleRepeatSide: String,
    playSide: String,
    optionsGroupSide: String,
    onSeekStyleChange: (String) -> Unit,
    onTimerQueueInfoSideChange: (String) -> Unit,
    onChangingActionsSideChange: (String) -> Unit,
    onShuffleRepeatSideChange: (String) -> Unit,
    onPlaySideChange: (String) -> Unit,
    onOptionsGroupSideChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    XvoxBox(
        onDismiss = onDismiss,
        title = "Now Playing controls"
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .xvoxBoxScroll(scrollState)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OptionGroup(
                title = "Seek bar",
                subtitle = "Choose the progress rail used in Now Playing.",
                side = optionsGroupSide
            ) {
                SettingsChoiceRow(
                    options = listOf(
                        "classic" to "Classic",
                        "pill" to "Pill",
                        "android_wave" to "Android Wave"
                    ),
                    selected = if (seekStyle in setOf("classic", "pill", "android_wave")) seekStyle else "classic",
                    onSelect = onSeekStyleChange,
                    alignEnd = optionsGroupSide == "right"
                )
            }

            Text(
                "Control placement",
                color = XvoxTheme.colors.primaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Each group can be tested on the left or right. When two groups choose the same side, they remain together without overlap.",
                color = XvoxTheme.colors.secondaryText,
                fontSize = 10.5.sp,
                lineHeight = 13.sp
            )

            SideChoice(
                title = "Timer · Queue · Info pill",
                selected = timerQueueInfoSide,
                onSelect = onTimerQueueInfoSideChange
            )
            SideChoice(
                title = "Six changing action buttons",
                selected = changingActionsSide,
                onSelect = onChangingActionsSideChange
            )
            SideChoice(
                title = "Shuffle / Repeat buttons",
                selected = shuffleRepeatSide,
                onSelect = onShuffleRepeatSideChange
            )
            SideChoice(
                title = "Play control",
                selected = playSide,
                onSelect = onPlaySideChange
            )
            SideChoice(
                title = "Options box / group",
                selected = optionsGroupSide,
                onSelect = onOptionsGroupSideChange
            )
        }
    }
}

@Composable
private fun OptionGroup(
    title: String,
    subtitle: String,
    side: String,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val alignment = if (side == "left") Alignment.Start else Alignment.End
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, color = colors.mutedText, fontSize = 10.sp, lineHeight = 12.sp)
        content()
    }
}

@Composable
private fun SideChoice(
    title: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    OptionGroup(
        title = title,
        subtitle = "Placement",
        side = selected
    ) {
        SettingsChoiceRow(
            options = listOf("left" to "Left", "right" to "Right"),
            selected = if (selected == "right") "right" else "left",
            onSelect = onSelect,
            alignEnd = selected == "right"
        )
    }
}
