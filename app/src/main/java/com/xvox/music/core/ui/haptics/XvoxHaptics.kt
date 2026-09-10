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
                    "very_low" -> 5L to 25
                    "low" -> 8L to 60
                    "high" -> 20L to 255
                    else -> 12L to 120 // medium
                }
                if (vibrator?.hasAmplitudeControl() == true) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(ms, amp))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(ms)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10L)
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
                    "very_low" -> 14L to 55
                    "low" -> 22L to 110
                    "high" -> 45L to 255
                    else -> 30L to 180 // medium
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
