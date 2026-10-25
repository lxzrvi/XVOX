package com.xvox.music.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class XvoxReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != XvoxReminderManager.ACTION_FIRE) return
        val pending = goAsync()
        Thread {
            try {
                val prefs = UserPreferencesRepository(context.applicationContext)
                val enabled = runBlocking { prefs.remindersEnabled.first() }
                if (enabled) {
                    if (!com.xvox.music.MainActivity.isAppInForeground && XvoxReminderManager.consumeBudget()) {
                        XvoxReminderManager.fireNow(context)
                    }
                    // Keep the enabled default chained under the existing daily budget.
                    XvoxReminderManager.scheduleNext(context)
                } else {
                    // An alarm may have been queued before the user turned the setting off.
                    XvoxReminderManager.cancel(context)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
