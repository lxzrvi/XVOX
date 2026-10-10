package com.xvox.music.core.diagnostics

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
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
 * Crash diagnostics with a durable two-stage public-download delivery path.
 *
 * A tiny Downloads/xvoxcrash.txt session sentinel is established before normal startup work. A
 * Java uncaught exception first fsyncs the complete report in app storage, then overwrites that
 * public file through MediaStore. If Android kills the process while MediaStore is unavailable,
 * the fsynced pending report is recovered and published on the next launch instead of disappearing.
 */
object XvoxCrashReporter {
    private const val ReportName = "xvoxcrash.txt"
    private const val MimeType = "text/plain"
    private const val PreferencesName = "xvox_crash_diagnostics"
    private const val PublicUriKey = "public_report_uri"
    private const val PendingFileName = "xvoxcrash.pending.txt"
    private const val SessionMarkerName = "xvoxcrash.session"

    private val installed = AtomicBoolean(false)
    private val handlingCrash = AtomicBoolean(false)

    fun install(context: Context) {
        if (!installed.compareAndSet(false, true)) return
        val appContext = context.applicationContext

        // Complete a previously fsynced write before setting up a new session. This covers the
        // exact failure class where an Android crash/kill beats the old uncaught-handler delivery.
        runCatching { recoverPendingReport(appContext) }
        runCatching { recoverUnexpectedPreviousSession(appContext) }
        runCatching { ensurePublicSessionSentinel(appContext) }
        markSessionActive(appContext)
        observeGracefulActivityFinish(appContext)

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (handlingCrash.compareAndSet(false, true)) {
                // Never let a MediaStore issue suppress the local durable record or the platform
                // handler. The public mirror is attempted synchronously while the process lives.
                runCatching { writeCrashReport(appContext, thread, throwable) }
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    private fun writeCrashReport(context: Context, thread: Thread, throwable: Throwable) {
        val report = buildReport(
            context = context,
            title = "XVOX crash report",
            detail = "Uncaught exception captured before Android process termination.",
            thread = thread,
            throwable = throwable
        )
        // The internal record is deliberately written before attempting MediaStore. It survives a
        // provider failure and is fsynced so the next process can publish it to Downloads.
        writeDurable(pendingFile(context), report)
        val published = runCatching { publishToPublicDownloads(context, report) }.getOrDefault(false)
        if (published) {
            pendingFile(context).delete()
            sessionMarker(context).delete()
        }
    }

    private fun recoverPendingReport(context: Context) {
        val pending = pendingFile(context)
        if (!pending.isFile || pending.length() == 0L) return
        val report = pending.readText()
        if (publishToPublicDownloads(context, report)) {
            pending.delete()
            sessionMarker(context).delete()
        }
    }

    /**
     * A prior active marker with no pending Java report means the process disappeared without
     * reaching our uncaught handler (for example a renderer/native close). Preserve useful device
     * and session diagnostics in the same public filename rather than leaving no file at all.
     */
    private fun recoverUnexpectedPreviousSession(context: Context) {
        val marker = sessionMarker(context)
        if (!marker.isFile || pendingFile(context).isFile) return
        val report = buildReport(
            context = context,
            title = "XVOX diagnostic recovery",
            detail = "The previous XVOX process ended before the Java crash handler completed. No Java stack trace was available; the session metadata below is retained for diagnosis.",
            thread = null,
            throwable = null
        )
        writeDurable(pendingFile(context), report)
        if (publishToPublicDownloads(context, report)) pendingFile(context).delete()
        marker.delete()
    }

    /** Establish the File-Manager-visible path before the app reaches risky startup/UI work. */
    private fun ensurePublicSessionSentinel(context: Context) {
        val cached = savedPublicUri(context) ?: run {
            val sentinel = buildReport(
                context = context,
                title = "XVOX diagnostics session",
                detail = "Diagnostics are armed for this XVOX session. A captured crash replaces this text with its stack trace.",
                thread = null,
                throwable = null
            )
            // Keep a local fallback too until the public URI has been established.
            writeDurable(pendingFile(context), sentinel)
            if (publishToPublicDownloads(context, sentinel)) pendingFile(context).delete()
            return
        }
        // Verify a cached MediaStore URI is still writable. If storage was cleared, create a
        // replacement before the rest of Application.onCreate begins.
        runCatching { context.contentResolver.openOutputStream(cached, "wa")?.close() }
            .getOrElse {
                clearSavedPublicUri(context)
                ensurePublicSessionSentinel(context)
            }
    }

    private fun markSessionActive(context: Context) {
        writeDurable(
            sessionMarker(context),
            "XVOX diagnostics session active\n${timestamp()}\nPID=${android.os.Process.myPid()}\n"
        )
    }

    private fun observeGracefulActivityFinish(context: Context) {
        (context as? android.app.Application)?.registerActivityLifecycleCallbacks(
            object : android.app.Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) {
                    if (activity.isFinishing && !activity.isChangingConfigurations) {
                        // A deliberate Close/Back exit must not be interpreted as a failed report
                        // on the next launch. A fatal process death never reliably reaches here.
                        sessionMarker(context).delete()
                    }
                }
            }
        )
    }

