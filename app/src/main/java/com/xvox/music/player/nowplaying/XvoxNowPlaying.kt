package com.xvox.music.player.nowplaying

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.xvox.music.core.ui.chrome.normalizedNowPlayingGrid
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion
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
 * The pager updates this small state holder every drag frame. Keeping those reads in the backdrop
 * child prevents a cover swipe from invalidating the full control/lyrics tree on every pixel.
 */
@Stable
private class XvoxNowPlayingBackdropState(initialSong: Song?) {
    var artwork by mutableStateOf(initialSong?.artworkUri)
        private set
    // URI alone is not enough: two coverless songs are both null but must retain distinct wave
    // identities while their pager/background blend travels.
    var artworkKey by mutableStateOf(initialSong?.xvoxArtworkPaletteKey() ?: "xvox_fallback_initial")
        private set
    var artworkSeed by mutableIntStateOf(initialSong?.id?.hashCode() ?: 11)
        private set
    var incomingArtwork by mutableStateOf<android.net.Uri?>(null)
        private set
    var incomingArtworkKey by mutableStateOf<String?>(null)
        private set
    var incomingArtworkSeed by mutableIntStateOf(11)
        private set
    var artworkTransitionFraction by mutableFloatStateOf(0f)
        private set

    fun update(base: Song?, incoming: Song?, fraction: Float) {
        artwork = base?.artworkUri
        artworkKey = base?.xvoxArtworkPaletteKey() ?: "xvox_fallback_initial"
        artworkSeed = base?.id?.hashCode() ?: 11
        incomingArtwork = incoming?.artworkUri
        incomingArtworkKey = incoming?.xvoxArtworkPaletteKey()
        incomingArtworkSeed = incoming?.id?.hashCode() ?: 11
        artworkTransitionFraction = fraction.coerceIn(0f, 1f)
    }
}

