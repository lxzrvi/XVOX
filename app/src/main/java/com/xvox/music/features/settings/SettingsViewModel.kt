package com.xvox.music.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferencesRepository(application)
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            launch { prefs.theme.collect { v -> _state.update { it.copy(theme = v) } } }
            launch { prefs.accentColor.collect { v -> _state.update { it.copy(accentColor = v) } } }
            launch { prefs.fontSizeScale.collect { v -> _state.update { it.copy(fontSizeScale = v) } } }
            launch { prefs.fourRowsGrid.collect { v -> _state.update { it.copy(fourRowsGrid = v) } } }

            launch { prefs.homeLayoutStyle.collect { v -> _state.update { it.copy(homeLayoutStyle = v) } } }
            launch { prefs.homeScrollDirection.collect { v -> _state.update { it.copy(homeScrollDirection = v) } } }
            launch { prefs.homeHorizontalRows.collect { v -> _state.update { it.copy(homeHorizontalRows = v) } } }
            launch { prefs.hideRecentlyPlayed.collect { v -> _state.update { it.copy(hideRecentlyPlayed = v) } } }

            launch { prefs.crossfade.collect { v -> _state.update { it.copy(crossfade = v) } } }
            launch { prefs.crossfadeDuration.collect { v -> _state.update { it.copy(crossfadeDuration = v) } } }
            launch { prefs.pauseOnHeadphoneDisconnect.collect { v -> _state.update { it.copy(pauseOnHeadphoneDisconnect = v) } } }
            launch { prefs.playOnHeadsetConnect.collect { v -> _state.update { it.copy(playOnHeadsetConnect = v) } } }

            launch { prefs.equalizerEnabled.collect { v -> _state.update { it.copy(equalizerEnabled = v) } } }
            launch { prefs.eqPreset.collect { v -> _state.update { it.copy(eqPreset = v) } } }
            launch { prefs.eqBands.collect { v -> _state.update { it.copy(eqBands = v) } } }
            launch { prefs.balance.collect { v -> _state.update { it.copy(balance = v) } } }
            launch { prefs.stereoWidening.collect { v -> _state.update { it.copy(stereoWidening = v) } } }
            launch { prefs.surroundPanSpeed.collect { v -> _state.update { it.copy(surroundPanSpeed = v) } } }

            launch { prefs.appVolume.collect { v -> _state.update { it.copy(appVolume = v) } } }
            launch { prefs.volumeLimit.collect { v -> _state.update { it.copy(volumeLimit = v) } } }

            launch { prefs.widgetTransparency.collect { v -> _state.update { it.copy(widgetTransparency = v) } } }
            launch { prefs.widgetTheme.collect { v -> _state.update { it.copy(widgetTheme = v) } } }
            launch { prefs.widgetCustomColor.collect { v -> _state.update { it.copy(widgetCustomColor = v) } } }
            launch { prefs.widgetShowLogo.collect { v -> _state.update { it.copy(widgetShowLogo = v) } } }
            launch { prefs.widgetCornerRadius.collect { v -> _state.update { it.copy(widgetCornerRadius = v) } } }
        }
    }

    fun setTheme(theme: String) = viewModelScope.launch { prefs.setTheme(theme) }
    fun setAccentColor(color: String) = viewModelScope.launch { prefs.setAccentColor(color) }
    fun setFontSizeScale(scale: Float) = viewModelScope.launch { prefs.setFontSizeScale(scale) }
    fun setFourRowsGrid(enabled: Boolean) = viewModelScope.launch { prefs.setFourRowsGrid(enabled) }

    fun setHomeLayoutStyle(style: String) = viewModelScope.launch { prefs.setHomeLayoutStyle(style) }
    fun setHomeScrollDirection(direction: String) = viewModelScope.launch { prefs.setHomeScrollDirection(direction) }
    fun setHomeHorizontalRows(rows: Int) = viewModelScope.launch { prefs.setHomeHorizontalRows(rows) }
    fun setHideRecentlyPlayed(hide: Boolean) = viewModelScope.launch { prefs.setHideRecentlyPlayed(hide) }

    fun setCrossfade(enabled: Boolean) = viewModelScope.launch { prefs.setCrossfade(enabled) }
    fun setCrossfadeDuration(duration: Int) = viewModelScope.launch { prefs.setCrossfadeDuration(duration) }
    fun setPauseOnHeadphoneDisconnect(enabled: Boolean) = viewModelScope.launch { prefs.setPauseOnHeadphoneDisconnect(enabled) }
    fun setPlayOnHeadsetConnect(enabled: Boolean) = viewModelScope.launch { prefs.setPlayOnHeadsetConnect(enabled) }

    fun setEqualizerEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setEqualizerEnabled(enabled) }
    fun setEqPreset(preset: String) = viewModelScope.launch { prefs.setEqPreset(preset) }
    fun setEqBands(bands: List<Int>) = viewModelScope.launch { prefs.setEqBands(bands) }
    fun setBalance(balance: Float) = viewModelScope.launch { prefs.setBalance(balance) }
    fun setStereoWidening(enabled: Boolean) = viewModelScope.launch { prefs.setStereoWidening(enabled) }
    fun setSurroundPanSpeed(speed: Int) = viewModelScope.launch { prefs.setSurroundPanSpeed(speed) }

    fun setAppVolume(volume: Float) = viewModelScope.launch { prefs.setAppVolume(volume) }
    fun setVolumeLimit(limit: Float) = viewModelScope.launch { prefs.setVolumeLimit(limit) }

    fun setWidgetTransparency(t: Float) = viewModelScope.launch { prefs.setWidgetTransparency(t) }
    fun setWidgetTheme(t: String) = viewModelScope.launch { prefs.setWidgetTheme(t) }
    fun setWidgetCustomColor(c: String) = viewModelScope.launch { prefs.setWidgetCustomColor(c) }
    fun setWidgetShowLogo(s: Boolean) = viewModelScope.launch { prefs.setWidgetShowLogo(s) }
    fun setWidgetCornerRadius(r: Int) = viewModelScope.launch { prefs.setWidgetCornerRadius(r) }
}
