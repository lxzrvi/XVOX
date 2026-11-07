// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/blur/XvoxGpuGuard.kt
package com.xvox.music.core.ui.blur

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Crash-loop guard for GPU effects. A native segfault in the render pipeline cannot be caught,
 * so we detect it on the NEXT launch: the activity marks "foreground" in onStart and clears it in
 * onStop. If the flag is still set when a new process starts, the previous run died on screen,
 * so live blur / AGSL / RenderEffect are switched off (safe mode) until the user re-enables them.
 */
object XvoxGpuGuard {
    private const val PREFS = "xvox_gpu_guard"
    private const val KEY_FOREGROUND = "foreground"
    private const val KEY_SAFE = "safe"

    var safeMode by mutableStateOf(false)
        private set

    private var checked = false

    fun onProcessStart(context: Context) {
        if (checked) return
        checked = true
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getBoolean(KEY_FOREGROUND, false)) p.edit().putBoolean(KEY_SAFE, true).apply()
        safeMode = p.getBoolean(KEY_SAFE, false)
    }

    fun markForeground(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FOREGROUND, true).apply()
    }

    fun markBackground(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FOREGROUND, false).apply()
    }

    fun clearSafeMode(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SAFE, false).apply()
        safeMode = false
    }
}
