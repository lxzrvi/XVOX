package com.xvox.music.features.settings

import com.xvox.music.audio.EqBands
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

/** A reversible live-preview snapshot for the compact Equalizer sheet. */
data class EqualizerControlsSnapshot(
    val equalizerEnabled: Boolean,
    val eqPreset: String,
    val eqBandCount: Int,
    val eqBands: List<Int>,
    val customEqBands: List<Int>,
    val appVolume: Float,
    val reverbPreset: String,
    val reverbAmount: Float,
    val noiseReduction: Float,
    val noiseReductionEnabled: Boolean,
    val softenHighs: Float,
    val grainControlEnabled: Boolean
) {
    companion object {
        fun from(state: SettingsState) = EqualizerControlsSnapshot(
            equalizerEnabled = state.equalizerEnabled,
            eqPreset = state.eqPreset,
            eqBandCount = state.eqBandCount,
            eqBands = state.eqBands.toList(),
            customEqBands = state.customEqBands.toList(),
            appVolume = state.appVolume,
            reverbPreset = state.reverbPreset,
            reverbAmount = state.reverbAmount,
            noiseReduction = state.noiseReduction,
            noiseReductionEnabled = state.noiseReductionEnabled,
            softenHighs = state.softenHighs,
            grainControlEnabled = state.grainControlEnabled
        )
    }
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferencesRepository(application)
    private var pendingWidget: com.xvox.music.widget.WidgetCustomization? = null
    private var pendingLyrics: com.xvox.music.data.preferences.LyricsSettings? = null
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            launch { prefs.nowPlayingStyle.collect { v -> _state.update { it.copy(nowPlayingStyle = v) } } }
            launch { prefs.splitShowPill.collect { v -> _state.update { it.copy(splitShowPill = v) } } }
            launch { prefs.splitHideCollection.collect { v -> _state.update { it.copy(splitHideCollection = v) } } }
            launch { prefs.playlistStyle.collect { v -> _state.update { it.copy(playlistStyle = v) } } }
            launch { prefs.playlistLongHeight.collect { v -> _state.update { it.copy(playlistLongHeight = v) } } }
            launch { prefs.playlistRows.collect { v -> _state.update { it.copy(playlistRows = v) } } }
            launch { prefs.playlistCardOrientation.collect { v -> _state.update { it.copy(playlistCardOrientation = v) } } }
            launch { prefs.settingsPreviewHidden.collect { v -> _state.update { it.copy(previewHidden = v) } } }
            launch { prefs.lastSettingsTab.collect { v -> _state.update { it.copy(lastSettingsTab = v) } } }
            launch { prefs.widgetSizes.collect { v -> _state.update { it.copy(widgetSizes = v) } } }
            launch { prefs.homeMerge.collect { v -> _state.update { it.copy(homeMerge = v) } } }
            launch { prefs.homeMergedSections.collect { v -> _state.update { it.copy(homeMergedSections = v) } } }
            launch { prefs.homeSectionOrder.collect { v -> _state.update { it.copy(homeSectionOrder = v) } } }
            launch { prefs.homeHiddenSections.collect { v -> _state.update { it.copy(homeHiddenSections = v) } } }
            launch { prefs.crossfadeSmart.collect { v -> _state.update { it.copy(crossfadeSmart = v) } } }
            launch { prefs.crossfadeClashControl.collect { v -> _state.update { it.copy(crossfadeClashControl = v) } } }
            launch { prefs.crossfadeBeatSync.collect { v -> _state.update { it.copy(crossfadeBeatSync = v) } } }
            launch { prefs.widgetCustomization.collect { value ->
                if (pendingWidget == null || pendingWidget == value) {
                    pendingWidget = null
                    _state.update { it.copy(widgetCustomization = value) }
                }
            } }
            launch { prefs.widgetPaddingX.collect { v -> _state.update { it.copy(widgetPaddingX = v) } } }
            launch { prefs.widgetPaddingY.collect { v -> _state.update { it.copy(widgetPaddingY = v) } } }
            launch { prefs.lyricsSettings.collect { value ->
                if (pendingLyrics == null || pendingLyrics == value) {
                    pendingLyrics = null
                    _state.update { it.copy(lyrics = value) }
                }
            } }
            launch { prefs.hapticFeedbackEnabled.collect { v -> _state.update { it.copy(hapticFeedbackEnabled = v) } } }
            launch { prefs.hapticIntensity.collect { v -> _state.update { it.copy(hapticIntensity = v) } } }
            launch { prefs.theme.collect { v -> _state.update { it.copy(theme = v) } } }
            launch { prefs.accentColor.collect { v -> _state.update { it.copy(accentColor = v) } } }
            launch { prefs.themeBackground.collect { v -> _state.update { it.copy(backgroundName = v) } } }
            launch { prefs.headerImageUri.collect { v -> _state.update { it.copy(headerImageUri = v) } } }
            launch { prefs.themeBackgroundImage.collect { v -> _state.update { it.copy(backgroundImageUri = v.ifBlank { null }) } } }
            launch { prefs.hideStatusBar.collect { v -> _state.update { it.copy(hideStatusBar = v) } } }
            launch { prefs.cardTransparency.collect { v -> _state.update { it.copy(cardTransparency = v) } } }
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
            launch { prefs.gapless.collect { v -> _state.update { it.copy(gapless = v) } } }
            launch { prefs.crossfadeDuration.collect { v -> _state.update { it.copy(crossfadeDuration = v) } } }
            launch { prefs.pauseOnHeadphoneDisconnect.collect { v -> _state.update { it.copy(pauseOnHeadphoneDisconnect = v) } } }
            launch { prefs.playOnHeadsetConnect.collect { v -> _state.update { it.copy(playOnHeadsetConnect = v) } } }
            launch { prefs.btDisconnectAction.collect { v -> _state.update { it.copy(btDisconnectAction = v) } } }
            launch { prefs.btConnectAction.collect { v -> _state.update { it.copy(btConnectAction = v) } } }

            launch { prefs.equalizerEnabled.collect { v -> _state.update { it.copy(equalizerEnabled = AudioEffectsManager.liveEq.value?.enabled ?: v) } } }
            launch { prefs.eqPreset.collect { v -> _state.update { it.copy(eqPreset = AudioEffectsManager.liveEq.value?.preset ?: v) } } }
            launch { prefs.eqBandCount.collect { v -> _state.update { it.copy(eqBandCount = AudioEffectsManager.liveEq.value?.bandCount ?: v) } } }
            launch { prefs.noiseReduction.collect { v -> _state.update { it.copy(noiseReduction = AudioEffectsManager.liveEq.value?.noiseReduction ?: v) } } }
            launch { prefs.noiseReductionEnabled.collect { v -> _state.update { it.copy(noiseReductionEnabled = AudioEffectsManager.liveEq.value?.noiseReductionEnabled ?: v) } } }
            launch { prefs.softenHighs.collect { v -> _state.update { it.copy(softenHighs = AudioEffectsManager.liveEq.value?.softenHighs ?: v) } } }
            launch { prefs.grainControlEnabled.collect { v -> _state.update { it.copy(grainControlEnabled = AudioEffectsManager.liveEq.value?.grainControlEnabled ?: v) } } }
            launch { prefs.eqBands.collect { v -> _state.update { it.copy(eqBands = AudioEffectsManager.liveEq.value?.bands ?: v) } } }
            launch { prefs.customEqBands.collect { v -> _state.update { it.copy(customEqBands = AudioEffectsManager.liveEq.value?.customBands ?: v) } } }
            launch { prefs.balance.collect { v -> _state.update { it.copy(balance = AudioEffectsManager.liveEq.value?.balance ?: v) } } }
            launch { prefs.stereoWidening.collect { v -> _state.update { it.copy(stereoWidening = AudioEffectsManager.liveEq.value?.surroundEnabled ?: v) } } }
            launch { prefs.surroundPanSpeed.collect { v -> _state.update { it.copy(surroundPanSpeed = AudioEffectsManager.liveEq.value?.orbitSeconds ?: v) } } }
            launch { prefs.surroundWidth.collect { v -> _state.update { it.copy(surroundWidth = v) } } }
            launch { prefs.surroundPosition.collect { v -> _state.update { it.copy(surroundPosition = v) } } }
            launch { prefs.roomAmount.collect { v -> _state.update { it.copy(roomAmount = AudioEffectsManager.liveEq.value?.roomAmount ?: v) } } }
            launch { prefs.reverbPreset.collect { v -> _state.update { it.copy(reverbPreset = AudioEffectsManager.liveEq.value?.reverbPreset ?: v) } } }
            launch { prefs.reverbAmount.collect { v -> _state.update { it.copy(reverbAmount = AudioEffectsManager.liveEq.value?.reverbAmount ?: v) } } }
            launch { prefs.hrtf.collect { v -> _state.update { it.copy(hrtf = v) } } }
            launch { prefs.centerPreservation.collect { v -> _state.update { it.copy(centerPreservation = v) } } }

            launch { prefs.appVolume.collect { v -> _state.update { it.copy(appVolume = AudioEffectsManager.liveEq.value?.appVolume ?: v) } } }
            launch { prefs.volumeLimit.collect { v -> _state.update { it.copy(volumeLimit = AudioEffectsManager.liveEq.value?.volumeLimit ?: v) } } }

            launch { prefs.widgetTransparency.collect { v -> _state.update { it.copy(widgetTransparency = v) } } }
            launch { prefs.widgetTheme.collect { v -> _state.update { it.copy(widgetTheme = v) } } }
            launch { prefs.widgetCustomColor.collect { v -> _state.update { it.copy(widgetCustomColor = v) } } }
            launch { prefs.widgetShowLogo.collect { v -> _state.update { it.copy(widgetShowLogo = v) } } }
            launch { prefs.widgetCornerRadius.collect { v -> _state.update { it.copy(widgetCornerRadius = v) } } }

            launch { prefs.chromeStyle.collect { v -> _state.update { it.copy(chromeStyle = v) } } }
            launch { prefs.backgroundBrightness.collect { v -> _state.update { it.copy(backgroundBrightness = v) } } }
            launch { prefs.audioOutputRoute.collect { v -> _state.update { it.copy(audioOutputRoute = v) } } }
            launch { prefs.playbackSpeed.collect { v -> _state.update { it.copy(playbackSpeed = v) } } }
            launch { prefs.playbackPitch.collect { v -> _state.update { it.copy(playbackPitch = v) } } }
            launch { prefs.profileLines.collect { v -> _state.update { it.copy(profileLines = v) } } }
            launch { prefs.greetingIntervalMs.collect { v -> _state.update { it.copy(greetingIntervalMs = v) } } }
            launch { prefs.remindersEnabled.collect { v -> _state.update { it.copy(remindersEnabled = v) } } }
            launch { prefs.persistentBackgroundPlayback.collect { v -> _state.update { it.copy(persistentBackgroundPlayback = v) } } }
        }
    }

    fun setSplitShowPill(v: Boolean) = viewModelScope.launch { prefs.setSplitShowPill(v) }
    fun setSplitHideCollection(v: Boolean) = viewModelScope.launch { prefs.setSplitHideCollection(v) }
    fun setPlaylistStyle(v: String) = viewModelScope.launch { prefs.setPlaylistStyle(v) }
    fun setPlaylistLongHeight(v: Int) = viewModelScope.launch { prefs.setPlaylistLongHeight(v) }
    fun setPlaylistRows(v: Int) = viewModelScope.launch { prefs.setPlaylistRows(v) }
    fun setPlaylistCardOrientation(v: String) = viewModelScope.launch { prefs.setPlaylistCardOrientation(v) }
    fun setPreviewHidden(v: Boolean) = viewModelScope.launch { prefs.setSettingsPreviewHidden(v) }
    fun setHeaderImageUri(uri: String?) = viewModelScope.launch { prefs.setHeaderImageUri(uri) }
    fun setNowPlayingStyle(style: String) = viewModelScope.launch { prefs.setNowPlayingStyle(style) }
    fun setPersistentBackgroundPlayback(enabled: Boolean) = viewModelScope.launch { prefs.setPersistentBackgroundPlayback(enabled) }
    fun setGreetingIntervalMs(value: Long) = viewModelScope.launch { prefs.setGreetingIntervalMs(value) }
    fun setLastSettingsTab(v: String) = viewModelScope.launch { prefs.setLastSettingsTab(v) }
    /** View-only: which widget size the settings preview is showing. */
    fun setWidgetPreviewSize(key: String) = _state.update { it.copy(widgetPreviewSize = key) }

    /** Saves the given size's own widget settings; other sizes are untouched. */
    fun setWidgetSizeCustomization(key: String, value: com.xvox.music.widget.WidgetCustomization) {
        val sanitized = value.sanitized()
        // Update the studio preview synchronously; DataStore will subsequently confirm this
        // per-size value without making a slider briefly snap back to its previous position.
        _state.update { current ->
            current.copy(widgetSizes = current.widgetSizes + (key to sanitized))
        }
        viewModelScope.launch { prefs.setWidgetSizeCustomization(key, sanitized) }
    }
    fun setWidgetCustomizationForSize(key: String, value: com.xvox.music.widget.WidgetCustomization) =
        setWidgetSizeCustomization(key, value)
    fun setWidgetCustomization(value: com.xvox.music.widget.WidgetCustomization) =
        updateWidget { value }
    fun setHomeMerge(value: Boolean) = viewModelScope.launch { prefs.setHomeMerge(value) }
    fun setHomeSectionMerged(id: String, merged: Boolean) = viewModelScope.launch { prefs.setHomeSectionMerged(id, merged) }
    fun moveHomeSection(from: Int, to: Int) = viewModelScope.launch {
        val order = _state.value.homeSectionOrder.toMutableList()
        if (from !in order.indices || to !in order.indices) return@launch
        order.add(to, order.removeAt(from))
        prefs.setHomeSectionOrder(order)
    }
    fun setHomeSectionVisible(id: String, visible: Boolean) = viewModelScope.launch { prefs.setHomeSectionVisible(id, visible) }
    fun setCrossfadeSmart(v: Boolean) = viewModelScope.launch { prefs.setCrossfadeSmart(v) }
    fun setCrossfadeClashControl(v: Float) = viewModelScope.launch { prefs.setCrossfadeClashControl(v) }
    fun setCrossfadeBeatSync(value: Boolean) = viewModelScope.launch { prefs.setCrossfadeBeatSync(value) }
    fun setWidgetPaddingX(value: Int) = viewModelScope.launch { prefs.setWidgetPaddingX(value) }
    fun setWidgetPaddingY(value: Int) = viewModelScope.launch { prefs.setWidgetPaddingY(value) }
    fun updateWidget(change: (com.xvox.music.widget.WidgetCustomization) -> com.xvox.music.widget.WidgetCustomization) {
        val next = change(_state.value.widgetCustomization).sanitized()
        pendingWidget = next
        _state.update { it.copy(widgetCustomization = next) }
        com.xvox.music.data.preferences.PreferenceWriteQueue.submit("widget") { prefs.setWidgetCustomization(next) }
    }
    fun updateLyrics(change: (com.xvox.music.data.preferences.LyricsSettings) -> com.xvox.music.data.preferences.LyricsSettings) {
        val next = change(_state.value.lyrics).sanitized()
        pendingLyrics = next
        _state.update { it.copy(lyrics = next) }
        com.xvox.music.data.preferences.PreferenceWriteQueue.submit("lyrics") { prefs.setLyricsSettings(next) }
    }
    fun setHapticFeedbackEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setHapticFeedbackEnabled(enabled) }
    fun setHapticIntensity(v: String) = viewModelScope.launch { prefs.setHapticIntensity(v) }
    fun setTheme(theme: String) = viewModelScope.launch { prefs.setTheme(theme) }
    fun setAccentColor(color: String) = viewModelScope.launch { prefs.setAccentColor(color) }
    fun setBackgroundName(name: String) = viewModelScope.launch { prefs.setThemeBackground(name) }
    fun setBackgroundImage(uri: String?) = viewModelScope.launch { prefs.setThemeBackgroundImage(uri) }
    fun setHideStatusBar(hide: Boolean) = viewModelScope.launch { prefs.setHideStatusBar(hide) }
    fun setCardTransparency(value: Float) = viewModelScope.launch { prefs.setCardTransparency(value) }
    fun setFontSizeScale(scale: Float) = viewModelScope.launch { prefs.setFontSizeScale(scale) }
    fun setFourRowsGrid(enabled: Boolean) = viewModelScope.launch { prefs.setFourRowsGrid(enabled) }

    fun setHomeLayoutStyle(style: String) = viewModelScope.launch { prefs.setHomeLayoutStyle(style) }
    fun setHomeScrollDirection(direction: String) = viewModelScope.launch { prefs.setHomeScrollDirection(direction) }
    fun setHomeHorizontalRows(rows: Int) = viewModelScope.launch { prefs.setHomeHorizontalRows(rows) }
    fun setHideRecentlyPlayed(hide: Boolean) = viewModelScope.launch { prefs.setHideRecentlyPlayed(hide) }
    fun setRecentsPlacement(value: String) = viewModelScope.launch { prefs.setRecentsPlacement(value) }
    fun setEqHeadroomDb(value: Float) = changeAudio { it.copy(eqHeadroomDb = value.coerceIn(0f, 18f)) }
    fun setSurroundDepth(value: Float) = changeAudio { it.copy(surroundDepth = value.coerceIn(0f, 1f)) }
    fun setSurroundWidth(value: Float) = changeAudio { it.copy(surroundWidth = value.coerceIn(.05f, 1f)) }
    fun setSurroundPosition(value: Float) = changeAudio { it.copy(surroundPosition = value.coerceIn(-1.5f, 1.5f)) }
    fun setRoomAmount(value: Float) = changeAudio { it.copy(roomAmount = value.coerceIn(0f, 1f)) }
    fun setReverbPreset(preset: String) = changeAudio { current ->
        val selected = com.xvox.music.audio.ReverbPresets.normalize(preset)
        current.copy(
            reverbPreset = selected,
            // A room selection should be audible immediately, but its amount never chooses another room.
            reverbAmount = if (selected != com.xvox.music.audio.ReverbPresets.OFF && current.reverbAmount <= .001f) .50f else current.reverbAmount
        )
    }
    fun setReverbAmount(value: Float) = changeAudio { it.copy(reverbAmount = value.coerceIn(0f, 1f)) }
    fun setHrtf(value: Float) = changeAudio { it.copy(hrtf = value.coerceIn(0f, 1f)) }
    fun setCenterPreservation(value: Float) = changeAudio { it.copy(centerPreservation = value.coerceIn(0f, 1f)) }
    fun setIgnoredFolders(folders: Set<String>) = viewModelScope.launch { prefs.setIgnoredFolders(folders) }
    fun setSortOrder(order: String) = viewModelScope.launch { prefs.setSortOrder(order) }

    fun setIgnoreBelowSec(sec: Int) = viewModelScope.launch { prefs.setIgnoreBelowSec(sec) }
    fun setIgnoreBelowKb(kb: Int) = viewModelScope.launch { prefs.setIgnoreBelowKb(kb) }
    fun toggleIgnoredFolder(folder: String) = viewModelScope.launch { prefs.toggleIgnoredFolder(folder) }

    fun setCrossfade(enabled: Boolean) = viewModelScope.launch { prefs.setCrossfade(enabled) }
    fun setGapless(enabled: Boolean) = viewModelScope.launch { prefs.setGapless(enabled) }
    fun setCrossfadeDuration(duration: Int) = viewModelScope.launch { prefs.setCrossfadeDuration(duration) }
    fun setPauseOnHeadphoneDisconnect(enabled: Boolean) = viewModelScope.launch { prefs.setPauseOnHeadphoneDisconnect(enabled) }
    fun setPlayOnHeadsetConnect(enabled: Boolean) = viewModelScope.launch { prefs.setPlayOnHeadsetConnect(enabled) }
    fun setBtDisconnectAction(action: String) = viewModelScope.launch { prefs.setBtDisconnectAction(action) }
    fun setBtConnectAction(action: String) = viewModelScope.launch { prefs.setBtConnectAction(action) }

    /** Captures only controls owned by the Equalizer sheet so Cancel can restore its live preview. */
    fun snapshotEqualizerControls(): EqualizerControlsSnapshot = EqualizerControlsSnapshot.from(_state.value)

    fun restoreEqualizerControls(snapshot: EqualizerControlsSnapshot) = changeAudio { current ->
        current.copy(
            equalizerEnabled = snapshot.equalizerEnabled,
            eqPreset = snapshot.eqPreset,
            eqBandCount = snapshot.eqBandCount,
            eqBands = snapshot.eqBands,
            customEqBands = snapshot.customEqBands,
            appVolume = snapshot.appVolume,
            reverbPreset = snapshot.reverbPreset,
            reverbAmount = snapshot.reverbAmount,
            noiseReduction = snapshot.noiseReduction,
            noiseReductionEnabled = snapshot.noiseReductionEnabled,
            softenHighs = snapshot.softenHighs,
            grainControlEnabled = snapshot.grainControlEnabled
        )
    }

    /** Restores the compact Equalizer controls to the supplied HTML design's default values. */
    fun resetEqualizerControls() = changeAudio { current ->
        current.copy(
            equalizerEnabled = false,
            eqPreset = "Flat",
            eqBandCount = 5,
            eqBands = List(5) { 0 },
            customEqBands = List(5) { 0 },
            appVolume = 1f,
            reverbPreset = com.xvox.music.audio.ReverbPresets.OFF,
            reverbAmount = .50f,
            noiseReduction = .50f,
            noiseReductionEnabled = false,
            softenHighs = .50f,
            grainControlEnabled = false
        )
    }

    private fun changeAudio(change: (SettingsState) -> SettingsState) {
        _state.update(change)
        val value = _state.value
        AudioEffectsManager.submit(
            getApplication<Application>(),
            LiveEqState(
                enabled = value.equalizerEnabled,
                preset = value.eqPreset,
                bands = value.eqBands,
                headroomDb = value.eqHeadroomDb,
                balance = value.balance,
                surroundEnabled = value.stereoWidening,
                surroundDepth = value.surroundDepth,
                orbitSeconds = value.surroundPanSpeed,
                appVolume = value.appVolume,
                volumeLimit = value.volumeLimit,
                bandCount = value.eqBandCount,
                noiseReduction = value.noiseReduction,
                softenHighs = value.softenHighs,
                surroundWidth = value.surroundWidth,
                surroundPosition = value.surroundPosition,
                roomAmount = value.roomAmount,
                reverbAmount = value.reverbAmount,
                hrtf = value.hrtf,
                centerPreservation = value.centerPreservation,
                reverbPreset = value.reverbPreset,
                noiseReductionEnabled = value.noiseReductionEnabled,
                grainControlEnabled = value.grainControlEnabled,
                customBands = value.customEqBands
            )
        )
    }
    private fun applyEq(enabled: Boolean, preset: String, bands: List<Int>) {
        val normalizedPreset = AudioEffectsManager.normalizeEqPreset(preset)
        val safe = EqBands.convert(bands, _state.value.eqBandCount)
        changeAudio { current ->
            current.copy(
                equalizerEnabled = enabled,
                eqPreset = normalizedPreset,
                eqBands = safe,
                // A manual adjustment is the only operation that overwrites the retained Custom curve.
                customEqBands = if (normalizedPreset == "Custom") EqBands.convert(safe, 5) else current.customEqBands
            )
        }
    }
    fun setEqualizerEnabled(enabled: Boolean) = applyEq(enabled, _state.value.eqPreset, _state.value.eqBands)
    fun setEqPreset(preset: String) {
        val normalized = AudioEffectsManager.normalizeEqPreset(preset)
        val bands = when (normalized) {
            "Custom" -> EqBands.convert(_state.value.customEqBands, _state.value.eqBandCount)
            else -> AudioEffectsManager.PRESETS[normalized] ?: _state.value.eqBands
        }
        applyEq(_state.value.equalizerEnabled, normalized, bands)
    }
    fun setEqBands(bands: List<Int>) = applyEq(_state.value.equalizerEnabled, "Custom", bands)
    fun setEqBand(index: Int, value: Int) {
        if (index !in 0 until _state.value.eqBandCount) return
        // Start from what is visibly being edited, then retain that new hand-authored curve as Custom.
        val bands = _state.value.eqBands.toMutableList()
        while (bands.size < _state.value.eqBandCount) bands.add(0)
        bands[index] = value.coerceIn(-12, 12)
        applyEq(_state.value.equalizerEnabled, "Custom", bands)
    }
    fun setEqBandCount(count: Int) = changeAudio { current ->
        val safeCount = EqBands.count(count)
        current.copy(eqBandCount = safeCount, eqBands = EqBands.convert(current.eqBands, safeCount))
    }
    fun setNoiseReduction(value: Float) = changeAudio { it.copy(noiseReduction = value.coerceIn(0f, 1f)) }
    fun setNoiseReductionEnabled(enabled: Boolean) = changeAudio { current ->
        current.copy(
            noiseReductionEnabled = enabled,
            noiseReduction = if (enabled && current.noiseReduction <= .001f) .50f else current.noiseReduction
        )
    }
    fun setSoftenHighs(value: Float) = changeAudio { it.copy(softenHighs = value.coerceIn(0f, 1f)) }
    fun setGrainControlEnabled(enabled: Boolean) = changeAudio { current ->
        current.copy(
            grainControlEnabled = enabled,
            softenHighs = if (enabled && current.softenHighs <= .001f) .50f else current.softenHighs
        )
    }
    fun setBalance(balance: Float) = changeAudio { it.copy(balance = balance.coerceIn(-1f, 1f)) }
    fun setStereoWidening(enabled: Boolean) = changeAudio { it.copy(stereoWidening = enabled) }
    fun setSurroundPanSpeed(speed: Int) = changeAudio { it.copy(surroundPanSpeed = speed.coerceIn(0, 20)) }

    fun setAppVolume(volume: Float) = changeAudio { it.copy(appVolume = volume.coerceIn(0f, 2f)) }
    fun setVolumeLimit(limit: Float) = changeAudio { it.copy(volumeLimit = limit.coerceIn(0f, 1f)) }

    fun setWidgetTransparency(t: Float) = viewModelScope.launch { prefs.setWidgetTransparency(t) }
    fun setWidgetTheme(t: String) = viewModelScope.launch { prefs.setWidgetTheme(t) }
    fun setWidgetCustomColor(c: String) = viewModelScope.launch { prefs.setWidgetCustomColor(c) }
    fun setWidgetShowLogo(s: Boolean) = viewModelScope.launch { prefs.setWidgetShowLogo(s) }
    fun setWidgetCornerRadius(r: Int) = viewModelScope.launch { prefs.setWidgetCornerRadius(r) }

    fun setChromeStyle(change: (com.xvox.music.core.ui.chrome.XvoxChromeStyle) -> com.xvox.music.core.ui.chrome.XvoxChromeStyle) {
        val next = change(_state.value.chromeStyle)
        _state.update { it.copy(chromeStyle = next) }
        viewModelScope.launch { prefs.setChromeStyle(next) }
    }
    fun setBackgroundBrightness(value: Float) = viewModelScope.launch { prefs.setBackgroundBrightness(value) }
    fun setAudioOutputRoute(route: String) = viewModelScope.launch { prefs.setAudioOutputRoute(route) }
    fun setPlaybackSpeed(value: Float) = viewModelScope.launch { prefs.setPlaybackSpeed(value) }
    fun setPlaybackPitch(value: Float) = viewModelScope.launch { prefs.setPlaybackPitch(value) }
    fun setProfileLines(lines: List<String>) = viewModelScope.launch { prefs.setProfileLines(lines) }
    fun setRemindersEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setRemindersEnabled(enabled) }
}
