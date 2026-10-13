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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        // Bluetooth status and headset routing are independent, boxed controls.  Keeping each
        // action inside its own half-width section prevents the old free-floating settings row
        // from reading as detached from the device it affects.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioOutputSideBox(
                title = "Bluetooth",
                detail = actualBluetooth?.let(::describeDevice) ?: "Not connected",
                active = actualBluetooth != null,
                actionLabel = "Open settings",
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.tap()
                    runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                }
            )
            AudioOutputSideBox(
                title = "Headset",
                detail = connectedHeadset?.let(::describeDevice) ?: "No headset connected",
                active = state.audioOutputRoute == "headset",
                actionLabel = if (connectedHeadset != null) "Use headset" else "Connect one",
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.tap()
                    if (connectedHeadset != null) onRouteChange("headset")
                    else overlays.showP("Connect a headset first")
                }
            )
        }

        // Automatic and phone routes are compact alternatives; headset selection remains in its
        // own contained section above so connected-device controls never spill across the sheet.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutputRouteSegment("Auto", state.audioOutputRoute == "auto", Modifier.weight(1f)) {
                haptics.tap()
                onRouteChange("auto")
            }
            OutputRouteSegment("Phone", state.audioOutputRoute == "phone", Modifier.weight(1f)) {
                haptics.tap()
                onRouteChange("phone")
            }
        }

        Text(
            when (state.audioOutputRoute) {
                "phone" -> "Playing through the built-in speaker"
                "headset" -> if (connectedHeadset != null) "Requested: ${describeDevice(connectedHeadset)}" else "No headset connected"
                else -> "Now playing on $currentName · follows the system route"
            },
            color = colors.secondaryText,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun AudioOutputSideBox(
    title: String,
    detail: String,
    active: Boolean,
    actionLabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .height(112.dp)
            .clip(shape)
            .background(if (active) colors.accentSoft else colors.cardElevated)
            .border(
                width = if (active) 1.dp else .8.dp,
                color = if (active) colors.primaryAccent else colors.cardBorder.copy(alpha = .72f),
                shape = shape
            )
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = if (active) colors.primaryAccent else colors.primaryText,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = detail,
            color = colors.secondaryText,
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (active) colors.primaryAccent.copy(alpha = .18f) else colors.card.copy(alpha = .72f))
                .xvoxPressScale(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = actionLabel,
                color = colors.primaryAccent,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
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
