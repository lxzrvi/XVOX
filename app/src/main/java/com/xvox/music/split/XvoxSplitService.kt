package com.xvox.music.split

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.xvox.music.MainActivity
import com.xvox.music.R
import kotlinx.coroutines.*
import java.io.File

/**
 * Foreground host for XvoxSplit. Native inference never runs on the UI/audio threads.
 *
 * If Android refuses the foreground start, the service no longer kills the feature: it hands the
 * job to [XvoxSplitPipeline.runInProcess] and steps aside, so preparation still happens while
 * XVOX is open.
 */
class XvoxSplitService : Service() {
    companion object { const val START = "com.xvox.music.split.START"; const val CANCEL = "com.xvox.music.split.CANCEL" }
    private val dispatcher = java.util.concurrent.Executors.newSingleThreadExecutor { task ->
        Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); task.run() }, "XvoxSplit worker")
    }.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var work: Job? = null
    private var notifications: Job? = null
    private var wake: PowerManager.WakeLock? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        XvoxSplitRepository.initialize(this)
        runCatching {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel("xvox_split", "XvoxSplit preparation", NotificationManager.IMPORTANCE_LOW))
        }
    }
    private fun notification(): Notification {
        val state = XvoxSplitRepository.state.value
        val current = state.processingId?.let { state.byId[it] }
        val open = PendingIntent.getActivity(this, 500, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 501, Intent(this, XvoxSplitService::class.java).setAction(CANCEL), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "xvox_split").setSmallIcon(R.drawable.ic_notification).setContentTitle("XvoxSplit · ${state.ready}/${state.total} ready")
            .setContentText(current?.let { "${it.title} · ${(state.processingProgress * 100).toInt()}%" } ?: state.stage)
            .setProgress(100, ((if (current != null) state.processingProgress else state.modelProgress) * 100).toInt(), current == null && !state.modelReady)
            .setOngoing(true).setSilent(true).setContentIntent(open)
            .addAction(R.drawable.ic_xvox_close, "Stop preparation", stop).build()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == CANCEL) {
            XvoxSplitPipeline.cancelInProcess(); XvoxSplitRepository.stop(); stopSelf(); return START_NOT_STICKY
        }
        val run = intent?.getLongExtra("run", -1) ?: -1

        // mediaProcessing on 35+, dataSync below it; if neither is permitted we simply keep going
        // in-process instead of abandoning the request.
        val type = if (Build.VERSION.SDK_INT >= 35) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            else if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        val foreground = runCatching { ServiceCompat.startForeground(this, 2002, notification(), type) }.isSuccess ||
            runCatching { ServiceCompat.startForeground(this, 2002, notification(), 0) }.isSuccess
        if (!foreground) {
            XvoxSplitRepository.notice("Running XvoxSplit inside XVOX; keep the app open")
            XvoxSplitPipeline.runInProcess(applicationContext, run)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (work?.isActive == true) work?.cancel()
        notifications?.cancel()
        val ownNotifications = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            while (isActive) { runCatching { getSystemService(NotificationManager::class.java).notify(2002, notification()) }; delay(1000) }
        }
        notifications = ownNotifications
        work = scope.launch {
            try {
                wake = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "XVOX:SplitPreparation").apply {
                    setReferenceCounted(false); acquire(2 * 60 * 60 * 1000L)
                }
                XvoxSplitPipeline.execute(this@XvoxSplitService, run)
            } finally {
                runCatching { wake?.release() }; wake = null
                ownNotifications.cancel()
                withContext(NonCancellable + Dispatchers.Main) {
                    if (XvoxSplitRepository.currentGeneration() == run || !XvoxSplitRepository.state.value.running) stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }
        }
        return START_NOT_STICKY
    }
    override fun onTaskRemoved(rootIntent: Intent?) { /* Keep explicitly requested foreground work running. */ }
    override fun onTimeout(startId: Int, fgsType: Int) {
        // The system's processing window closed; continue in-app rather than losing the queue.
        XvoxSplitRepository.notice("Continuing XvoxSplit inside XVOX")
        XvoxSplitPipeline.runInProcess(applicationContext, XvoxSplitRepository.currentGeneration())
        stopSelf()
    }
    override fun onDestroy() {
        work?.cancel(); notifications?.cancel(); scope.cancel(); dispatcher.close()
        runCatching { wake?.release() }; wake = null
        super.onDestroy()
    }
}
