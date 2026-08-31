package com.xvox.music.features.setup

import android.net.Uri

enum class PfpType(
    val label: String
) {
    DEFAULT("Default"),
    HEART("Heart"),
    STAR("Nova"),
    CIRCLE("Orbit"),
    DIAMOND("Gem"),
    HEXAGON("Hex"),
    CUSTOM("Add")
}

data class SetupUiState(
    val name: String = "",
    val selectedPfp: PfpType = PfpType.DEFAULT,
    val customPfpUri: Uri? = null,
    val audioGranted: Boolean = false,
    val notificationGranted: Boolean = false
) {
    val setupComplete: Boolean
        get() =
            name.isNotBlank() &&
                audioGranted &&
                notificationGranted
}
