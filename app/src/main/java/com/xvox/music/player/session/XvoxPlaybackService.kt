package com.xvox.music.player.session

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.xvox.music.MainActivity

class XvoxPlaybackService :
    MediaSessionService() {

    private var player: ExoPlayer? = null

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(
                    C.USAGE_MEDIA
                )
                .setContentType(
                    C.AUDIO_CONTENT_TYPE_MUSIC
                )
                .build()

        val exoPlayer =
            ExoPlayer.Builder(this)
                .build()
                .apply {
                    setAudioAttributes(
                        audioAttributes,
                        true
                    )

                    repeatMode =
                        Player.REPEAT_MODE_OFF
                }

        player = exoPlayer

        val sessionActivityPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        session =
            MediaSession.Builder(
                this,
                exoPlayer
            ).setSessionActivity(sessionActivityPendingIntent).build()
    }

    override fun onGetSession(
        controllerInfo:
            MediaSession.ControllerInfo
    ): MediaSession? {
        return session
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 10 – Recents se remove → playback stop, notification remove
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
        session?.release()
        session = null

        player?.release()
        player = null

        super.onDestroy()
    }
}
