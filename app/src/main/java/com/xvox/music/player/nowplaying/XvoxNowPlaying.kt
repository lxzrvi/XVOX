// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/player/nowplaying/XvoxNowPlaying.kt
package com.xvox.music.player.nowplaying

import androidx.compose.ui.graphics.RectangleShape
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import com.xvox.music.core.ui.miniplayer.XvoxGhost
import com.xvox.music.core.ui.miniplayer.XvoxShared
import com.xvox.music.core.ui.miniplayer.XvoxSharedGhosts
import com.xvox.music.core.ui.miniplayer.xvoxShared
import com.xvox.music.features.home.XvoxSongArtwork
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.blur.LocalXvoxNowPlayingBottomSky
import com.xvox.music.core.ui.blur.LocalXvoxNowPlayingSky
import com.xvox.music.core.ui.blur.rememberXvoxContentSky
import com.xvox.music.core.ui.blur.xvoxBlurSource
import com.xvox.music.core.ui.blur.xvoxLiveBlur
import com.xvox.music.core.ui.chrome.normalizedNowPlayingGrid
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.miniplayer.XvoxNowPlayingMs
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
import com.xvox.music.core.ui.miniplayer.xvoxHandoffMs
import com.xvox.music.core.ui.miniplayer.xvoxNowPlayingSpec
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxBox
import com.xvox.music.core.ui.overlay.XvoxBoxPresentation
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.features.home.XvoxSongActions
import com.xvox.music.features.player.styles.XvoxPlayerStyle
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.sections.EqualizerFooterActions
import com.xvox.music.features.settings.sections.EqualizerSettingsSection
import com.xvox.music.features.settings.components.XvoxTransactionalFooterActions
import com.xvox.music.features.settings.sections.HeadsetSettingsDraftSection
import com.xvox.music.features.settings.sections.LyricsFooterActions
import com.xvox.music.features.settings.sections.LyricsSettingsDraftSection
import com.xvox.music.features.settings.sections.PlaybackSettingsDraftSection
import com.xvox.music.features.settings.sections.ThreeDSoundDraftSection
import com.xvox.music.player.nowplaying.components.NowPlayingActions
import com.xvox.music.player.nowplaying.components.NowPlayingOptionsBox
import com.xvox.music.player.nowplaying.components.NowPlayingAlignmentGuideOverlay
import com.xvox.music.player.nowplaying.components.NowPlayingEditItem
import com.xvox.music.player.nowplaying.components.NowPlayingAlignmentGuide
import com.xvox.music.player.nowplaying.components.NowPlayingEditSession
import com.xvox.music.player.nowplaying.components.NowPlayingEditableCover
import com.xvox.music.player.nowplaying.components.NowPlayingEditableTarget
import com.xvox.music.player.nowplaying.lyrics.XvoxArtworkLyrics
import com.xvox.music.player.nowplaying.lyrics.XvoxLyricsViewModel
import com.xvox.music.player.nowplaying.lyrics.lyricsFullscreenMotion
import com.xvox.music.player.playback.RepeatMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private val XvoxSmoothEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Cover-matched lyric colour with real tonal separation, without adding a text glow/shadow. */
private fun Color.xvoxReadableCoverLyricColor(): Color {
    val pole = if (luminance() > .48f) Color.Black else Color.White
    val pull = .64f
    return Color(
        red = red * (1f - pull) + pole.red * pull,
        green = green * (1f - pull) + pole.green * pull,
        blue = blue * (1f - pull) + pole.blue * pull,
        alpha = 1f
    )
}


/**
 * The Cover background uses the pager's stable outgoing/incoming pair. Its adjacent layer is
 * keyed only to the active target and alpha follows the same fraction as the visible pager, so a
 * rapid reversal removes the abandoned adjacent cover instead of replaying it behind the player.
 */
@Composable
private fun XvoxNowPlayingBackdropLayer(
    song: Song,
    paletteState: XvoxNowPlayingPaletteState,
    style: String,
    coverBlur: Float,
    swipeBase: Song?,
    swipeAdjacent: Song?,
    swipeFraction: () -> Float,
    modifier: Modifier = Modifier
) {
    val base = swipeBase ?: song
    XvoxNowPlayingBackdrop(
        dominant = paletteState.color,
        artwork = base.artworkUri,
        artworkKey = base.xvoxArtworkPaletteKey(),
        artworkSeed = base.id.hashCode(),
        style = style,
        coverBlur = coverBlur,
        isCoverTransitionInProgress = paletteState.isCoverTransitionInProgress,
        adjacentCover = swipeAdjacent?.let {
            XvoxBackdropCover(it.artworkUri, it.xvoxArtworkPaletteKey(), it.id.hashCode())
        },
        adjacentFraction = swipeFraction,
        modifier = modifier
    )
}

