package com.xvox.music.core.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import com.xvox.music.R

@Composable
fun XvoxNavigationItem(
    destination: XvoxDestination,
    proximity: Float,
    dragging: Boolean,
    inactiveColor: Color,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val tint =
        navigationColor(
            inactive =
                inactiveColor,
            active =
                activeColor,
            proximity =
                proximity
        )

    val scale =
        1f +
            if (dragging) {
                0.10f *
                    proximity
            } else {
                0f
            }

    Box(
        modifier =
            modifier.fillMaxHeight(),
        contentAlignment =
            Alignment.Center
    ) {
        Image(
            painter =
                painterResource(
                    navigationIcon(
                        destination
                    )
                ),
            contentDescription =
                destination.label,
            colorFilter =
                ColorFilter.tint(
                    tint
                ),
            modifier = Modifier
                .offset(
                    x =
                        if (
                            destination ==
                            XvoxDestination.SETTINGS
                        ) {
                            2.dp
                        } else {
                            0.dp
                        }
                )
                .size(
                    XvoxNavigationGeometry
                        .iconSize
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale

                    alpha =
                        0.88f +
                            0.12f *
                            proximity
                }
        )
    }
}

@DrawableRes
private fun navigationIcon(
    destination: XvoxDestination
): Int {
    return when (destination) {
        XvoxDestination.HOME ->
            R.drawable.ic_xvox_home

        XvoxDestination.SEARCH ->
            R.drawable.ic_xvox_search

        XvoxDestination.SETTINGS ->
            R.drawable.ic_xvox_settings
    }
}
