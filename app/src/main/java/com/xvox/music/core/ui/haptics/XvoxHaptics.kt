package com.xvox.music.core.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

class XvoxHaptics(
    private val context: Context? = null,
    var enabled: Boolean = true,
    var strength: String = "medium"
) {
    private val vibrator: Vibrator? by lazy {
        context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun tap() {
        if (!enabled) return
        runCatching {
            val level = strength.lowercase()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val (ms, amp) = when (level) {
                    "very_low" -> 6L to 30 // Soft
                    "low" -> 12L to 85 // Medium
                    "high" -> 28L to 255 // Sharp
                    else -> 18L to 160 // Strong (default)
                }
                if (vibrator?.hasAmplitudeControl() == true) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(ms, amp))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = when (level) {
                        "very_low" -> VibrationEffect.EFFECT_TICK
                        "low" -> VibrationEffect.EFFECT_CLICK
                        "high" -> VibrationEffect.EFFECT_HEAVY_CLICK
                        else -> VibrationEffect.EFFECT_CLICK
                    }
                    vibrator?.vibrate(VibrationEffect.createPredefined(effect))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(ms)
                }
            } else {
                @Suppress("DEPRECATION")
                val ms = when (level) {
                    "very_low" -> 5L
                    "low" -> 10L
                    "high" -> 25L
                    else -> 15L
                }
                vibrator?.vibrate(ms)
            }
        }
    }

    fun click() {
        tap()
    }

    fun toggle() {
        tap()
    }

    fun sliderTick() {
        tap()
    }

    fun heavy() {
        if (!enabled) return
        runCatching {
            val level = strength.lowercase()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val (ms, amp) = when (level) {
                    "very_low" -> 16L to 60
                    "low" -> 26L to 130
                    "high" -> 50L to 255
                    else -> 36L to 200
                }
                if (vibrator?.hasAmplitudeControl() == true) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(ms, amp))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(ms)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35L)
            }
        }
    }

    fun success() {
        tap()
    }
}

val LocalXvoxHaptics: ProvidableCompositionLocal<XvoxHaptics> = staticCompositionLocalOf {
    XvoxHaptics()
}

@Composable
fun rememberXvoxHaptics(
    enabled: Boolean = true,
    strength: String = "medium"
): XvoxHaptics {
    val context = LocalContext.current.applicationContext
    return remember(context, enabled, strength) {
        XvoxHaptics(context = context, enabled = enabled, strength = strength)
    }
}
