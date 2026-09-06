package com.xvox.music.player.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.LocalCoverBackgroundEnabled
import com.xvox.music.core.ui.effects.XvoxCoverBackground

@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    song: Song? = null,
    modifier: Modifier = Modifier
) {
    val coverBgEnabled = LocalCoverBackgroundEnabled.current

    if (coverBgEnabled && song?.artworkUri != null) {
        XvoxCoverBackground(
            song = song,
            modifier = modifier,
            enabled = true
        )
    } else {
        val dark = lerp(dominant, Color.Black, 0.45f)
        val deep = lerp(dominant, Color.Black, 0.72f)

        Canvas(
            modifier = modifier.fillMaxSize()
        ) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to dark.copy(alpha = 0.95f),
                    0.40f to deep,
                    1f to Color(0xFF08080A)
                )
            )
        }
    }
}
