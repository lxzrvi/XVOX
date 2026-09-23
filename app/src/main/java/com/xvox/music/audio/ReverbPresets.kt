package com.xvox.music.audio

/**
 * The reverb room character is deliberately independent from its wet amount. A selected name
 * chooses one of these profiles; the amount slider only changes how much of that profile is mixed
 * into the track.
 */
data class ReverbProfile(
    val feedbackBase: Double,
    val feedbackDepth: Double,
    val wetBase: Double,
    val wetDepth: Double
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
        OFF to ReverbProfile(0.0, 0.0, 0.0, 0.0),
        "Small Room" to ReverbProfile(0.22, 0.20, 0.12, 0.16),
        "Medium Room" to ReverbProfile(0.25, 0.25, 0.14, 0.20),
        "Large Room" to ReverbProfile(0.30, 0.30, 0.15, 0.24),
        "Hall" to ReverbProfile(0.35, 0.30, 0.18, 0.27),
        "Studio" to ReverbProfile(0.20, 0.22, 0.10, 0.17),
        "Cathedral" to ReverbProfile(0.45, 0.25, 0.20, 0.33),
        "Live" to ReverbProfile(0.26, 0.22, 0.13, 0.20),
        "Arena" to ReverbProfile(0.40, 0.28, 0.20, 0.30)
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
