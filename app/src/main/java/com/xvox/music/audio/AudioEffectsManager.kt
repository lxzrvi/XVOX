package com.xvox.music.audio

/** Preset catalogue. DSP lives in the PCM pipeline, never in a repeatedly re-created hardware EQ. */
object AudioEffectsManager {
    val PRESETS = mapOf(
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Bass Boost" to listOf(7, 5, 2, 0, -1),
        "Treble" to listOf(-2, 0, 2, 5, 7),
        "Rock" to listOf(5, 3, -1, 3, 5),
        "Pop" to listOf(-1, 3, 5, 3, -1),
        "Jazz" to listOf(4, 2, -1, 2, 4),
        "Electronic" to listOf(6, 4, 0, 3, 5),
        "Vocal" to listOf(-3, 1, 6, 4, 1)
    )
}
