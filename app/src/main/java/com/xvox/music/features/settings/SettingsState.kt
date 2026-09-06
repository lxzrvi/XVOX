package com.xvox.music.features.settings

data class SettingsState(
    val theme: String = "System",
    val accentColor: String = "Default",
    val fontSizeScale: Float = 1.0f,
    val fourRowsGrid: Boolean = true,
    val crossfade: Boolean = false,
    val crossfadeDuration: Int = 3,
    val pauseOnHeadphoneDisconnect: Boolean = true,
    val playOnHeadsetConnect: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val eqPreset: String = "Flat",
    val eqBands: List<Int> = listOf(0, 0, 0, 0, 0),
    val balance: Float = 0f,
    val stereoWidening: Boolean = false,
    val appVolume: Float = 1.0f,
    val volumeLimit: Float = 1.0f,
    val widgetTransparency: Float = 0.25f,
    val widgetTheme: String = "Dark",
    val widgetCustomColor: String = "#000000",
    val widgetShowLogo: Boolean = true,
    val widgetCornerRadius: Int = 16
)
