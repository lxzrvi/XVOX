package com.xvox.music.player.session

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/** Detect actual output routes (A2DP, BLE, wired and USB), not arbitrary Bluetooth device connections. */
class XvoxAudioRouteObserver(context: Context, private val onConnected: () -> Unit, private val onDisconnected: () -> Unit) {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var known = emptySet<Int>()
    private var registered = false
    private fun isHeadset(device: AudioDeviceInfo): Boolean {
        if (!device.isSink) return false
        return when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> true
            else -> Build.VERSION.SDK_INT >= 31 &&
                (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET || device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
        }
    }
    fun hasHeadphoneOutput(): Boolean = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any(::isHeadset)
    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            val added = addedDevices.filter(::isHeadset).map { it.id }.toSet()
            val newlyConnected = added - known
            known = known + added
            if (newlyConnected.isNotEmpty()) onConnected()
        }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            val removed = removedDevices.map { it.id }.toSet()
            val wasHeadset = removed.any { it in known }
            known = known - removed
            if (wasHeadset) onDisconnected()
        }
    }
    fun start() {
        if (registered) return
        // Registration reports existing devices too; they are not new connection events.
        known = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter(::isHeadset).map { it.id }.toSet()
        manager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        registered = true
    }
    fun stop() {
        if (registered) manager.unregisterAudioDeviceCallback(callback)
        registered = false; known = emptySet()
    }
}
