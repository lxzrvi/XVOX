package com.xvox.music.features.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.player.nowplaying.XvoxArtworkPaletteLoader

/** The active song is softly tinted by its own cover, not a fixed app accent. */
@Composable
fun rememberSongCardColor(song: Song, current: Boolean, selected: Boolean = false): Color {
    val colors = XvoxTheme.colors
    val context = LocalContext.current.applicationContext
    val loader = remember(context) { XvoxArtworkPaletteLoader(context) }
    var dominant by remember(song.artworkUri) { mutableStateOf<Color?>(null) }
    LaunchedEffect(current, song.artworkUri) {
        if (current) dominant = loader.load(song.artworkUri)
    }
    val target = when {
        selected -> colors.primaryAccent.copy(alpha = 0.22f).compositeOver(colors.card)
        current && dominant != null -> dominant!!.copy(alpha = 0.24f).compositeOver(colors.card)
        else -> colors.card
    }
    return animateColorAsState(target, tween(480, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "coverTint").value
}
