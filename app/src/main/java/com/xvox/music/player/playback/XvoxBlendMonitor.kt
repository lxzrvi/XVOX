package com.xvox.music.player.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BlendVisualState(
    val enabled: Boolean = false,
    val configuredSeconds: Int = 3,
    val active: Boolean = false,
    val outgoingId: Long? = null,
    val incomingId: Long? = null,
    val outgoingTitle: String = "",
    val incomingTitle: String = "",
    val outgoingPosition: Long = 0,
    val outgoingDuration: Long = 0,
    val incomingPosition: Long = 0,
    val incomingDuration: Long = 0,
    val windowMs: Long = 0,
    val progress: Float = 0f,
    val beatAligned: Boolean = false
)

/** Progress-only UI flow, separate from the library / full-screen player state to avoid grid recomposition. */
object XvoxBlendMonitor {
    private val _state = MutableStateFlow(BlendVisualState())
    val state = _state.asStateFlow()
    fun configure(enabled: Boolean, seconds: Int) {
        _state.update { it.copy(enabled = enabled, configuredSeconds = seconds.coerceIn(1, 12)) }
    }
    fun publish(visual: BlendVisualState) { _state.value = visual }
    fun end() { _state.update { it.copy(active = false, progress = 0f, beatAligned = false) } }
}
