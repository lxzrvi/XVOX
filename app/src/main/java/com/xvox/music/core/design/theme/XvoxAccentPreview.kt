package com.xvox.music.core.design.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Frame-immediate custom-accent preview.
 *
 * This intentionally uses Compose snapshot state rather than a Flow. HSV wheel samples can then
 * invalidate the themed composition in the same frame as the pointer move, without a coroutine
 * hop, SettingsState emission, or DataStore collection round-trip. Persistence remains separately
 * coalesced by SettingsViewModel.
 */
object XvoxAccentPreview {
    var value: String? by mutableStateOf(null)
        private set

    fun publish(color: String) {
        value = color
    }

    fun clearWhenPersisted(color: String) {
        if (value == color) value = null
    }
}
