package com.xvox.music.split

import android.content.Context
import android.os.Process
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * The XvoxSplit work itself, independent of how it is hosted.
 *
 * XvoxSplit used to exist only inside a foreground service. On modern Android that service can be
 * refused outright — background-start restrictions, a missing notification permission, an OEM
 * policy — and the whole feature then died with "Android blocked something" and no model download.
 *
 * The pipeline now lives here so it can run either way: the service uses it when it is allowed to
 * start, and [runInProcess] runs exactly the same work inside the app process when it is not.
 * Either way the model gets downloaded and tracks get separated.
 */
object XvoxSplitPipeline {
    private val execution = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var inProcess: Job? = null

    /** True while the app-process fallback is doing the work instead of the service. */
    val fallbackActive: Boolean get() = inProcess?.isActive == true

    /** Runs the model download + separation queue. Safe to call from a service or the app process. */
    suspend fun execute(context: Context, run: Long): Unit = execution.withLock {
        var model: XvoxSplitEngine? = null
        val oldPriority = runCatching { Process.getThreadPriority(Process.myTid()) }.getOrDefault(0)
        try {
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND) }
            if (!XvoxSplitRepository.valid(run)) return@withLock
            val file = XvoxSplitEngine.ensureModel(context, XvoxSplitRepository.allowMeteredDownload) { XvoxSplitRepository.valid(run) }
            XvoxSplitRepository.modelReady()
            model = XvoxSplitEngine(file)
            while (currentCoroutineContext().isActive && XvoxSplitRepository.valid(run)) {
                val track = XvoxSplitRepository.nextTask() ?: break
                val source = File(XvoxSplitRepository.directory(), "working_${run}_${track.id}.f32")
                val temporary = File(XvoxSplitRepository.directory(), "pair_${run}_${track.id}.part")
                val target = File(XvoxSplitRepository.directory(), "pair_${track.id}_${System.currentTimeMillis()}.wav")
                try {
                    XvoxSplitRepository.evictCache()
                    XvoxSplitRepository.updateFor(run, track.id, SplitStatus.DECODING)
                    val audio = SplitAudioFiles.decode(context, track, source, { XvoxSplitRepository.valid(run, track.id) }) {
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
                    if (!currentCoroutineContext().isActive || !XvoxSplitRepository.valid(run)) throw cancelled
                    XvoxSplitRepository.updateFor(run, track.id, SplitStatus.CANCELLED)
                } catch (error: Exception) {
                    XvoxSplitRepository.updateFor(run, track.id, SplitStatus.FAILED, error = error.message ?: "Unable to separate this track")
                } finally {
                    source.delete(); temporary.delete()
                    if (XvoxSplitRepository.state.value.tracks.firstOrNull { it.id == track.id }?.fileName != target.name) target.delete()
                }
            }
            if (XvoxSplitRepository.valid(run)) XvoxSplitRepository.finished()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: OutOfMemoryError) {
            XvoxSplitRepository.stop("Not enough memory for XvoxSplit. Normal playback restored.")
        } catch (_: LinkageError) {
            XvoxSplitRepository.stop("This device cannot load the separation runtime. Normal playback restored.")
        } catch (error: Exception) {
            XvoxSplitRepository.stop(error.message ?: "XvoxSplit setup failed")
        } finally {
            runCatching { model?.close() }
            runCatching { Process.setThreadPriority(oldPriority) }
        }
    }

    /**
     * Fallback host: the same work, in the app process.
     *
     * Used when Android refuses the foreground service. Progress and cancellation behave exactly
     * as before; the only difference is that the work pauses if the app is killed.
     */
    fun runInProcess(context: Context, run: Long) {
        inProcess?.cancel()
        val application = context.applicationContext
        inProcess = scope.launch { execute(application, run) }
    }

    fun cancelInProcess() {
        inProcess?.cancel()
        inProcess = null
    }

    /**
     * Model download on its own, with no service involved at all.
     *
     * This is the path the "Download model" action uses, so a blocked foreground service can never
     * be the reason the 28 MB model fails to arrive.
     */
    fun downloadModel(context: Context, allowMetered: Boolean) {
        val application = context.applicationContext
        scope.launch {
            try {
                XvoxSplitRepository.modelProgress(0f, "Downloading separation model")
                XvoxSplitEngine.ensureModel(application, allowMetered) { true }
                XvoxSplitRepository.modelReady()
                XvoxSplitRepository.notice("Separation model ready")
            } catch (_: CancellationException) {
                // Ignored: the user navigated away.
            } catch (error: Exception) {
                XvoxSplitRepository.modelNeedsDownload()
                XvoxSplitRepository.notice(error.message ?: "Model download failed")
            }
        }
    }
}
