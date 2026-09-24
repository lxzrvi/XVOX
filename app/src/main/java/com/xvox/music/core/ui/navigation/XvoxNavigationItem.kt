package com.xvox.music.core.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource

/** Generic item so the Extended layouts can mix destinations with library actions. */
@Composable
fun XvoxNavigationItem(
    @DrawableRes icon: Int,
    label: String,
    proximity: Float,
    dragging: Boolean,
    inactiveColor: Color,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val tint = navigationColor(
        inactive = inactiveColor,
        active = activeColor,
        proximity = proximity
    )
    val iconScale = 1f + if (dragging) .10f * proximity else 0f

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = label,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier
                .size(XvoxNavigationGeometry.iconSize)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha = .88f + .12f * proximity
                }
        )
    }
}
