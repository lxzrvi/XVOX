package com.xvox.music.features.settings.sections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.*

@Composable
fun PlaylistSettingsSection(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    PinnedSettingsEditor(preview = {
        SettingsPreviewFrame("Playlists · layout") {
            Canvas(Modifier.fillMaxWidth().height(145.dp)) {
                val gap = 8.dp.toPx()
                val long = state.playlistStyle == "long"
                repeat(2) { i ->
                    val w = if (long) size.width else (size.width - gap) / 2
                    val h = if (long) (size.height - gap) / 2 else size.height
                    val at = if (long) Offset(0f, i * (h + gap)) else Offset(i * (w + gap), 0f)
                    drawRoundRect(colors.card, at, Size(w, h), CornerRadius(12.dp.toPx()))
                    drawRoundRect(colors.primaryAccent.copy(alpha = .25f), at + Offset(5.dp.toPx(), 5.dp.toPx()),
                        Size(w - 10.dp.toPx(), h - 24.dp.toPx()), CornerRadius(8.dp.toPx()))
                }
            }
        }
    }, controls = {
        Text("Playlist card style", color = colors.primaryText, fontSize = 14.sp)
        SettingsChoiceRow(listOf("cards" to "Original cards", "long" to "Long cards"), state.playlistStyle, viewModel::setPlaylistStyle)
        Text("Long cards fill the row and stack vertically. Their height, colours, corners and lower title area match the original card style; the artwork becomes wider instead of making the card taller.",
            color = colors.secondaryText, fontSize = 12.sp)
    })
}
