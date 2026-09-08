package com.xvox.music.player.session

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.xvox.music.audio.AudioDspSettings
import com.xvox.music.audio.StereoBalanceAudioProcessor
import com.xvox.music.player.playback.CrossfadeMath
import com.xvox.music.player.playback.BeatGrid
import com.xvox.music.player.playback.BeatAlignment
import com.xvox.music.player.playback.EnergyBlendPlan
import com.xvox.music.player.playback.EnergyBlendPlanner
import com.xvox.music.player.playback.TrackBlendProfile
import com.xvox.music.player.playback.XvoxBeatAnalyzer
import com.xvox.music.player.playback.XvoxBlendMonitor
import com.xvox.music.player.playback.BlendVisualState
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Service-owned dual-deck playback. The prepared next deck is promoted without seeking/rebuilding it;
 * the old deck keeps its audible tail. The same MediaSession is rebound to the new active Player.
 * No Activity job or controller-volume coroutine can cancel a transition halfway through.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class XvoxCrossfadeEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onActivePlayerChanged: (ExoPlayer) -> Unit,
    private val onStateChanged: (Player) -> Unit
) {
    private class Deck(val player: ExoPlayer, val processor: StereoBalanceAudioProcessor, val listener: Player.Listener, var gain: Float)
    private data class Prepared(val deck: Deck, val fromId: String, val nextIndex: Int, var started: Boolean = false, var plan: EnergyBlendPlan? = null)
    private data class Overlap(val outgoing: Deck, val startPosition: Long, val duration: Long, val beatAligned: Boolean, val handoff: Float)

    var crossfadeEnabled = false
        set(value) { field = value; XvoxBlendMonitor.configure(value, crossfadeSeconds); refreshZones() }
    var crossfadeSeconds = 3
        set(value) { field = value.coerceIn(1, 12); XvoxBlendMonitor.configure(crossfadeEnabled, field); refreshZones() }
    var beatSyncEnabled = true
    var smartBlendEnabled = true
    var clashControl = .7f
    private val analyzer = XvoxBeatAnalyzer(context)
    private var analysisKey: String? = null
    private var analysisJob: Job? = null
    private var outgoingBeats: TrackBlendProfile? = null
    private var incomingBeats: TrackBlendProfile? = null
    private var lastVisualUpdate = 0L
    private var playlistChangedAt = 0L
    private var parameters = AudioDspSettings()
    private var prepared: Prepared? = null
    private var failedForId: String? = null
    private var overlap: Overlap? = null
    private var released = false
    private var internalFocusChange = false
    private var focusGranted = false
    private var resumeOnGain = false
    private var duck = 1f
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setOnAudioFocusChangeListener { change -> handleFocusChange(change) }.build()
    private val attributes = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()
    private var active: Deck = createDeck(1f)
    val player: ExoPlayer get() = active.player
    private val ticker: Job = scope.launch {
        while (isActive) {
            tick()
            delay(if (player.playWhenReady || overlap != null) 16L else 150L)
        }
    }

    private fun refreshZones() {
        XvoxBlendMonitor.current(player.currentMediaItem?.mediaId?.toLongOrNull(), player.duration,
            player.hasNextMediaItem() || player.repeatMode != Player.REPEAT_MODE_OFF)
    }
    private fun createDeck(gain: Float): Deck {
        val processor = StereoBalanceAudioProcessor().apply {
            // Preload full PCM, not silence: output envelopes belong AFTER the buffered audio sink.
            engine.settings = parameters.copy(masterVolume = 1f)
        }
        val factory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context).setAudioProcessors(arrayOf(processor)).build()
        }
        val exo = ExoPlayer.Builder(context, factory).build().apply {
            // One focus owner for both decks prevents the incoming track stealing focus from its own tail.
            setAudioAttributes(attributes, false)
            setWakeMode(C.WAKE_MODE_LOCAL)
            repeatMode = Player.REPEAT_MODE_OFF
            volume = (gain * duck * parameters.masterVolume).coerceIn(0f, 1f)
        }
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (released || exo !== player) return
                if (playWhenReady) {
                    if (!internalFocusChange && !requestFocus()) { exo.pause(); return }
                    overlap?.outgoing?.player?.let { if (it.playbackState != Player.STATE_ENDED) it.play() }
                } else {
                    overlap?.outgoing?.player?.pause()
                    prepared?.deck?.player?.pause()
                    prepared?.started = false
                    if (!internalFocusChange) { resumeOnGain = false; abandonFocus() }
                }
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if (released || exo !== player) return
                if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT ||
                    reason == Player.DISCONTINUITY_REASON_REMOVE) {
                    finishOverlap(); discardPrepared()
                    if (newPosition.positionMs == 0L) XvoxBlendMonitor.newTrack(exo.currentMediaItem?.mediaId?.toLongOrNull())
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (!released && exo === player) {
                    failedForId = null; finishOverlap(); discardPrepared()
                    XvoxBlendMonitor.newTrack(exo.currentMediaItem?.mediaId?.toLongOrNull())
                }
            }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (!released && exo === player && reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                    playlistChangedAt = SystemClock.elapsedRealtime()
                    finishOverlap(); discardPrepared()
                }
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                if (!released && exo === player) discardPrepared()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (!released && exo === player && (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED)) {
                    finishOverlap(); discardPrepared(); abandonFocus()
                }
            }
            override fun onEvents(player: Player, events: Player.Events) {
                if (!released && exo === this@XvoxCrossfadeEngine.player) {
                    XvoxBlendMonitor.current(exo.currentMediaItem?.mediaId?.toLongOrNull(), exo.duration,
                        exo.hasNextMediaItem() || exo.repeatMode != Player.REPEAT_MODE_OFF)
                    onStateChanged(exo)
                }
            }
        }
        exo.addListener(listener)
        return Deck(exo, processor, listener, gain)
    }

    fun updateSettings(settings: AudioDspSettings) {
        if (parameters == settings) return
        parameters = settings
        decks().forEach {
            val dsp = settings.copy(masterVolume = 1f)
            if (it.processor.engine.settings != dsp) it.processor.engine.settings = dsp
            applyOutputVolume(it)
        }
    }

    private fun tick() {
        if (released) return
        val mixing = overlap
        if (mixing != null) {
            if (!crossfadeEnabled) { finishOverlap(); return }
            if (!player.isPlaying) { mixing.outgoing.player.pause(); XvoxBlendMonitor.pauseVisual(); return }
            if (mixing.outgoing.player.playbackState == Player.STATE_ENDED || mixing.outgoing.player.playerError != null ||
                (mixing.outgoing.player.duration > 0 && mixing.outgoing.player.currentPosition >= mixing.outgoing.player.duration - 8)) {
                finishOverlap(); return
            }
            if (!mixing.outgoing.player.playWhenReady) mixing.outgoing.player.play()
            val progress = CrossfadeMath.progress(player.currentPosition, mixing.startPosition, mixing.duration)
            val gains = EnergyBlendPlanner.gains(progress, mixing.handoff, smartBlendEnabled)
            val bass = if (smartBlendEnabled) EnergyBlendPlanner.bassGains(progress, mixing.handoff, clashControl, gains)
                else com.xvox.music.player.playback.CrossfadeGains(1f, 1f)
            mixing.outgoing.processor.engine.transitionBassGain = bass.outgoing
            active.processor.engine.transitionBassGain = bass.incoming
            setGain(mixing.outgoing, gains.outgoing)
            setGain(active, gains.incoming)
            publishBlend(mixing, progress)
            if (progress >= 1f) finishOverlap()
            return
        }
        if (!crossfadeEnabled) { discardPrepared(); return }
        if (SystemClock.elapsedRealtime() - playlistChangedAt < 700L) return // Don't thrash decoders while a large queue is being installed/reordered.
        if (!player.isPlaying || player.duration <= 0) return
        val nextIndex = if (player.repeatMode == Player.REPEAT_MODE_ONE) player.currentMediaItemIndex else player.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET || nextIndex !in 0 until player.mediaItemCount) { discardPrepared(); return }
        val remaining = player.duration - player.currentPosition
        val window = CrossfadeMath.windowMs(crossfadeSeconds, player.duration)
        if (window == 0L) return
        val id = player.currentMediaItem?.mediaId ?: return
        if ((beatSyncEnabled || smartBlendEnabled) && remaining in (window + 3000)..30000L) startBeatAnalysis(nextIndex)
        if (!beatSyncEnabled && !smartBlendEnabled) cancelAnalysis()
        if (remaining > window + 5000) return
        if (id == failedForId) return
        if (prepared?.let { it.fromId != id || it.nextIndex != nextIndex } == true) discardPrepared()
        if (prepared == null) {
            val next = createDeck(0f)
            next.player.repeatMode = player.repeatMode
            next.player.shuffleModeEnabled = player.shuffleModeEnabled
            next.player.setMediaItems(List(player.mediaItemCount) { player.getMediaItemAt(it) }, nextIndex, 0L)
            next.player.prepare()
            prepared = Prepared(next, id, nextIndex)
        }
        val ready = prepared ?: return
        if (ready.deck.player.playerError != null) { failedForId = id; discardPrepared(); return }
        if (ready.deck.player.playbackState != Player.STATE_READY) return
        val nextDuration = ready.deck.player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        if (!beatSyncEnabled && !smartBlendEnabled) ready.plan = null
        val actualWindow = CrossfadeMath.windowMs(crossfadeSeconds, player.duration, nextDuration)
        if (actualWindow == 0L || remaining > actualWindow || remaining < 50) return
        if (!ready.started) {
            if (ready.plan == null) {
                ready.plan = if (smartBlendEnabled) EnergyBlendPlanner.plan(player.duration, actualWindow, outgoingBeats, incomingBeats, beatSyncEnabled)
                    else if (beatSyncEnabled) BeatAlignment.plan(player.duration, actualWindow, outgoingBeats?.beats, incomingBeats?.beats)?.let {
                        EnergyBlendPlan(it.startPositionMs, it.overlapMs, beatAligned = true)
                    } else null
            }
            if (ready.plan?.let { player.currentPosition < it.startPositionMs } == true) return
            ready.started = true
            ready.deck.player.play()
            return // Wait for actual playback readiness; don't fade away a still-playing old track.
        }
        if (!ready.deck.player.isPlaying) return
        val outgoing = active
        outgoing.player.pauseAtEndOfMediaItems = true
        active = ready.deck
        prepared = null
        overlap = Overlap(outgoing, player.currentPosition, remaining.coerceAtLeast(50), ready.plan?.beatAligned == true, ready.plan?.handoff ?: .5f)
        cancelAnalysis()
        XvoxBlendMonitor.markIncoming(player.currentMediaItem?.mediaId?.toLongOrNull(), remaining)
        publishBlend(overlap!!, 0f, force = true)
        onActivePlayerChanged(player) // Retains the preloaded next track's position and buffers.
        onStateChanged(player)
    }

    private fun startBeatAnalysis(nextIndex: Int) {
        val current = player.currentMediaItem ?: return
        val next = player.getMediaItemAt(nextIndex)
        val fromUri = current.localConfiguration?.uri ?: return
        val toUri = next.localConfiguration?.uri ?: return
        val key = "${current.mediaId}:${next.mediaId}:${player.duration}"
        if (analysisKey == key) return
        cancelAnalysis()
        analysisKey = key
        val tailStart = (player.duration - 16000L).coerceAtLeast(0)
        analysisJob = scope.launch {
            val out = analyzer.analyze(fromUri, tailStart)
            val incoming = analyzer.analyze(toUri, 0)
            if (analysisKey == key) { outgoingBeats = out; incomingBeats = incoming }
        }
    }
    private fun cancelAnalysis() {
        analysisJob?.cancel(); analysisJob = null; analysisKey = null
        outgoingBeats = null; incomingBeats = null
    }
    private fun publishBlend(mix: Overlap, progress: Float, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastVisualUpdate < 80L) return
        lastVisualUpdate = now
        val old = mix.outgoing.player
        XvoxBlendMonitor.publish(XvoxBlendMonitor.state.value.copy(
            enabled = crossfadeEnabled, configuredSeconds = crossfadeSeconds, active = true,
            outgoingId = old.currentMediaItem?.mediaId?.toLongOrNull(), incomingId = player.currentMediaItem?.mediaId?.toLongOrNull(),
            outgoingTitle = old.mediaMetadata.title?.toString().orEmpty(), incomingTitle = player.mediaMetadata.title?.toString().orEmpty(),
            outgoingPosition = old.currentPosition, outgoingDuration = old.duration.coerceAtLeast(0),
            incomingPosition = player.currentPosition, incomingDuration = player.duration.coerceAtLeast(0),
            windowMs = mix.duration, progress = progress, beatAligned = mix.beatAligned
        ))
    }

    private fun finishOverlap() {
        val old = overlap?.outgoing
        overlap = null
        XvoxBlendMonitor.end()
        active.processor.engine.transitionBassGain = 1f
        setGain(active, 1f)
        old?.let(::releaseDeck)
    }
    // AudioTrack volume applies to already-buffered PCM. Baking a zero gain into a prepared deck
    // would otherwise swallow the first buffered portion of the next song when the blend starts.
    private fun setGain(deck: Deck, gain: Float) { deck.gain = gain; applyOutputVolume(deck) }
    private fun applyOutputVolume(deck: Deck) {
        deck.player.volume = (deck.gain * duck * parameters.masterVolume).coerceIn(0f, 1f)
    }
    private fun discardPrepared() {
        cancelAnalysis()
        val old = prepared?.deck
        prepared = null
        old?.let(::releaseDeck)
    }
    private fun releaseDeck(deck: Deck) {
        deck.player.removeListener(deck.listener)
        deck.player.stop()
        deck.player.release()
    }
    private fun decks(): List<Deck> = listOfNotNull(active, prepared?.deck, overlap?.outgoing)
    fun acquirePlaybackFocus(): Boolean = requestFocus()
    fun resetResumeGain() {
        if (overlap == null) { setGain(active, 1f); active.processor.engine.mixGain = 1f }
        duck = 1f
        decks().forEach(::applyOutputVolume)
    }
    private fun requestFocus(): Boolean {
        if (focusGranted) return true
        focusGranted = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (focusGranted) { duck = 1f; decks().forEach(::applyOutputVolume) }
        return focusGranted
    }
    private fun abandonFocus() {
        if (focusGranted) audioManager.abandonAudioFocusRequest(focusRequest)
        focusGranted = false
    }
    private fun handleFocusChange(change: Int) {
        if (released) return
        internalFocusChange = true
        try {
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    focusGranted = true; duck = 1f
                    if (resumeOnGain) player.play()
                    resumeOnGain = false
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> duck = .20f
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS -> {
                    resumeOnGain = change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT && player.playWhenReady
                    focusGranted = false
                    player.pause(); overlap?.outgoing?.player?.pause()
                }
            }
            decks().forEach(::applyOutputVolume)
        } finally { internalFocusChange = false }
    }
    fun stop() {
        finishOverlap(); discardPrepared()
        player.stop(); player.clearMediaItems()
        resumeOnGain = false; abandonFocus()
    }
    fun release() {
        if (released) return
        released = true
        ticker.cancel()
        finishOverlap(); discardPrepared()
        releaseDeck(active)
        resumeOnGain = false
        audioManager.abandonAudioFocusRequest(focusRequest)
        focusGranted = false
    }
}
