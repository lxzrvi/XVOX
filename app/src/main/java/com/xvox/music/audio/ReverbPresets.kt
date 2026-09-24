package com.xvox.music.audio

/**
 * A room selection describes the *character* of a small feedback-delay network. The wet amount
 * stays deliberately separate, so moving Amount never silently turns a Small Room into a Hall.
 */
data class ReverbProfile(
    val feedbackBase: Double,
    val feedbackDepth: Double,
    val wetBase: Double,
    val wetDepth: Double,
    /** Scales the decorrelated delay lines, making small rooms dense and halls spacious. */
    val delayScale: Double,
    /** One-pole feedback damping: higher values keep the tail warmer/darker. */
    val damping: Double,
    /** Cross-channel feed makes every preset feel like a room rather than two identical echoes. */
    val stereoWidth: Double
)

object ReverbPresets {
    const val OFF = "Off"

    val names: List<String> = listOf(
        OFF,
        "Small Room",
        "Medium Room",
        "Large Room",
        "Hall",
        "Studio",
        "Cathedral",
        "Live",
        "Arena"
    )

    private val profiles = mapOf(
        OFF to ReverbProfile(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0),
        "Small Room" to ReverbProfile(0.29, 0.22, 0.13, 0.18, 0.54, 0.20, 0.24),
        "Medium Room" to ReverbProfile(0.39, 0.25, 0.15, 0.21, 0.74, 0.31, 0.31),
        "Large Room" to ReverbProfile(0.48, 0.27, 0.17, 0.24, 0.99, 0.40, 0.42),
        "Hall" to ReverbProfile(0.57, 0.27, 0.19, 0.28, 1.20, 0.53, 0.58),
        "Studio" to ReverbProfile(0.25, 0.20, 0.11, 0.16, 0.62, 0.56, 0.16),
        "Cathedral" to ReverbProfile(0.67, 0.22, 0.22, 0.32, 1.62, 0.70, 0.72),
        "Live" to ReverbProfile(0.43, 0.23, 0.15, 0.22, 0.90, 0.24, 0.64),
        "Arena" to ReverbProfile(0.62, 0.24, 0.21, 0.30, 1.42, 0.44, 0.80)
    )

    fun normalize(value: String?): String = value?.takeIf { it in profiles } ?: OFF

    fun profile(value: String?): ReverbProfile = profiles[normalize(value)] ?: profiles.getValue(OFF)

    /** Preserves the room selected by releases that stored only one room amount. */
    fun legacySelection(roomAmount: Float, reverbAmount: Float): String {
        if (reverbAmount <= 0.001f) return OFF
        val room = roomAmount.coerceIn(0f, 1f)
        return when {
            room < 0.30f -> "Small Room"
            room < 0.50f -> "Medium Room"
            room < 0.70f -> "Large Room"
            room < 0.90f -> "Hall"
            else -> "Cathedral"
        }
    }
}
