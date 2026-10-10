package com.xvox.music.core.diagnostics

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Writes an actionable crash report before Android terminates the process.
 *
 * Modern Android versions receive a public Downloads/xvoxcrash.txt entry via MediaStore, so the
 * user can open it immediately from any file manager without granting broad storage access. Older
 * devices use their public Downloads directory when available and retain an app-external fallback
 * only if platform storage access is denied during a fatal shutdown.
 */
object XvoxCrashReporter {
    private const val ReportName = "xvoxcrash.txt"
    private val handlingCrash = AtomicBoolean(false)

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (handlingCrash.compareAndSet(false, true)) {
                runCatching { writeReport(appContext, thread, throwable) }
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    private fun writeReport(context: Context, thread: Thread, throwable: Throwable) {
        val report = buildString {
            appendLine("XVOX crash report")
            appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())}")
            appendLine("Thread: ${thread.name} (id=${thread.id})")
            appendLine("App version: ${appVersion(context)}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Build: ${Build.FINGERPRINT}")
            appendLine()
            appendLine("Exception:")
            append(stackTrace(throwable))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeToPublicDownloads(context, report)
        } else {
            writeToLegacyDownloads(context, report)
        }
    }

    private fun writeToPublicDownloads(context: Context, report: String) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/"

        // Keep the public filename deterministic. A later crash replaces the prior report rather
        // than leaving users to guess between xvoxcrash, xvoxcrash (1), and so on.
        runCatching {
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                arrayOf(ReportName, relativePath),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    resolver.delete(ContentUris.withAppendedId(collection, id), null, null)
                }
            }
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, ReportName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: error("Unable to create public crash report")
        try {
            resolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { it.write(report) }
                ?: error("Unable to open public crash report")
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        } catch (failure: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw failure
        }
    }

    @Suppress("DEPRECATION")
    private fun writeToLegacyDownloads(context: Context, report: String) {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val publicFile = File(downloads, ReportName)
        runCatching {
            downloads.mkdirs()
            FileOutputStream(publicFile, false).bufferedWriter().use { it.write(report) }
        }.getOrElse {
            // The fallback remains externally browsable on devices that deny the legacy public
            // path during a fatal shutdown; it is still more useful than losing the stack trace.
            val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir
            fallbackDir.mkdirs()
            FileOutputStream(File(fallbackDir, ReportName), false).bufferedWriter().use { writer ->
                writer.write(report)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun appVersion(context: Context): String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        "${info.versionName ?: "unknown"} ($code)"
    }.getOrDefault("unknown")

    private fun stackTrace(throwable: Throwable): String = StringWriter().also { writer ->
        PrintWriter(writer).use { throwable.printStackTrace(it) }
    }.toString()
}
