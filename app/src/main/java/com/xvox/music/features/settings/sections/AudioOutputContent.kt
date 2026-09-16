package com.xvox.music.features.settings.sections

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
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

/**
 * Audio output routing:
 * - Auto: follows connected accessory or phone speaker dynamically.
 * - Phone: forces sound to phone speaker even when Bluetooth is connected.
 * - Headset: routes to Bluetooth / wired headphones; prompts to connect if none attached.
 */
@Composable
fun AudioOutputContent(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current
    val device = rememberHeadset(context)
    val currentName = device?.let(::describeDevice) ?: "Phone speaker"

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_bluetooth),
                contentDescription = null,
                tint = if (device != null) colors.primaryAccent else colors.mutedText,
                modifier = Modifier.size(20.dp)
            )
            Column(Modifier.weight(1f)) {
                Text("Now playing on", color = colors.secondaryText, fontSize = 11.sp)
                Text(currentName, color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        OutputChip("Auto", "Route according to connected audio device", state.audioOutputRoute == "auto") {
            haptics.tap()
            viewModel.setAudioOutputRoute("auto")
        }
        OutputChip("Phone", "Play from built-in phone speaker", state.audioOutputRoute == "phone") {
            haptics.tap()
            viewModel.setAudioOutputRoute("phone")
        }
        OutputChip("Headset", if (device != null) "Connected: $currentName" else "Bluetooth / wired headset", state.audioOutputRoute == "headset") {
            haptics.tap()
            if (device != null) {
                viewModel.setAudioOutputRoute("headset")
            } else {
                overlays.showP("Connect to Bluetooth first")
            }
        }

        Spacer(Modifier.height(2.dp))
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
private fun OutputChip(title: String, subtitle: String, active: Boolean, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (active) colors.primaryAccent.copy(alpha = 0.16f) else colors.cardElevated)
            .xvoxPressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(title, color = if (active) colors.primaryAccent else colors.primaryText,
            fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
        Text(subtitle, color = colors.secondaryText, fontSize = 10.sp)
    }
}

/** The currently connected headset/BT output, or null when only the phone speaker is available. */
@Composable
private fun rememberHeadset(context: Context): AudioDeviceInfo? = remember(context) {
    val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { isHeadsetOutput(it) }
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
