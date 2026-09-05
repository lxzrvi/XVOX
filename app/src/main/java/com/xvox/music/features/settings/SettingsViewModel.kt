package com.xvox.music.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.audio.AudioEffectsManager
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.widget.XvoxAppWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferencesRepository(application.applicationContext)
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            launch { prefs.theme.collect { v -> _state.update { it.copy(theme = v) } } }
            launch { prefs.accentColor.collect { v -> _state.update { it.copy(accentColor = v) } } }
            launch { prefs.fontSizeScale.collect { v -> _state.update { it.copy(fontSizeScale = v) } } }
            launch { prefs.hapticFeedback.collect { v -> _state.update { it.copy(hapticFeedback = v) } } }
            launch { prefs.hapticStrength.collect { v -> _state.update { it.copy(hapticStrength = v) } } }

            launch { prefs.gaplessPlayback.collect { v -> _state.update { it.copy(gaplessPlayback = v) } } }
            launch { prefs.crossfade.collect { v -> _state.update { it.copy(crossfade = v) } } }
            launch { prefs.crossfadeDuration.collect { v -> _state.update { it.copy(crossfadeDuration = v) } } }
            launch { prefs.fadeIn.collect { v -> _state.update { it.copy(fadeIn = v) } } }
            launch { prefs.fadeOut.collect { v -> _state.update { it.copy(fadeOut = v) } } }
            launch { prefs.replayGain.collect { v -> _state.update { it.copy(replayGain = v) } } }
            launch { prefs.loudnessNormalization.collect { v -> _state.update { it.copy(loudnessNormalization = v) } } }
            launch { prefs.skipSilence.collect { v -> _state.update { it.copy(skipSilence = v) } } }
            launch { prefs.audioFocus.collect { v -> _state.update { it.copy(audioFocus = v) } } }
            launch { prefs.pauseOnHeadphoneDisconnect.collect { v -> _state.update { it.copy(pauseOnHeadphoneDisconnect = v) } } }
            launch { prefs.playOnHeadsetConnect.collect { v -> _state.update { it.copy(playOnHeadsetConnect = v) } } }
            launch { prefs.clearQueueAfterPlayback.collect { v -> _state.update { it.copy(clearQueueAfterPlayback = v) } } }
            launch { prefs.rememberQueue.collect { v -> _state.update { it.copy(rememberQueue = v) } } }

            launch { prefs.equalizerEnabled.collect { v -> _state.update { it.copy(equalizerEnabled = v) } } }
            launch { prefs.eqPreset.collect { v -> _state.update { it.copy(eqPreset = v) } } }
            launch { prefs.eqBands.collect { v -> _state.update { it.copy(eqBands = v) } } }
            launch { prefs.bassBoost.collect { v -> _state.update { it.copy(bassBoost = v) } } }
            launch { prefs.bassBoostStrength.collect { v -> _state.update { it.copy(bassBoostStrength = v) } } }
            launch { prefs.virtualizerEnabled.collect { v -> _state.update { it.copy(virtualizerEnabled = v) } } }
            launch { prefs.virtualizerStrength.collect { v -> _state.update { it.copy(virtualizerStrength = v) } } }
            launch { prefs.loudnessEnhancer.collect { v -> _state.update { it.copy(loudnessEnhancer = v) } } }
            launch { prefs.loudnessGainMb.collect { v -> _state.update { it.copy(loudnessGainMb = v) } } }
            launch { prefs.balance.collect { v -> _state.update { it.copy(balance = v) } } }

            launch { prefs.appVolume.collect { v -> _state.update { it.copy(appVolume = v) } } }
            launch { prefs.rememberVolume.collect { v -> _state.update { it.copy(rememberVolume = v) } } }
            launch { prefs.volumeLimit.collect { v -> _state.update { it.copy(volumeLimit = v) } } }

            launch { prefs.mediaNotification.collect { v -> _state.update { it.copy(mediaNotification = v) } } }

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
    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch { prefs.setHapticFeedback(enabled) }
    fun setHapticStrength(strength: String) = viewModelScope.launch { prefs.setHapticStrength(strength) }

    fun setGaplessPlayback(enabled: Boolean) = viewModelScope.launch { prefs.setGaplessPlayback(enabled) }
    fun setCrossfade(enabled: Boolean) = viewModelScope.launch { prefs.setCrossfade(enabled) }
    fun setCrossfadeDuration(seconds: Int) = viewModelScope.launch { prefs.setCrossfadeDuration(seconds) }
    fun setFadeIn(enabled: Boolean) = viewModelScope.launch { prefs.setFadeIn(enabled) }
    fun setFadeOut(enabled: Boolean) = viewModelScope.launch { prefs.setFadeOut(enabled) }
    fun setReplayGain(enabled: Boolean) = viewModelScope.launch { prefs.setReplayGain(enabled) }
    fun setLoudnessNormalization(enabled: Boolean) = viewModelScope.launch { prefs.setLoudnessNormalization(enabled) }
    fun setSkipSilence(enabled: Boolean) = viewModelScope.launch { prefs.setSkipSilence(enabled) }
    fun setAudioFocus(enabled: Boolean) = viewModelScope.launch { prefs.setAudioFocus(enabled) }
    fun setPauseOnHeadphoneDisconnect(enabled: Boolean) = viewModelScope.launch { prefs.setPauseOnHeadphoneDisconnect(enabled) }
    fun setPlayOnHeadsetConnect(enabled: Boolean) = viewModelScope.launch { prefs.setPlayOnHeadsetConnect(enabled) }
    fun setClearQueueAfterPlayback(enabled: Boolean) = viewModelScope.launch { prefs.setClearQueueAfterPlayback(enabled) }
    fun setRememberQueue(enabled: Boolean) = viewModelScope.launch { prefs.setRememberQueue(enabled) }

    fun setEqualizerEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setEqualizerEnabled(enabled) }
    fun setEqPreset(preset: String) = viewModelScope.launch {
        prefs.setEqPreset(preset)
        if (preset != "Custom" && AudioEffectsManager.PRESETS.containsKey(preset)) {
            val bandVals = AudioEffectsManager.PRESETS[preset] ?: listOf(0, 0, 0, 0, 0)
            prefs.setEqBands(bandVals)
        }
    }
    fun setEqBands(bands: List<Int>) = viewModelScope.launch {
        prefs.setEqBands(bands)
        prefs.setEqPreset("Custom")
    }
    fun resetEqualizer() = viewModelScope.launch {
        prefs.setEqPreset("Flat")
        prefs.setEqBands(listOf(0, 0, 0, 0, 0))
        prefs.setEqPreamp(0f)
        prefs.setBassBoost(false)
        prefs.setBassBoostStrength(0)
        prefs.setVirtualizer(false)
        prefs.setVirtualizerStrength(0)
        prefs.setLoudnessEnhancer(false)
        prefs.setLoudnessGainMb(0)
        prefs.setBalance(0f)
    }

    fun setBassBoost(enabled: Boolean) = viewModelScope.launch { prefs.setBassBoost(enabled) }
    fun setBassBoostStrength(strength: Int) = viewModelScope.launch { prefs.setBassBoostStrength(strength) }
    fun setVirtualizer(enabled: Boolean) = viewModelScope.launch { prefs.setVirtualizer(enabled) }
    fun setVirtualizerStrength(strength: Int) = viewModelScope.launch { prefs.setVirtualizerStrength(strength) }
    fun setLoudnessEnhancer(enabled: Boolean) = viewModelScope.launch { prefs.setLoudnessEnhancer(enabled) }
    fun setLoudnessGainMb(gainMb: Int) = viewModelScope.launch { prefs.setLoudnessGainMb(gainMb) }
    fun setBalance(balance: Float) = viewModelScope.launch { prefs.setBalance(balance) }

    fun setAppVolume(volume: Float) = viewModelScope.launch { prefs.setAppVolume(volume) }
    fun setRememberVolume(enabled: Boolean) = viewModelScope.launch { prefs.setRememberVolume(enabled) }
    fun setVolumeLimit(limit: Float) = viewModelScope.launch { prefs.setVolumeLimit(limit) }

    fun setMediaNotification(enabled: Boolean) = viewModelScope.launch { prefs.setMediaNotification(enabled) }

    fun setWidgetTransparency(transparency: Float) = viewModelScope.launch { prefs.setWidgetTransparency(transparency) }
    fun setWidgetTheme(theme: String) = viewModelScope.launch {
        prefs.setWidgetTheme(theme)
        XvoxAppWidgetProvider.notifyWidgetUpdate(getApplication())
    }
    fun setWidgetCornerRadius(radiusDp: Int) = viewModelScope.launch {
        prefs.setWidgetCornerRadius(radiusDp)
        XvoxAppWidgetProvider.notifyWidgetUpdate(getApplication())
    }
    fun setWidgetShowLogo(show: Boolean) = viewModelScope.launch {
        prefs.setWidgetShowLogo(show)
        XvoxAppWidgetProvider.notifyWidgetUpdate(getApplication())
    }
}
