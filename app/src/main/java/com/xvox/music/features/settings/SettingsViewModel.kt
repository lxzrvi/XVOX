package com.xvox.music.features.settings

import com.xvox.music.audio.LiveEqState
import com.xvox.music.audio.AudioEffectsManager
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
            launch { prefs.homeMerge.collect { v -> _state.update { it.copy(homeMerge = v) } } }
            launch { prefs.homeSectionOrder.collect { v -> _state.update { it.copy(homeSectionOrder = v) } } }
            launch { prefs.homeHiddenSections.collect { v -> _state.update { it.copy(homeHiddenSections = v) } } }
            launch { prefs.crossfadeBeatSync.collect { v -> _state.update { it.copy(crossfadeBeatSync = v) } } }
            launch { prefs.widgetPaddingX.collect { v -> _state.update { it.copy(widgetPaddingX = v) } } }
            launch { prefs.widgetPaddingY.collect { v -> _state.update { it.copy(widgetPaddingY = v) } } }
            launch { prefs.theme.collect { v -> _state.update { it.copy(theme = v) } } }
            launch { prefs.accentColor.collect { v -> _state.update { it.copy(accentColor = v) } } }
            launch { prefs.fontSizeScale.collect { v -> _state.update { it.copy(fontSizeScale = v) } } }
            launch { prefs.fourRowsGrid.collect { v -> _state.update { it.copy(fourRowsGrid = v) } } }

            launch { prefs.homeLayoutStyle.collect { v -> _state.update { it.copy(homeLayoutStyle = v) } } }
            launch { prefs.homeScrollDirection.collect { v -> _state.update { it.copy(homeScrollDirection = v) } } }
            launch { prefs.homeHorizontalRows.collect { v -> _state.update { it.copy(homeHorizontalRows = v) } } }
            launch { prefs.hideRecentlyPlayed.collect { v -> _state.update { it.copy(hideRecentlyPlayed = v) } } }
            launch { prefs.recentsPlacement.collect { v -> _state.update { it.copy(recentsPlacement = v) } } }
            launch { prefs.eqHeadroomDb.collect { v -> _state.update { it.copy(eqHeadroomDb = AudioEffectsManager.liveEq.value?.headroomDb ?: v) } } }
            launch { prefs.surroundDepth.collect { v -> _state.update { it.copy(surroundDepth = AudioEffectsManager.liveEq.value?.surroundDepth ?: v) } } }
            launch { prefs.sortOrder.collect { v -> _state.update { it.copy(sortOrder = v) } } }

            launch { prefs.ignoreBelowSec.collect { v -> _state.update { it.copy(ignoreBelowSec = v) } } }
            launch { prefs.ignoreBelowKb.collect { v -> _state.update { it.copy(ignoreBelowKb = v) } } }
            launch { prefs.ignoredFolders.collect { v -> _state.update { it.copy(ignoredFolders = v) } } }

            launch { prefs.crossfade.collect { v -> _state.update { it.copy(crossfade = v) } } }
            launch { prefs.crossfadeDuration.collect { v -> _state.update { it.copy(crossfadeDuration = v) } } }
            launch { prefs.pauseOnHeadphoneDisconnect.collect { v -> _state.update { it.copy(pauseOnHeadphoneDisconnect = v) } } }
            launch { prefs.playOnHeadsetConnect.collect { v -> _state.update { it.copy(playOnHeadsetConnect = v) } } }
            launch { prefs.btDisconnectAction.collect { v -> _state.update { it.copy(btDisconnectAction = v) } } }
            launch { prefs.btConnectAction.collect { v -> _state.update { it.copy(btConnectAction = v) } } }

            launch { prefs.equalizerEnabled.collect { v -> _state.update { it.copy(equalizerEnabled = AudioEffectsManager.liveEq.value?.enabled ?: v) } } }
            launch { prefs.eqPreset.collect { v -> _state.update { it.copy(eqPreset = AudioEffectsManager.liveEq.value?.preset ?: v) } } }
            launch { prefs.eqBands.collect { v -> _state.update { it.copy(eqBands = AudioEffectsManager.liveEq.value?.bands ?: v) } } }
            launch { prefs.balance.collect { v -> _state.update { it.copy(balance = AudioEffectsManager.liveEq.value?.balance ?: v) } } }
            launch { prefs.stereoWidening.collect { v -> _state.update { it.copy(stereoWidening = AudioEffectsManager.liveEq.value?.surroundEnabled ?: v) } } }
            launch { prefs.surroundPanSpeed.collect { v -> _state.update { it.copy(surroundPanSpeed = AudioEffectsManager.liveEq.value?.orbitSeconds ?: v) } } }

            launch { prefs.appVolume.collect { v -> _state.update { it.copy(appVolume = AudioEffectsManager.liveEq.value?.appVolume ?: v) } } }
            launch { prefs.volumeLimit.collect { v -> _state.update { it.copy(volumeLimit = AudioEffectsManager.liveEq.value?.volumeLimit ?: v) } } }

            launch { prefs.widgetTransparency.collect { v -> _state.update { it.copy(widgetTransparency = v) } } }
            launch { prefs.widgetTheme.collect { v -> _state.update { it.copy(widgetTheme = v) } } }
            launch { prefs.widgetCustomColor.collect { v -> _state.update { it.copy(widgetCustomColor = v) } } }
            launch { prefs.widgetShowLogo.collect { v -> _state.update { it.copy(widgetShowLogo = v) } } }
            launch { prefs.widgetCornerRadius.collect { v -> _state.update { it.copy(widgetCornerRadius = v) } } }
        }
    }

    fun setHomeMerge(value: Boolean) = viewModelScope.launch { prefs.setHomeMerge(value) }
    fun moveHomeSection(from: Int, to: Int) = viewModelScope.launch {
        val order = _state.value.homeSectionOrder.toMutableList()
        if (from !in order.indices || to !in order.indices) return@launch
        order.add(to, order.removeAt(from))
        prefs.setHomeSectionOrder(order)
    }
    fun setHomeSectionVisible(id: String, visible: Boolean) = viewModelScope.launch { prefs.setHomeSectionVisible(id, visible) }
    fun setCrossfadeBeatSync(value: Boolean) = viewModelScope.launch { prefs.setCrossfadeBeatSync(value) }
    fun setWidgetPaddingX(value: Int) = viewModelScope.launch { prefs.setWidgetPaddingX(value) }
    fun setWidgetPaddingY(value: Int) = viewModelScope.launch { prefs.setWidgetPaddingY(value) }
    fun setTheme(theme: String) = viewModelScope.launch { prefs.setTheme(theme) }
    fun setAccentColor(color: String) = viewModelScope.launch { prefs.setAccentColor(color) }
    fun setFontSizeScale(scale: Float) = viewModelScope.launch { prefs.setFontSizeScale(scale) }
    fun setFourRowsGrid(enabled: Boolean) = viewModelScope.launch { prefs.setFourRowsGrid(enabled) }

    fun setHomeLayoutStyle(style: String) = viewModelScope.launch { prefs.setHomeLayoutStyle(style) }
    fun setHomeScrollDirection(direction: String) = viewModelScope.launch { prefs.setHomeScrollDirection(direction) }
    fun setHomeHorizontalRows(rows: Int) = viewModelScope.launch { prefs.setHomeHorizontalRows(rows) }
    fun setHideRecentlyPlayed(hide: Boolean) = viewModelScope.launch { prefs.setHideRecentlyPlayed(hide) }
    fun setRecentsPlacement(value: String) = viewModelScope.launch { prefs.setRecentsPlacement(value) }
    fun setEqHeadroomDb(value: Float) = changeAudio { it.copy(eqHeadroomDb = value.coerceIn(0f, 18f)) }
    fun setSurroundDepth(value: Float) = changeAudio { it.copy(surroundDepth = value.coerceIn(0f, 1f)) }
    fun setIgnoredFolders(folders: Set<String>) = viewModelScope.launch { prefs.setIgnoredFolders(folders) }
    fun setSortOrder(order: String) = viewModelScope.launch { prefs.setSortOrder(order) }

    fun setIgnoreBelowSec(sec: Int) = viewModelScope.launch { prefs.setIgnoreBelowSec(sec) }
    fun setIgnoreBelowKb(kb: Int) = viewModelScope.launch { prefs.setIgnoreBelowKb(kb) }
    fun toggleIgnoredFolder(folder: String) = viewModelScope.launch { prefs.toggleIgnoredFolder(folder) }

    fun setCrossfade(enabled: Boolean) = viewModelScope.launch { prefs.setCrossfade(enabled) }
    fun setCrossfadeDuration(duration: Int) = viewModelScope.launch { prefs.setCrossfadeDuration(duration) }
    fun setPauseOnHeadphoneDisconnect(enabled: Boolean) = viewModelScope.launch { prefs.setPauseOnHeadphoneDisconnect(enabled) }
    fun setPlayOnHeadsetConnect(enabled: Boolean) = viewModelScope.launch { prefs.setPlayOnHeadsetConnect(enabled) }
    fun setBtDisconnectAction(action: String) = viewModelScope.launch { prefs.setBtDisconnectAction(action) }
    fun setBtConnectAction(action: String) = viewModelScope.launch { prefs.setBtConnectAction(action) }

    private fun changeAudio(change: (SettingsState) -> SettingsState) {
        _state.update(change)
        val value = _state.value
        AudioEffectsManager.submit(getApplication<Application>(), LiveEqState(value.equalizerEnabled, value.eqPreset, value.eqBands,
            value.eqHeadroomDb, value.balance, value.stereoWidening, value.surroundDepth, value.surroundPanSpeed,
            value.appVolume, value.volumeLimit))
    }
    private fun applyEq(enabled: Boolean, preset: String, bands: List<Int>) {
        val safe = List(5) { bands.getOrElse(it) { 0 }.coerceIn(-12, 12) }
        changeAudio { it.copy(equalizerEnabled = enabled, eqPreset = preset, eqBands = safe) }
    }
    fun setEqualizerEnabled(enabled: Boolean) = applyEq(enabled, _state.value.eqPreset, _state.value.eqBands)
    fun setEqPreset(preset: String) = applyEq(_state.value.equalizerEnabled, preset,
        AudioEffectsManager.PRESETS[preset] ?: _state.value.eqBands)
    fun setEqBands(bands: List<Int>) = applyEq(_state.value.equalizerEnabled, "Custom", bands)
    fun setEqBand(index: Int, value: Int) {
        if (index !in 0..4) return
        val bands = _state.value.eqBands.toMutableList()
        while (bands.size < 5) bands.add(0)
        bands[index] = value.coerceIn(-12, 12)
        applyEq(_state.value.equalizerEnabled, "Custom", bands)
    }
    fun setBalance(balance: Float) = changeAudio { it.copy(balance = balance.coerceIn(-1f, 1f)) }
    fun setStereoWidening(enabled: Boolean) = changeAudio { it.copy(stereoWidening = enabled) }
    fun setSurroundPanSpeed(speed: Int) = changeAudio { it.copy(surroundPanSpeed = speed.coerceIn(2, 10)) }

    fun setAppVolume(volume: Float) = changeAudio { it.copy(appVolume = volume.coerceIn(0f, 1f)) }
    fun setVolumeLimit(limit: Float) = changeAudio { it.copy(volumeLimit = limit.coerceIn(0f, 1f)) }

    fun setWidgetTransparency(t: Float) = viewModelScope.launch { prefs.setWidgetTransparency(t) }
    fun setWidgetTheme(t: String) = viewModelScope.launch { prefs.setWidgetTheme(t) }
    fun setWidgetCustomColor(c: String) = viewModelScope.launch { prefs.setWidgetCustomColor(c) }
    fun setWidgetShowLogo(s: Boolean) = viewModelScope.launch { prefs.setWidgetShowLogo(s) }
    fun setWidgetCornerRadius(r: Int) = viewModelScope.launch { prefs.setWidgetCornerRadius(r) }
}
