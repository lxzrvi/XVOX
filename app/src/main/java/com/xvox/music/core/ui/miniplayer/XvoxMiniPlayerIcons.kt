package com.xvox.music.core.ui.miniplayer

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.xvox.music.R

enum class XvoxMiniIcon {
    PLAY,
    PAUSE,
    HEART,
    ADD,
    CLOSE
}

@Composable
fun XvoxMiniPlayerIcon(
    icon: XvoxMiniIcon,
    color: Color,
    modifier: Modifier = Modifier
) {
    val resource =
        when (icon) {
            XvoxMiniIcon.PLAY ->
                R.drawable.ic_xvox_play

            XvoxMiniIcon.PAUSE ->
                R.drawable.ic_xvox_pause

            XvoxMiniIcon.HEART ->
                R.drawable.ic_xvox_heart

            XvoxMiniIcon.ADD ->
                R.drawable.ic_xvox_add

            XvoxMiniIcon.CLOSE ->
                R.drawable.ic_xvox_close
        }

    Icon(
        painter =
            painterResource(resource),
        contentDescription = null,
        tint = color,
        modifier = modifier
    )
}