    /** Returns true only after the report stream has been closed and published. */
    private fun publishToPublicDownloads(context: Context, report: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return writeToScopedPublicDownloads(context, report)
        }
        return writeToLegacyDownloads(context, report)
    }

    private fun writeToScopedPublicDownloads(context: Context, report: String): Boolean {
        val resolver = context.contentResolver
        val reusable = savedPublicUri(context)
        if (reusable != null && runCatching {
                writeToMediaStoreUri(resolver, reusable, report)
                true
            }.getOrDefault(false)
        ) return true

        clearSavedPublicUri(context)
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/"

        // Best effort only: an app upgrade/reinstall can leave an older owned row behind. Removing
        // matching rows keeps the visible filename deterministic where the provider permits it.
        runCatching {
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                arrayOf(ReportName, relativePath),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    resolver.delete(ContentUris.withAppendedId(collection, cursor.getLong(0)), null, null)
                }
            }
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, ReportName)
            put(MediaStore.MediaColumns.MIME_TYPE, MimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return false
        return runCatching {
            writeToMediaStoreUri(resolver, uri, report)
            savePublicUri(context, uri)
            true
        }.getOrElse {
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    }

    private fun writeToMediaStoreUri(resolver: android.content.ContentResolver, uri: Uri, report: String) {
        // Pending protects file managers from observing a half-written stack trace. Updating an
        // existing owned row is supported on scoped storage and avoids duplicate file names.
        resolver.update(uri, ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            put(MediaStore.MediaColumns.MIME_TYPE, MimeType)
        }, null, null)
        resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.write(report)
            writer.flush()
        } ?: error("Unable to open Downloads/$ReportName")
        resolver.update(uri, ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }, null, null)
    }

    @Suppress("DEPRECATION")
    private fun writeToLegacyDownloads(context: Context, report: String): Boolean = runCatching {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        downloads.mkdirs()
        FileOutputStream(File(downloads, ReportName), false).bufferedWriter().use { writer ->
            writer.write(report)
            writer.flush()
        }
        true
    }.getOrElse { false }

    private fun buildReport(
        context: Context,
        title: String,
        detail: String,
        thread: Thread?,
        throwable: Throwable?
    ): String = buildString {
        appendLine(title)
        appendLine("Timestamp: ${timestamp()}")
        appendLine("Detail: $detail")
        appendLine("App version: ${appVersion(context)}")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Build: ${Build.FINGERPRINT}")
        thread?.let { appendLine("Thread: ${it.name} (id=${it.id})") }
        throwable?.let {
            appendLine()
            appendLine("Exception:")
            append(stackTrace(it))
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())

    private fun pendingFile(context: Context) = File(context.filesDir, PendingFileName)
    private fun sessionMarker(context: Context) = File(context.filesDir, SessionMarkerName)

    /** Atomic + fsync local persistence for fatal-process timing. */
    private fun writeDurable(target: File, text: String) {
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.new")
        FileOutputStream(temporary, false).use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
            stream.flush()
            stream.fd.sync()
        }
        if (!temporary.renameTo(target)) {
            FileOutputStream(target, false).use { stream ->
                stream.write(temporary.readBytes())
                stream.flush()
                stream.fd.sync()
            }
            temporary.delete()
        }
    }

    private fun savedPublicUri(context: Context): Uri? = context
        .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getString(PublicUriKey, null)
        ?.let(Uri::parse)

    private fun savePublicUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(PublicUriKey, uri.toString())
            .commit()
    }

    private fun clearSavedPublicUri(context: Context) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .remove(PublicUriKey)
            .commit()
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
