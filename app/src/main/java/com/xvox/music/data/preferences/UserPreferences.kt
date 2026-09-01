package com.xvox.music.data.preferences

data class UserPreferences(
    val setupCompleted: Boolean = false,
    val username: String = "",
    val selectedPfp: String = "DEFAULT",
    val customPfpUri: String? = null
)
