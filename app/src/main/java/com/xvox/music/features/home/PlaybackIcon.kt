// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/home/PlaybackIcon.kt
package com.xvox.music.features.home

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xvox.music.core.ui.icons.painterResource
import com.xvox.music.R

enum class PlaybackIconType {
    PLAY,
    PAUSE
}

@Composable
fun PlaybackIcon(
    type: PlaybackIconType,
    color: Color,
    modifier: Modifier = Modifier
) {
    val resource =
        when (type) {
            PlaybackIconType.PLAY ->
                R.drawable.ic_xvox_play

            PlaybackIconType.PAUSE ->
                R.drawable.ic_xvox_pause
        }

    Icon(
        painter =
            painterResource(resource),
        contentDescription = null,
        tint = color,
        modifier = modifier
    )
}
