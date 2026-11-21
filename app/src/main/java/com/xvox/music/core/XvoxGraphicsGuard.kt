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
        val saved = prefs.getBoolean(KEY_SAFE, false)
        val native = nativeCrashDetected(context)
        if (native && !saved) {
            prefs.edit().putBoolean(KEY_SAFE, true).apply()
        }
        // DIAGNOSTIC BUILD: safe graphics is forced on from the very first frame, so the answer is
        // unambiguous - if the app still closes, the renderer layer stack is not the reason.
        safe.value = true
    }

    /**
     * A SIGSEGV never reaches a Java handler, so the crash recorder cannot see it. The platform
     * exit history can: if the previous process died from a native signal, the layer stack is
     * armed off before the first frame of this launch.
     *
     * The call is reflective on purpose: it keeps the guard working whatever the compile SDK
     * exposes, and a missing API simply means "no evidence of a native crash".
     */
    private fun nativeCrashDetected(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            ?: return false
        val method = manager.javaClass.methods.firstOrNull { it.name == "getHistoricalProcessExitInfos" }
            ?: return false
        val raw = method.invoke(manager) ?: return false
        val items: List<Any?> = when (raw) {
            is List<*> -> raw
            else -> runCatching {
                val orElse = raw.javaClass.methods.firstOrNull { it.name == "orElse" } ?: return false
                orElse.invoke(raw, emptyList<Any?>()) as? List<Any?>
            }.getOrNull() ?: return false
        }
        items.filterNotNull().take(4).any { info ->
            val reason = runCatching {
                info.javaClass.getMethod("getReason").invoke(info) as? Int
            }.getOrNull() ?: 0
            // ApplicationExitInfo.REASON_CRASH_NATIVE = 5, REASON_SIGNALED = 12.
            if (reason != 5 && reason != 12) return@any false
            val description = runCatching {
                info.javaClass.getMethod("getDescription").invoke(info) as? String
            }.getOrNull().orEmpty()
            val trace = runCatching {
                (info.javaClass.getMethod("getTraceInputStream").invoke(info) as? java.io.InputStream)
                    ?.bufferedReader()?.use { it.readText() }
            }.getOrNull().orEmpty()
            description.contains("SIGSEGV", true) || description.contains("SIGSYS", true) ||
                trace.contains("SIGSEGV", true) || trace.contains("libhwui", true) ||
                trace.contains("promotedToLayer", true)
        }
    }.getOrDefault(false)

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
