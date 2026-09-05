package com.xvox.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxLibraryPreferences
import com.xvox.music.player.session.XvoxPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class XvoxAppWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "XvoxWidgetProvider"
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Cached state for fast widget rendering
        private var lastSong: Song? = null
        private var lastIsPlaying: Boolean = false
        private var lastPosition: Long = 0L
        private var lastDuration: Long = 0L

        fun updateAllWidgets(
            context: Context,
            song: Song?,
            isPlaying: Boolean,
            position: Long = 0L,
            duration: Long = 0L
        ) {
            lastSong = song
            lastIsPlaying = isPlaying
            lastPosition = position
            lastDuration = duration

            widgetScope.launch {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return@launch
                val thisWidget = ComponentName(context, XvoxAppWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return@launch

                val state = XvoxWidgetHelper.loadCurrentWidgetState(
                    context, song, isPlaying, position, duration
                )

                for (widgetId in allWidgetIds) {
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
                    val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0
                    val isCompact = minWidth in 1..210 || minHeight in 1..95

                    val remoteViews = XvoxWidgetHelper.buildRemoteViews(context, state, isCompact)
                    appWidgetManager.updateAppWidget(widgetId, remoteViews)
                }
            }
        }

        fun notifyWidgetUpdate(context: Context) {
            updateAllWidgets(context, lastSong, lastIsPlaying, lastPosition, lastDuration)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAllWidgets(context, lastSong, lastIsPlaying, lastPosition, lastDuration)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val isCompact = minWidth in 1..210 || minHeight in 1..95

        widgetScope.launch {
            val state = XvoxWidgetHelper.loadCurrentWidgetState(
                context, lastSong, lastIsPlaying, lastPosition, lastDuration
            )
            val remoteViews = XvoxWidgetHelper.buildRemoteViews(context, state, isCompact)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            XvoxWidgetHelper.ACTION_PLAY_PAUSE -> {
                sendMediaCommand(context) { controller ->
                    if (controller.isPlaying) {
                        controller.pause()
                        lastIsPlaying = false
                    } else {
                        controller.play()
                        lastIsPlaying = true
                    }
                    notifyWidgetUpdate(context)
                }
            }

            XvoxWidgetHelper.ACTION_PREVIOUS -> {
                sendMediaCommand(context) { controller ->
                    controller.seekToPreviousMediaItem()
                    notifyWidgetUpdate(context)
                }
            }

            XvoxWidgetHelper.ACTION_NEXT -> {
                sendMediaCommand(context) { controller ->
                    controller.seekToNextMediaItem()
                    notifyWidgetUpdate(context)
                }
            }

            XvoxWidgetHelper.ACTION_TOGGLE_LIKE -> {
                val current = lastSong
                if (current != null) {
                    widgetScope.launch {
                        val libPrefs = XvoxLibraryPreferences(context)
                        val isLiked = libPrefs.likedSongIds.first().contains(current.id)
                        libPrefs.setLiked(current.id, !isLiked)
                        notifyWidgetUpdate(context)
                    }
                }
            }

            XvoxWidgetHelper.ACTION_UPDATE_WIDGET -> {
                notifyWidgetUpdate(context)
            }
        }
    }

    private fun sendMediaCommand(context: Context, block: (MediaController) -> Unit) {
        val appContext = context.applicationContext
        val token = SessionToken(appContext, ComponentName(appContext, XvoxPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(appContext, token).buildAsync()

        controllerFuture.addListener(
            {
                runCatching {
                    val controller = controllerFuture.get()
                    block(controller)
                    controller.release()
                }.onFailure {
                    Log.e(TAG, "Failed to connect MediaController for widget command: ${it.message}")
                }
            },
            androidx.core.content.ContextCompat.getMainExecutor(appContext)
        )
    }
}
