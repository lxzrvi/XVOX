package com.xvox.music.core.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

enum class HapticStrength {
    OFF,
    LIGHT,
    MEDIUM,
    STRONG
}

class XvoxHaptics(
    private val context: Context,
    var enabled: Boolean = true,
    var strength: HapticStrength = HapticStrength.MEDIUM
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun tap() {
        if (!enabled || strength == HapticStrength.OFF) return
        vibrate(getDuration(12L), getAmplitude(40, 85, 160))
    }

    fun click() {
        if (!enabled || strength == HapticStrength.OFF) return
        vibrate(getDuration(18L), getAmplitude(60, 120, 200))
    }

    fun toggle() {
        if (!enabled || strength == HapticStrength.OFF) return
        vibrate(getDuration(22L), getAmplitude(70, 140, 220))
    }

    fun sliderTick() {
        if (!enabled || strength == HapticStrength.OFF) return
        vibrate(getDuration(8L), getAmplitude(25, 50, 100))
    }

    fun heavy() {
        if (!enabled || strength == HapticStrength.OFF) return
        vibrate(getDuration(35L), getAmplitude(100, 180, 255))
    }

    fun success() {
        if (!enabled || strength == HapticStrength.OFF) return
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vib.hasAmplitudeControl()) {
            val timings = longArrayOf(0, 18, 50, 22)
            val amp = getAmplitude(60, 120, 220)
            val amplitudes = intArrayOf(0, amp, 0, (amp * 1.2f).toInt().coerceAtMost(255))
            runCatching {
                vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
        } else {
            vibrate(30L, 120)
        }
    }

    private fun getDuration(base: Long): Long = when (strength) {
        HapticStrength.LIGHT -> (base * 0.75f).toLong().coerceAtLeast(6L)
        HapticStrength.MEDIUM -> base
        HapticStrength.STRONG -> (base * 1.5f).toLong()
        HapticStrength.OFF -> 0L
    }

    private fun getAmplitude(light: Int, medium: Int, strong: Int): Int = when (strength) {
        HapticStrength.LIGHT -> light
        HapticStrength.MEDIUM -> medium
        HapticStrength.STRONG -> strong
        HapticStrength.OFF -> 0
    }

    private fun vibrate(duration: Long, amplitude: Int) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = if (vib.hasAmplitudeControl()) amplitude.coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE
                vib.vibrate(VibrationEffect.createOneShot(duration, amp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(duration)
            }
        }
    }
}

val LocalXvoxHaptics: ProvidableCompositionLocal<XvoxHaptics> = staticCompositionLocalOf {
    error("No XvoxHaptics provided")
}

@Composable
fun rememberXvoxHaptics(
    enabled: Boolean = true,
    strength: String = "Medium"
): XvoxHaptics {
    val context = LocalContext.current.applicationContext
    val parsedStrength = when (strength.lowercase()) {
        "off" -> HapticStrength.OFF
        "light" -> HapticStrength.LIGHT
        "strong" -> HapticStrength.STRONG
        else -> HapticStrength.MEDIUM
    }
    return remember(enabled, parsedStrength) {
        XvoxHaptics(context, enabled, parsedStrength)
    }
}
