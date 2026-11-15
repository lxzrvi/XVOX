// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/player/nowplaying/XvoxBounce.kt
package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Subtle elastic bump whenever [key] changes (play/pause): the button dips a little, then springs
 * back past 1 once. Used on Previous, Play and Next together so the transport moves as one unit.
 * The first value never animates, so nothing bounces when the screen opens.
 */
@Composable
fun Modifier.xvoxBounceOn(key: Any?, dip: Float = .9f): Modifier {
    val scale = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(key) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        scale.animateTo(dip, tween(70))
        scale.animateTo(1f, spring(dampingRatio = .32f, stiffness = Spring.StiffnessMediumLow))
    }
    return this.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}
