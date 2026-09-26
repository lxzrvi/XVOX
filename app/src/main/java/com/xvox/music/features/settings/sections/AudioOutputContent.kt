package com.xvox.music.features.settings.sections

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel

/** Direct-persistence entry point retained for ordinary Settings screens. */
@Composable
fun AudioOutputContent(state: SettingsState, viewModel: SettingsViewModel) {
    AudioOutputControls(state = state, onRouteChange = viewModel::setAudioOutputRoute)
}

/** State-hoisted route picker used by the live Now Playing Bluetooth sheet. */
@Composable
fun AudioOutputDraftContent(state: SettingsState, onStateChange: (SettingsState) -> Unit) {
    AudioOutputControls(
        state = state,
        onRouteChange = { route -> onStateChange(state.copy(audioOutputRoute = route)) }
    )
}

/**
 * Audio output routing:
 * - Auto follows the platform route.
 * - Phone forces the built-in speaker.
 * - Headset requests the currently connected wired/Bluetooth headset.
 *
 * The Bluetooth glyph is intentionally accent coloured only when AudioManager reports a real
 * Bluetooth output route, not merely because a remembered device happens to be paired.
 */
@Composable
private fun AudioOutputControls(state: SettingsState, onRouteChange: (String) -> Unit) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current
    val outputs = rememberAudioOutputs(context)
    val connectedHeadset = outputs.connectedHeadset
    val actualBluetooth = outputs.actualBluetooth
    val currentName = actualBluetooth?.let(::describeDevice)
        ?: connectedHeadset?.let(::describeDevice)
        ?: "Phone speaker"

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_bluetooth),
                contentDescription = null,
                // A connected BT device alone is not enough: accent signals active BT routing.
                tint = if (actualBluetooth != null) colors.primaryAccent else colors.mutedText,
                modifier = Modifier.size(20.dp)
            )
            Column(Modifier.weight(1f)) {
                Text("Now playing on", color = colors.secondaryText, fontSize = 11.sp)
                Text(currentName, color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // The three routing choices share one compact, equal-width row rather than three cards.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            OutputRouteSegment("Auto", state.audioOutputRoute == "auto", Modifier.weight(1f)) {
                haptics.tap()
                onRouteChange("auto")
            }
            OutputRouteSegment("Phone", state.audioOutputRoute == "phone", Modifier.weight(1f)) {
                haptics.tap()
                onRouteChange("phone")
            }
            OutputRouteSegment("Headset", state.audioOutputRoute == "headset", Modifier.weight(1f)) {
                haptics.tap()
                if (connectedHeadset != null) {
                    onRouteChange("headset")
                } else {
                    overlays.showP("Connect a headset first")
                }
            }
        }

        Text(
            when (state.audioOutputRoute) {
                "phone" -> "Playing through the built-in speaker"
                "headset" -> if (connectedHeadset != null) "Requested: ${describeDevice(connectedHeadset)}" else "No headset connected"
                else -> "Automatically follows the system audio route"
            },
            color = colors.secondaryText,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(1.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(colors.cardElevated)
                .xvoxPressScale {
                    haptics.tap()
                    runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Open Bluetooth settings", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun OutputRouteSegment(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(if (selected) colors.primaryAccent else colors.cardElevated)
            .border(.8.dp, if (selected) colors.primaryAccent else colors.cardBorder.copy(alpha = .65f), shape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) colors.background else colors.primaryText,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

private data class XvoxAudioOutputs(
    val connectedHeadset: AudioDeviceInfo?,
    val actualBluetooth: AudioDeviceInfo?
)

@Composable
private fun rememberAudioOutputs(context: Context): XvoxAudioOutputs {
    val manager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var devices by remember(manager) {
        mutableStateOf(manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList())
    }
    // Route selection can change without a device connect/disconnect event on some devices.
    // This sheet is short-lived, so a small refresh cadence keeps the glyph truthfully route-based.
    LaunchedEffect(manager) {
        while (true) {
            devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
            kotlinx.coroutines.delay(750)
        }
    }
    DisposableEffect(manager) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
            }
        }
        manager.registerAudioDeviceCallback(callback, null)
        onDispose { manager.unregisterAudioDeviceCallback(callback) }
    }

    val headset = devices.firstOrNull(::isHeadsetOutput)
    // These AudioManager properties reflect the selected Bluetooth playback path. Device presence
    // is only used to attach a friendly name once the route is actually on.
    @Suppress("DEPRECATION")
    val bluetoothRouted = manager.isBluetoothA2dpOn || manager.isBluetoothScoOn
    val bluetooth = devices.firstOrNull(::isBluetoothOutput).takeIf { bluetoothRouted }
    return XvoxAudioOutputs(connectedHeadset = headset, actualBluetooth = bluetooth)
}

private fun isHeadsetOutput(device: AudioDeviceInfo): Boolean {
    if (!device.isSink) return false
    return when (device.type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> true
        else -> Build.VERSION.SDK_INT >= 31 &&
            (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET || device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
    }
}

private fun isBluetoothOutput(device: AudioDeviceInfo): Boolean = when (device.type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true
    else -> Build.VERSION.SDK_INT >= 31 &&
        (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET || device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
}

private fun describeDevice(device: AudioDeviceInfo): String = when (device.type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
        if (Build.VERSION.SDK_INT >= 28) device.productName?.toString()?.ifBlank { "Bluetooth headset" }
            ?: "Bluetooth headset"
        else "Bluetooth headset"
    }
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
    AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB headset"
    else -> {
        if (Build.VERSION.SDK_INT >= 31) device.productName?.toString()?.ifBlank { "Wireless device" }
            ?: "Wireless device"
        else "Wireless device"
    }
}
