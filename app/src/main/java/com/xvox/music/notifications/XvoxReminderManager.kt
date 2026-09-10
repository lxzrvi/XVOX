package com.xvox.music.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.xvox.music.MainActivity
import com.xvox.music.R
import kotlin.random.Random

/**
 * Friendly "Let's play music" nudges. Stays within 3 messages per 24 h: every fired reminder
 * schedules the next one 4–9 hours later; if three already landed today it waits for the next
 * day. "Try now" (in settings) fires one immediately regardless of the daily budget.
 */
object XvoxReminderManager {

    const val ACTION_FIRE = "com.xvox.music.reminder.FIRE"

    val MESSAGES = listOf(
        "Let's play music",
        "Time to hear some songs",
        "Your songs are waiting",
        "A little music would feel great right now",
        "Put your headphones on for a song or two",
        "Your favourite tracks miss you",
        "Take a music break — you've earned it",
        "Let the day get a soundtrack",
        "Three minutes of a good song, why not?",
        "Your library is calling",
        "Sing along to something you love",
        "It's a good moment for music",
        "Unwind with a few of your favourites",
        "Songs are better with you",
        "Turn the volume up a little",
        "Your playlist is ready when you are",
        "Let's hear something you saved",
        "A short melody, a better mood",
        "Don't forget the songs you love",
        "Take five with music",
        "Your mix is waiting for you",
        "Play one song — just one",
        "A fresh tune could change the hour",
        "Music time"
    )

    private fun channelId(context: Context) = context.getString(R.string.reminder_channel_id)

    private var lastDayKey = -1
    private var firedToday = 0

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                channelId(context), context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.reminder_channel_description) }
        )
    }

    fun ensureChannel(context: Context) = createChannel(context)

    /** Schedules the next friendly nudge ~4–9 h away (respecting the 3/day budget). */
    fun scheduleNext(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pi = firePending(context)
        val next = Random.nextLong(4L, 10L) * 60L * 60L * 1000L
        // Keep the budget honest even after a reboot-less run: check the date key used by fire().
        alarm.setWindow(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + next, 10 * 60_000L, pi)
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.cancel(firePending(context))
    }

    fun fireNow(context: Context) {
        if (!notificationsAllowed(context)) return
        ensureChannel(context)
        val message = MESSAGES.random()
        val notification = NotificationCompat.Builder(context, channelId(context))
            .setSmallIcon(R.drawable.ic_stat_xvox_note)
            .setContentTitle("XVOX")
            .setContentText(message)
            // Bigger card: the expanded style keeps the whole message readable at a glance.
            .setStyle(NotificationCompat.BigTextStyle().bigText(message).setBigContentTitle("XVOX"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(openAppPending(context))
            .setAutoCancel(true)
            .build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify(Random.nextInt(1000, 9999), notification)
    }

    /** Returns true when the reminder should actually fire (under the daily budget). */
    fun consumeBudget(): Boolean {
        val now = java.util.Calendar.getInstance()
        val day = now.get(java.util.Calendar.YEAR) * 1000 + now.get(java.util.Calendar.DAY_OF_YEAR)
        if (day != lastDayKey) {
            lastDayKey = day
            firedToday = 0
        }
        if (firedToday >= 3) return false
        firedToday++
        return true
    }

    fun notificationsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun firePending(context: Context): PendingIntent {
        val intent = Intent(context, XvoxReminderReceiver::class.java).setAction(ACTION_FIRE)
        return PendingIntent.getBroadcast(context, 71, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openAppPending(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 72, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