@Composable
fun XvoxNowPlaying(
    song: Song,
    queue: List<Song>,
    /** Media3 occurrence IDs make repeated queue rows distinguishable during a preview. */
    queueOccurrenceIds: List<String> = emptyList(),
    currentIndex: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    /** Set only after Mini Player has fully crossed below the bottom boundary. */
    entranceEnabled: Boolean = true,
    onClose: () -> Unit,
    /** Begins dismissal housekeeping while this surface completes its fixed descent. */
    onDismissStart: () -> Unit = {},
    onTogglePlay: () -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    displayMode: Int = 0,
    onDisplayModeChange: ((Int) -> Unit)? = null,
    isLiked: Boolean = false,
    onToggleLiked: (() -> Unit)? = null,
    onTimer: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    onStarPlaylist: (() -> Unit)? = null,
    isShuffleEnabled: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    onToggleShuffle: (() -> Unit)? = null,
    onToggleRepeat: (() -> Unit)? = null,
    shuffleMethod: String = "RANDOM",
    repeatTimesLeft: Int = 0,
    abLoopStartMs: Long = -1L,
    abLoopEndMs: Long = -1L,
    onApplyShuffleMethod: ((String) -> Unit)? = null,
    onSetRepeatSong: ((Int) -> Unit)? = null,
    onSetRepeatPlain: ((RepeatMode) -> Unit)? = null,
    onSetAbLoop: ((Long, Long) -> Unit)? = null,
    playerStyle: XvoxPlayerStyle = XvoxPlayerStyle.NORMAL,
    sleepTimerProgress: Float? = null,
    playingSource: String = "All Songs",
    isInPlaylist: Boolean = false,
    /** Durable selection for the swipable right-side action cluster. */
    actionPageIndex: Int = 0,
    onActionPageChange: (Int) -> Unit = {},
    lyricsViewModel: XvoxLyricsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val colors = XvoxTheme.colors
    val persistedChrome = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current
        .normalizedNowPlayingGrid()
    // Customize is a true local transaction over this live player—not a second replica screen.
    // The draft is discarded by X/back and reaches DataStore only from Okay.
    var customizeDraft by remember { mutableStateOf<com.xvox.music.core.ui.chrome.XvoxChromeStyle?>(null) }
    var customizeAlignmentGuide by remember { mutableStateOf<NowPlayingAlignmentGuide?>(null) }
    val chrome = (customizeDraft ?: persistedChrome).normalizedNowPlayingGrid()
    val customizeSession = customizeDraft?.let {
        NowPlayingEditSession(
            draft = chrome,
            onDraftChange = { updated ->
                customizeDraft = updated.normalizedNowPlayingGrid()
            },
            alignmentGuide = customizeAlignmentGuide,
            onAlignmentGuideChange = { guide -> customizeAlignmentGuide = guide }
        )
    }
    val isCustomizing = customizeSession != null
    val overlays = LocalXvoxOverlayController.current
    // The player chrome adopts the lyrics-box material once the display mode is known below.
    // The three Customize knobs remain independent multipliers above that shared baseline.
    val transitionStyle = XvoxPlayerTransitionMotion.normalizedStyle(chrome.miniPlayerTransitionStyle)
    val context = LocalContext.current
    val lyricsState by lyricsViewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    // The master Appearance intensity also scales the cover backdrop. This makes one slider
    // control the true expensive player blur, and lets low settings reduce GPU work immediately.
    // Now Playing blur is controlled ONLY from Now Playing (its own cover-blur setting); the
    // Settings master blur switch/intensity no longer changes it.
    val effectiveCoverBlur = settingsState.nowPlayingCoverBlur
    val nowPlayingGlassChrome = remember(chrome, settingsState.nowPlayingCoverBlur) {
        chrome.copy(
            // Blur mode (Settings > App background > Blur) off => Now Playing glass is plain
            // theme material with default borders; on => live blur + reflective rim.
            glassBlurEnabled = chrome.glassBlurEnabled,
            glassBlurIntensity = if (chrome.glassBlurEnabled) (settingsState.nowPlayingCoverBlur / 30f).coerceIn(.4f, 1f) else 0f
        )
    }
    val paletteState = rememberXvoxNowPlayingPalette(song, queue, currentIndex)
    // Top controls sample this Now Playing backdrop source, not the app wallpaper or the shell
    // underneath the player. It is intentionally only the backdrop, never its own controls.
    val nowPlayingSky = rememberXvoxContentSky()
    LaunchedEffect(
        song.xvoxArtworkPaletteKey(),
        paletteState.color,
        settingsState.nowPlayingBackgroundStyle,
        effectiveCoverBlur
    ) {
        nowPlayingSky.invalidate()
    }
    // These reflect the pager's stable outgoing/incoming pair for the local Cover background.
    // They are intentionally separate from palette colour state so each blur layer can read the
    // live gesture fraction without depending on an image-ready callback.
    var coverSwipeBase by remember { mutableStateOf<Song?>(null) }
    var coverSwipeAdjacent by remember { mutableStateOf<Song?>(null) }
    var coverSwipeFraction by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(
        coverSwipeBase?.xvoxArtworkPaletteKey(),
        coverSwipeAdjacent?.xvoxArtworkPaletteKey(),
        coverSwipeFraction
    ) {
        // A cover pager drag is a real moving Now Playing backdrop, so top control glass follows
        // the stable outgoing/incoming pair in the same gesture rather than keeping a stale grab.
        nowPlayingSky.invalidate()
    }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val screenHeight = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    // The return handoff always travels to the physical bottom. Legacy experimental overshoot
    // values are deliberately ignored now that Default/Scale are the only exposed motion modes.
    val belowBottomY = screenHeight

    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isCompact = (settingsState.nowPlayingStyle == "compact" || settingsState.nowPlayingStyle == "immersive") && !isLandscape

    // These are intentionally composition-local, not saveable: every prepared handoff must begin
    // below the viewport rather than restoring a prior open player's settled position.
    var screenY by remember { mutableFloatStateOf(belowBottomY) }
    var entered by remember { mutableStateOf(false) }
    // Drag dismissal is intentionally armed only after the entrance has settled. A touch that
    // lands while the prepared player is still travelling upward must never interpret its current
    // in-flight Y as a user swipe and auto-close the player.
    var dragDismissReady by remember { mutableStateOf(false) }
    var internalDisplayMode by rememberSaveable { mutableIntStateOf(0) }
    val currentMode = if (onDisplayModeChange != null) displayMode else internalDisplayMode
    val isLyricsShowing = currentMode >= 1
    val isFullscreen = currentMode == 2
    // This is intentionally scoped to visible lyric text. In cover mode do not subscribe the
    // outer player composition to a per-frame palette blend; the local backdrop child owns that.
    val lyricsTextColor = when (settingsState.lyrics.textColorMode) {
        "black" -> Color.Black
        "white" -> Color.White
        else -> if (isLyricsShowing) paletteState.color.xvoxReadableCoverLyricColor() else colors.primaryText
    }
    // Exactly match XvoxArtworkLyrics' compact/expanded surface alpha (.27/.35).
    val lyricsBoxAlpha = if (isFullscreen) .35f else .27f
    val bottomPanelAlpha = lyricsBoxAlpha * chrome.nowPlayingBottomBoxAlpha.coerceIn(0f, 1f)
    val controlsSurfaceAlpha = lyricsBoxAlpha
    val setMode: (Int) -> Unit = { mode ->
        internalDisplayMode = mode
        onDisplayModeChange?.invoke(mode)
    }

    var activeSettingsBox by rememberSaveable { mutableStateOf<String?>(null) }
    // Real (RenderEffect) blur of the whole player while an option sheet is open: smooth, not
    // pixelated, and no capture-layer recursion. Only with liquid glass ON.
    val npGlassOn = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current.glassBlurEnabled
    val npSheetBlur by animateDpAsState(
        targetValue = if (activeSettingsBox != null && npGlassOn && android.os.Build.VERSION.SDK_INT >= 31) 14.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(260),
        label = "npSheetBlur"
    )
    // Ordinary setting sheets are live: this local value only supplies immediate UI state while
    // the persisted Settings flow catches up. Closing, Cancel, and Okay never roll back a change.
    var optionDraft by remember { mutableStateOf(settingsState) }
    fun openSettingsBox(name: String) {
        optionDraft = settingsState
        if (name == "Customize") {
            customizeDraft = persistedChrome.normalizedNowPlayingGrid()
            customizeAlignmentGuide = null
        }
        activeSettingsBox = name
    }
    fun applyCrossfadeDraft(draft: com.xvox.music.features.settings.SettingsState = optionDraft) {
        settingsViewModel.setCrossfade(draft.crossfade)
        settingsViewModel.setGapless(draft.gapless)
        settingsViewModel.setCrossfadeDuration(draft.crossfadeDuration)
        settingsViewModel.setCrossfadeSmart(draft.crossfadeSmart)
        settingsViewModel.setCrossfadeClashControl(draft.crossfadeClashControl)
        settingsViewModel.setCrossfadeBeatSync(draft.crossfadeBeatSync)
        settingsViewModel.setCrossfadeCurveStyle(draft.crossfadeCurveStyle)
    }
    fun applyThreeDSoundDraft(draft: com.xvox.music.features.settings.SettingsState = optionDraft) {
        settingsViewModel.setStereoWidening(draft.stereoWidening)
        settingsViewModel.setSurroundWidth(draft.surroundWidth)
        settingsViewModel.setSurroundDepth(draft.surroundDepth)
        settingsViewModel.setSurroundPanSpeed(draft.surroundPanSpeed)
        settingsViewModel.setHrtf(draft.hrtf)
        settingsViewModel.setBalance(draft.balance)
    }
    fun applyBluetoothDraft(draft: com.xvox.music.features.settings.SettingsState = optionDraft) {
        settingsViewModel.setAudioOutputRoute(draft.audioOutputRoute)
        settingsViewModel.setBtConnectAction(draft.btConnectAction)
        settingsViewModel.setPlayOnHeadsetConnect(draft.playOnHeadsetConnect)
        settingsViewModel.setBtDisconnectAction(draft.btDisconnectAction)
        settingsViewModel.setPauseOnHeadphoneDisconnect(draft.pauseOnHeadphoneDisconnect)
    }
    fun applyLyricsDraft(draft: com.xvox.music.features.settings.SettingsState = optionDraft) {
        settingsViewModel.updateLyrics { draft.lyrics }
    }
    var dismissing by remember { mutableStateOf(false) }
    var navigationRequest by remember { mutableIntStateOf(0) }
    // Separate visual browsing from the audio deck. Covers may move immediately while the
    // audible item stays untouched until the navigation button is released. Queue indices are
    // occurrence identities here: two copies of one Song.id must remain independently swipeable.
    var previewIndex by rememberSaveable { mutableIntStateOf(currentIndex.coerceIn(0, queue.lastIndex.coerceAtLeast(0))) }
    // An index is only a visual position. Keep the real occurrence token/reference beside it so a
    // drag or playback reorder cannot turn a selected duplicate into its neighbouring copy.
    var previewOccurrenceId by rememberSaveable {
        mutableStateOf(queueOccurrenceIds.getOrNull(currentIndex))
    }
    var previewSongReference by remember { mutableStateOf(queue.getOrNull(currentIndex)) }
    var previewGestureActive by remember { mutableStateOf(false) }
    // Audio commits after a preview settles. The backdrop separately follows the pager's stable
    // base/adjacent pair, so it can reverse without retaining a stale outgoing blur layer.
    var previewCommitJob by remember { mutableStateOf<Job?>(null) }
    var previewCommitVersion by remember { mutableIntStateOf(0) }
    // Lyrics replaces the visible pager, so its Next/Previous path commits directly and records
    // this flag to prevent the button-release callback from dispatching the same track twice.
    var lyricsDirectNavigation by remember { mutableStateOf(false) }
    var holdPagerPaletteDuringShuffle by remember { mutableStateOf(false) }
    var motionJob by remember { mutableStateOf<Job?>(null) }
    val latestQueue by rememberUpdatedState(queue)
    val latestQueueOccurrenceIds by rememberUpdatedState(queueOccurrenceIds)
    val latestCurrentIndex by rememberUpdatedState(currentIndex)

    fun occurrenceIndexInLatestQueue(token: String?, songReference: Song?): Int {
        return token?.let { latestQueueOccurrenceIds.indexOf(it) }?.takeIf { it >= 0 }
            ?: songReference?.let { reference -> latestQueue.indexOfFirst { it === reference } }
                ?.takeIf { it >= 0 }
            ?: -1
    }

    /** Keep the Cover pair and Default canvas on the exact stable pager frame. */
    fun applyPagerPalette(base: Song, adjacent: Song?, fraction: Float) {
        if (holdPagerPaletteDuringShuffle) return
        val safeFraction = fraction.coerceIn(0f, 1f)
        coverSwipeBase = base
        coverSwipeAdjacent = adjacent
        coverSwipeFraction = safeFraction
        paletteState.blend(base, adjacent, safeFraction)
    }

    // Lyrics has no artwork pager to emit a pair. Reset its local backdrop to the committed
    // song, and also protect externally advanced playback from retaining an old swipe pair.
    LaunchedEffect(song.xvoxArtworkPaletteKey(), isLyricsShowing, holdPagerPaletteDuringShuffle) {
        if (!holdPagerPaletteDuringShuffle) {
            coverSwipeBase = song
            coverSwipeAdjacent = null
            coverSwipeFraction = 0f
        }
        if (isLyricsShowing) {
            paletteState.show(song)
        }
    }

    var headerHeightDp by remember { mutableStateOf(56.dp) }
    var bottomHeightDp by remember { mutableStateOf(if (isCompact) 175.dp else 245.dp) }

    fun animateScreen(target: Float, finished: (() -> Unit)? = null) {
        motionJob?.cancel()
        val start = screenY
        motionJob = scope.launch {
            val animation = Animatable(start)
            // Both directions use the same Now Playing curve and full-distance duration. A
            // reversal keeps the current physical frame, then scales time only to the remaining
            // travel so the surface maintains the same visual speed.
            val travel = kotlin.math.abs(target - start) / screenHeight.coerceAtLeast(1f)
            animation.animateTo(
                target,
                xvoxNowPlayingSpec(xvoxHandoffMs(XvoxNowPlayingMs, travel))
            ) {
                screenY = value
            }
            screenY = target
            motionJob = null
            finished?.invoke()
        }
    }

    LaunchedEffect(screenHeight, belowBottomY, entranceEnabled) {
        if (!entered && screenHeight > 0f) {
            // Precomposition lays the whole player out below the selected physical handoff point.
            // Its entrance remains held until the outgoing Mini Player has reached its own point.
            screenY = belowBottomY
            dragDismissReady = false
            if (entranceEnabled) {
                entered = true
                animateScreen(target = 0f) {
                    if (!dismissing) dragDismissReady = true
                }
            }
        }
    }
    fun dismiss() {
        if (dismissing) return
        dismissing = true
        dragDismissReady = false
        previewCommitVersion++
        previewCommitJob?.cancel()
        previewCommitJob = null
        onDismissStart()
        animateScreen(target = belowBottomY) {
            // This is the first frame after the full player has physically cleared the bottom.
            // onClose mounts the Mini Player immediately; there is no post-animation delay.
            onClose()
        }
    }

    fun returnToRest() {
        // A stale drag callback must not cancel an already intentional close animation.
        if (dismissing) return
        animateScreen(0f) {
            if (!dismissing) dragDismissReady = true
        }
    }

    fun finishDragDismissal() {
        if (dismissing) return
        if (!dragDismissReady) {
            // A header/lyrics touch arrived during the handoff. Recover to the settled player
            // rather than converting the handoff's travel distance into an unwanted close.
            returnToRest()
        } else if (screenY > screenHeight * 0.18f) {
            dismiss()
        } else {
            returnToRest()
        }
    }

    fun cancelPendingPreviewCommit() {
        previewCommitVersion++
        previewCommitJob?.cancel()
        previewCommitJob = null
    }

    fun setPreviewTarget(index: Int): Boolean {
        if (index !in queue.indices) return false
        previewIndex = index
        previewOccurrenceId = queueOccurrenceIds.getOrNull(index)
        previewSongReference = queue[index]
        return true
    }

    LaunchedEffect(currentIndex, queue, queueOccurrenceIds) {
        // External playback changes win over an old release request. A held preview follows its
        // exact occurrence through a reorder; it never silently becomes a neighbouring duplicate.
        if (!previewGestureActive) cancelPendingPreviewCommit()
        if (previewGestureActive) {
            val remappedIndex = occurrenceIndexInLatestQueue(previewOccurrenceId, previewSongReference)
            if (remappedIndex >= 0) {
                previewIndex = remappedIndex
            } else {
                previewGestureActive = false
                cancelPendingPreviewCommit()
                if (currentIndex in queue.indices) setPreviewTarget(currentIndex)
                else if (queue.isNotEmpty()) setPreviewTarget(0)
            }
        } else if (currentIndex in queue.indices) {
            setPreviewTarget(currentIndex)
        } else if (queue.isNotEmpty()) {
            setPreviewTarget(0)
        }
    }

    // Shuffle must keep the current song's already-selected palette pinned while the pager
    // remaps pages to the new queue order. Release the hold only after that short reflow settles.
    LaunchedEffect(holdPagerPaletteDuringShuffle, queue, currentIndex, song.id) {
        if (holdPagerPaletteDuringShuffle) {
            paletteState.pin(song)
            delay(XvoxPlayerTransitionMotion.Duration.toLong() + 40L)
            holdPagerPaletteDuringShuffle = false
        }
    }

    fun movePreview(direction: Int): Boolean {
        if (queue.isEmpty() || repeatMode == RepeatMode.ONE) return false
        cancelPendingPreviewCommit()
        val from = previewIndex.takeIf { it in queue.indices }
            ?: currentIndex.takeIf { it in queue.indices }
            ?: return false
        val target = when {
            direction > 0 && from < queue.lastIndex -> from + 1
            direction < 0 && from > 0 -> from - 1
            direction > 0 && repeatMode == RepeatMode.ALL && queue.size > 1 -> 0
            direction < 0 && repeatMode == RepeatMode.ALL && queue.size > 1 -> queue.lastIndex
            else -> return false
        }
        setPreviewTarget(target)
        if (isLyricsShowing) {
            // The pager is intentionally absent behind lyrics. Drive the exact selected occurrence
            // directly while warming/pinning its palette so the lyrics canvas and full backdrop
            // morph through the same cover-matched colours a visible cover would use.
            lyricsDirectNavigation = true
            previewGestureActive = false
            val targetSong = queue[target]
            paletteState.pin(targetSong)
            scope.launch { paletteState.show(targetSong) }
            onPlayQueueIndex(target)
        } else {
            previewGestureActive = true
            navigationRequest += direction.coerceIn(-1, 1)
        }
        return true
    }

    fun commitPreview() {
        if (lyricsDirectNavigation) {
            // PreviewNavigationControl commits on release. Lyrics already committed its selected
            // occurrence on press because there is no visible pager to settle, so release must be
            // a no-op rather than replaying the same queue index.
            lyricsDirectNavigation = false
            previewGestureActive = false
            return
        }
        val targetOccurrence = previewIndex
        val targetToken = previewOccurrenceId
        val targetSongReference = previewSongReference ?: latestQueue.getOrNull(targetOccurrence)
        cancelPendingPreviewCommit()
        previewGestureActive = false
        if (targetSongReference == null) return

        val requestVersion = previewCommitVersion
        previewCommitJob = scope.launch {
            // Keep audio on the current song until the released cover has rested in place.
            delay(300)
            val remappedTarget = occurrenceIndexInLatestQueue(targetToken, targetSongReference)
            if (
                requestVersion == previewCommitVersion &&
                !previewGestureActive &&
                remappedTarget in latestQueue.indices &&
                remappedTarget != latestCurrentIndex
            ) {
                // Retain the exact occurrence if a queue drag happened in this 300ms window.
                previewIndex = remappedTarget
                onPlayQueueIndex(remappedTarget)
            }
            if (requestVersion == previewCommitVersion) previewCommitJob = null
        }
    }

    /** The pager has already waited its 300 ms release window; commit that exact occurrence. */
    fun commitSettledPreview(index: Int, settledSong: Song) {
        cancelPendingPreviewCommit()
        previewGestureActive = false
        // onPreviewIndexChange saved this exact token before the pager's stability delay. Resolve
        // it again now so a duplicate moved near the top cannot be mistaken for another copy.
        val resolvedIndex = occurrenceIndexInLatestQueue(previewOccurrenceId, settledSong)
        if (resolvedIndex !in latestQueue.indices) return
        previewIndex = resolvedIndex
        previewSongReference = latestQueue[resolvedIndex]
        previewOccurrenceId = latestQueueOccurrenceIds.getOrNull(resolvedIndex)
        if (resolvedIndex != latestCurrentIndex) onPlayQueueIndex(resolvedIndex)
    }

    fun cancelPreview() {
        cancelPendingPreviewCommit()
        lyricsDirectNavigation = false
        previewGestureActive = false
        if (latestCurrentIndex in latestQueue.indices) {
            previewIndex = latestCurrentIndex
            previewSongReference = latestQueue[latestCurrentIndex]
            previewOccurrenceId = latestQueueOccurrenceIds.getOrNull(latestCurrentIndex)
        }
    }

    LaunchedEffect(song.id) {
        lyricsViewModel.load(song)
    }

    // Predictive back: the player visibly shrinks under the finger and either closes or springs back.
    var backProgress by remember { mutableFloatStateOf(0f) }
    // Layout switch: the new layout springs in (scale + fade from partial, never from nothing).
    val layoutSwap = remember { androidx.compose.animation.core.Animatable(1f) }
    var shownLayout by remember { mutableStateOf(settingsState.nowPlayingLayout) }
    LaunchedEffect(settingsState.nowPlayingLayout) {
        if (shownLayout != settingsState.nowPlayingLayout) {
            shownLayout = settingsState.nowPlayingLayout
            layoutSwap.snapTo(0f)
            layoutSwap.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = .62f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
        }
    }
    val predictiveBackActive = entranceEnabled && activeSettingsBox == null && currentMode == 0 && !dismissing && !isCustomizing
    androidx.activity.compose.PredictiveBackHandler(enabled = predictiveBackActive) { events ->
        try {
            events.collect { backProgress = it.progress }
            dismiss()
            androidx.compose.animation.core.animate(backProgress, 0f, animationSpec = tween(300)) { v, _ -> backProgress = v }
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            androidx.compose.animation.core.animate(backProgress, 0f, animationSpec = tween(220)) { v, _ -> backProgress = v }
        }
    }
    BackHandler(enabled = entranceEnabled && !predictiveBackActive) {
        when {
            activeSettingsBox != null -> {
                // Customize is transactional: X/back drops the in-place draft without touching
                // live chrome. Ordinary option sheets remain live and simply close.
                if (activeSettingsBox == "Customize") {
                    customizeDraft = null
                    customizeAlignmentGuide = null
                }
                activeSettingsBox = null
            }
            currentMode == 2 -> setMode(1)
            currentMode == 1 -> setMode(0)
            else -> dismiss()
        }
    }

    val isSlidingDown = screenY > 1f
    val slideFraction = (screenY / screenHeight.coerceAtLeast(1f)).coerceIn(0f, 1f)
    // Scale is endpoint-safe: Now Playing is an exact full-size surface when settled, then it
    // contracts toward the bottom Mini Player while its sheet physically travels downward.
    val handoffScale = XvoxPlayerTransitionMotion.nowPlayingScale(transitionStyle, slideFraction)
    val miniRectForGrow = com.xvox.music.core.ui.miniplayer.XvoxMiniBounds.rect
    val growActive = transitionStyle == XvoxPlayerTransitionMotion.GrowStyle && miniRectForGrow != null &&
        slideFraction < .999f
    androidx.compose.runtime.SideEffect {
        XvoxShared.growHide = growActive && !isLandscape && slideFraction > .02f
    }
    // Layout switch: cover/title/artist of the OLD layout travel to their place in the NEW one.
    val layoutKey = settingsState.nowPlayingLayout
    val layoutTravel = remember { androidx.compose.animation.core.Animatable(1f) }
    var layoutMoving by remember { mutableStateOf(false) }
    val firstLayoutPass = remember { booleanArrayOf(true) }
    val layoutFromRects = remember(layoutKey) {
        val snapshot = HashMap(XvoxShared.rects)
        if (firstLayoutPass[0]) firstLayoutPass[0] = false
        else if (!isLandscape) { XvoxShared.hidden = setOf("cover", "title", "artist"); layoutMoving = true }
        snapshot
    }
    LaunchedEffect(layoutKey) {
        if (layoutMoving) {
            layoutTravel.snapTo(0f)
            androidx.compose.runtime.withFrameNanos { }
            androidx.compose.runtime.withFrameNanos { }
            layoutTravel.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = .74f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
            XvoxShared.hidden = emptySet()
            layoutMoving = false
        }
    }
    val cornerRadiusDp = maxOf(
        if (isSlidingDown) (28.dp * slideFraction).coerceIn(0.dp, 28.dp) else 0.dp,
        28.dp * backProgress
    )
    val sheetCorner = RoundedCornerShape(
        topStart = cornerRadiusDp,
        topEnd = cornerRadiusDp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    // Morphing animations for smooth style switching (Default <-> Compact)
    val animTopRadius by animateDpAsState(
        targetValue = if (isCompact) 20.dp else 28.dp,
        animationSpec = tween(320, easing = XvoxSmoothEasing),
        label = "animTopRadius"
    )
    val animBottomPadTop by animateDpAsState(
        targetValue = if (isCompact) 6.dp else 12.dp,
        animationSpec = tween(320, easing = XvoxSmoothEasing),
        label = "animBottomPadTop"
    )
    val animBottomPadBottom by animateDpAsState(
        targetValue = if (isCompact) 4.dp else 8.dp,
        animationSpec = tween(320, easing = XvoxSmoothEasing),
        label = "animBottomPadBottom"
    )

    // Synchronized Fullscreen Morphing Progress (0f = card, 1f = fullscreen). Fullscreen lyrics
    // deliberately uses only the locked, cross-orientation-safe style 20; there is no selector or
    // in-flight pulse capable of revealing cropped edge strips.
    val fullscreenMotion = remember { lyricsFullscreenMotion() }
    val fullscreenProgress by animateFloatAsState(
        targetValue = if (isFullscreen) 1f else 0f,
        animationSpec = tween(fullscreenMotion.durationMillis, easing = fullscreenMotion.easing),
        label = "fullscreenProgress_style_20"
    )
    // Preserve the permanent former-style-20 in-flight travel. It is zero at both endpoints,
    // so the finished compact and fullscreen surfaces retain their exact edge geometry.
    val fullscreenMotionPulse = sin(fullscreenProgress * PI).toFloat()

    // All four portrait edges participate in the lyric expansion on one shared clock. The
    // compact 6dp side frame restores the earlier, tighter artwork geometry.
    val currentPadH = lerp(6.dp, 0.dp, fullscreenProgress)
    val currentCardRadius = lerp(20.dp, 0.dp, fullscreenProgress)
    val currentPadTop = lerp(headerHeightDp + 4.dp, 0.dp, fullscreenProgress)
    val currentPadBottom = lerp(bottomHeightDp + 8.dp, 0.dp, fullscreenProgress)

    // The app root owns landscape status chrome. The player retains only the portrait fullscreen
    // lyrics exception, so a lyric-card morph cannot override the global immersive policy.
    val playerView = LocalView.current
    val playerWindow = (playerView.context as? android.app.Activity)?.window
    val playerInsetsController = remember(playerWindow, playerView) {
        playerWindow?.let { WindowCompat.getInsetsController(it, playerView) }
    }
    // XvoxAppRoot owns the app-wide landscape policy. The player only owns the exceptional
    // portrait fullscreen-lyrics state, which prevents a player disposal from re-showing the
    // status bar while the rest of the app remains in landscape.
    DisposableEffect(playerInsetsController, isLandscape) {
        playerInsetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            if (!isLandscape) {
                playerInsetsController?.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }
    LaunchedEffect(isLandscape, isFullscreen, entranceEnabled, playerInsetsController) {
        when {
            entranceEnabled && isFullscreen -> {
                playerInsetsController?.hide(WindowInsetsCompat.Type.statusBars())
            }
            !isLandscape -> {
                playerInsetsController?.show(WindowInsetsCompat.Type.statusBars())
            }
            // Landscape is intentionally a no-op here: the root policy keeps it hidden.
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Swipe down ANYWHERE closes the player and follows the finger 1:1. It sits OUTSIDE the
            // graphicsLayer below, so the pointer's coordinate space does not move with the sheet.
            // Only composed while the player is fully open. A pointerInput node that merely returns
            // early is STILL hit-tested over the whole screen (it sits outside the graphicsLayer),
            // which froze Home. Absent modifier = Home/tabs get every touch.
            .then(
                if (dragDismissReady && !isCustomizing && activeSettingsBox == null && !dismissing) {
                    Modifier.xvoxSwipeDownToClose(
                        onDrag = { dy -> screenY = (screenY + dy).coerceAtLeast(0f) },
                        onEnd = { finishDragDismissal() },
                        onCancel = { returnToRest() }
                    )
                } else Modifier
            )
            .graphicsLayer {
                translationY = if (growActive) 0f else screenY
                // Bottom anchoring makes the Scale close read as a true contraction into the
                // Mini Player rather than a centered zoom. At rest this is exactly 1f.
                transformOrigin = TransformOrigin(.5f, 1f)
                val back = (1f - .1f * backProgress) * (.93f + .07f * layoutSwap.value)
                scaleX = handoffScale * back
                scaleY = handoffScale * back
            }
            .clip(
                if (growActive) XvoxGrowShape(miniRectForGrow!!, 1f - slideFraction, with(LocalDensity.current) { chrome.miniCornerRadius.coerceIn(6f, 32f).dp.toPx() })
                else sheetCorner
            )
            .background(
                if (growActive) {
                    // No black frame: the growing surface starts transparent over the still-visible
                    // Mini Player, tinted by the cover, and settles into the normal background.
                    val e = (1f - slideFraction).coerceIn(0f, 1f)
                    androidx.compose.ui.graphics.lerp(paletteState.color, colors.background, (e * 1.4f).coerceIn(0f, 1f))
                        .copy(alpha = (e / .3f).coerceIn(0f, 1f))
                } else colors.background
            )
            .drawWithContent {
                val a = minOf(
                    if (growActive) ((1f - slideFraction - .12f) / .4f).coerceIn(0f, 1f) else 1f,
                    (.2f + .8f * layoutSwap.value).coerceIn(0f, 1f)
                )
                if (a >= .999f) drawContent() else {
                    drawContext.canvas.saveLayer(
                        androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                        androidx.compose.ui.graphics.Paint().apply { alpha = a }
                    )
                    drawContent()
                    drawContext.canvas.restore()
                }
            }
            .pointerInput(activeSettingsBox) {
                // The full-screen customizer owns its long-press/drag gestures. Outside it, the
                // player still consumes backdrop taps so no event can click through to Home.
                if (activeSettingsBox != "Customize") detectTapGestures { }
            }
    ) {
        // Everything the player draws is ONE capture source for its option sheets, so their empty
        // (scrim) area can live-blur the real player. The sheets are siblings of this Box and never
        // sit inside the layer they sample.
        Box(Modifier.fillMaxSize().then(if (npSheetBlur > 0.dp) Modifier.blur(npSheetBlur) else Modifier)) {
        // This sibling is the sole capture source for Now Playing top chrome. It contains the
        // literal player backdrop and responds to cover/palette changes, but never captures the
        // controls that consume it.
        Box(
            modifier = Modifier
                .matchParentSize()
                .xvoxBlurSource(nowPlayingSky)
        ) {
            XvoxNowPlayingBackdropLayer(
                song = song,
                paletteState = paletteState,
                style = if (settingsState.nowPlayingLayout == "ios") XvoxNowPlayingBackgroundStyles.COVER else settingsState.nowPlayingBackgroundStyle,
                coverBlur = effectiveCoverBlur,
                swipeBase = coverSwipeBase,
                swipeAdjacent = coverSwipeAdjacent,
                swipeFraction = { coverSwipeFraction },
                modifier = Modifier.fillMaxSize()
            )
        }

        CompositionLocalProvider(
            LocalXvoxNowPlayingSky provides nowPlayingSky,
            com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle provides nowPlayingGlassChrome
        ) {
        if (isCustomizing) {
            // Modal shield behind the real editable controls. The individual dotted targets and
            // header actions are drawn afterwards, while empty player/cover space cannot fall
            // through to Home or trigger an underlying player gesture.
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput("nowPlayingCustomizeModal") { detectTapGestures { } }
            )
        }

        val altLayout = settingsState.nowPlayingLayout.let { (it == "ios" || it == "material") }
        if (altLayout) {
            val isIos = settingsState.nowPlayingLayout == "ios"
            val altActions = remember(song, isLiked, isLyricsShowing) {
                XvoxAltNpActions(
                    onClose = { dismiss() },
                    onMore = { openSettingsBox("Style") },
                    onShare = { onShare?.invoke() ?: XvoxSongActions.share(context, song) },
                    onToggleLiked = { onToggleLiked?.invoke() },
                    onShuffle = { onToggleShuffle?.invoke() },
                    onRepeat = { onToggleRepeat?.invoke() },
                    onShuffleLong = { openSettingsBox("ShuffleMethod") },
                    onRepeatLong = { openSettingsBox("RepeatOptions") },
                    onQueue = { onQueue?.invoke() },
                    onOutput = { openSettingsBox("Bluetooth") },
                    onPrevious = { movePreview(-1) },
                    onNext = { movePreview(1) },
                    onTogglePlay = onTogglePlay,
                    onSeek = onSeek,
                    onOpenLyrics = { setMode(1) },
                    onCloseLyrics = { setMode(0) },
                    onLyricsSettings = { openSettingsBox("Lyrics") }
                )
            }
            val altArtwork: XvoxAltArtwork = { m, inset, pad, spacing, corner, depth ->
                XvoxNowPlayingArtworkPager(
                    queue = queue,
                    occurrenceIds = queueOccurrenceIds,
                    currentIndex = currentIndex,
                    navigationRequest = navigationRequest,
                    previewIndex = previewIndex,
                    forceCurrentIndex = holdPagerPaletteDuringShuffle,
                    onPreviewIndexChange = { index ->
                        previewGestureActive = true
                        setPreviewTarget(index)
                    },
                    onArtworkTap = { setMode(1) },
                    onSwipePalette = { base, adjacent, fraction -> applyPagerPalette(base, adjacent, fraction) },
                    onSettledPage = { settledIndex, settledSong -> commitSettledPreview(settledIndex, settledSong) },
                    modifier = m.xvoxShared("cover"),
                    contentPadding = pad,
                    pageSpacing = spacing,
                    artworkHorizontalInset = inset,
                    artworkCornerRadius = corner,
                    applyDepth = depth,
                    repeatMode = repeatMode
                )
            }
            if (isIos) {
                XvoxNowPlayingIosLayout(
                    song = song, isPlaying = isPlaying, position = position, duration = duration,
                    isLiked = isLiked, isShuffleEnabled = isShuffleEnabled, repeatMode = repeatMode,
                    playingSource = playingSource, lyrics = lyricsState.lyrics, lyricsLoading = lyricsState.loading,
                    lyricsOpen = isLyricsShowing, accent = colors.primaryAccent, tone = paletteState.color, landscape = isLandscape,
                    actions = altActions, artwork = altArtwork
                )
            } else {
                XvoxNowPlayingMaterialLayout(
                    song = song, isPlaying = isPlaying, position = position, duration = duration,
                    isLiked = isLiked, isShuffleEnabled = isShuffleEnabled, repeatMode = repeatMode,
                    playingSource = playingSource, lyrics = lyricsState.lyrics, lyricsLoading = lyricsState.loading,
                    lyricsOpen = isLyricsShowing, tone = paletteState.color, landscape = isLandscape,
                    actions = altActions, artwork = altArtwork
                )
            }
        } else if (isLandscape) {
            // The ordinary landscape cover keeps its framed, rounded top/bottom containment.
            // Fully expanded lyrics deliberately override that frame and reach the player edge.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val compactInset = 10.dp
                val compactGutter = 10.dp
                // The compact artwork frame includes an equal inset on *all four* edges. Its
                // content width plus 20dp, the 10dp centre gutter, and the control frame exactly
                // cover the screen width—so no side gets a hidden extra strip.
                val compactArtworkWidth = ((maxWidth - compactInset * 2 - compactGutter) * .60f)
                    .coerceAtLeast(0.dp)
                val compactArtworkFrameWidth = compactArtworkWidth + compactInset * 2
                val compactControlsWidth = (maxWidth - compactArtworkFrameWidth - compactGutter)
                    .coerceAtLeast(0.dp)
                val expandedArtworkFrameWidth = maxWidth.coerceAtLeast(0.dp)
                val artworkFrameWidth = lerp(compactArtworkFrameWidth, expandedArtworkFrameWidth, fullscreenProgress)
                val artworkInset = lerp(compactInset, 0.dp, fullscreenProgress)
                val artworkRadius = lerp(20.dp, 0.dp, fullscreenProgress)
                // The right card travels exactly far enough to leave the stable 10dp gutter
                // between itself and the expanding artwork; it is never faded prematurely.
                val controlsSlidePx = with(density) {
                    (maxWidth - compactArtworkWidth - compactInset * 2).coerceAtLeast(0.dp).toPx()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .width(artworkFrameWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(artworkRadius))
                        // Once the expanding surface begins covering the second pane it receives
                        // touch priority, including the fullscreen pull-down band.
                        .zIndex(if (isFullscreen) 1f else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = isLyricsShowing,
                        animationSpec = tween(280, easing = XvoxPlayerTransitionMotion.easing),
                        label = "coverLyricsFadeLandscape"
                    ) { lyricsActive ->
                        if (lyricsActive) {
                            XvoxArtworkLyrics(
                                state = lyricsState,
                                position = position,
                                onSeek = onSeek,
                                onAttach = { uri -> lyricsViewModel.attach(uri) { message -> overlays.showP(message) } },
                                onDelete = lyricsViewModel::removeCustom,
                                onClose = { setMode(0) },
                                expanded = isFullscreen,
                                showCloseButton = !isFullscreen,
                                onToggleExpand = { setMode(if (isFullscreen) 1 else 2) },
                                onOpenSettings = { openSettingsBox("Lyrics") },
                                onDismissNowPlaying = ::dismiss,
                                onSwipeDownDelta = { delta: Float ->
                                    screenY = (screenY + delta).coerceAtLeast(0f)
                                },
                                onSwipeDownEnd = {
                                    finishDragDismissal()
                                },
                                backgroundColor = paletteState.color,
                                textColor = lyricsTextColor,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(artworkInset)
                                    .graphicsLayer {
                                        translationX = with(density) {
                                            (fullscreenMotion.horizontalPulseDp * fullscreenMotionPulse).dp.toPx()
                                        }
                                        translationY = with(density) {
                                            (fullscreenMotion.verticalPulseDp * fullscreenMotionPulse).dp.toPx()
                                        }
                                        val scale = 1f - fullscreenMotion.scaleDip * fullscreenMotionPulse
                                        scaleX = scale
                                        scaleY = scale
                                        rotationZ = fullscreenMotion.tiltDegrees * fullscreenMotionPulse
                                    }
                                    .clip(RoundedCornerShape(artworkRadius))
                            )
                        } else {
                            XvoxNowPlayingArtworkPager(
                                queue = queue,
                                occurrenceIds = queueOccurrenceIds,
                                currentIndex = currentIndex,
                                navigationRequest = navigationRequest,
                                previewIndex = previewIndex,
                                forceCurrentIndex = holdPagerPaletteDuringShuffle,
                                onPreviewIndexChange = { index ->
                                    previewGestureActive = true
                                    setPreviewTarget(index)
                                },
                                onArtworkTap = { setMode(1) },
                                onSwipePalette = { base, adjacent, fraction ->
                                    applyPagerPalette(base, adjacent, fraction)
                                },
                                onSettledPage = { settledIndex, settledSong -> commitSettledPreview(settledIndex, settledSong) },
                                modifier = Modifier.fillMaxSize(),
                                // Content padding is the one shared frame for left/right/top/bottom.
                                // A zero page gap keeps incoming vertical covers inside that same
                                // geometry instead of showing a cropped strip at a screen edge.
                                contentPadding = PaddingValues(artworkInset),
                                artworkCornerRadius = artworkRadius,
                                verticalPaging = true,
                                pageSpacing = 0.dp,
                                artworkHorizontalInset = 0.dp,
                                applyDepth = true,
                                repeatMode = repeatMode
                            )
                        }
                    }
                }

                // Right: the control card stays mounted throughout the morph, preserving state
                // and giving the reverse animation the same smooth, no-cut path.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = compactInset, end = compactInset, bottom = compactInset)
                        .width(compactControlsWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            // Keep rendering until translation has physically taken the card
                            // outside the viewport; opacity changes expose a premature blank.
                            translationX = fullscreenProgress * controlsSlidePx
                        }
                ) {
                    // Right: framed, rounded Option/Control Card.
                    val landscapeScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.cardElevated.copy(alpha = colors.cardElevated.alpha * bottomPanelAlpha))
                            .verticalScroll(landscapeScroll)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top header row inside the right box (useSystemInsets = false)
                        XvoxNowPlayingHeader(
                            onClose = { if (isLyricsShowing) setMode(0) else dismiss() },
                            onShare = { onShare?.invoke() ?: XvoxSongActions.share(context, song) },
                            onMore = { openSettingsBox("Style") },
                            playingSource = playingSource,
                            useSystemInsets = false,
                            optionsGroupSide = chrome.nowPlayingOptionsGroupSide,
                            controlsAlpha = if (isCustomizing) 1f else chrome.nowPlayingControlsAlpha,
                            surfaceAlpha = controlsSurfaceAlpha,
                            customizeMode = isCustomizing,
                            onCustomizeCancel = {
                                customizeDraft = null
                                customizeAlignmentGuide = null
                                activeSettingsBox = null
                            },
                            onCustomizeReset = {
                                customizeDraft = persistedChrome.resetNowPlayingLayout().normalizedNowPlayingGrid()
                                customizeAlignmentGuide = null
                            },
                            onCustomizeOkay = {
                                // Commit the same draft currently drawing the real player. Nothing
                                // is copied from a preview: the persisted layout is exactly what
                                // the user has just moved/resized on this Now Playing surface.
                                settingsViewModel.setChromeStyle { chrome.normalizedNowPlayingGrid() }
                                customizeDraft = null
                                customizeAlignmentGuide = null
                                activeSettingsBox = null
                                overlays.showP("Layout & cover size applied")
                            },
                            modifier = Modifier
                        )

                        Spacer(Modifier.height(2.dp))

                        NowPlayingActions(
                            isLiked = isLiked,
                            isInPlaylist = isInPlaylist,
                            onTimer = { onTimer?.invoke() },
                            onQueue = { onQueue?.invoke() },
                            onInfo = { onInfo?.invoke() },
                            onToggleLiked = { onToggleLiked?.invoke() },
                            onStarPlaylist = { onStarPlaylist?.invoke() },
                            timerProgress = sleepTimerProgress,
                            crossfadeOn = settingsState.crossfade,
                            onToggleCrossfade = { settingsViewModel.setCrossfade(!settingsState.crossfade) },
                            equalizerOn = settingsState.equalizerEnabled,
                            spaceOn = settingsState.stereoWidening,
                            lyricsOn = isLyricsShowing,
                            onToggleEqualizer = { settingsViewModel.setEqualizerEnabled(!settingsState.equalizerEnabled) },
                            onToggleSpace = { settingsViewModel.setStereoWidening(!settingsState.stereoWidening) },
                            onToggleLyrics = { setMode(if (isLyricsShowing) 0 else 1) },
                            onOpenOptions = { optionName -> openSettingsBox(optionName) },
                            actionPageIndex = actionPageIndex,
                            onActionPageChange = onActionPageChange,
                            utilityPillSide = chrome.nowPlayingPillSide,
                            actionClusterSide = chrome.nowPlayingChangingActionsSide,
                            controlsAlpha = if (isCustomizing) 1f else chrome.nowPlayingControlsAlpha,
                            surfaceAlpha = controlsSurfaceAlpha,
                            utilityOffsetX = chrome.nowPlayingUtilityOffsetX,
                            utilityOffsetY = chrome.nowPlayingUtilityOffsetY,
                            actionsOffsetX = chrome.nowPlayingActionsOffsetX,
                            actionsOffsetY = chrome.nowPlayingActionsOffsetY,
                            editSession = customizeSession
                        )

                        Spacer(Modifier.height(2.dp))

                        NowPlayingEditableTarget(
                            session = customizeSession,
                            itemId = NowPlayingEditItem.Metadata,
                            modifier = Modifier.offset(
                                x = chrome.nowPlayingMetadataOffsetX.dp,
                                y = chrome.nowPlayingMetadataOffsetY.dp
                            )
                        ) {
                            Column {
                            Text(
                                text = song.title,
                                color = colors.primaryText,
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.xvoxShared("title")
                            )

                            Text(
                                text = song.artist,
                                color = colors.secondaryText,
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.xvoxShared("artist")
                            )
                            }
                        }

                        Spacer(Modifier.height(2.dp))

                        NowPlayingEditableTarget(
                            session = customizeSession,
                            itemId = NowPlayingEditItem.Progress,
                            modifier = Modifier.offset(
                                x = chrome.nowPlayingProgressOffsetX.dp,
                                y = chrome.nowPlayingProgressOffsetY.dp
                            )
                        ) {
                        XvoxNowPlayingProgress(
                            currentSongId = song.id,
                            position = position,
                            duration = duration,
                            isPlaying = isPlaying,
                            onSeek = onSeek,
                            showTime = true,
                            style = chrome.nowPlayingSeekStyle
                        )
                        }

                        Spacer(Modifier.height(2.dp))

                        XvoxNowPlayingControls(
                            isPlaying = isPlaying,
                            isShuffleEnabled = isShuffleEnabled,
                            repeatMode = repeatMode,
                            onShuffle = {
                                cancelPreview()
                                holdPagerPaletteDuringShuffle = true
                                paletteState.pin(song)
                                coverSwipeBase = song
                                coverSwipeAdjacent = null
                                coverSwipeFraction = 0f
                                onToggleShuffle?.invoke()
                            },
                            onPreviewPrevious = { movePreview(-1) },
                            onTogglePlay = onTogglePlay,
                            onPreviewNext = { movePreview(1) },
                            onCommitPreview = ::commitPreview,
                            onCancelPreview = ::cancelPreview,
                            onRepeat = { onToggleRepeat?.invoke() },
                            onShuffleLongPress = { openSettingsBox("ShuffleMethod") },
                            onRepeatLongPress = { openSettingsBox("RepeatOptions") },
                            previewIndex = previewIndex,
                            queueSize = queue.size,
                            shuffleRepeatSide = chrome.nowPlayingShuffleRepeatSide,
                            controlsAlpha = if (isCustomizing) 1f else chrome.nowPlayingControlsAlpha,
                            playAlpha = if (isCustomizing) 1f else chrome.nowPlayingPlayAlpha,
                            surfaceAlpha = controlsSurfaceAlpha,
                            shuffleRepeatOffsetX = chrome.nowPlayingShuffleRepeatOffsetX,
                            shuffleRepeatOffsetY = chrome.nowPlayingShuffleRepeatOffsetY,
                            shuffleOffsetX = chrome.nowPlayingShuffleOffsetX,
                            shuffleOffsetY = chrome.nowPlayingShuffleOffsetY,
                            repeatOffsetX = chrome.nowPlayingRepeatOffsetX,
                            repeatOffsetY = chrome.nowPlayingRepeatOffsetY,
                            previousOffsetX = chrome.nowPlayingPreviousOffsetX,
                            previousOffsetY = chrome.nowPlayingPreviousOffsetY,
                            playOffsetX = chrome.nowPlayingPlayOffsetX,
                            playOffsetY = chrome.nowPlayingPlayOffsetY,
                            nextOffsetX = chrome.nowPlayingNextOffsetX,
                            nextOffsetY = chrome.nowPlayingNextOffsetY,
                            editSession = customizeSession,
                            modifier = Modifier.fillMaxWidth()
                        )

                        NowPlayingEditableTarget(
                            session = customizeSession,
                            itemId = NowPlayingEditItem.Brand,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .offset(x = chrome.nowPlayingBrandOffsetX.dp, y = chrome.nowPlayingBrandOffsetY.dp)
                        ) {
                            Text(
                                text = "XVOX",
                                // The right card is an XVOX theme surface; this mark must not follow
                                // an artwork/adaptive accent that belongs to the player backdrop.
                                color = colors.secondaryText.copy(alpha = 0.58f),
                                fontFamily = XvoxLogoFont,
                                fontSize = 11.5.sp,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait Mode
            // Middle Container: Cover & Lyrics
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = currentPadTop,
                        bottom = currentPadBottom
                    )
                    .heightIn(min = 200.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = isLyricsShowing,
                    animationSpec = tween(400, easing = XvoxPlayerTransitionMotion.easing),
                    label = "coverLyricsFade"
                ) { lyricsActive ->
                    if (lyricsActive) {
                        XvoxArtworkLyrics(
                            state = lyricsState,
                            position = position,
                            onSeek = onSeek,
                            onAttach = { uri -> lyricsViewModel.attach(uri) { message -> overlays.showP(message) } },
                            onDelete = lyricsViewModel::removeCustom,
                            onClose = { setMode(0) },
                            expanded = isFullscreen,
                            onToggleExpand = { setMode(if (isFullscreen) 1 else 2) },
                            onOpenSettings = { openSettingsBox("Lyrics") },
                            onDismissNowPlaying = ::dismiss,
                            onSwipeDownDelta = { delta ->
                                screenY = (screenY + delta).coerceAtLeast(0f)
                            },
                            onSwipeDownEnd = {
                                finishDragDismissal()
                            },
                            backgroundColor = paletteState.color,
                            textColor = lyricsTextColor,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = currentPadH)
                                .graphicsLayer {
                                    translationX = with(density) {
                                        (fullscreenMotion.horizontalPulseDp * fullscreenMotionPulse).dp.toPx()
                                    }
                                    translationY = with(density) {
                                        (fullscreenMotion.verticalPulseDp * fullscreenMotionPulse).dp.toPx()
                                    }
                                    val scale = 1f - fullscreenMotion.scaleDip * fullscreenMotionPulse
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = fullscreenMotion.tiltDegrees * fullscreenMotionPulse
                                }
                                .clip(RoundedCornerShape(currentCardRadius))
                        )
                    } else {
                        NowPlayingEditableCover(
                            session = customizeSession,
                            modifier = Modifier.fillMaxSize()
                        ) {
                        XvoxNowPlayingArtworkPager(
                            queue = queue,
                            occurrenceIds = queueOccurrenceIds,
                            currentIndex = currentIndex,
                            navigationRequest = navigationRequest,
                            previewIndex = previewIndex,
                            forceCurrentIndex = holdPagerPaletteDuringShuffle,
                            onPreviewIndexChange = { index ->
                                    previewGestureActive = true
                                    setPreviewTarget(index)
                                },
                            onArtworkTap = { setMode(1) },
                            onSwipePalette = { base, adjacent, fraction ->
                                applyPagerPalette(base, adjacent, fraction)
                            },
                            onSettledPage = { settledIndex, settledSong -> commitSettledPreview(settledIndex, settledSong) },
                            // Same footprint and rounding as the lyrics box; neighbours never leak out.
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = currentPadH)
                                .xvoxShared("cover")
                                .clip(RoundedCornerShape(currentCardRadius)),
                            contentPadding = PaddingValues(0.dp),
                            // An opaque zero-gap deck hands the incoming cover directly to the
                            // outgoing one; the backdrop continues its own palette blend below.
                            pageSpacing = 0.dp,
                            repeatMode = repeatMode
                        )
                        }
                    }
                }
            }

            // Header: smooth slide up & fade out during fullscreen opening
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { headerHeightDp = with(density) { it.size.height.toDp() } }
                    .graphicsLayer {
                        // Travel fully beyond the top edge rather than fading before it has
                        // actually left the visible player.
                        translationY = -fullscreenProgress * (headerHeightDp + 4.dp).toPx()
                    }
            ) {
                XvoxNowPlayingHeader(
                    onClose = ::dismiss,
                    onShare = { onShare?.invoke() ?: XvoxSongActions.share(context, song) },
                    onMore = { openSettingsBox("Style") },
                    playingSource = playingSource,
                    optionsGroupSide = chrome.nowPlayingOptionsGroupSide,
                    controlsAlpha = if (isCustomizing) 1f else chrome.nowPlayingControlsAlpha,
                    surfaceAlpha = controlsSurfaceAlpha,
                    customizeMode = isCustomizing,
                    onCustomizeCancel = {
                        customizeDraft = null
                        customizeAlignmentGuide = null
                        activeSettingsBox = null
                    },
                    onCustomizeReset = {
                        customizeDraft = persistedChrome.resetNowPlayingLayout().normalizedNowPlayingGrid()
                        customizeAlignmentGuide = null
                    },
                    onCustomizeOkay = {
                        // Persist the exact in-place draft that is currently rendered below.
                        settingsViewModel.setChromeStyle { chrome.normalizedNowPlayingGrid() }
                        customizeDraft = null
                        customizeAlignmentGuide = null
                        activeSettingsBox = null
                        overlays.showP("Layout & cover size applied")
                    },
                    modifier = Modifier
                )
            }

            // Bottom controls are physically flush to the left, right, and bottom screen edges.
            // The single fullscreen clock carries this whole card beyond the bottom edge while
            // the lyrics surface expands through the newly released space; only top corners round.
            val bottomBoxShape = RoundedCornerShape(
                topStart = animTopRadius,
                topEnd = animTopRadius,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )

            // Bottom controls have their own source: the painted bottom-box layer itself. This
            // keeps the utility/play glass faithful to its panel rather than sampling wallpaper
            // or the cover behind the entire player.
            val bottomBoxSky = rememberXvoxContentSky()
            LaunchedEffect(bottomPanelAlpha, colors.cardElevated, bottomBoxShape) {
                bottomBoxSky.invalidate()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 0.dp, end = 0.dp, bottom = 0.dp)
                    .onGloballyPositioned { bottomHeightDp = with(density) { it.size.height.toDp() } }
                    .graphicsLayer {
                        // Push the entire bottom card below the screen during fullscreen, not
                        // merely past its normal controls' center line. It remains opaque until
                        // it is genuinely outside the viewport.
                        translationY = fullscreenProgress * (bottomHeightDp + 8.dp).toPx()
                    }
                    .clip(bottomBoxShape)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .xvoxBlurSource(bottomBoxSky)
                        // Live blur of the Now Playing backdrop; deliberately NO border here.
                        .xvoxLiveBlur(
                            shape = RectangleShape,
                            level = com.xvox.music.core.ui.blur.XvoxBlurLevel.Bar,
                            // Thinner tint so the live-blurred player behind the controls stays visible.
                            baseColor = colors.cardElevated.copy(alpha = colors.cardElevated.alpha * bottomPanelAlpha * .55f),
                            sky = LocalXvoxNowPlayingSky.current,
                            borderAlpha = 0f,
                            borderWidth = 0.dp
                        )
                )
                CompositionLocalProvider(LocalXvoxNowPlayingBottomSky provides bottomBoxSky) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(
                            start = 14.dp,
                            top = animBottomPadTop,
                            end = 14.dp,
                            bottom = animBottomPadBottom
                        )
                ) {
                AnimatedVisibility(
                    visible = !isCompact,
                    enter = expandVertically(tween(280, easing = XvoxSmoothEasing)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(240, easing = XvoxSmoothEasing)) + fadeOut(tween(160))
                ) {
                    Column {
                        NowPlayingActions(
                            isLiked = isLiked,
                            isInPlaylist = isInPlaylist,
                            onTimer = { onTimer?.invoke() },
                            onQueue = { onQueue?.invoke() },
                            onInfo = { onInfo?.invoke() },
                            onToggleLiked = { onToggleLiked?.invoke() },
                            onStarPlaylist = { onStarPlaylist?.invoke() },
                            timerProgress = sleepTimerProgress,
                            crossfadeOn = settingsState.crossfade,
                            onToggleCrossfade = { settingsViewModel.setCrossfade(!settingsState.crossfade) },
                            equalizerOn = settingsState.equalizerEnabled,
                            spaceOn = settingsState.stereoWidening,
                            lyricsOn = isLyricsShowing,
                            onToggleEqualizer = { settingsViewModel.setEqualizerEnabled(!settingsState.equalizerEnabled) },
                            onToggleSpace = { settingsViewModel.setStereoWidening(!settingsState.stereoWidening) },
                            onToggleLyrics = { setMode(if (isLyricsShowing) 0 else 1) },
                            onOpenOptions = { optionName -> openSettingsBox(optionName) },
                            actionPageIndex = actionPageIndex,
                            onActionPageChange = onActionPageChange,
                            utilityPillSide = chrome.nowPlayingPillSide,
                            actionClusterSide = chrome.nowPlayingChangingActionsSide,
                            controlsAlpha = if (isCustomizing) 1f else chrome.nowPlayingControlsAlpha,
                            surfaceAlpha = controlsSurfaceAlpha,
                            utilityOffsetX = chrome.nowPlayingUtilityOffsetX,
                            utilityOffsetY = chrome.nowPlayingUtilityOffsetY,
                            actionsOffsetX = chrome.nowPlayingActionsOffsetX,
                            actionsOffsetY = chrome.nowPlayingActionsOffsetY,
                            editSession = customizeSession
                        )

                        Spacer(Modifier.height(14.dp))
                    }
                }

                NowPlayingEditableTarget(
                    session = customizeSession,
                    itemId = NowPlayingEditItem.Metadata,
                    modifier = Modifier.offset(
                        x = chrome.nowPlayingMetadataOffsetX.dp,
                        y = chrome.nowPlayingMetadataOffsetY.dp
                    )
                ) {
                    Column {
                    Text(
                        text = song.title,
                        color = colors.primaryText,
                        fontSize = if (isCompact) 17.sp else 20.sp,
                        lineHeight = if (isCompact) 21.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.xvoxShared("title")
                    )

                    Text(
                        text = song.artist,
                        color = colors.secondaryText,
                        fontSize = if (isCompact) 11.sp else 13.sp,
                        lineHeight = if (isCompact) 15.sp else 17.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.xvoxShared("artist")
                    )
                    }
                }

                Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))

                NowPlayingEditableTarget(
                    session = customizeSession,
                    itemId = NowPlayingEditItem.Progress,
                    modifier = Modifier.offset(
                        x = chrome.nowPlayingProgressOffsetX.dp,
                        y = chrome.nowPlayingProgressOffsetY.dp
                    )
                ) {
                XvoxNowPlayingProgress(
                    currentSongId = song.id,
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
                    onSeek = onSeek,
                    showTime = !isCompact,
                    style = chrome.nowPlayingSeekStyle
                )
                }

                Spacer(Modifier.height(if (isCompact) 4.dp else 8.dp))

                XvoxNowPlayingControls(
                    isPlaying = isPlaying,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    onShuffle = {
                        cancelPreview()
                        holdPagerPaletteDuringShuffle = true
                        paletteState.pin(song)
                        coverSwipeBase = song
                        coverSwipeAdjacent = null
                        coverSwipeFraction = 0f
                        onToggleShuffle?.invoke()
                    },
                    onPreviewPrevious = { movePreview(-1) },
                    onTogglePlay = onTogglePlay,
                    onPreviewNext = { movePreview(1) },
                    onCommitPreview = ::commitPreview,
                    onCancelPreview = ::cancelPreview,
                    onRepeat = { onToggleRepeat?.invoke() },
                            onShuffleLongPress = { openSettingsBox("ShuffleMethod") },
                            onRepeatLongPress = { openSettingsBox("RepeatOptions") },
                    previewIndex = previewIndex,
                    queueSize = queue.size,
                    shuffleRepeatSide = chrome.nowPlayingShuffleRepeatSide,
                    controlsAlpha = if (isCustomizing) 1f else chrome.nowPlayingControlsAlpha,
                    playAlpha = if (isCustomizing) 1f else chrome.nowPlayingPlayAlpha,
                    surfaceAlpha = controlsSurfaceAlpha,
                    shuffleRepeatOffsetX = chrome.nowPlayingShuffleRepeatOffsetX,
                    shuffleRepeatOffsetY = chrome.nowPlayingShuffleRepeatOffsetY,
                    shuffleOffsetX = chrome.nowPlayingShuffleOffsetX,
                    shuffleOffsetY = chrome.nowPlayingShuffleOffsetY,
                    repeatOffsetX = chrome.nowPlayingRepeatOffsetX,
                    repeatOffsetY = chrome.nowPlayingRepeatOffsetY,
                    previousOffsetX = chrome.nowPlayingPreviousOffsetX,
                    previousOffsetY = chrome.nowPlayingPreviousOffsetY,
                    playOffsetX = chrome.nowPlayingPlayOffsetX,
                    playOffsetY = chrome.nowPlayingPlayOffsetY,
                    nextOffsetX = chrome.nowPlayingNextOffsetX,
                    nextOffsetY = chrome.nowPlayingNextOffsetY,
                    editSession = customizeSession,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(if (isCompact) 6.dp else 10.dp))

                NowPlayingEditableTarget(
                    session = customizeSession,
                    itemId = NowPlayingEditItem.Brand,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .offset(x = chrome.nowPlayingBrandOffsetX.dp, y = chrome.nowPlayingBrandOffsetY.dp)
                ) {
                    Text(
                        text = "XVOX",
                        color = colors.primaryText.copy(alpha = 0.55f),
                        fontFamily = XvoxLogoFont,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(Modifier.height(if (isCompact) 2.dp else 4.dp))
                }
                }
            }
        }

        }
        } // end of blurred player layer: sheets below are siblings, never blurred
        // Alignment guides sit over the exact live player layout, never in a separate preview.
        if (isCustomizing) {
            NowPlayingAlignmentGuideOverlay(
                guide = customizeAlignmentGuide,
                modifier = Modifier.fillMaxSize().zIndex(40f)
            )
        }

        // Shared-element ghosts (layout switch + Grow title/artist).
        run {
            val titleSp = when (layoutKey) { "ios" -> 24f; "material" -> 30f; else -> if (isCompact) 17f else 20f }
            val artistSp = when (layoutKey) { "ios" -> 21f; "material" -> 19f; else -> if (isCompact) 11f else 13f }
            val titleColor = if (layoutKey == "ios") Color.White else colors.primaryText
            val artistColor = when (layoutKey) { "ios" -> Color.White.copy(alpha = .6f); "material" -> colors.mutedText; else -> colors.secondaryText }
            val titleWeight = if (layoutKey == "material") FontWeight.Medium else FontWeight.Bold
            val coverR = when (layoutKey) { "ios" -> 0f; "material" -> 22f; else -> 20f }
            if (layoutMoving && !isLandscape) {
                XvoxSharedGhosts(
                    ghosts = listOf(
                        XvoxGhost("cover", "cover", null, Color.White, 0f, FontWeight.Normal, song, coverR, coverR),
                        XvoxGhost("title", "title", song.title, titleColor, titleSp, titleWeight),
                        XvoxGhost("artist", "artist", song.artist, artistColor, artistSp, FontWeight.Normal)
                    ),
                    from = layoutFromRects,
                    progress = { layoutTravel.value },
                    modifier = Modifier.zIndex(60f)
                )
            }
            if (growActive && !isLandscape && slideFraction > .02f) {
                val e = (1f - slideFraction).coerceIn(0f, 1f)
                XvoxSharedGhosts(
                    ghosts = listOf(
                        XvoxGhost("mini_title", "title", song.title, titleColor, titleSp, titleWeight),
                        XvoxGhost("mini_artist", "artist", song.artist, artistColor, artistSp, FontWeight.Normal)
                    ),
                    from = null,
                    progress = { e },
                    modifier = Modifier.zIndex(60f)
                )
            }
        }

        // Grow: the Mini Player's cover itself travels to Now Playing's cover slot (and back).
        if (growActive && !isLandscape && slideFraction > .02f) {
            val from = com.xvox.music.core.ui.miniplayer.XvoxMiniBounds.artRect
            val to = com.xvox.music.core.ui.miniplayer.XvoxMiniBounds.coverRect
            if (from != null && to != null) {
                val e = (1f - slideFraction).coerceIn(0f, 1f)
                fun mix(a: Float, b: Float) = a + (b - a) * e
                val targetRadius = when (settingsState.nowPlayingLayout) { "ios" -> 0.dp; "material" -> 14.dp; else -> 20.dp }
                val travelRadius = lerp((chrome.miniCornerRadius - 4f).coerceIn(7f, 28f).dp, targetRadius, e)
                val growDensity = LocalDensity.current
                Box(Modifier.fillMaxSize().zIndex(50f)) {
                    XvoxSongArtwork(
                        artwork = song.artworkUri,
                        requestSize = 256,
                        modifier = Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(mix(from.left, to.left).toInt(), mix(from.top, to.top).toInt()) }
                            .size(
                                with(growDensity) { mix(from.width, to.width).toDp() },
                                with(growDensity) { mix(from.height, to.height).toDp() }
                            )
                            .clip(RoundedCornerShape(travelRadius))
                    )
                }
            }
        }

        // Dedicated contextual Settings / Options Boxes on Long-Press of bottom buttons
        if (activeSettingsBox != null) {
            if (activeSettingsBox == "ShuffleMethod") {
                com.xvox.music.player.nowplaying.components.NowPlayingShuffleBox(
                    shuffleEnabled = isShuffleEnabled,
                    method = shuffleMethod,
                    onApplyMethod = { onApplyShuffleMethod?.invoke(it) },
                    onTurnOff = { if (isShuffleEnabled) onToggleShuffle?.invoke() },
                    onDismiss = { activeSettingsBox = null }
                )
            } else if (activeSettingsBox == "RepeatOptions") {
                com.xvox.music.player.nowplaying.components.NowPlayingRepeatBox(
                    repeatMode = repeatMode,
                    repeatTimesLeft = repeatTimesLeft,
                    durationMs = duration,
                    positionMs = position,
                    abStartMs = abLoopStartMs,
                    abEndMs = abLoopEndMs,
                    onRepeatOff = { onSetRepeatPlain?.invoke(RepeatMode.OFF) },
                    onRepeatAll = { onSetRepeatPlain?.invoke(RepeatMode.ALL) },
                    onRepeatSong = { onSetRepeatSong?.invoke(it) },
                    onAbLoop = { a, b -> onSetAbLoop?.invoke(a, b) },
                    onDismiss = { activeSettingsBox = null }
                )
            } else if (activeSettingsBox == "Style") {
                NowPlayingOptionsBox(
                    backgroundStyle = settingsState.nowPlayingBackgroundStyle,
                    coverBlur = settingsState.nowPlayingCoverBlur,
                    onApplyBackground = settingsViewModel::applyNowPlayingBackground,
                    onDismiss = { activeSettingsBox = null }
                )
            } else if (activeSettingsBox != "Customize") {
                val boxTitle = when (activeSettingsBox) {
                    "Equalizer" -> "Equalizer"
                    "3D sound" -> "3D Sound"
                    "Crossfade" -> "Playback & Crossfade"
                    "Bluetooth" -> "Audio Output"
                    "Lyrics" -> "Lyrics Settings"
                    else -> activeSettingsBox!!
                }

                XvoxBox(
                    onDismiss = {
                        // These are live option sheets: any dismiss route is just a close.
                        activeSettingsBox = null
                    },
                    title = boxTitle,
                    presentation = if (activeSettingsBox == "Equalizer") {
                        XvoxBoxPresentation.EQUALIZER
                    } else {
                        XvoxBoxPresentation.DEFAULT
                    },
                    bottomAction = when (activeSettingsBox) {
                        "Equalizer" -> {
                            {
                                EqualizerFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onReset = settingsViewModel::resetEqualizerControls,
                                    onDone = { activeSettingsBox = null }
                                )
                            }
                        }
                        "3D sound" -> {
                            {
                                XvoxTransactionalFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onReset = {
                                        optionDraft = optionDraft.copy(
                                            stereoWidening = false,
                                            surroundWidth = .78f,
                                            surroundDepth = .65f,
                                            surroundPanSpeed = 6,
                                            hrtf = .6f,
                                            balance = 0f
                                        )
                                        applyThreeDSoundDraft()
                                    },
                                    onOkay = {
                                        applyThreeDSoundDraft(optionDraft)
                                        activeSettingsBox = null
                                    }
                                )
                            }
                        }
                        "Crossfade" -> {
                            {
                                XvoxTransactionalFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onReset = {
                                        optionDraft = optionDraft.copy(
                                            crossfade = false,
                                            gapless = true,
                                            crossfadeDuration = 3,
                                            crossfadeSmart = true,
                                            crossfadeClashControl = .7f,
                                            crossfadeBeatSync = true,
                                            crossfadeCurveStyle = com.xvox.music.player.playback.CrossfadeMath.CURVE_EQUAL_POWER
                                        )
                                        applyCrossfadeDraft()
                                    },
                                    onOkay = {
                                        applyCrossfadeDraft(optionDraft)
                                        activeSettingsBox = null
                                    }
                                )
                            }
                        }
                        "Bluetooth" -> {
                            {
                                XvoxTransactionalFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onOkay = {
                                        applyBluetoothDraft(optionDraft)
                                        activeSettingsBox = null
                                    }
                                )
                            }
                        }
                        "Lyrics" -> {
                            {
                                LyricsFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onResetAll = {
                                        optionDraft = optionDraft.copy(
                                            lyrics = com.xvox.music.data.preferences.LyricsSettings()
                                        )
                                        applyLyricsDraft(optionDraft)
                                    },
                                    onResetTiming = {
                                        optionDraft = optionDraft.copy(
                                            lyrics = optionDraft.lyrics.copy(offsetMs = 0)
                                        )
                                        applyLyricsDraft(optionDraft)
                                    },
                                    onDone = {
                                        applyLyricsDraft(optionDraft)
                                        activeSettingsBox = null
                                    }
                                )
                            }
                        }
                        else -> null
                    }
                ) {
                    if (activeSettingsBox == "Equalizer") {
                        // EQ owns its vertical scroll. Wrapping it in this generic scroll body
                        // measured one vertical scroller with an unbounded height and crashed as
                        // soon as the Equalizer sheet opened.
                        EqualizerSettingsSection(
                            state = settingsState,
                            viewModel = settingsViewModel,
                            onCancel = { activeSettingsBox = null },
                            onDone = { activeSettingsBox = null },
                            showFooter = false
                        )
                    } else {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                // The universal sheet constrains only genuine screen overflow.
                                .verticalScroll(scrollState)
                                .xvoxBoxScroll(scrollState)
                        ) {
                            when (activeSettingsBox) {
                                "3D sound" -> ThreeDSoundDraftSection(
                                    state = optionDraft,
                                    onStateChange = { updated ->
                                        optionDraft = updated
                                        applyThreeDSoundDraft(updated)
                                    }
                                )
                                "Crossfade" -> PlaybackSettingsDraftSection(
                                    state = optionDraft,
                                    onStateChange = { updated ->
                                        optionDraft = updated
                                        applyCrossfadeDraft(updated)
                                    }
                                )
                                "Bluetooth" -> HeadsetSettingsDraftSection(
                                    state = optionDraft,
                                    onStateChange = { updated ->
                                        optionDraft = updated
                                        applyBluetoothDraft(updated)
                                    }
                                )
                                "Lyrics" -> LyricsSettingsDraftSection(
                                    settings = optionDraft.lyrics,
                                    onSettingsChange = { lyrics ->
                                        optionDraft = optionDraft.copy(lyrics = lyrics)
                                        applyLyricsDraft(optionDraft)
                                    },
                                    // Reset actions live in XvoxBox's fixed footer, never below the
                                    // long scrollable editor where they could disappear off-screen.
                                    showInlineActions = false
                                )
                                else -> Unit
                            }
                        }
                    }
                }
            }
          
        }
    }
}


