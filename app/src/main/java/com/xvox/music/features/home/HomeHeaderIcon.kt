package com.xvox.music.features.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale

enum class HomeHeaderIconType {
    SCAN,
    HEART,
    HEART_OUTLINE,
    PLAYLIST,
    SONGS,
    ARTIST_OUTLINE,
    ARTIST_FILLED
}

@Composable
fun HomeHeaderIcon(
    type: HomeHeaderIconType,
    onClick: () -> Unit
) {
    val color = XvoxTheme.colors.primaryText

    val resource = when (type) {
        HomeHeaderIconType.SCAN -> R.drawable.ic_xvox_refresh
        HomeHeaderIconType.HEART -> R.drawable.ic_xvox_heart
        HomeHeaderIconType.HEART_OUTLINE -> R.drawable.ic_xvox_heart_outline
        HomeHeaderIconType.PLAYLIST -> R.drawable.ic_xvox_playlist
        HomeHeaderIconType.SONGS -> R.drawable.ic_xvox_music_note
        HomeHeaderIconType.ARTIST_OUTLINE -> R.drawable.ic_xvox_microphone_outline
        HomeHeaderIconType.ARTIST_FILLED -> R.drawable.ic_xvox_microphone
    }

    Icon(
        painter = painterResource(resource),
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .size(36.dp)
            .xvoxPressScale(
                pressedScale = 0.90f,
                onClick = onClick
            )
            .padding(8.dp)
    )
}
