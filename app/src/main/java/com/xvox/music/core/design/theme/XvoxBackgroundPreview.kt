package com.xvox.music.core.design.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Same-frame app-background color-wheel preview, cleared once DataStore observes the value. */
object XvoxBackgroundPreview {
    var value: String? by mutableStateOf(null)
        private set

    fun publish(background: String) {
        value = background
    }

    fun clearWhenPersisted(background: String) {
        if (value == background) value = null
    }
}
