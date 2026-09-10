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
    var strength: String = "Normal"
) {
    private val vibrator: Vibrator? by lazy {
        context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun tap() {
        if (!enabled) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10L)
            }
        }
    }

    fun click() {
        if (!enabled) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(16L)
            }
        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35L)
            }
        }
    }

    fun success() {
        if (!enabled) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20L)
            }
        }
    }
}

val LocalXvoxHaptics: ProvidableCompositionLocal<XvoxHaptics> = staticCompositionLocalOf {
    XvoxHaptics()
}

@Composable
fun rememberXvoxHaptics(
    enabled: Boolean = true,
    strength: String = "Normal"
): XvoxHaptics {
    val context = LocalContext.current.applicationContext
    return remember(context, enabled, strength) {
        XvoxHaptics(context = context, enabled = enabled, strength = strength)
    }
}
