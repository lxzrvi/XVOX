package com.xvox.music.features.settings

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.home.HomeGreeting
import com.xvox.music.features.home.HomeProfileAvatar
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.HomeFooter
import kotlinx.coroutines.launch

@Composable
private fun SettingsHeaderNoPill(
    homeViewModel: HomeViewModel
) {
    val homeState by homeViewModel.state.collectAsState()
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .height(54.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeProfileAvatar(
            profile = homeState.profile,
            modifier = Modifier.size(42.dp),
            onClick = {}
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = homeState.profile.username,
                color = colors.primaryText,
                fontFamily = com.xvox.music.core.design.theme.XvoxPersonalFont,
                fontSize = 18.sp,
                lineHeight = 19.sp,
                maxLines = 1
            )
            HomeGreeting()
        }
        // No pill – 7/8
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    val gapless by prefs.gaplessPlayback.collectAsState(initial = true)
    val crossfade by prefs.crossfade.collectAsState(initial = false)
    val fadeIn by prefs.fadeIn.collectAsState(initial = false)
    val fadeOut by prefs.fadeOut.collectAsState(initial = false)
    val replayGain by prefs.replayGain.collectAsState(initial = false)
    val loudnessNorm by prefs.loudnessNormalization.collectAsState(initial = false)
    val skipSilence by prefs.skipSilence.collectAsState(initial = false)
    val audioFocus by prefs.audioFocus.collectAsState(initial = true)
    val pauseOnDisconnect by prefs.pauseOnHeadphoneDisconnect.collectAsState(initial = true)
    val clearQueue by prefs.clearQueueAfterPlayback.collectAsState(initial = false)
    val rememberQueue by prefs.rememberQueue.collectAsState(initial = true)
    val playOnConnect by prefs.playOnHeadsetConnect.collectAsState(initial = false)
    val equalizer by prefs.equalizerEnabled.collectAsState(initial = false)
    val bassBoost by prefs.bassBoost.collectAsState(initial = false)
    val virtualizer by prefs.virtualizerEnabled.collectAsState(initial = false)
    val mono by prefs.monoAudio.collectAsState(initial = false)
    val stereo by prefs.stereoWidening.collectAsState(initial = false)
    val mediaNotif by prefs.mediaNotification.collectAsState(initial = true)
    val rememberVol by prefs.rememberVolume.collectAsState(initial = true)
    val theme by prefs.theme.collectAsState(initial = "System")
    val haptic by prefs.hapticFeedback.collectAsState(initial = true)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 8 – Settings header Home-style language without pill
        item(key = "settings_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background.copy(alpha = 0.78f))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(bottom = 8.dp)
            ) {
                SettingsHeaderNoPill(homeViewModel = homeViewModel)
            }
        }

        item(key = "settings_title") {
            Text(text = "Settings", color = colors.primaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
        }

        item(key = "playback") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Playback") {
                    SettingsToggle("Gapless playback", "Seamless transition between tracks", gapless) { scope.launch { prefs.setGaplessPlayback(it) } }
                    SettingsToggle("Crossfade", "Blend end/start of tracks (Media3 crossfade)", crossfade) { scope.launch { prefs.setCrossfade(it) } }
                    SettingsToggle("Fade in", "Gradual volume rise on play", fadeIn) { scope.launch { prefs.setFadeIn(it) } }
                    SettingsToggle("Fade out", "Gradual volume fall on pause/stop", fadeOut) { scope.launch { prefs.setFadeOut(it) } }
                    SettingsToggle("ReplayGain", "Normalize loudness via metadata", replayGain) { scope.launch { prefs.setReplayGain(it) } }
                    SettingsToggle("Loudness normalization", "EBU R128 style normalization", loudnessNorm) { scope.launch { prefs.setLoudnessNormalization(it) } }
                    SettingsToggle("Skip silence", "Trim silent gaps (ExoPlayer skipSilence)", skipSilence) {
                        scope.launch { prefs.setSkipSilence(it) }
                    }
                    SettingsToggle("Audio focus", "React to calls/other apps", audioFocus) { scope.launch { prefs.setAudioFocus(it) } }
                    SettingsToggle("Pause on headphone disconnect", "Auto pause when headset removed", pauseOnDisconnect) { scope.launch { prefs.setPauseOnHeadphoneDisconnect(it) } }
                    SettingsToggle("Play on headset connect", "Resume when headset plugged", playOnConnect) { scope.launch { prefs.setPlayOnHeadsetConnect(it) } }
                    SettingsToggle("Clear queue after playback", "Empty queue when completed", clearQueue) { scope.launch { prefs.setClearQueueAfterPlayback(it) } }
                    SettingsToggle("Remember queue", "Restore queue next launch", rememberQueue) { scope.launch { prefs.setRememberQueue(it) } }
                }
            }
        }

        item(key = "eq") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Equalizer / Audio") {
                    SettingsToggle("Equalizer", "Master enable (DSP pipeline)", equalizer) { scope.launch { prefs.setEqualizerEnabled(it) } }
                    SettingsInfo("Presets", "Flat / Bass Boost / Treble / Vocal / Custom — loads bands + preamp, persists")
                    SettingsToggle("Bass boost", "Low frequency enhancement", bassBoost) { scope.launch { prefs.setBassBoost(it) } }
                    SettingsToggle("Virtualizer", "Stereo widening effect", virtualizer) { scope.launch { prefs.setVirtualizer(it) } }
                    SettingsToggle("Mono audio", "Downmix to mono", mono) { scope.launch { prefs.setMonoAudio(it) } }
                    SettingsToggle("Stereo widening", "Expand stereo image", stereo) { scope.launch { prefs.setStereoWidening(it) } }
                    SettingsInfo("Bands & Preamp", "5-10 band EQ interconnected, custom persists after reload")
                    SettingsInfo("Loudness / Compressor / Limiter", "Dynamics processors in PCM pipeline")
                    SettingsInfo("Balance L/R", "Left-right panning control")
                    SettingsInfo("Volume normalization", "Per-track gain compensation")
                }
            }
        }

        item(key = "bt") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Headphones / Bluetooth") {
                    SettingsToggle("Play on connect", "Auto play when BT/headset connects", playOnConnect) { scope.launch { prefs.setPlayOnHeadsetConnect(it) } }
                    SettingsToggle("Pause on disconnect", "Synced pause on remove", pauseOnDisconnect) { scope.launch { prefs.setPauseOnHeadphoneDisconnect(it) } }
                    SettingsInfo("Bluetooth", "Uses BluetoothHeadset & AudioManager callbacks, synchronized with playback")
                }
            }
        }

        item(key = "notif") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Notification") {
                    SettingsToggle("Media notification", "Foreground MediaSession notification", mediaNotif) { scope.launch { prefs.setMediaNotification(it) } }
                    SettingsInfo("MediaSession", "Connected to PlaybackController, respects foreground restrictions")
                }
            }
        }

        item(key = "volume") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Volume") {
                    SettingsInfo("App volume", "In-app stream volume, 0-100%")
                    SettingsToggle("Remember volume", "Restore level next launch", rememberVol) { }
                    SettingsInfo("Volume limit", "Cap max output (e.g. 85%) — enforced on every set")
                }
            }
        }

        item(key = "output") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
                val devices = remember {
                    try {
                        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.toReadableName() }.distinct()
                    } catch (_: Exception) { listOf("Phone") }
                }
                SettingsSection(title = "Output") {
                    SettingsInfo("Available routes", devices.joinToString(", ").ifEmpty { "Phone" })
                    SettingsInfo("Selection", "Phone speaker / Wired headphones / Bluetooth / USB audio — via AudioManager, no fake routes")
                    devices.forEach { name ->
                        SettingsInfo("• $name", "Detected via AudioManager.getDevices()", small = true)
                    }
                }
            }
        }

        item(key = "custom") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Customizations") {
                    Text(text = "Theme: $theme", color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        listOf("System", "Light", "Dark", "AMOLED").forEach { t ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (theme == t) colors.primaryText else colors.card)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { scope.launch { prefs.setTheme(t) } }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = t, color = if (theme == t) colors.background else colors.primaryText, fontSize = 11.sp)
                            }
                        }
                    }
                    SettingsInfo("Accent color", "Global tint applied instantly")
                    SettingsInfo("Mini / Full player layout", "Compact / Expanded")
                    SettingsInfo("Home layout", "Grid / List")
                    SettingsInfo("Font size", "Scalable 0.85x - 1.25x")
                    SettingsToggle("Haptic feedback", "Vibration on interactions", haptic) { scope.launch { prefs.setHapticFeedback(it) } }
                }
            }
        }

        item(key = "widgets") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Widgets") {
                    SettingsInfo("Now Playing / Controls / Album Art / Like / Progress / Queue / Recently Played / Quick EQ", "Each widget bound to real playback state")
                    SettingsInfo("Connection", "Widgets observe PlaybackController flow, update instantly")
                }
            }
        }

        item(key = "dsp") {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                SettingsSection(title = "Custom DSP Engine") {
                    SettingsInfo("Pipeline", "Media3 → PCM → XVOX DSP (EQ/Bass/Virtualizer/Compressor/Limiter/Balance) → Oboe → Output")
                    SettingsInfo("Status", "Real PCM handoff, not label — AudioProcessor chain processes buffers before render")
                }
            }
        }

        // 1 – bottom brand space like Home
        item(key = "settings_brand") {
            Spacer(Modifier.height(24.dp))
            HomeFooter(modifier = Modifier.fillMaxWidth().height(220.dp).padding(bottom = 20.dp))
        }

        item(key = "settings_bottom_inset") { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val colors = XvoxTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(12.dp)
    ) {
        Text(text = title, color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

@Composable
private fun SettingsToggle(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = title, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(text = subtitle, color = colors.secondaryText, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.primaryText,
                uncheckedThumbColor = colors.secondaryText,
                uncheckedTrackColor = colors.card
            )
        )
    }
}

@Composable
private fun SettingsInfo(title: String, subtitle: String, small: Boolean = false) {
    val colors = XvoxTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = title, color = colors.primaryText, fontSize = if (small) 11.sp else 12.sp, fontWeight = if (small) FontWeight.Normal else FontWeight.Medium)
        Text(text = subtitle, color = colors.secondaryText, fontSize = if (small) 10.sp else 11.sp)
    }
}

private fun AudioDeviceInfo.toReadableName(): String = when (type) {
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker"
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headphones"
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth"
    AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB audio"
    else -> "Output $type"
}
