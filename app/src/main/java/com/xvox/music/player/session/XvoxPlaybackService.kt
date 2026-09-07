package com.xvox.music.player.session

import android.app.PendingIntent
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
    private var receiverRegistered = false
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
            if (p.mediaItemCount == 0) {
                val library = PlaybackLibraryLoader.load(this@XvoxPlaybackService) ?: return@launch
                if (routes?.hasHeadphoneOutput() != true || p.mediaItemCount > 0 || p.playWhenReady) return@launch
                p.setMediaItems(library.songs.map { it.toMediaItem() }, library.startIndex, 0)
            }
            if (p.playbackState == Player.STATE_ENDED) p.seekToDefaultPosition()
            p.prepare()
            p.play()
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
        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).build().apply {
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
        session = MediaSession.Builder(this, playback.player).setSessionActivity(openApp).build()
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
        XvoxAppWidgetProvider.updateAllWidgets(this, song, player.isPlaying,
            player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onTaskRemoved(rootIntent: Intent?) {
        engine?.stop()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() {
        routes?.stop(); routes = null
        connectJob?.cancel(); disconnectJob?.cancel()
        if (receiverRegistered) { unregisterReceiver(audioReceiver); receiverRegistered = false }
        serviceScope.cancel()
        session?.release(); session = null
        engine?.release(); engine = null
        super.onDestroy()
    }
}
