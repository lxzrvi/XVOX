package com.xvox.music.features.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song

/**
 * Card selection tint is intentionally frame-stable. Starting an animation for every newly
 * composed artwork tile during a fling was unnecessary work on the UI thread and made Home feel
 * behind the finger; artwork decode size and quality are unchanged.
 */
@Composable
fun rememberSongCardColor(
    @Suppress("UNUSED_PARAMETER") song: Song,
    @Suppress("UNUSED_PARAMETER") current: Boolean,
    selected: Boolean = false
): Color {
    val colors = XvoxTheme.colors
    return if (selected) colors.primaryAccent.copy(alpha = .16f).compositeOver(colors.card) else colors.card
}
