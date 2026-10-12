package com.xvox.music.core.design.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Frame-immediate preview for the Now Playing song-card translucency rail. DataStore remains the
 * durable source; this small snapshot bridge means the canvas changes under the user's finger.
 */
object XvoxCardTransparencyPreview {
    var value: Float? by mutableStateOf(null)
        private set

    fun publish(transparency: Float) {
        value = transparency.coerceIn(0f, .6f)
    }

    fun clearWhenPersisted(transparency: Float) {
        if (value == transparency.coerceIn(0f, .6f)) value = null
    }
}
