package com.xvox.music.features.settings

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.audio.AudioEffectsManager
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.home.HomeFooter
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.widget.XvoxAppWidgetProvider
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    // 1. Appearance & Theme (TOP)
    val theme by prefs.theme.collectAsState(initial = "System")
    val accentColor by prefs.accentColor.collectAsState(initial = "Default")
    val fontSizeScale by prefs.fontSizeScale.collectAsState(initial = 1.0f)
    val haptic by prefs.hapticFeedback.collectAsState(initial = true)
    val hapticStrength by prefs.hapticStrength.collectAsState(initial = "Medium")

    // 2. Playback
    val gapless by prefs.gaplessPlayback.collectAsState(initial = true)
    val crossfade by prefs.crossfade.collectAsState(initial = false)
    val crossfadeDuration by prefs.crossfadeDuration.collectAsState(initial = 3)
    val fadeIn by prefs.fadeIn.collectAsState(initial = false)
    val fadeOut by prefs.fadeOut.collectAsState(initial = false)
    val replayGain by prefs.replayGain.collectAsState(initial = false)
    val loudnessNorm by prefs.loudnessNormalization.collectAsState(initial = false)
    val skipSilence by prefs.skipSilence.collectAsState(initial = false)
    val audioFocus by prefs.audioFocus.collectAsState(initial = true)
    val pauseOnDisconnect by prefs.pauseOnHeadphoneDisconnect.collectAsState(initial = true)
    val playOnConnect by prefs.playOnHeadsetConnect.collectAsState(initial = false)
    val clearQueue by prefs.clearQueueAfterPlayback.collectAsState(initial = false)
    val rememberQueue by prefs.rememberQueue.collectAsState(initial = true)

    // 3. Equalizer & Audio DSP
    val equalizer by prefs.equalizerEnabled.collectAsState(initial = false)
    val eqPreset by prefs.eqPreset.collectAsState(initial = "Flat")
    val eqBands by prefs.eqBands.collectAsState(initial = listOf(0, 0, 0, 0, 0))
    val bassBoost by prefs.bassBoost.collectAsState(initial = false)
    val bassBoostStrength by prefs.bassBoostStrength.collectAsState(initial = 0)
    val virtualizer by prefs.virtualizerEnabled.collectAsState(initial = false)
    val virtualizerStrength by prefs.virtualizerStrength.collectAsState(initial = 0)
    val loudnessEnhancer by prefs.loudnessEnhancer.collectAsState(initial = false)
    val loudnessGainMb by prefs.loudnessGainMb.collectAsState(initial = 0)
    val balance by prefs.balance.collectAsState(initial = 0f)
    val mono by prefs.monoAudio.collectAsState(initial = false)
    val stereo by prefs.stereoWidening.collectAsState(initial = false)

    // 4. Volume & Output
    val appVolume by prefs.appVolume.collectAsState(initial = 1.0f)
    val rememberVol by prefs.rememberVolume.collectAsState(initial = true)
    val volumeLimit by prefs.volumeLimit.collectAsState(initial = 1.0f)

    // 5. Notification
    val mediaNotif by prefs.mediaNotification.collectAsState(initial = true)

    // 6. Widget Customizer (BOTTOM)
    val widgetTransparency by prefs.widgetTransparency.collectAsState(initial = 0.25f)
    val widgetTheme by prefs.widgetTheme.collectAsState(initial = "Dynamic")
    val widgetCustomColor by prefs.widgetCustomColor.collectAsState(initial = "#171717")
    val widgetShowLogo by prefs.widgetShowLogo.collectAsState(initial = true)
    val widgetCornerRadius by prefs.widgetCornerRadius.collectAsState(initial = 24)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "settings_title") {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_xvox_settings),
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    color = colors.primaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ==========================================
        // 1. THEMES & APPEARANCE (TOP)
        // ==========================================
        item(key = "appearance_section") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Themes & Appearance", iconRes = R.drawable.ic_xvox_settings) {
                    // App Theme
                    Text(
                        text = "App Theme: $theme",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("System", "Light", "Dark", "AMOLED").forEach { t ->
                            val isSel = theme == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) colors.primaryAccent else colors.cardElevated)
                                    .border(1.dp, if (isSel) colors.primaryAccent else colors.cardBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptics.tap()
                                        scope.launch { prefs.setTheme(t) }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t,
                                    color = if (isSel) colors.background else colors.primaryText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Refined Accent Colors: Default, XVOX Red, XVOX Blue
                    Text(
                        text = "Accent Color: $accentColor",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    val accentOptions = listOf(
                        "Default" to Color(0xFFF5F5F5),
                        "XVOX Red" to Color(0xFFFA2D48),
                        "XVOX Blue" to Color(0xFF007AFF)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        accentOptions.forEach { (name, colorVal) ->
                            val isSel = accentColor == name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.cardElevated)
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) colorVal else colors.cardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        haptics.tap()
                                        scope.launch { prefs.setAccentColor(name) }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(colorVal)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        color = if (isSel) colors.primaryText else colors.secondaryText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Font Size Scale
                    Text(
                        text = "Font Scale: ${fontSizeScale}x",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.85f to "Small", 1.0f to "Normal", 1.15f to "Large", 1.25f to "XL").forEach { (scale, label) ->
                            val isSel = fontSizeScale == scale
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) colors.primaryAccent else colors.cardElevated)
                                    .border(1.dp, if (isSel) colors.primaryAccent else colors.cardBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptics.tap()
                                        scope.launch { prefs.setFontSizeScale(scale) }
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) colors.background else colors.primaryText,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Haptic Feedback with Strength Selector
                    SettingsToggle("Haptic feedback", "Vibrate gently on every button tap and slider", haptic) {
                        haptics.toggle()
                        scope.launch { prefs.setHapticFeedback(it) }
                    }

                    if (haptic) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Haptic Strength: $hapticStrength",
                            color = colors.secondaryText,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Light", "Medium", "Strong").forEach { s ->
                                val isSel = hapticStrength.equals(s, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) colors.primaryAccent else colors.cardElevated)
                                        .border(1.dp, if (isSel) colors.primaryAccent else colors.cardBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptics.heavy()
                                        scope.launch { prefs.setHapticStrength(s) }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s,
                                    color = if (isSel) colors.background else colors.primaryText,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // ==========================================
        // 2. EQUALIZER & AUDIO DSP SECTION
        // ==========================================
        item(key = "equalizer_section") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Equalizer & DSP Engine", iconRes = R.drawable.ic_xvox_equalizer) {
                    SettingsToggle(
                        title = "Master Equalizer",
                        subtitle = "Real-time hardware sound processing",
                        checked = equalizer
                    ) {
                        haptics.toggle()
                        scope.launch { prefs.setEqualizerEnabled(it) }
                    }

                    AnimatedVisibility(
                        visible = equalizer,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Presets",
                                color = colors.secondaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf("Flat", "Bass Boost", "Treble", "Rock", "Pop", "Jazz", "Electronic", "Vocal", "Custom")
                                presets.forEach { p ->
                                    val isSelected = eqPreset == p
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                                            .clickable {
                                                haptics.tap()
                                                scope.launch {
                                                    prefs.setEqPreset(p)
                                                    if (p != "Custom" && AudioEffectsManager.PRESETS.containsKey(p)) {
                                                        val bandVals = AudioEffectsManager.PRESETS[p] ?: listOf(0, 0, 0, 0, 0)
                                                        prefs.setEqBands(bandVals)
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = p,
                                            color = if (isSelected) colors.background else colors.primaryText,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
                            Text(
                                text = "5-Band Equalizer (-15 dB to +15 dB)",
                                color = colors.primaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            bandLabels.forEachIndexed { index, label ->
                                val currentVal = eqBands.getOrElse(index) { 0 }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        color = colors.secondaryText,
                                        fontSize = 11.sp,
                                        modifier = Modifier.width(55.dp)
                                    )
                                    XvoxSlimSlider(
                                        value = currentVal.toFloat(),
                                        onValueChange = { newVal ->
                                            haptics.sliderTick()
                                            val updated = eqBands.toMutableList()
                                            while (updated.size <= index) updated.add(0)
                                            updated[index] = newVal.roundToInt()
                                            scope.launch {
                                                prefs.setEqBands(updated)
                                                prefs.setEqPreset("Custom")
                                            }
                                        },
                                        valueRange = -15f..15f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${if (currentVal > 0) "+" else ""}$currentVal dB",
                                        color = colors.primaryText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(44.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            SettingsToggle("Bass Boost", "Deep low-end enhancement", bassBoost) {
                                haptics.toggle()
                                scope.launch { prefs.setBassBoost(it) }
                            }
                            if (bassBoost) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Strength", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                                    XvoxSlimSlider(
                                        value = bassBoostStrength.toFloat(),
                                        onValueChange = {
                                            haptics.sliderTick()
                                            scope.launch { prefs.setBassBoostStrength(it.roundToInt()) }
                                        },
                                        valueRange = 0f..1000f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${(bassBoostStrength / 10)}%", color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                                }
                            }

                            SettingsToggle("Virtualizer / 3D Surround", "Stereo sound stage expander", virtualizer) {
                                haptics.toggle()
                                scope.launch { prefs.setVirtualizer(it) }
                            }
                            if (virtualizer) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Strength", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                                    XvoxSlimSlider(
                                        value = virtualizerStrength.toFloat(),
                                        onValueChange = {
                                            haptics.sliderTick()
                                            scope.launch { prefs.setVirtualizerStrength(it.roundToInt()) }
                                        },
                                        valueRange = 0f..1000f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${(virtualizerStrength / 10)}%", color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                                }
                            }

                            SettingsToggle("Loudness Enhancer", "Volume boost for quiet audio files", loudnessEnhancer) {
                                haptics.toggle()
                                scope.launch { prefs.setLoudnessEnhancer(it) }
                            }
                            if (loudnessEnhancer) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Gain", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                                    XvoxSlimSlider(
                                        value = loudnessGainMb.toFloat(),
                                        onValueChange = {
                                            haptics.sliderTick()
                                            scope.launch { prefs.setLoudnessGainMb(it.roundToInt()) }
                                        },
                                        valueRange = 0f..2000f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("+${(loudnessGainMb / 100)} dB", color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(44.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Stereo Balance L / R", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("L", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(18.dp))
                                XvoxSlimSlider(
                                    value = balance,
                                    onValueChange = {
                                        haptics.sliderTick()
                                        scope.launch { prefs.setBalance(it) }
                                    },
                                    valueRange = -1.0f..1.0f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("R", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. PLAYBACK SETTINGS SECTION
        // ==========================================
        item(key = "playback_section") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Playback", iconRes = R.drawable.ic_xvox_play) {
                    SettingsToggle("Gapless playback", "Seamless transition between tracks", gapless) {
                        haptics.toggle()
                        scope.launch { prefs.setGaplessPlayback(it) }
                    }

                    SettingsToggle("Crossfade", "iOS-level beat sync blend between tracks", crossfade) {
                        haptics.toggle()
                        scope.launch { prefs.setCrossfade(it) }
                    }
                    if (crossfade) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Duration", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                            XvoxSlimSlider(
                                value = crossfadeDuration.toFloat(),
                                onValueChange = {
                                    haptics.sliderTick()
                                    scope.launch { prefs.setCrossfadeDuration(it.roundToInt()) }
                                },
                                valueRange = 1f..12f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${crossfadeDuration}s", color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(32.dp))
                        }
                    }

                    SettingsToggle("Fade in", "Gradual volume rise on playback start", fadeIn) {
                        haptics.toggle()
                        scope.launch { prefs.setFadeIn(it) }
                    }

                    SettingsToggle("Fade out", "Gradual volume fall on pause/stop", fadeOut) {
                        haptics.toggle()
                        scope.launch { prefs.setFadeOut(it) }
                    }

                    SettingsToggle("ReplayGain", "Normalize loudness via metadata tags", replayGain) {
                        haptics.toggle()
                        scope.launch { prefs.setReplayGain(it) }
                    }

                    SettingsToggle("Loudness normalization", "EBU R128 standard volume leveling", loudnessNorm) {
                        haptics.toggle()
                        scope.launch { prefs.setLoudnessNormalization(it) }
                    }

                    SettingsToggle("Skip silence", "Trim leading and trailing silent gaps", skipSilence) {
                        haptics.toggle()
                        scope.launch { prefs.setSkipSilence(it) }
                    }

                    SettingsToggle("Audio focus", "React to phone calls, navigation & other apps", audioFocus) {
                        haptics.toggle()
                        scope.launch { prefs.setAudioFocus(it) }
                    }

                    SettingsToggle("Pause on headphone disconnect", "Auto pause when headset or Bluetooth disconnects", pauseOnDisconnect) {
                        haptics.toggle()
                        scope.launch { prefs.setPauseOnHeadphoneDisconnect(it) }
                    }

                    SettingsToggle("Play on headset connect", "Resume playback when headset or Bluetooth connects", playOnConnect) {
                        haptics.toggle()
                        scope.launch { prefs.setPlayOnHeadsetConnect(it) }
                    }

                    SettingsToggle("Clear queue after playback", "Empty queue when current list completes", clearQueue) {
                        haptics.toggle()
                        scope.launch { prefs.setClearQueueAfterPlayback(it) }
                    }

                    SettingsToggle("Remember queue", "Restore playing queue on next launch", rememberQueue) {
                        haptics.toggle()
                        scope.launch { prefs.setRememberQueue(it) }
                    }
                }
            }
        }

        // ==========================================
        // 4. VOLUME & OUTPUT SECTION
        // ==========================================
        item(key = "volume_section") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Volume & Output", iconRes = R.drawable.ic_xvox_volume_high) {
                    Text(
                        text = "In-App Stream Volume: ${(appVolume * 100).roundToInt()}%",
                        color = colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    XvoxSlimSlider(
                        value = appVolume,
                        onValueChange = {
                            haptics.sliderTick()
                            scope.launch { prefs.setAppVolume(it) }
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SettingsToggle("Remember Volume", "Restore volume level on next app start", rememberVol) {
                        haptics.toggle()
                        scope.launch { prefs.setRememberVolume(it) }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Volume Limiter Cap: ${(volumeLimit * 100).roundToInt()}%",
                        color = colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    XvoxSlimSlider(
                        value = volumeLimit,
                        onValueChange = {
                            haptics.sliderTick()
                            scope.launch { prefs.setVolumeLimit(it) }
                        },
                        valueRange = 0.5f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ==========================================
        // 5. NOTIFICATION SECTION
        // ==========================================
        item(key = "notification_section") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Notification", iconRes = R.drawable.ic_xvox_settings) {
                    SettingsToggle("Media notification", "Show foreground player notification with controls", mediaNotif) {
                        haptics.toggle()
                        scope.launch { prefs.setMediaNotification(it) }
                    }
                }
            }
        }

        // ==========================================
        // 6. HOME SCREEN WIDGET (BOTTOM)
        // ==========================================
        item(key = "widget_customizer") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Home Screen Widget Customizer", iconRes = R.drawable.ic_xvox_music_note) {
                    Text(
                        text = "Create and customize your home screen music widget. Supports all sizes (1x1, 2x2, 3x3, 4x4, 2x1, 3x1, 4x1, 5x1).",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Live Widget Preview Card
                    WidgetLivePreviewCard(
                        transparency = widgetTransparency,
                        theme = widgetTheme,
                        customColor = widgetCustomColor,
                        showLogo = widgetShowLogo,
                        cornerRadiusDp = widgetCornerRadius,
                        accentColorName = accentColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ONE-TAP ADD / PIN WIDGET TO HOME SCREEN BUTTON
                    Button(
                        onClick = {
                            haptics.success()
                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val myProvider = ComponentName(context, XvoxAppWidgetProvider::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                                val callbackIntent = Intent(context, XvoxAppWidgetProvider::class.java)
                                val callbackPendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    0,
                                    callbackIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                appWidgetManager.requestPinAppWidget(myProvider, null, callbackPendingIntent)
                                Toast.makeText(context, "Adding XVOX widget to home screen...", Toast.LENGTH_SHORT).show()
                            } else {
                                XvoxAppWidgetProvider.notifyWidgetUpdate(context)
                                Toast.makeText(context, "Add XVOX widget from your launcher widget picker", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.background
                        )
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_xvox_plus), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Add Widget to Home Screen", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Transparency Slider
                    Text(
                        text = "Widget Transparency: ${(widgetTransparency * 100).roundToInt()}%",
                        color = colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    XvoxSlimSlider(
                        value = widgetTransparency,
                        onValueChange = {
                            haptics.sliderTick()
                            scope.launch { prefs.setWidgetTransparency(it) }
                        },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Widget Theme Selector
                    Text(
                        text = "Widget Theme Style",
                        color = colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Dynamic", "AMOLED", "Dark", "Light", "Glass").forEach { t ->
                            val isSelected = widgetTheme == t
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) colors.primaryAccent else colors.cardElevated)
                                    .border(1.dp, if (isSelected) colors.primaryAccent else colors.cardBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptics.tap()
                                        scope.launch {
                                            prefs.setWidgetTheme(t)
                                            XvoxAppWidgetProvider.notifyWidgetUpdate(context)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t,
                                    color = if (isSelected) colors.background else colors.primaryText,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Corner Radius Slider
                    Text(
                        text = "Corner Radius: ${widgetCornerRadius}dp",
                        color = colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    XvoxSlimSlider(
                        value = widgetCornerRadius.toFloat(),
                        onValueChange = {
                            haptics.sliderTick()
                            scope.launch { prefs.setWidgetCornerRadius(it.toInt()) }
                        },
                        valueRange = 12f..36f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Show XVOX Logo Toggle
                    SettingsToggle(
                        title = "Show XVOX Logo (Cinzel Font)",
                        subtitle = "Display brand watermark on widget",
                        checked = widgetShowLogo
                    ) {
                        haptics.toggle()
                        scope.launch {
                            prefs.setWidgetShowLogo(it)
                            XvoxAppWidgetProvider.notifyWidgetUpdate(context)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Refresh Button
                    Button(
                        onClick = {
                            haptics.success()
                            XvoxAppWidgetProvider.notifyWidgetUpdate(context)
                            Toast.makeText(context, "Home Widgets Updated!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.cardElevated,
                            contentColor = colors.primaryText
                        )
                    ) {
                        Text(text = "Refresh Active Widgets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Bottom Branding
        item(key = "settings_brand") {
            Spacer(Modifier.height(16.dp))
            HomeFooter(modifier = Modifier.fillMaxWidth().height(180.dp).padding(bottom = 20.dp))
        }

        item(key = "settings_bottom_inset") { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun XvoxSlimSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier.height(26.dp),
        colors = SliderDefaults.colors(
            thumbColor = colors.primaryAccent,
            activeTrackColor = colors.primaryAccent,
            inactiveTrackColor = colors.cardBorder
        )
    )
}

@Composable
private fun WidgetLivePreviewCard(
    transparency: Float,
    theme: String,
    customColor: String,
    showLogo: Boolean,
    cornerRadiusDp: Int,
    accentColorName: String
) {
    val colors = XvoxTheme.colors

    val previewBgColor = when (theme) {
        "AMOLED" -> Color(0xFF000000)
        "Dark" -> Color(0xFF141414)
        "Light" -> Color(0xFFFAFAFA)
        "Glass" -> Color(0xFF1C1C22)
        "Custom" -> runCatching { Color(android.graphics.Color.parseColor(customColor)) }.getOrDefault(Color(0xFF171717))
        else -> colors.cardElevated
    }

    val alphaVal = (1.0f - transparency).coerceIn(0f, 1f)
    val effectiveBg = previewBgColor.copy(alpha = alphaVal)
    val isLight = theme == "Light" && transparency < 0.6f
    val textColor = if (isLight) Color(0xFF111111) else Color(0xFFFFFFFF)
    val subTextColor = if (isLight) Color(0xFF555555) else Color(0xFFA0A0A0)

    val artGradient = Brush.linearGradient(
        listOf(
            colors.primaryAccent.copy(alpha = 0.85f),
            colors.primaryAccent.copy(alpha = 0.45f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(effectiveBg)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(cornerRadiusDp.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(artGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_xvox_music_note),
                    contentDescription = null,
                    tint = colors.background,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (showLogo) {
                    Text(
                        text = "XVOX",
                        fontFamily = XvoxLogoFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = "Live Track Preview",
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "XVOX Sound Engine",
                    color = subTextColor,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_xvox_heart),
                    contentDescription = null,
                    tint = Color(0xFFFF453A),
                    modifier = Modifier.size(18.dp)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_xvox_skip_previous),
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_xvox_pause),
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_xvox_skip_next),
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    iconRes: Int? = null,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        content()
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = title,
                color = colors.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.secondaryText,
                    fontSize = 11.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.primaryAccent,
                uncheckedThumbColor = colors.secondaryText,
                uncheckedTrackColor = colors.cardElevated
            )
        )
    }
}
