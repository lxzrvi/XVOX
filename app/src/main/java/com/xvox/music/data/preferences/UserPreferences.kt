package com.xvox.music.data.preferences

object ProfileDefaults {
    /** Starter lines offered under the name; removing one keeps it removed. */
    val lines = listOf("Music is my reset button", "Always looking for the next great track")
}

data class UserPreferences(
    val setupCompleted: Boolean = false,
    val username: String = "",
    val selectedPfp: String = "DEFAULT",
    val customPfpUri: String? = null,
    /** Custom pictures the user has kept; they stack alongside the built-in avatars. */
    val customPfpUris: List<String> = emptyList(),
    /** Short lines the user wrote; the first one or two appear under the name on Home. */
    val profileLines: List<String> = emptyList(),
    /** When false the name stands alone next to the avatar (no lines block under it). */
    val showProfileLines: Boolean = true,
    /** Custom photo shown behind the Home header; null keeps the theme surface. */
    val headerImageUri: String? = null,
    /** True once the starter lines have been offered, so removing one keeps it removed. */
    val profileLinesInitialized: Boolean = false,
    /** How long each rotating line under the name stays before the next one fades in. */
    val greetingIntervalMs: Long = 8_000L
)