/**
 * Whole-surface swipe-down: starts only when nobody else consumed the gesture (sliders, lists,
 * the horizontal cover pager keep priority) and the motion is clearly downward.
 */
private fun Modifier.xvoxSwipeDownToClose(
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Final)
        val slop = viewConfiguration.touchSlop
        var dragging = false
        var tx = 0f
        var ty = 0f
        while (true) {
            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Final)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                if (dragging) onEnd()
                break
            }
            val d = change.positionChange()
            if (!dragging) {
                if (change.isConsumed) break
                tx += d.x
                ty += d.y
                if (kotlin.math.abs(tx) > slop || kotlin.math.abs(ty) > slop) {
                    if (ty > slop && ty > kotlin.math.abs(tx) * 1.4f) {
                        dragging = true
                        onDrag(ty)
                        change.consume()
                    } else break
                }
            } else {
                onDrag(d.y)
                change.consume()
            }
        }
    }
}

/** Reveal clip for the "Grow" handoff: a rounded rect that opens from the Mini Player card to the full screen. */
private class XvoxGrowShape(
    private val from: androidx.compose.ui.geometry.Rect,
    private val e: Float,
    private val startRadius: Float
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val t = e.coerceIn(0f, 1f)
        fun l(a: Float, b: Float) = a + (b - a) * t
        val r = androidx.compose.ui.geometry.CornerRadius(l(startRadius, 0f))
        return androidx.compose.ui.graphics.Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                l(from.left, 0f), l(from.top, 0f), l(from.right, size.width), l(from.bottom, size.height), r
            )
        )
    }
}
