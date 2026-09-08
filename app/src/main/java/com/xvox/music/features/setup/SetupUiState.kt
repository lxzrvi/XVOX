package com.xvox.music.features.setup

import android.net.Uri

enum class PfpType(
    val label: String,
) {
    DEFAULT("Default"),
    HEART("Amour"),
    STAR("Nova"),
    CIRCLE("Luna"),
    DIAMOND("Prism"),
    HEXAGON("Nexus"),
    CUSTOM("Add"),
}

data class SetupUiState(
    val name: String = "",
    val selectedPfp: PfpType = PfpType.DEFAULT,
    val customPfpUri: Uri? = null,
    /** Every custom picture kept so far; they stack next to the built-in avatars. */
    val customPfpUris: List<String> = emptyList(),
    val audioGranted: Boolean = false,
    val notificationGranted: Boolean = false,
) {
    val setupComplete: Boolean
        get() =
            name.isNotBlank() &&
                audioGranted &&
                notificationGranted &&
                (selectedPfp != PfpType.CUSTOM || customPfpUri != null)
}
