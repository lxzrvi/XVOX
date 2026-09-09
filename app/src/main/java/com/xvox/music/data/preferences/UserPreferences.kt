package com.xvox.music.data.preferences

data class UserPreferences(
    val setupCompleted: Boolean = false,
    val username: String = "",
    val selectedPfp: String = "DEFAULT",
    val customPfpUri: String? = null,
    /** Custom pictures the user has kept; they stack alongside the built-in avatars. */
    val customPfpUris: List<String> = emptyList(),
    /** Short lines the user wrote; the first one or two appear under the name on Home. */
    val profileLines: List<String> = emptyList()
)
