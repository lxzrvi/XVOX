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
                // The normal app response is deliberately a restrained medium pulse. It replaces
                // the previous strong default so ordinary taps, toggles, and slider ticks remain
                // clear without making the whole interface feel buzzy.
                val (ms, amp) = when (level) {
                    "very_low" -> 6L to 24
                    "low" -> 10L to 64
                    "high" -> 18L to 150
                    else -> 14L to 110 // Medium default
                }
                if (vibrator?.hasAmplitudeControl() == true) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(ms, amp))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = when (level) {
                        "very_low", "low" -> VibrationEffect.EFFECT_TICK
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
                    "low" -> 8L
                    "high" -> 16L
                    else -> 12L // Medium default
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

    /**
     * Keeps existing important-action call sites compatible without reintroducing the old heavy
     * pulse. The app now uses one medium haptic policy everywhere it provides feedback.
     */
    fun heavy() = tap()

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
