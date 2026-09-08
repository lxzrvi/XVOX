package com.xvox.music.player.session

import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.xvox.music.data.preferences.XvoxLibraryPreferences
import com.xvox.music.widget.XvoxWidgetHelper
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.xvox.music.MainActivity
import com.xvox.music.R
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.player.playback.PlaybackLibraryLoader
import com.xvox.music.player.playback.toMediaItem
import com.xvox.music.widget.XvoxAppWidgetProvider
import kotlinx.coroutines.*
import com.xvox.music.audio.AudioEffectsManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class XvoxPlaybackService : MediaSessionService() {
    private var engine: XvoxCrossfadeEngine? = null
    private var session: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var commandJob: Job? = null
    private var fillJob: Job? = null
    private var persistedSongId: Long? = null
    private var receiverRegistered = false

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "xvox_playback"
    }

    /** Promote first, then ask for audio focus: required for cold/background playback on recent Android. */
    private fun ensurePlaybackForeground(): Boolean = runCatching {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "XVOX playback", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("XVOX")
            .setContentText("Preparing your music…").setContentIntent(open).setOngoing(true).setSilent(true).build()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        true
    }.getOrDefault(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val actions = setOf(XvoxWidgetHelper.ACTION_PLAY_PAUSE, XvoxWidgetHelper.ACTION_PREVIOUS,
            XvoxWidgetHelper.ACTION_NEXT, XvoxWidgetHelper.ACTION_TOGGLE_LIKE)
        if (action !in actions) return super.onStartCommand(intent, flags, startId)
        if (!ensurePlaybackForeground()) { stopSelf(startId); return START_NOT_STICKY }
        commandJob?.cancel()
        commandJob = serviceScope.launch {
            try {
                val p = engine?.player ?: return@launch
                when (action) {
                    XvoxWidgetHelper.ACTION_PLAY_PAUSE -> {
                        if (p.playWhenReady && p.playbackState != Player.STATE_ENDED && p.playbackState != Player.STATE_IDLE) p.pause()
                        else resumeFromService()
                    }
                    XvoxWidgetHelper.ACTION_PREVIOUS -> if (p.hasPreviousMediaItem()) p.seekToPreviousMediaItem() else if (p.mediaItemCount > 0) p.seekTo(0)
                    XvoxWidgetHelper.ACTION_NEXT -> if (p.hasNextMediaItem()) p.seekToNextMediaItem()
                    XvoxWidgetHelper.ACTION_TOGGLE_LIKE -> p.currentMediaItem?.mediaId?.toLongOrNull()?.let { id ->
                        val library = XvoxLibraryPreferences(this@XvoxPlaybackService)
                        library.setLiked(id, id !in library.likedSongIds.first())
                    }
                }
                syncWidgetState(p)
                refreshMediaNotification()
                if (!p.playWhenReady) stopForeground(if (p.mediaItemCount == 0) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
                if (p.mediaItemCount == 0) stopSelf(startId)
            } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) {
                engine?.player?.pause()
                stopForeground(STOP_FOREGROUND_DETACH)
                XvoxAppWidgetProvider.notifyWidgetUpdate(this@XvoxPlaybackService)
            }
        }
        return START_NOT_STICKY
    }

    private fun refreshMediaNotification() {
        val activeSession = session ?: return
        super.onUpdateNotification(activeSession, engine?.player?.playWhenReady == true)
    }
    private suspend fun resumeFromService(requireHeadset: Boolean = false) {
        val playback = engine ?: return
        val p = playback.player
        var resumeToken = com.xvox.music.player.playback.QueuePopulationEpoch.snapshot()
        if (p.mediaItemCount == 0) {
            val library = PlaybackLibraryLoader.load(this) ?: run {
                stopForeground(STOP_FOREGROUND_REMOVE)
                return
            }
            if (!com.xvox.music.player.playback.QueuePopulationEpoch.current(resumeToken)) return
            if (requireHeadset && routes?.hasHeadphoneOutput() != true) return
            val anchor = library.startIndex
            val seedEnd = minOf(library.songs.size, anchor + 5)
            p.setMediaItems(library.songs.subList(anchor, seedEnd).map { it.toMediaItem() }, 0, 0)
            fillJob?.cancel()
            val populationToken = com.xvox.music.player.playback.QueuePopulationEpoch.begin()
            resumeToken = populationToken
            fillJob = serviceScope.launch {
                var expectedCount = p.mediaItemCount
                var expectedFirst = p.getMediaItemAt(0).mediaId
                var expectedLast = p.getMediaItemAt(p.mediaItemCount - 1).mediaId
                fun unchanged(): Boolean = com.xvox.music.player.playback.QueuePopulationEpoch.current(populationToken) && engine?.player === p && p.mediaItemCount == expectedCount &&
                    p.mediaItemCount > 0 && p.getMediaItemAt(0).mediaId == expectedFirst && p.getMediaItemAt(p.mediaItemCount - 1).mediaId == expectedLast
                for (chunk in library.songs.subList(seedEnd, library.songs.size).chunked(48)) {
                    val items = withContext(Dispatchers.Default) { chunk.map { it.toMediaItem() } }
                    if (!unchanged()) return@launch
                    p.addMediaItems(items)
                    expectedCount += items.size; expectedLast = items.last().mediaId
                    delay(16)
                }
                var insert = 0
                for (chunk in library.songs.subList(0, anchor).chunked(48)) {
                    val items = withContext(Dispatchers.Default) { chunk.map { it.toMediaItem() } }
                    if (!unchanged()) return@launch
                    p.addMediaItems(insert, items)
                    if (insert == 0) expectedFirst = items.first().mediaId
                    insert += items.size; expectedCount += items.size
                    delay(16)
                }
            }
        }
        if (requireHeadset && routes?.hasHeadphoneOutput() != true) return
        if (p.playbackState == Player.STATE_ENDED) p.seekToDefaultPosition()
        playback.resetResumeGain()
        p.prepare() // Buffering a paused item is safe; denied focus must leave a real paused session, not a stuck bootstrap.
        // Foreground registration is synchronous, but allow a bounded retry for focus arbitration.
        for (attempt in 0..2) {
            if (!com.xvox.music.player.playback.QueuePopulationEpoch.current(resumeToken)) return
            if (playback.acquirePlaybackFocus()) {
                p.play()
                return
            }
            delay(120L)
        }
        p.pause() // Don't claim to be playing while Android has denied audible playback.
        refreshMediaNotification()
        stopForeground(STOP_FOREGROUND_DETACH)
    }

    private var routes: XvoxAudioRouteObserver? = null
    private var connectJob: Job? = null
    private var disconnectJob: Job? = null

    private fun onHeadsetConnected() {
        disconnectJob?.cancel()
        if (connectJob?.isActive == true) return
        connectJob = serviceScope.launch {
            delay(350) // Let HFP/A2DP or BLE route negotiation settle; coalesce duplicate callbacks.
            val prefs = UserPreferencesRepository(this@XvoxPlaybackService)
            if (!prefs.playOnHeadsetConnect.first() || routes?.hasHeadphoneOutput() != true) return@launch
            val p = engine?.player ?: return@launch
            if (p.playWhenReady && p.playbackState != Player.STATE_ENDED) return@launch
            if (ensurePlaybackForeground()) resumeFromService(requireHeadset = true)
        }
    }
    private fun onHeadsetDisconnected() {
        connectJob?.cancel()
        disconnectJob?.cancel()
        disconnectJob = serviceScope.launch {
            delay(250)
            if (routes?.hasHeadphoneOutput() == false && UserPreferencesRepository(this@XvoxPlaybackService).pauseOnHeadphoneDisconnect.first()) {
                engine?.player?.pause()
            }
        }
    }

    private val audioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            serviceScope.launch {
                val prefs = UserPreferencesRepository(this@XvoxPlaybackService)
                if (action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && prefs.pauseOnHeadphoneDisconnect.first()) {
                    engine?.player?.pause()
                }
                val connected = (action == Intent.ACTION_HEADSET_PLUG && intent.getIntExtra("state", -1) == 1) ||
                    ((action == "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" ||
                        action == "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED") &&
                        intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) == BluetoothProfile.STATE_CONNECTED)
                if (connected) onHeadsetConnected()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Explicit monochrome XVOX small icon for the status bar / media notification.
        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).setNotificationId(NOTIFICATION_ID).setChannelId(CHANNEL_ID).build().apply {
            setSmallIcon(R.drawable.ic_notification)
        })
        val playback = XvoxCrossfadeEngine(this, serviceScope,
            onActivePlayerChanged = { next -> session?.setPlayer(next) },
            onStateChanged = ::syncWidgetState)
        engine = playback
        val openApp = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val createdSession = MediaSession.Builder(this, playback.player).setSessionActivity(openApp).build()
        session = createdSession
        addSession(createdSession)
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        }
        ContextCompat.registerReceiver(this, audioReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        receiverRegistered = true
        routes = XvoxAudioRouteObserver(this, ::onHeadsetConnected, ::onHeadsetDisconnected).also { it.start() }
        val prefs = UserPreferencesRepository(this)
        serviceScope.launch {
            combine(prefs.audioDspSettings, AudioEffectsManager.liveEq) { saved, live ->
                saved to (live?.applyTo(saved) ?: saved)
            }.collect { (saved, effective) ->
                playback.updateSettings(effective)
                AudioEffectsManager.clearIfPersisted(saved)
            }
        }
        serviceScope.launch {
            while (isActive) {
                engine?.player?.let(::syncWidgetState)
                delay(1000) // Progress still updates when the Activity is gone; renderer uses tiny partial updates.
            }
        }
        serviceScope.launch { prefs.crossfadeSmart.collect { playback.smartBlendEnabled = it } }
        serviceScope.launch { prefs.crossfadeClashControl.collect { playback.clashControl = it } }
        serviceScope.launch { prefs.crossfadeBeatSync.collect { playback.beatSyncEnabled = it } }
        serviceScope.launch { prefs.crossfade.distinctUntilChanged().collect { playback.crossfadeEnabled = it } }
        serviceScope.launch { prefs.crossfadeDuration.distinctUntilChanged().collect { playback.crossfadeSeconds = it.coerceIn(1, 12) } }
    }

    private fun syncWidgetState(player: Player) {
        val item = player.currentMediaItem
        val song = item?.let {
            Song(id = it.mediaId.toLongOrNull() ?: 0L,
                title = it.mediaMetadata.title?.toString() ?: "Unknown title",
                artist = it.mediaMetadata.artist?.toString() ?: "Unknown artist",
                contentUri = it.localConfiguration?.uri ?: Uri.EMPTY,
                artworkUri = it.mediaMetadata.artworkUri, duration = player.duration.coerceAtLeast(0L))
        }
        if (player.isPlaying && song != null && song.id != persistedSongId) {
            persistedSongId = song.id
            serviceScope.launch { UserPreferencesRepository(this@XvoxPlaybackService).setLastPlayedSongId(song.id) }
        }
        XvoxAppWidgetProvider.updateAllWidgets(this, song, player.isPlaying,
            player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Closing the Activity must not tear down an audible player or a widget's in-flight resume.
        val p = engine?.player
        if (p?.isPlaying == true || (p?.playWhenReady == true && p.playbackState == Player.STATE_BUFFERING) || commandJob?.isActive == true || connectJob?.isActive == true) return
        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() {
        commandJob?.cancel(); fillJob?.cancel()
        routes?.stop(); routes = null
        connectJob?.cancel(); disconnectJob?.cancel()
        if (receiverRegistered) { unregisterReceiver(audioReceiver); receiverRegistered = false }
        serviceScope.cancel()
        session?.release(); session = null
        engine?.release(); engine = null
        super.onDestroy()
    }
}
