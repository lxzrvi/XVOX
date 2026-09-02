package com.xvox.music.player.session

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

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

        session =
            MediaSession.Builder(
                this,
                exoPlayer
            ).build()
    }

    override fun onGetSession(
        controllerInfo:
            MediaSession.ControllerInfo
    ): MediaSession? {
        return session
    }

    override fun onDestroy() {
        session?.release()
        session = null

        player?.release()
        player = null

        super.onDestroy()
    }
}
