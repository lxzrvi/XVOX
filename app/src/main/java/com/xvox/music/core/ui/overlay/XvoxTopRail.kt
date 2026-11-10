// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/overlay/XvoxTopRail.kt
package com.xvox.music.core.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

/**
 * Hands [content] to the shell, which draws it ABOVE the mini player and navbar and outside the
 * blur source, so it can use a real live blur without sampling itself. Emits nothing locally.
 */
@Composable
fun XvoxTopRail(content: @Composable () -> Unit) {
    val overlays = LocalXvoxOverlayController.current
    val latest = rememberUpdatedState(content)
    DisposableEffect(overlays) {
        val mine: @Composable () -> Unit = { latest.value() }
        overlays.railContent = mine
        // A newer rail (artist -> playlist selection) may already have replaced this one.
        onDispose { if (overlays.railContent === mine) overlays.railContent = null }
    }
}
