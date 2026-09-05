package com.xvox.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.xvox.music.core.model.Song
import com.xvox.music.player.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class XvoxAppWidgetProvider : AppWidgetProvider() {

    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val playbackController = PlaybackController.activeInstance
        val curSong = playbackController?.currentQueue()?.getOrNull(playbackController.state.value.currentIndex)
        val isPlaying = playbackController?.state?.value?.isPlaying ?: false
        val position = playbackController?.state?.value?.position ?: 0L
        val duration = playbackController?.state?.value?.duration ?: 0L

        updateWidgets(context, appWidgetManager, appWidgetIds, curSong, isPlaying, position, duration)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val playbackController = PlaybackController.activeInstance
        val curSong = playbackController?.currentQueue()?.getOrNull(playbackController.state.value.currentIndex)
        val isPlaying = playbackController?.state?.value?.isPlaying ?: false
        val position = playbackController?.state?.value?.position ?: 0L
        val duration = playbackController?.state?.value?.duration ?: 0L

        updateSingleWidget(context, appWidgetManager, appWidgetId, curSong, isPlaying, position, duration)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            XvoxWidgetHelper.ACTION_PLAY_PAUSE -> {
                val controller = PlaybackController.activeInstance
                if (controller != null) {
                    controller.togglePlay()
                } else {
                    notifyWidgetUpdate(context)
                }
            }
            XvoxWidgetHelper.ACTION_PREVIOUS -> {
                val controller = PlaybackController.activeInstance
                if (controller != null) {
                    controller.playPrevious()
                } else {
                    notifyWidgetUpdate(context)
                }
            }
            XvoxWidgetHelper.ACTION_NEXT -> {
                val controller = PlaybackController.activeInstance
                if (controller != null) {
                    controller.playNext()
                } else {
                    notifyWidgetUpdate(context)
                }
            }
            XvoxWidgetHelper.ACTION_TOGGLE_LIKE -> {
                notifyWidgetUpdate(context)
            }
            XvoxWidgetHelper.ACTION_UPDATE_WIDGET -> {
                notifyWidgetUpdate(context)
            }
        }
    }

    private fun updateSingleWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        song: Song?,
        isPlaying: Boolean,
        position: Long,
        duration: Long
    ) {
        providerScope.launch {
            val state = XvoxWidgetHelper.loadCurrentWidgetState(context, song, isPlaying, position, duration)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 90)

            val layoutType = when {
                minHeight >= minWidth * 0.85f -> WidgetLayoutType.SQUARE // 1x1, 2x2, 3x3, 4x4
                minWidth >= minHeight * 1.8f -> WidgetLayoutType.HORIZONTAL // 2x1, 3x1, 4x1, 5x1
                minHeight < 100 -> WidgetLayoutType.COMPACT
                else -> WidgetLayoutType.HORIZONTAL
            }

            val views = XvoxWidgetHelper.buildRemoteViews(context, state, layoutType)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        song: Song?,
        isPlaying: Boolean,
        position: Long,
        duration: Long
    ) {
        appWidgetIds.forEach { widgetId ->
            updateSingleWidget(context, appWidgetManager, widgetId, song, isPlaying, position, duration)
        }
    }

    companion object {
        fun notifyWidgetUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, XvoxAppWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(component)
            if (widgetIds.isNotEmpty()) {
                val intent = Intent(context, XvoxAppWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(intent)
            }
        }

        fun updateAllWidgets(
            context: Context,
            song: Song?,
            isPlaying: Boolean,
            position: Long,
            duration: Long
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, XvoxAppWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(component)
            if (widgetIds.isNotEmpty()) {
                val provider = XvoxAppWidgetProvider()
                provider.updateWidgets(context, appWidgetManager, widgetIds, song, isPlaying, position, duration)
            }
        }
    }
}
