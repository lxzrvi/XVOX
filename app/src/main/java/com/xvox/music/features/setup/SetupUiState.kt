package com.xvox.music.features.setup

import android.net.Uri

enum class PfpType {
    HEART,
    STAR,
    CIRCLE,
    DIAMOND,
    HEXAGON,
    CUSTOM
}

data class SetupUiState(
    val name: String = "",
    val selectedPfp: PfpType = PfpType.HEART,
    val customPfpUri: Uri? = null,
    val audioGranted: Boolean = false,
    val notificationGranted: Boolean = false
) {
    val setupComplete: Boolean
        get() = name.isNotBlank() &&
            audioGranted &&
            notificationGranted
}
