package com.xvox.music.player.nowplaying.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.features.settings.components.SettingsChoiceRow

/**
 * The three-dot Now Playing editor stays in its fixed position. It edits visual seek rails only:
 * transport controls, utility pill, action cluster and the Play circle remain in their stable,
 * predictable positions.
 */
@Composable
fun NowPlayingOptionsBox(
    seekStyle: String,
    onSeekStyleChange: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Seek bar style",
                color = colors.primaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Choose the progress rail. Every control stays in its fixed place.",
                color = colors.secondaryText,
                fontSize = 10.5.sp,
                lineHeight = 13.sp
            )
            SettingsChoiceRow(
                options = listOf(
                    "classic" to "Classic",
                    "pill" to "Capsule",
                    "android_wave" to "Android Wave",
                    "pulse" to "Pulse Bars",
                    "aurora" to "Aurora"
                ),
                selected = when (seekStyle) {
                    "pill", "android_wave", "pulse", "aurora" -> seekStyle
                    else -> "classic"
                },
                onSelect = onSeekStyleChange
            )
            Text(
                "Android Wave uses a continuous Android-style signal wave; Capsule has a redesigned filled track. Pulse Bars and Aurora add two lightweight visual alternatives.",
                color = colors.mutedText,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}
