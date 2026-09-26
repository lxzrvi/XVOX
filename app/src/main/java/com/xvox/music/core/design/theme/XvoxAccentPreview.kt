package com.xvox.music.core.design.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Frame-immediate custom-accent preview.  A colour wheel can update this state every drag frame
 * while persistence is coalesced separately, so a DataStore write never stalls pointer input.
 */
object XvoxAccentPreview {
    private val _value = MutableStateFlow<String?>(null)
    val value: StateFlow<String?> = _value.asStateFlow()

    fun publish(color: String) {
        _value.value = color
    }

    fun clearWhenPersisted(color: String) {
        if (_value.value == color) _value.value = null
    }
}
