package com.xvox.music.player.session

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Transport speed boost for the hold-Next gesture.
 *
 * Holding Next raises the deck speed instead of re-seeking, so the audio never breaks: ExoPlayer
 * re-times the stream through its Sonic processor, which is continuous. The service combines this
 * factor with the user's configured playback speed, and 1f means "no boost".
 */
object XvoxTransportBoost {
    /** 1f = normal. Only values above 1 slow nothing down and never cut the stream. */
    val factor = MutableStateFlow(1f)

    fun set(value: Float) {
        factor.value = value.coerceIn(1f, 3f)
    }

    fun release() {
        factor.value = 1f
    }
}
