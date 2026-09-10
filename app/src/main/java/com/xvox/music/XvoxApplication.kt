package com.xvox.music

import android.app.Application
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.bitmapConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toOkioPath

class XvoxApplication : Application() {

    /**
     * Notification channel for "Notify" reminders, plus a once-per-boot re-arm: if reminders are
     * on but no alarm is pending (e.g. the phone was rebooted) the next nudge is scheduled again.
     * Launching the app never postpones an alarm that already exists.
     */
    private fun armReminders() {
        com.xvox.music.notifications.XvoxReminderManager.ensureChannel(this)
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        scope.launch {
            val prefs = com.xvox.music.data.preferences.UserPreferencesRepository(this@XvoxApplication)
            if (!prefs.remindersEnabled.first()) return@launch
            val pi = android.app.PendingIntent.getBroadcast(
                this@XvoxApplication, 71,
                android.content.Intent(this@XvoxApplication, com.xvox.music.notifications.XvoxReminderReceiver::class.java)
                    .setAction(com.xvox.music.notifications.XvoxReminderManager.ACTION_FIRE),
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_NO_CREATE
            )
            if (pi == null) com.xvox.music.notifications.XvoxReminderManager.scheduleNext(this@XvoxApplication)
        }
    }

    override fun onCreate() {
        super.onCreate()
        armReminders()

        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .decoderCoroutineContext(com.xvox.music.artwork.XvoxImageWork.decoderContext)
                .fetcherCoroutineContext(com.xvox.music.artwork.XvoxImageWork.fetchContext)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(
                            context,
                            0.25
                        )
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(
                            context.cacheDir
                                .resolve(
                                    "xvox_artwork"
                                )
                                .toOkioPath()
                        )
                        .maxSizeBytes(
                            100L *
                                1024L *
                                1024L
                        )
                        .build()
                }
                .bitmapConfig(
                    Bitmap.Config.RGB_565
                )
                .build()
        }
    }
}
