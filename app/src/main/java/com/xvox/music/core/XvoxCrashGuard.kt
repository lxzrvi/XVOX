package com.xvox.music.core

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Last-resort crash recorder.
 *
 * When the process dies without a visible dialog there is nothing to report, so the default
 * uncaught handler is wrapped: the throwable, the thread and the device are written to a small file
 * before the process actually terminates. The next launch reads that file and shows it on screen,
 * so a silent close always leaves a readable trace behind.
 */
object XvoxCrashGuard {

    private const val FILE_NAME = "xvox-last-crash.txt"
    private const val TAG = "XVOX_CRASH"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                Log.e(TAG, "uncaught on ${thread.name}", error)
                val text = buildString {
                    appendLine("thread: ${thread.name}")
                    appendLine("device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} / API ${android.os.Build.VERSION.SDK_INT}")
                    appendLine(Log.getStackTraceString(error))
                }
                File(appContext.filesDir, FILE_NAME).writeText(text)
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return runCatching { file.readText().takeIf { it.isNotBlank() } }.getOrNull()
    }

    fun clear(context: Context) {
        runCatching { File(context.applicationContext.filesDir, FILE_NAME).delete() }
    }
}
