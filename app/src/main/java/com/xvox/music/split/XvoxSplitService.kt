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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/** Foreground, cancellable processing service; native inference never runs on the UI/audio threads. */
class XvoxSplitService : Service() {
    companion object { private val execution = Mutex(); const val START = "com.xvox.music.split.START"; const val CANCEL = "com.xvox.music.split.CANCEL" }
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
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("xvox_split", "XvoxSplit preparation", NotificationManager.IMPORTANCE_LOW))
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
        if (intent?.action == CANCEL) { XvoxSplitRepository.stop(); stopSelf(); return START_NOT_STICKY }
        val run = intent?.getLongExtra("run", -1) ?: -1
        val type = if (Build.VERSION.SDK_INT >= 35) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            else if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        try { ServiceCompat.startForeground(this, 2002, notification(), type) }
        catch (_: Exception) { XvoxSplitRepository.stop("Android blocked background preparation"); stopSelf(); return START_NOT_STICKY }
        if (work?.isActive == true) work?.cancel()
        notifications?.cancel()
        val ownNotifications = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            while (isActive) { getSystemService(NotificationManager::class.java).notify(2002, notification()); delay(1000) }
        }
        notifications = ownNotifications
        work = scope.launch { execution.withLock {
            var model: XvoxSplitEngine? = null
            val oldPriority = Process.getThreadPriority(Process.myTid())
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                if (!XvoxSplitRepository.valid(run)) return@launch
                wake = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "XVOX:SplitPreparation").apply {
                    setReferenceCounted(false); acquire(2 * 60 * 60 * 1000L)
                }
                val file = XvoxSplitEngine.ensureModel(this@XvoxSplitService, XvoxSplitRepository.allowMeteredDownload) { XvoxSplitRepository.valid(run) }
                XvoxSplitRepository.modelReady()
                model = XvoxSplitEngine(file)
                while (isActive && XvoxSplitRepository.valid(run)) {
                    val track = XvoxSplitRepository.nextTask() ?: break
                    val source = File(XvoxSplitRepository.directory(), "working_${run}_${track.id}.f32")
                    val temporary = File(XvoxSplitRepository.directory(), "pair_${run}_${track.id}.part")
                    val target = File(XvoxSplitRepository.directory(), "pair_${track.id}_${System.currentTimeMillis()}.wav")
                    try {
                        XvoxSplitRepository.evictCache()
                        XvoxSplitRepository.updateFor(run, track.id, SplitStatus.DECODING)
                        val audio = SplitAudioFiles.decode(this@XvoxSplitService, track, source, { XvoxSplitRepository.valid(run, track.id) }) {
                            XvoxSplitRepository.updateFor(run, track.id, SplitStatus.DECODING, it * .15f)
                        }
                        if (!XvoxSplitRepository.valid(run, track.id)) continue
                        XvoxSplitRepository.updateFor(run, track.id, SplitStatus.SEPARATING, .15f)
                        model.separate(audio, temporary, { XvoxSplitRepository.valid(run, track.id) }) {
                            XvoxSplitRepository.updateFor(run, track.id, SplitStatus.SEPARATING, .15f + it * .85f)
                        }
                        if (!XvoxSplitRepository.valid(run, track.id)) continue
                        check(temporary.renameTo(target)) { "Could not save prepared audio" }
                        XvoxSplitRepository.updateFor(run, track.id, SplitStatus.READY, 1f, file = target)
                    } catch (cancelled: CancellationException) {
                        if (!isActive || !XvoxSplitRepository.valid(run)) throw cancelled
                        XvoxSplitRepository.updateFor(run, track.id, SplitStatus.CANCELLED)
                    } catch (error: Exception) {
                        XvoxSplitRepository.updateFor(run, track.id, SplitStatus.FAILED, error = error.message ?: "Unable to separate this track")
                    } finally {
                        source.delete(); temporary.delete()
                        if (XvoxSplitRepository.state.value.tracks.firstOrNull { it.id == track.id }?.fileName != target.name) target.delete()
                    }
                }
                if (XvoxSplitRepository.valid(run)) XvoxSplitRepository.finished()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: OutOfMemoryError) { XvoxSplitRepository.stop("Not enough memory for XvoxSplit. Normal playback restored.") }
            catch (_: LinkageError) { XvoxSplitRepository.stop("This device cannot load the separation runtime. Normal playback restored.") }
            catch (error: Exception) { XvoxSplitRepository.stop(error.message ?: "XvoxSplit setup failed") }
            finally {
                runCatching { model?.close() }; runCatching { wake?.release() }; wake = null
                runCatching { Process.setThreadPriority(oldPriority) }
                ownNotifications.cancel()
                withContext(NonCancellable + Dispatchers.Main) {
                    if (XvoxSplitRepository.currentGeneration() == run || !XvoxSplitRepository.state.value.running) stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }
        } }
        return START_NOT_STICKY
    }
    override fun onTaskRemoved(rootIntent: Intent?) { /* Keep explicitly requested foreground work running. */ }
    override fun onTimeout(startId: Int, fgsType: Int) { XvoxSplitRepository.stop("Android's background processing limit was reached. Prepared tracks are kept."); stopSelf() }
    override fun onDestroy() {
        work?.cancel(); notifications?.cancel(); scope.cancel(); dispatcher.close()
        runCatching { wake?.release() }; wake = null
        super.onDestroy()
    }
}
