package com.xvox.music.player.session

import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.xvox.music.MainActivity
import com.xvox.music.audio.AudioEffectsManager
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.widget.XvoxAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class XvoxPlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var prefsSyncJob: Job? = null

    private var noisyReceiverRegistered = false
    private var headsetReceiverRegistered = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                serviceScope.launch {
                    val prefs = UserPreferencesRepository(this@XvoxPlaybackService)
                    if (prefs.pauseOnHeadphoneDisconnect.first()) {
                        player?.pause()
                    }
                }
            }
        }
    }

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == Intent.ACTION_HEADSET_PLUG || action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                serviceScope.launch {
                    val prefs = UserPreferencesRepository(this@XvoxPlaybackService)
                    if (prefs.playOnHeadsetConnect.first()) {
                        player?.let { p ->
                            if (p.playbackState != Player.STATE_IDLE && p.playbackState != Player.STATE_ENDED && !p.isPlaying) {
                                p.play()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                repeatMode = Player.REPEAT_MODE_OFF
            }

        player = exoPlayer

        // Attach DSP & Equalizer engine
        val sessionId = exoPlayer.audioSessionId
        AudioEffectsManager.attachAudioSession(sessionId, this)

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        session = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        // Listen to Player Events to sync widgets and handle playback transitions
        exoPlayer.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                syncWidgetState(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                syncWidgetState(exoPlayer)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncWidgetState(exoPlayer)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncWidgetState(exoPlayer)
            }
        })

        // Register hardware audio broadcast receivers
        registerAudioReceivers()

        // Observe Preferences to update ExoPlayer runtime config
        observePreferences(exoPlayer)
    }

    private fun syncWidgetState(player: Player) {
        val mediaItem = player.currentMediaItem
        val song = if (mediaItem != null) {
            val metadata = mediaItem.mediaMetadata
            Song(
                id = mediaItem.mediaId.toLongOrNull() ?: 0L,
                title = metadata.title?.toString() ?: "Unknown Title",
                artist = metadata.artist?.toString() ?: "Unknown Artist",
                contentUri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY,
                artworkUri = metadata.artworkUri,
                duration = player.duration.coerceAtLeast(0L)
            )
        } else null

        XvoxAppWidgetProvider.updateAllWidgets(
            context = this,
            song = song,
            isPlaying = player.isPlaying,
            position = player.currentPosition.coerceAtLeast(0L),
            duration = player.duration.coerceAtLeast(0L)
        )
    }

    private fun registerAudioReceivers() {
        runCatching {
            registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            noisyReceiverRegistered = true
        }

        runCatching {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_HEADSET_PLUG)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            }
            registerReceiver(headsetReceiver, filter)
            headsetReceiverRegistered = true
        }
    }

    private fun observePreferences(exoPlayer: ExoPlayer) {
        val prefs = UserPreferencesRepository(this)
        prefsSyncJob?.cancel()
        prefsSyncJob = serviceScope.launch {
            launch {
                prefs.skipSilence.collect { skip ->
                    exoPlayer.skipSilenceEnabled = skip
                }
            }

            launch {
                prefs.audioFocus.collect { focus ->
                    val attrs = AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build()
                    exoPlayer.setAudioAttributes(attrs, focus)
                }
            }

            launch {
                prefs.appVolume.collect { vol ->
                    val limit = prefs.volumeLimit.first()
                    exoPlayer.volume = (vol * limit).coerceIn(0f, 1f)
                }
            }

            launch {
                prefs.volumeLimit.collect { limit ->
                    val vol = prefs.appVolume.first()
                    exoPlayer.volume = (vol * limit).coerceIn(0f, 1f)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return session
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            player?.stop()
            player?.clearMediaItems()
            session?.release()
            session = null
            player?.release()
            player = null
        } catch (_: Exception) {}
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (noisyReceiverRegistered) {
            runCatching { unregisterReceiver(noisyReceiver) }
            noisyReceiverRegistered = false
        }
        if (headsetReceiverRegistered) {
            runCatching { unregisterReceiver(headsetReceiver) }
            headsetReceiverRegistered = false
        }

        prefsSyncJob?.cancel()
        serviceScope.cancel()

        AudioEffectsManager.releaseEffects()

        session?.release()
        session = null

        player?.release()
        player = null

        super.onDestroy()
    }
}
