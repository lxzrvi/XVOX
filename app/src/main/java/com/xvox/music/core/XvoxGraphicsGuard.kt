package com.xvox.music.core

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * Graphics safety switch.
 *
 * The stack overflow seen on device lands inside `libhwui` (`RenderProperties::promotedToLayer`),
 * which means the render tree asked for more nested layers and render effects than the driver's
 * stack could carry. Once that happens the safest answer is to stop building those layers: the app
 * then still renders, just without the capture-and-blur stack.
 *
 * The flag lives in SharedPreferences, and it is also exposed as Compose state so switching it in
 * Settings takes effect on the next frame instead of on the next launch.
 */
object XvoxGraphicsGuard {

    private const val PREFS = "xvox_graphics"
    private const val KEY_SAFE = "safe_mode"

    private var store: android.content.SharedPreferences? = null

    /** True when the capture/blur stack must be skipped. */
    val safe = mutableStateOf(false)

    fun install(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        store = prefs
        // Also honour the existing GPU guard so one switch can cover both.
        safe.value = prefs.getBoolean(KEY_SAFE, false) ||
            com.xvox.music.core.ui.blur.XvoxGpuGuard.safeMode
    }

    fun set(context: Context, value: Boolean) {
        val prefs = store ?: context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SAFE, value).apply()
        safe.value = value
    }

    /** Called from the crash recorder when the trace points at the renderer. */
    fun markAfterCrash(context: Context, trace: String) {
        val rendererCrash = trace.contains("libhwui", ignoreCase = true) ||
            trace.contains("promotedToLayer", ignoreCase = true) ||
            trace.contains("SIGSEGV", ignoreCase = true)
        if (rendererCrash) set(context, true)
    }
}
