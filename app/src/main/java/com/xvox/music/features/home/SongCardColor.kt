package com.xvox.music.features.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

/** A short press pulse provides feedback. Artwork colour is not loaded or retained on clicked cards. */
@Composable
fun rememberSongCardColor(song: Song, current: Boolean, selected: Boolean = false): Color {
    val colors = XvoxTheme.colors
    val target = if (selected) colors.primaryAccent.copy(alpha = .16f).compositeOver(colors.card) else colors.card
    return animateColorAsState(target, tween(140), label = "selectionTint").value
}
