package com.xvox.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.SizeF
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxLibraryPreferences
import com.xvox.music.player.playback.PlaybackLibraryLoader
import com.xvox.music.player.playback.toMediaItem
import com.xvox.music.player.session.XvoxPlaybackService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class XvoxAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = refreshReceiver(context)
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) = refreshReceiver(context)

    private fun refreshReceiver(context: Context) {
        val pending = goAsync()
        scope.launch {
            try { render(context.applicationContext, force = true) } catch (_: Exception) { /* Leave the launcher's last valid widget in place. */ }
            finally { pending.finish() }
        }
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == XvoxWidgetHelper.ACTION_UPDATE_WIDGET) { refreshReceiver(context); return }
        if (action !in setOf(XvoxWidgetHelper.ACTION_PLAY_PAUSE, XvoxWidgetHelper.ACTION_PREVIOUS,
                XvoxWidgetHelper.ACTION_NEXT, XvoxWidgetHelper.ACTION_TOGGLE_LIKE)) return
        // Compatibility for widgets installed before direct service PendingIntents were introduced.
        runCatching {
            ContextCompat.startForegroundService(context,
                Intent(context, XvoxPlaybackService::class.java).setAction(action))
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val requests = Channel<Unit>(Channel.CONFLATED)
        private val renderMutex = Mutex()
        private val fullRequested = AtomicBoolean(false)
        private var worker: Job? = null
        private var preferencesJob: Job? = null
        private var app: Context? = null
        @Volatile private var snapshot = Snapshot()
        private var lastRequest = 0L
        private var lastSignature: String? = null
        private var renderedState: XvoxWidgetHelper.WidgetDisplayState? = null
        private var cachedIds = intArrayOf()
        private var lastIdCheck = 0L
        private val dimensions = mutableMapOf<Int, List<SizeF>>()

        private data class Snapshot(val song: Song? = null, val playing: Boolean = false, val position: Long = 0, val duration: Long = 0) {
            fun signature(): String = "${song?.id}:${song?.title}:${song?.artist}:${song?.artworkUri}:$playing"
        }
        @Synchronized
        fun updateAllWidgets(context: Context, song: Song?, isPlaying: Boolean, position: Long, duration: Long) {
            val next = Snapshot(song, isPlaying, position, duration)
            val changed = next.signature() != snapshot.signature()
            snapshot = next
            if (!changed) return
            lastRequest = SystemClock.elapsedRealtime()
            enqueue(context, full = changed)
        }
        fun notifyWidgetUpdate(context: Context) { enqueue(context, full = true) }
        @Synchronized
        private fun enqueue(context: Context, full: Boolean) {
            app = context.applicationContext
            if (full) fullRequested.set(true)
            if (worker?.isActive != true) worker = scope.launch {
                for (ignored in requests) {
                    val context = app ?: continue
                    try { render(context, fullRequested.getAndSet(false)) } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { /* Keep the previous valid RemoteViews. */ }
                }
            }
            requests.trySend(Unit)
        }
        @OptIn(FlowPreview::class)
        private fun observePreferences(context: Context) {
            if (preferencesJob?.isActive == true) return
            preferencesJob = scope.launch {
                combine(UserPreferencesRepository(context).widgetStyle, XvoxLibraryPreferences(context).likedSongIds) { style, likes -> style to likes }
                    .drop(1).debounce(120).collect { enqueue(context, full = true) }
            }
        }
        private suspend fun render(context: Context, force: Boolean) = renderMutex.withLock {
            val manager = AppWidgetManager.getInstance(context)
            val now = SystemClock.elapsedRealtime()
            if (force || now - lastIdCheck >= 5000) {
                cachedIds = manager.getAppWidgetIds(ComponentName(context, XvoxAppWidgetProvider::class.java))
                lastIdCheck = now
            }
            if (cachedIds.isEmpty()) return@withLock
            observePreferences(context)
            val current = snapshot
            val signature = current.signature()
            val full = force || renderedState == null || signature != lastSignature
            if (!full) return@withLock
            val state = XvoxWidgetHelper.loadCurrentWidgetState(context, current.song, current.playing, current.position, current.duration)
            for (id in cachedIds) {
                val sizes = if (full || id !in dimensions) sizes(context, manager.getAppWidgetOptions(id)).also { dimensions[id] = it }
                    else dimensions.getValue(id)
                val views = LinkedHashMap<SizeF, RemoteViews>()
                for (size in sizes) {
                    views[size] = XvoxWidgetHelper.buildRemoteViews(context, state, size.width.roundToInt(), size.height.roundToInt())
                }
                val remote = if (Build.VERSION.SDK_INT >= 31 && views.size > 1) RemoteViews(views) else views.values.first()
                manager.updateAppWidget(id, remote)
            }
            renderedState = state; lastSignature = signature
        }
        @Suppress("DEPRECATION")
        private fun sizes(context: Context, options: Bundle): List<SizeF> {
            if (Build.VERSION.SDK_INT >= 31) {
                val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                    ?.filter { it.width >= 40 && it.height >= 40 }?.distinct()?.take(4)
                if (!sizes.isNullOrEmpty()) return sizes
            }
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 240)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 64)
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)
            return listOf(SizeF((if (landscape) maxW else minW).coerceAtLeast(40).toFloat(),
                (if (landscape) minH else maxH).coerceAtLeast(40).toFloat()))
        }
    }
}