@Composable
private fun XvoxNowPlayingBackdropLayer(
    backdropState: XvoxNowPlayingBackdropState,
    paletteState: XvoxNowPlayingPaletteState,
    style: String,
    coverBlur: Float,
    modifier: Modifier = Modifier
) {
    XvoxNowPlayingBackdrop(
        dominant = paletteState.color,
        artwork = backdropState.artwork,
        artworkKey = backdropState.artworkKey,
        artworkSeed = backdropState.artworkSeed,
        incomingArtwork = backdropState.incomingArtwork,
        incomingArtworkKey = backdropState.incomingArtworkKey,
        incomingArtworkSeed = backdropState.incomingArtworkSeed,
        artworkTransitionFraction = backdropState.artworkTransitionFraction,
        style = style,
        coverBlur = coverBlur,
        isCoverTransitionInProgress = paletteState.isCoverTransitionInProgress,
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
    val paletteState = rememberXvoxNowPlayingPalette(song, queue, currentIndex)
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
    // The backdrop follows the pager's actual outgoing/incoming covers, not merely the audible
    // song. Its state stays local to the draw layer so cover drag frames do not recompose controls.
    val backdropState = remember { XvoxNowPlayingBackdropState(song) }
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

    /** Reject stale pager emissions from the cover that playback just left. */
    fun applyPagerPalette(base: Song, adjacent: Song?, fraction: Float) {
        val baseIsAudible = base.xvoxArtworkPaletteKey() == song.xvoxArtworkPaletteKey()
        val isActivePagerTravel = fraction > .0001f
        if (!holdPagerPaletteDuringShuffle && (baseIsAudible || previewGestureActive || isActivePagerTravel)) {
            val safeFraction = fraction.coerceIn(0f, 1f)
            paletteState.blend(base, adjacent, safeFraction)
            backdropState.update(base, adjacent, safeFraction)
        }
    }

    // Lyrics replaces the pager. When it is visible, update its matching local backdrop from the
    // audible cover and let the backdrop crossfade the two images rather than retaining a stale
    // preview card behind the orb/aurora canvas.
    LaunchedEffect(song.xvoxArtworkPaletteKey(), isLyricsShowing) {
        if (isLyricsShowing) {
            backdropState.update(song, null, 0f)
            // There is no pager underneath lyrics to emit palette frames. Resolve the same cover
            // directly; XvoxArtworkLyrics animates this live colour into its orb/aurora canvas.
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
            animation.animateTo(
                target,
                XvoxPlayerTransitionMotion.nowPlayingSpec(
                    XvoxPlayerTransitionMotion.NowPlayingDuration,
                    .55f
                )
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
            if (entranceEnabled) {
                entered = true
                animateScreen(target = 0f)
            }
        }
    }

    fun dismiss() {
        if (dismissing) return
        dismissing = true
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
        animateScreen(0f)
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

    BackHandler(enabled = entranceEnabled) {
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
    val cornerRadiusDp = if (isSlidingDown) (28.dp * slideFraction).coerceIn(0.dp, 28.dp) else 0.dp
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

    // System chrome is owned by the player, rather than by an individual lyrics surface. This
    // makes the policy deterministic while the same lyric card is morphing: portrait hides only
    // for fullscreen lyrics; landscape stays status-bar-free in every Now Playing state.
    val playerView = LocalView.current
    val playerWindow = (playerView.context as? android.app.Activity)?.window
    val playerInsetsController = remember(playerWindow, playerView) {
        playerWindow?.let { WindowCompat.getInsetsController(it, playerView) }
    }
    // Global queue/info sheets live in a sibling overlay host. Mark them immersive while this
    // landscape player is mounted so opening any sheet cannot make a status bar reappear.
    DisposableEffect(overlays, isLandscape, entranceEnabled) {
        val immersiveSheets = entranceEnabled && isLandscape
        overlays.immersiveNowPlayingSheets = immersiveSheets
        onDispose {
            if (overlays.immersiveNowPlayingSheets == immersiveSheets) {
                overlays.immersiveNowPlayingSheets = false
            }
        }
    }
    DisposableEffect(playerInsetsController) {
        playerInsetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            // The app root normally presents status bars. Restore that baseline only after the
            // whole player leaves composition, never during a fullscreen reverse morph.
            playerInsetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }
    LaunchedEffect(isLandscape, isFullscreen, entranceEnabled, playerInsetsController) {
        if (entranceEnabled && (isLandscape || isFullscreen)) {
            playerInsetsController?.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            playerInsetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = screenY
                // Bottom anchoring makes the Scale close read as a true contraction into the
                // Mini Player rather than a centered zoom. At rest this is exactly 1f.
                transformOrigin = TransformOrigin(.5f, 1f)
                scaleX = handoffScale
                scaleY = handoffScale
            }
            .clip(sheetCorner)
            .background(colors.background)
            .pointerInput(activeSettingsBox) {
                // The full-screen customizer owns its long-press/drag gestures. Outside it, the
                // player still consumes backdrop taps so no event can click through to Home.
                if (activeSettingsBox != "Customize") detectTapGestures { }
            }
    ) {
        XvoxNowPlayingBackdropLayer(
            backdropState = backdropState,
            paletteState = paletteState,
            style = settingsState.nowPlayingBackgroundStyle,
            coverBlur = settingsState.nowPlayingCoverBlur,
            modifier = Modifier.fillMaxSize()
        )

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

        if (isLandscape) {
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
                                onAttach = lyricsViewModel::attach,
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
                                    if (screenY > screenHeight * 0.18f) dismiss() else returnToRest()
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
                            .background(colors.cardElevated.copy(alpha = bottomPanelAlpha))
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
                            modifier = if (!isCustomizing) Modifier.pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        screenY = (screenY + dragAmount).coerceAtLeast(0f)
                                    },
                                    onDragEnd = {
                                        if (screenY > screenHeight * 0.18f) {
                                            dismiss()
                                        } else {
                                            returnToRest()
                                        }
                                    },
                                    onDragCancel = { returnToRest() }
                                )
                            } else Modifier
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
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = song.artist,
                                color = colors.secondaryText,
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                onToggleShuffle?.invoke()
                            },
                            onPreviewPrevious = { movePreview(-1) },
                            onTogglePlay = onTogglePlay,
                            onPreviewNext = { movePreview(1) },
                            onCommitPreview = ::commitPreview,
                            onCancelPreview = ::cancelPreview,
                            onRepeat = { onToggleRepeat?.invoke() },
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
                            onAttach = lyricsViewModel::attach,
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
                                if (screenY > screenHeight * 0.18f) {
                                    dismiss()
                                } else {
                                    returnToRest()
                                }
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
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = currentPadH),
                            pageSpacing = 12.dp,
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
                    modifier = if (!isCustomizing) Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                screenY = (screenY + dragAmount).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (screenY > screenHeight * 0.18f) {
                                    dismiss()
                                } else {
                                    returnToRest()
                                }
                            },
                            onDragCancel = { returnToRest() }
                        )
                    } else Modifier
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

            Column(
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
                    .background(colors.cardElevated.copy(alpha = bottomPanelAlpha))
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
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = song.artist,
                        color = colors.secondaryText,
                        fontSize = if (isCompact) 11.sp else 13.sp,
                        lineHeight = if (isCompact) 15.sp else 17.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        onToggleShuffle?.invoke()
                    },
                    onPreviewPrevious = { movePreview(-1) },
                    onTogglePlay = onTogglePlay,
                    onPreviewNext = { movePreview(1) },
                    onCommitPreview = ::commitPreview,
                    onCancelPreview = ::cancelPreview,
                    onRepeat = { onToggleRepeat?.invoke() },
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

        // Alignment guides sit over the exact live player layout, never in a separate preview.
        if (isCustomizing) {
            NowPlayingAlignmentGuideOverlay(
                guide = customizeAlignmentGuide,
                modifier = Modifier.fillMaxSize().zIndex(40f)
            )
        }

        // Dedicated contextual Settings / Options Boxes on Long-Press of bottom buttons
        if (activeSettingsBox != null) {
            if (activeSettingsBox == "Style") {
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
                                    onOkay = { activeSettingsBox = null }
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
                                            crossfadeBeatSync = true
                                        )
                                        applyCrossfadeDraft()
                                    },
                                    onOkay = { activeSettingsBox = null }
                                )
                            }
                        }
                        "Bluetooth" -> {
                            {
                                XvoxTransactionalFooterActions(
                                    onCancel = { activeSettingsBox = null },
                                    onOkay = { activeSettingsBox = null }
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
                                    onDone = { activeSettingsBox = null }
                                )
                            }
                        }
                        else -> null
                    }
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // The universal sheet constrains only genuine screen overflow. Every
                            // editor therefore begins at its own content height, including EQ.
                            .verticalScroll(scrollState)
                            .xvoxBoxScroll(scrollState)
                    ) {
                        when (activeSettingsBox) {
                            "Equalizer" -> EqualizerSettingsSection(
                                state = settingsState,
                                viewModel = settingsViewModel,
                                onCancel = { activeSettingsBox = null },
                                onDone = { activeSettingsBox = null },
                                showFooter = false
                            )
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
                        }
                    }
                }
            }
        }
    }
}
